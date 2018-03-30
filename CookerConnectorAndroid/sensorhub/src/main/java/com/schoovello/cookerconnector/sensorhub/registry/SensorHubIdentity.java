package com.schoovello.cookerconnector.sensorhub.registry;

import android.content.Context;
import android.content.SharedPreferences;

import com.schoovello.cookerconnector.sensorhub.SensorHubApplication;
import com.schoovello.cookerconnector.sensorhub.repository.FirebaseDbInstance;

public class SensorHubIdentity {

	private static final String PREFS_IDENTITY = "identity";
	private static final String KEY_SENSOR_HUB_ID = "sensor_hub_id";

	private static SensorHubIdentity sInstance;

	public static synchronized SensorHubIdentity getInstance() {
		if (sInstance == null) {
			sInstance = new SensorHubIdentity();
		}
		return sInstance;
	}

	private String mId;

	private SensorHubIdentity() {
		Context context = SensorHubApplication.getInstance();
		SharedPreferences prefs = context.getSharedPreferences(PREFS_IDENTITY, Context.MODE_PRIVATE);

		String id = prefs.getString(KEY_SENSOR_HUB_ID, null);
		if (id == null || id.isEmpty()) {
			id = FirebaseDbInstance.get().getReference().push().getKey();
			prefs.edit().putString(KEY_SENSOR_HUB_ID, id).apply();
		}

		mId = id;
	}

	public String getId() {
		return mId;
	}
}
