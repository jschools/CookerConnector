package com.schoovello.cookerconnector.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.schoovello.cookerconnector.BuildConfig;
import com.schoovello.cookerconnector.CookerConnectorApp;
import com.schoovello.cookerconnector.R;
import com.schoovello.cookerconnector.activity.SessionActivity;
import com.schoovello.cookerconnector.data.FirebaseDbInstance;
import com.schoovello.cookerconnector.datamodels.SessionDataStream;
import com.schoovello.cookerconnector.model.SessionInfo;
import com.schoovello.cookerconnector.model.TimeDataPoint;
import com.schoovello.cookerconnector.util.ObjectWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MonitorNotificationService extends Service {
	private static final String LOG_TAG = "NotificationService";

	private static final int NOTIFICATION_ID_FOREGROUND = 1;
	private static final int NOTIFICATION_ID_OTHERS = 2;
	private static final long POLLING_INTERVAL_MS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
	private static final String CHANNEL_SESSION_STATUS = "session_status";

	private HandlerThread mThread;
	private Handler mHandler;

	private FirebaseDatabase mDb;

	private boolean mForeground;
	private final MultiNotificationHelper mNotificationHelper = new MultiNotificationHelper(NOTIFICATION_ID_OTHERS);

	public static void updateNow() {
		CookerConnectorApp.getInstance().startService(getStartIntent());
	}

	@NonNull
	private static Intent getStartIntent() {
		return new Intent(CookerConnectorApp.getInstance(), MonitorNotificationService.class);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mThread = new HandlerThread(MonitorNotificationService.class.getName());
		mThread.start();
		mHandler = new Handler(mThread.getLooper());
		mForeground = false;

		mDb = FirebaseDbInstance.get();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(CHANNEL_SESSION_STATUS, "Active Sessions", NotificationManager.IMPORTANCE_LOW);
			final NotificationManager notificationManager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
			notificationManager.createNotificationChannel(channel);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mHandler.removeCallbacksAndMessages(null);
		mHandler = null;

		mThread.quit();
		mThread = null;

		mDb = null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mHandler.post(mBackgroundJobRunnable);

		return START_STICKY;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		// do not bind
		return null;
	}

	private final Runnable mBackgroundJobRunnable = new Runnable() {
		@Override
		public void run() {
			mHandler.removeCallbacks(this);
			doBackgroundJob();
			mHandler.postDelayed(this, POLLING_INTERVAL_MS);
		}
	};

	@WorkerThread
	private void doBackgroundJob() {
		if (BuildConfig.DEBUG) {
			Log.d(LOG_TAG, "Updating notifications");
		}

		// get all session IDs
		final List<String> sessionIds = getAllSessionIdsBlocking();

		// (possibly) generate a notification for each session
		final Map<String, Notification> newNotifications = new HashMap<>();
		if (sessionIds != null) {
			for (String sessionId : sessionIds) {
				final Notification notification = buildNotificationForSessionBlocking(sessionId);
				if (notification != null) {
					newNotifications.put(sessionId, notification);
				}
			}
		}

		final NotificationManagerCompat mgr = NotificationManagerCompat.from(this);

		// remove any notifications that are no longer needed
		final Set<String> existingTags = mNotificationHelper.getAllTags();
		for (String tag : existingTags) {
			if (!newNotifications.containsKey(tag)) {
				mNotificationHelper.remove(tag);
			}
		}

		// add all new notifications
		for (Map.Entry<String, Notification> entry : newNotifications.entrySet()) {
			final String tag = entry.getKey();
			final Notification notification = entry.getValue();
			mNotificationHelper.put(tag, notification);
		}

		// sync
		final Notification foregroundNotification = mNotificationHelper.sync(mgr);

		if (foregroundNotification == null) {
			if (mForeground) {
				stopForeground(true);
				stopSelf();
				mForeground = false;
			}
		} else {
			if (!mForeground) {
				// not in foreground yet
				mForeground = true;
				startForeground(NOTIFICATION_ID_FOREGROUND, foregroundNotification);
			} else {
				// already in foreground. just update the notification.
				mgr.notify(NOTIFICATION_ID_FOREGROUND, foregroundNotification);
			}
		}
	}

	@WorkerThread
	@Nullable
	private List<String> getAllSessionIdsBlocking() {
		final ObjectWrapper<List<String>> resultWrapper = new ObjectWrapper<>();

		final ConditionVariable condition = new ConditionVariable();
		condition.close();

		// get all session IDs in a list
		final DatabaseReference ref = mDb.getReference("sessions");
		ref.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				final List<String> result = new ArrayList<>();
				if (dataSnapshot != null) {
					for (DataSnapshot childSnap : dataSnapshot.getChildren()) {
						// add each child's key to the list
						result.add(childSnap.getKey());
					}
				}
				resultWrapper.object = result;

				condition.open();
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				condition.open();
			}
		});

		condition.block(TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS));

		return resultWrapper.object;
	}

	@WorkerThread
	@Nullable
	private Notification buildNotificationForSessionBlocking(@NonNull String sessionId) {
		// get the session
		final SessionInfo session = getSessionBlocking(sessionId);
		if (session == null) {
			// error. quit.
			return null;
		}

		// if not running, cancel it
		if (session.isPaused() || session.isStopped()) {
			return null;
		}

		// show a notification for this session
		return buildNotificationForSessionBlocking(sessionId, session);
	}

	@WorkerThread
	@Nullable
	private SessionInfo getSessionBlocking(@NonNull String sessionId) {
		final ConditionVariable condition = new ConditionVariable();
		condition.close();

		final ObjectWrapper<SessionInfo> resultWrapper = new ObjectWrapper<>();

		final DatabaseReference sessionRef = mDb.getReference("sessions").child(sessionId);
		sessionRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				resultWrapper.object = dataSnapshot.getValue(SessionInfo.class);
				condition.open();
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				condition.open();
			}
		});

		condition.block(TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS));

		return resultWrapper.object;
	}

	@WorkerThread
	@NonNull
	private Notification buildNotificationForSessionBlocking(@NonNull String sessionId, @NonNull SessionInfo session) {
		// get last minute of data from each stream
		final Map<String, List<TimeDataPoint>> sessionData = getSessionDataBlocking(sessionId, session);

		// summarize the data
		final StringBuilder summary = new StringBuilder();
		long lastDataPointTimeMillis = -1L;
		for (Map.Entry<String, SessionDataStream> entry : session.getDataStreams().entrySet()) {
			final String streamId = entry.getKey();
			final SessionDataStream dataStream = entry.getValue();
			final List<TimeDataPoint> streamData = sessionData.get(streamId);

			final String streamSummary = buildStreamSummary(dataStream, streamData);

			if (summary.length() > 0) {
				summary.append(" • ");
			}
			summary.append(streamSummary);

			// keep track of latest data timestamp
			final TimeDataPoint lastDataPoint = !streamData.isEmpty() ? streamData.get(streamData.size() - 1) : null;
			final long lastTimeMillis = lastDataPoint != null ? lastDataPoint.getTimeMillis() : lastDataPointTimeMillis;
			lastDataPointTimeMillis = Math.max(lastDataPointTimeMillis, lastTimeMillis);
		}

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_SESSION_STATUS);
		builder.setContentTitle(session.getTitle());
		builder.setContentText(summary);
		builder.setOngoing(true);
		builder.setSmallIcon(R.drawable.ic_router_white_24px);
		builder.setGroup("active_sessions");

		if (lastDataPointTimeMillis > 0) {
			builder.setShowWhen(true);
			builder.setWhen(lastDataPointTimeMillis);
		} else {
			builder.setShowWhen(false);
		}

		final Intent contentIntent = SessionActivity.buildIntent(sessionId);
		contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		contentIntent.setData(Uri.parse(contentIntent.toUri(Intent.URI_INTENT_SCHEME)));
		final PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(contentPendingIntent);

		final Intent refreshIntent = getStartIntent();
		final PendingIntent refreshPendingIntent = PendingIntent.getService(this, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		final NotificationCompat.Action refreshAction = new NotificationCompat.Action(0, "Refresh", refreshPendingIntent);
		builder.addAction(refreshAction);

		return builder.build();
	}

	@NonNull
	private static String buildStreamSummary(SessionDataStream dataStream, List<TimeDataPoint> data) {
		final StringBuilder builder = new StringBuilder();

		builder.append(dataStream.getTitle()).append(": ");
		final TimeDataPoint lastDataPoint = !data.isEmpty() ? data.get(data.size() - 1) : null;

		if (lastDataPoint != null) {
			final String formattedValue = String.format(Locale.getDefault(), "%1$.1f°F", lastDataPoint.getCalibratedValue());
			builder.append(formattedValue);
		} else {
			builder.append("???");
		}

		return builder.toString();
	}

	@NonNull
	private Map<String, List<TimeDataPoint>> getSessionDataBlocking(@NonNull String sessionId, @NonNull SessionInfo session) {
		final Map<String, List<TimeDataPoint>> sessionData = new HashMap<>();

		final long nowMillis = System.currentTimeMillis();
		final long startTimeMillis = nowMillis - TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
		final Map<String, SessionDataStream> dataStreams = session.getDataStreams();
		if (dataStreams != null) {
			for (String streamId : dataStreams.keySet()) {
				final List<TimeDataPoint> streamData = getStreamDataBlocking(sessionId, streamId, startTimeMillis);
				sessionData.put(streamId, streamData);
			}
		}

		return sessionData;
	}

	@NonNull
	private List<TimeDataPoint> getStreamDataBlocking(@NonNull String sessionId, @NonNull String dataStreamId, long startTimeMillis) {
		final List<TimeDataPoint> data = new ArrayList<>();

		final Query query = mDb.getReference("sessionData")
				.child(sessionId)
				.child(dataStreamId)
				.orderByChild("timeMillis")
				.startAt(startTimeMillis);

		final CountDownLatch latch = new CountDownLatch(2);

		final ValueEventListener listener = new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				if (dataSnapshot != null) {
					data.clear();

					Iterable<DataSnapshot> children = dataSnapshot.getChildren();
					for (DataSnapshot childSnap : children) {
						TimeDataPoint dataPoint = childSnap.getValue(TimeDataPoint.class);
						data.add(dataPoint);
					}
				}

				if (BuildConfig.DEBUG) {
					Log.d(LOG_TAG, "Received " + data.size() + " data points");
				}
				latch.countDown();
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				latch.countDown();
			}
		};
		query.addValueEventListener(listener);

		try {
			latch.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// interrupted. don't care.
		}

		query.removeEventListener(listener);

		return data;
	}

}
