package com.schoovello.cookerconnector.sensorhub.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.schoovello.cookerconnector.datamodels.DataStreamConfig;
import com.schoovello.cookerconnector.sensorhub.R;
import com.schoovello.cookerconnector.sensorhub.SensorHubApplication;
import com.schoovello.cookerconnector.sensorhub.registry.SensorHubIdentity;
import com.schoovello.cookerconnector.sensorhub.registry.SensorHubRegistryInitializer;
import com.schoovello.cookerconnector.sensorhub.repository.AlarmDetectorOutput;
import com.schoovello.cookerconnector.sensorhub.repository.DataStreamOutput;
import com.schoovello.cookerconnector.sensorhub.repository.FirebaseDataStreamOutput;
import com.schoovello.cookerconnector.sensorhub.repository.LoggingDataStreamOutput;
import com.schoovello.cookerconnector.sensorhub.repository.SyndicatorDataStreamOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorHubService extends Service {

	private static final String NOTIFICATION_CHANNEL = "SENSOR_HUB_SERVICE";

	private boolean mDestroyed;

	private boolean mStarted;
	private SensorHubIdentity mIdentity;
	private SensorHubRegistryInitializer mRegistryInitializer;
	private ConfigurationLiveData mConfigurationLiveData;

	private Map<String, DataStreamGenerator> mGenerators;
	private AlarmDetectorOutput mAlarmDetector;
	private DataStreamOutput mOutput;

	private ServoController mServoController;

	private Handler mMainHandler;

	public static void startSelf() {
		SensorHubApplication.getInstance().startService(getStartIntent());
	}

	public static Intent getStartIntent() {
		//noinspection UnnecessaryLocalVariable
		Intent intent = new Intent(SensorHubApplication.getInstance(), SensorHubService.class);
		return intent;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mMainHandler = new Handler(Looper.getMainLooper());

		mIdentity = SensorHubIdentity.getInstance();

		mRegistryInitializer = new SensorHubRegistryInitializer();
		mRegistryInitializer.onCreate();

		mConfigurationLiveData = new ConfigurationLiveData(mIdentity.getId());

		mGenerators = new HashMap<>();

		mAlarmDetector = new AlarmDetectorOutput();
		mOutput = new SyndicatorDataStreamOutput(
				new FirebaseDataStreamOutput(),
				mAlarmDetector,
				new LoggingDataStreamOutput()
		);

		mConfigurationLiveData.observeForever(mConfigurationObserver);

		mDestroyed = false;

		mServoController = new ServoController();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mDestroyed = true;

		mRegistryInitializer.onDestroy();

		mConfigurationLiveData.removeObserver(mConfigurationObserver);

		mGenerators.clear();

		mMainHandler.removeCallbacksAndMessages(null);

		mServoController.stop();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!mStarted) {
			startForegroundSelf();

			mServoController.start();
		}

		mStarted = true;

		return START_STICKY;
	}

	private void startForegroundSelf() {
		Intent notificationIntent = new Intent();
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, 0);

		Notification.Builder builder =
				new Notification.Builder(this)
						.setContentTitle("SensorHub Service")
						.setContentText("The SensorHub Service is running")
						.setSmallIcon(R.drawable.ic_settings_input_antenna_black_24dp)
						.setContentIntent(pendingIntent)
						.setTicker("The SensorHub Service is running");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, "SensorHub Service", NotificationManager.IMPORTANCE_DEFAULT);
			notificationMgr.createNotificationChannel(channel);
			builder.setChannelId(NOTIFICATION_CHANNEL);
		}

		startForeground(1, builder.build());
	}

	private synchronized void onConfigsUpdated(@NonNull Map<String, DataStreamConfig> configs) {
		// remove deleted generators
		final List<String> generatorsToRemove = new ArrayList<>(mGenerators.size());
		for (String existingKey : mGenerators.keySet()) {
			if (!configs.containsKey(existingKey)) {
				generatorsToRemove.add(existingKey);
			}
		}
		for (String generatorId : generatorsToRemove) {
			removeGenerator(generatorId);
		}

		// install or update the generator
		for (Map.Entry<String, DataStreamConfig> entry : configs.entrySet()) {
			installOrUpdateGenerator(entry.getKey(), entry.getValue());
		}
	}

	private synchronized void installOrUpdateGenerator(String generatorId, DataStreamConfig config) {
		DataStreamGenerator existingGenerator = mGenerators.get(generatorId);

		// if one exists but has an incompatible configuration, then remove it
		if (existingGenerator != null && !existingGenerator.tryUpdateConfig(config)) {
			removeGenerator(generatorId);
			existingGenerator = null;
		}

		// install if necessary
		if (existingGenerator == null) {
			existingGenerator = new DataStreamGenerator(config, mOutput);
			mGenerators.put(generatorId, existingGenerator);
		}

		// start/stop as necessary
		if (existingGenerator.isRunning() && config.isPaused()) {
			existingGenerator.stop();
			mAlarmDetector.onDataStreamDisabled(config);
		} else if (!existingGenerator.isRunning() && !config.isPaused()) {
			existingGenerator.start();
			mAlarmDetector.onDataStreamEnabled(config);
		}
	}

	private synchronized void removeGenerator(String generatorId) {
		DataStreamGenerator existingGenerator = mGenerators.get(generatorId);
		if (existingGenerator != null) {
			if (existingGenerator.isRunning()) {
				existingGenerator.stop();
			}
			mGenerators.remove(generatorId);
		}
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private Observer<Map<String, DataStreamConfig>> mConfigurationObserver = new Observer<Map<String, DataStreamConfig>>() {
		@Override
		public void onChanged(@Nullable Map<String, DataStreamConfig> dataStreamModels) {
			if (dataStreamModels == null) {
				dataStreamModels = new HashMap<>(0);
			}
			onConfigsUpdated(dataStreamModels);
		}
	};
	
}
