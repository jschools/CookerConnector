package com.schoovello.cookerconnector.data;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.schoovello.cookerconnector.datamodels.SensorHubRegistryModel;

public class SensorHubRegistryLiveData extends LiveData<SensorHubRegistryModel> implements ValueEventListener {

	private final String mSensorHubId;

	private FirebaseDatabase mDatabase;
	private DatabaseReference mReference;

	public SensorHubRegistryLiveData(@NonNull FirebaseDatabase firebaseDatabase, @NonNull String sensorHubId) {
		mDatabase = firebaseDatabase;
		mSensorHubId = sensorHubId;
	}

	@Override
	protected void onActive() {
		mReference = mDatabase
				.getReference("sensorHubs")
				.child(mSensorHubId)
				.child("registry");

		mReference.addValueEventListener(this);
	}

	@Override
	protected void onInactive() {
		mReference.removeEventListener(this);
	}

	@Override
	public void onDataChange(DataSnapshot dataSnapshot) {
		if (dataSnapshot != null) {
			SensorHubRegistryModel model = dataSnapshot.getValue(SensorHubRegistryModel.class);
			postValue(model);
		}
	}

	@Override
	public void onCancelled(DatabaseError databaseError) {
		// don't care
	}
}
