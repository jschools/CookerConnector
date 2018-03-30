package com.schoovello.cookerconnector.sensorhub.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.schoovello.cookerconnector.sensorhub.service.SensorHubService;

public class SensorActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SensorHubService.startSelf();
	}

}
