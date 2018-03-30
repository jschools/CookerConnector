package com.schoovello.cookerconnector.sensorhub.repository;

import com.google.firebase.database.DatabaseReference;
import com.schoovello.cookerconnector.datamodels.DataModel;
import com.schoovello.cookerconnector.datamodels.DataStreamConfig;

public class FirebaseDataStreamOutput implements DataStreamOutput {

	@Override
	public void onSensorEvent(DataStreamConfig config, DataModel data) {
		getDataStreamReference(config).push().setValue(data);
	}

	private static DatabaseReference getDataStreamReference(DataStreamConfig config) {
		return FirebaseDbInstance.get()
				.getReference("sessionData")
				.child(config.getSessionId())
				.child(config.getDataStreamId());
	}
}
