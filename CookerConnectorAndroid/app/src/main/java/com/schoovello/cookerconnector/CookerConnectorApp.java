package com.schoovello.cookerconnector;

import android.app.Application;

import com.schoovello.cookerconnector.service.MonitorNotificationService;

public class CookerConnectorApp extends Application {

	private static CookerConnectorApp sInstance;

	public static CookerConnectorApp getInstance() {
		return sInstance;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		sInstance = this;

		MonitorNotificationService.updateNow();
	}
}
