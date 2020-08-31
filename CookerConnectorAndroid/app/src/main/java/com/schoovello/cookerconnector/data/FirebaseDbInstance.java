package com.schoovello.cookerconnector.data;

import com.google.firebase.database.FirebaseDatabase;

public final class FirebaseDbInstance {

	private static FirebaseDatabase sInstance;

	public static synchronized FirebaseDatabase get() {
		if (sInstance == null) {
			sInstance = FirebaseDatabase.getInstance();
			sInstance.setPersistenceEnabled(false);
		}
		return sInstance;
	}


	private FirebaseDbInstance() {
		// do not instantiate
	}

}
