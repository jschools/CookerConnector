package com.schoovello.cookerconnector.sensorhub.service;

import android.arch.lifecycle.LiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.schoovello.cookerconnector.datamodels.DataStreamConfig;
import com.schoovello.cookerconnector.sensorhub.repository.FirebaseDbInstance;

import java.util.Map;

public class ConfigurationLiveData extends LiveData<Map<String, DataStreamConfig>> {

	private final String mDeviceId;

	private DatabaseReference mReference;

	public ConfigurationLiveData(String deviceId) {
		mDeviceId = deviceId;
	}

	@Override
	protected void onActive() {
		mReference = FirebaseDbInstance.get()
				.getReference("sensorHubs")
				.child(mDeviceId)
				.child("activeDataStreams");

		mReference.addValueEventListener(mListener);
	}

	@Override
	protected void onInactive() {
		if (mReference != null) {
			mReference.removeEventListener(mListener);
			mReference = null;
		}
	}

	private GenericTypeIndicator<Map<String, DataStreamConfig>> mFormat = new GenericTypeIndicator<Map<String, DataStreamConfig>>() {};

	private ValueEventListener mListener = new ValueEventListener() {
		@Override
		public void onDataChange(DataSnapshot dataSnapshot) {
			Map<String, DataStreamConfig> value = dataSnapshot.getValue(mFormat);
			postValue(value);
		}

		@Override
		public void onCancelled(DatabaseError databaseError) {

		}
	};
}
