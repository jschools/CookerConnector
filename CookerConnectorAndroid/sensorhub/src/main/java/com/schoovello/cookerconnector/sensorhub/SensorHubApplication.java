package com.schoovello.cookerconnector.sensorhub;

import android.app.Application;

public class SensorHubApplication extends Application {

	private static SensorHubApplication sInstance;

	public static SensorHubApplication getInstance() {
		return sInstance;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		sInstance = this;
	}
}
