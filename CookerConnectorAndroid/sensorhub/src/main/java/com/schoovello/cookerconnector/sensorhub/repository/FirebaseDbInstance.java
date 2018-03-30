package com.schoovello.cookerconnector.sensorhub.repository;

import com.google.firebase.database.FirebaseDatabase;

public final class FirebaseDbInstance {

	private static FirebaseDatabase sInstance;

	public static synchronized FirebaseDatabase get() {
		if (sInstance == null) {
			sInstance = FirebaseDatabase.getInstance();
			sInstance.setPersistenceCacheSizeBytes(90 * 1024 * 1024);
			sInstance.setPersistenceEnabled(true);
		}
		return sInstance;
	}


	private FirebaseDbInstance() {
		// do not instantiate
	}

}
