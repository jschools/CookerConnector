package com.schoovello.cookerconnector.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.schoovello.cookerconnector.CookerConnectorApp;
import com.schoovello.cookerconnector.R;
import com.schoovello.cookerconnector.data.FirebaseDbInstance;
import com.schoovello.cookerconnector.datamodels.DataStreamConfig;
import com.schoovello.cookerconnector.datamodels.SessionDataStream;
import com.schoovello.cookerconnector.model.SessionInfo;
import com.schoovello.cookerconnector.util.CompletionBarrier;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NewSessionFragment extends Fragment implements OnClickListener {

	private static final String KEY_SESSION_NAME = "sessionName";
	private static final String KEY_KNOWN_SENSOR_HUB_IDS = "knownSensorHubIds";
	private static final String KEY_STARTED = "started";
	private static final String KEY_DONE = "done";
	private static final String KEY_PENDING_SESSION_ID = "pendingSessionId";

	private String mSessionName;

	private DatabaseReference mSensorHubsRef;

	private Set<String> mKnownSensorHubIds;

	private boolean mStarted;
	private boolean mDone;
	private String mPendingSessionId;

	private Views mViews;

	private static final class Views {
		final EditText sessionName;
		final ViewGroup sensorHubsContainer;
		final View startButton;
		final View progress;

		Views(View v) {
			sessionName = v.findViewById(R.id.sessionNameInput);
			sensorHubsContainer = v.findViewById(R.id.sensorHubsContainer);
			startButton = v.findViewById(R.id.startButton);
			progress = v.findViewById(R.id.progress);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(KEY_SESSION_NAME, mSessionName);
		outState.putStringArray(KEY_KNOWN_SENSOR_HUB_IDS, mKnownSensorHubIds.toArray(new String[0]));
		outState.putBoolean(KEY_STARTED, mStarted);
		outState.putBoolean(KEY_DONE, mDone);
		outState.putString(KEY_PENDING_SESSION_ID, mPendingSessionId);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mKnownSensorHubIds = new HashSet<>();

		if (savedInstanceState != null) {
			mSessionName = savedInstanceState.getString(KEY_SESSION_NAME);
			mKnownSensorHubIds.addAll(Arrays.asList(savedInstanceState.getStringArray(KEY_KNOWN_SENSOR_HUB_IDS)));
			mStarted = savedInstanceState.getBoolean(KEY_STARTED);
			mDone = savedInstanceState.getBoolean(KEY_DONE);
			mPendingSessionId = savedInstanceState.getString(KEY_PENDING_SESSION_ID);
		} else {
			final DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
			final String formattedDate = dateFormat.format(new Date());
			mSessionName = getString(R.string.default_session_name_date, formattedDate);
		}

		registerBroadcastListener();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		unregisterBroadcastListener();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_new_session, container, false);
		mViews = new Views(v);

		mViews.startButton.setOnClickListener(this);

		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		mViews = null;
	}

	@Override
	public void onStart() {
		super.onStart();

		mViews.sessionName.setText(mSessionName);
		mViews.sessionName.addTextChangedListener(mSessionNameListener);

		mSensorHubsRef = FirebaseDbInstance.INSTANCE.getInstance().getReference("sensorHubs");
		mSensorHubsRef.addChildEventListener(mSensorHubsListener);

		updateViews();
	}

	@Override
	public void onStop() {
		super.onStop();

		mSensorHubsRef.removeEventListener(mSensorHubsListener);
	}

	private TextWatcher mSessionNameListener = new TextWatcher() {
		@Override
		public void afterTextChanged(Editable s) {
			mSessionName = s.toString();
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			// don't care
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			// don't care
		}
	};

	private ChildEventListener mSensorHubsListener = new ChildEventListener() {
		@Override
		public void onChildAdded(DataSnapshot dataSnapshot, String s) {
			final String sensorHubId = dataSnapshot.getKey();
			mKnownSensorHubIds.add(sensorHubId);
			addSensorHubFragment(sensorHubId);
		}

		@Override
		public void onChildRemoved(DataSnapshot dataSnapshot) {
			final String sensorHubId = dataSnapshot.getKey();
			mKnownSensorHubIds.remove(sensorHubId);
			removeSensorHubFragment(sensorHubId);
		}

		@Override
		public void onChildChanged(DataSnapshot dataSnapshot, String s) {

		}

		@Override
		public void onChildMoved(DataSnapshot dataSnapshot, String s) {

		}

		@Override
		public void onCancelled(DatabaseError databaseError) {

		}
	};

	private void addSensorHubFragment(@NonNull String sensorHubId) {
		final FragmentManager fm = getChildFragmentManager();
		final String fragTag = getSensorHubFragmentTag(sensorHubId);
		final Fragment existing = fm.findFragmentByTag(fragTag);
		if (existing == null) {
			final SensorHubConfigFragment fragment = SensorHubConfigFragment.newInstance(sensorHubId);
			final FragmentTransaction ft = fm.beginTransaction();
			ft.add(R.id.sensorHubsContainer, fragment, fragTag);
			ft.commitAllowingStateLoss();
		}
	}

	private void removeSensorHubFragment(@NonNull String sensorHubId) {
		final FragmentManager fm = getChildFragmentManager();
		final String fragTag = getSensorHubFragmentTag(sensorHubId);
		final Fragment existing = fm.findFragmentByTag(fragTag);
		if (existing != null) {
			final FragmentTransaction ft = fm.beginTransaction();
			ft.remove(existing);
			ft.commitAllowingStateLoss();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.startButton:
				saveConfigAndCreateSession();
				break;
			default:
				break;
		}
	}

	private void updateViews() {
		if (mViews == null) {
			return;
		}

		if (mDone) {
			getActivity().finish();
			return;
		}

		mViews.progress.setVisibility(mStarted ? View.VISIBLE : View.GONE);
		mViews.startButton.setEnabled(!mStarted);
	}

	private void saveConfigAndCreateSession() {
		final FirebaseDatabase db = FirebaseDbInstance.INSTANCE.getInstance();
		final DatabaseReference sessionRef = db.getReference("sessions").push();
		final String sessionId = sessionRef.getKey();

		final Map<String, SessionDataStream> sessionStreams = new HashMap<>();
		final CompletionBarrier completionBarrier = new CompletionBarrier();

		for (String sensorHubId : mKnownSensorHubIds) {
			final String fragTag = getSensorHubFragmentTag(sensorHubId);
			final FragmentManager fm = getChildFragmentManager();
			final Fragment fragment = fm.findFragmentByTag(fragTag);
			if (fragment instanceof SensorHubConfigFragment) {
				final SensorHubConfigFragment configFragment = (SensorHubConfigFragment) fragment;
				final List<DataStreamSpec> specs = configFragment.getEnabledStreamSpecs();

				// add the stream configs from each sensor hub
				for (DataStreamSpec spec : specs) {
					final DatabaseReference streamRef = db
							.getReference("sensorHubs")
							.child(sensorHubId)
							.child("activeDataStreams")
							.push();
					final String streamId = streamRef.getKey();

					final DataStreamConfig streamConfig = spec.dataStreamConfig;
					streamConfig.setSessionId(sessionId);
					streamConfig.setDataStreamId(streamId);

					// set the data stream
					streamRef.setValue(streamConfig, completionBarrier.startNewOperation());

					// create the session's data stream and save it for later
					final SessionDataStream sessionDataStream = new SessionDataStream();
					final String dataStreamPath = String.format(Locale.US, "%1$s/activeDataStreams/%2$s", sensorHubId, streamId);
					sessionDataStream.setActiveDataStream(dataStreamPath);
					sessionDataStream.setTitle(spec.streamName);
					sessionDataStream.setDescription("");
					sessionStreams.put(streamId, sessionDataStream);
				}
			}
		}

		// create the session, but wait to save it until all streams have been created
		final SessionInfo sessionInfo = new SessionInfo();
		sessionInfo.setStopped(false);
		sessionInfo.setPaused(true);
		sessionInfo.setStartTimeMillis(System.currentTimeMillis());
		sessionInfo.setTitle(mSessionName);
		sessionInfo.setComments("");
		sessionInfo.setDataStreams(sessionStreams);

		completionBarrier.startWaiting(new CompletionBarrier.CompletionBarrierListener() {
			@Override
			public void onOperationFailed(@NonNull DatabaseError databaseError, DatabaseReference databaseReference) {
				mDone = true;
				updateViews();
			}

			@Override
			public void onAllOperationsCompleted() {
				sessionRef.setValue(sessionInfo, new DatabaseReference.CompletionListener() {
					@Override
					public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
						mDone = true;
						updateViews();
					}
				});
			}
		});

		mDone = false;
		mStarted = true;
		mPendingSessionId = sessionId;
		updateViews();
	}

	private static final String ACTION_SESSION_CREATED = NewSessionFragment.class.getName() + ".ACTION_SESSION_CREATED";
	private static final IntentFilter COMPLETION_FILTER = new IntentFilter(ACTION_SESSION_CREATED);
	private static final String EXTRA_SESSION_ID = "SESSION_ID";

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String sessionId = intent.getStringExtra(EXTRA_SESSION_ID);
			if (sessionId != null && sessionId.equals(mPendingSessionId)) {
				mDone = true;
			}
			updateViews();
		}
	};

	private void registerBroadcastListener() {
		LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(CookerConnectorApp.getInstance());
		mgr.registerReceiver(mReceiver, COMPLETION_FILTER);
	}

	private void unregisterBroadcastListener() {
		LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(CookerConnectorApp.getInstance());
		mgr.unregisterReceiver(mReceiver);
	}

	private static String getSensorHubFragmentTag(@NonNull String sensorHubId) {
		return "SensorHubConfigFragment." + sensorHubId;
	}

	public static class DataStreamSpec implements Parcelable {

		public int channel;
		public String streamName;
		public DataStreamConfig dataStreamConfig;

		public DataStreamSpec(int channel, String streamName, DataStreamConfig dataStreamConfig) {
			this.channel = channel;
			this.streamName = streamName;
			this.dataStreamConfig = dataStreamConfig;
		}

		protected DataStreamSpec(Parcel in) {
			channel = in.readInt();
			streamName = in.readString();
			dataStreamConfig = in.readParcelable(DataStreamConfig.class.getClassLoader());
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(channel);
			dest.writeString(streamName);
			dest.writeParcelable(dataStreamConfig, flags);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		public static final Creator<DataStreamSpec> CREATOR = new Creator<DataStreamSpec>() {
			@Override
			public DataStreamSpec createFromParcel(Parcel in) {
				return new DataStreamSpec(in);
			}

			@Override
			public DataStreamSpec[] newArray(int size) {
				return new DataStreamSpec[size];
			}
		};
	}

}
