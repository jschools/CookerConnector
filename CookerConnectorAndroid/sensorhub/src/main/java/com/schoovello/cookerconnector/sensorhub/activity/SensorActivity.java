package com.schoovello.cookerconnector.sensorhub.activity;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.schoovello.cookerconnector.sensorhub.service.SensorHubService;

public class SensorActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SensorHubService.startSelf();
	}

}
