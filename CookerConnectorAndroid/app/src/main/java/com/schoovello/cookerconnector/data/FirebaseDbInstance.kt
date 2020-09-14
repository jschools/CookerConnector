package com.schoovello.cookerconnector.data;

import com.google.firebase.database.FirebaseDatabase;

object FirebaseDbInstance {

	val instance: FirebaseDatabase by lazy {
		FirebaseDatabase.getInstance().apply {
			setPersistenceEnabled(true)
			setPersistenceCacheSizeBytes(1024 * 1024 * 64) // 64MB
		}
	}
}
