package com.schoovello.cookerconnector.sensorhub.registry;

import androidx.lifecycle.LiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.schoovello.cookerconnector.datamodels.SensorHubRegistryModel;
import com.schoovello.cookerconnector.sensorhub.repository.FirebaseDbInstance;

public class RegistryLiveData extends LiveData<SensorHubRegistryModel> implements ValueEventListener {

	private final SensorHubIdentity mIdentity;

	private DatabaseReference mReference;

	public RegistryLiveData(SensorHubIdentity identity) {
		mIdentity = identity;
	}

	@Override
	protected void onActive() {
		mReference = FirebaseDbInstance.get()
				.getReference("sensorHubs")
				.child(mIdentity.getId())
				.child("registry");

		mReference.addValueEventListener(this);
	}

	@Override
	protected void onInactive() {
		mReference.removeEventListener(this);
	}

	@Override
	public void onDataChange(DataSnapshot dataSnapshot) {
		SensorHubRegistryModel model = dataSnapshot.getValue(SensorHubRegistryModel.class);
		postValue(model);
	}

	@Override
	public void onCancelled(DatabaseError databaseError) {
		// don't care
	}
}
