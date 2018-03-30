package com.schoovello.cookerconnector.data;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.schoovello.cookerconnector.datamodels.AlarmSpec;

public class AlarmSpecLiveData extends LiveData<AlarmSpec> implements ValueEventListener {

	private final String mSessionId;
	private final String mDataStreamId;
	private final String mAlarmId;

	private DatabaseReference mReference;

	public AlarmSpecLiveData(@NonNull String sessionId, @NonNull String dataStreamId, @NonNull String alarmId) {
		mSessionId = sessionId;
		mDataStreamId = dataStreamId;
		mAlarmId = alarmId;
	}

	@Override
	protected void onActive() {
		mReference = FirebaseDbInstance.get()
				.getReference("sessions").child(mSessionId)
				.child("dataStreams").child(mDataStreamId)
				.child("alarms").child(mAlarmId);

		mReference.addValueEventListener(this);
	}

	@Override
	protected void onInactive() {
		mReference.removeEventListener(this);
	}

	@Override
	public void onDataChange(DataSnapshot dataSnapshot) {
		AlarmSpec result = dataSnapshot.getValue(AlarmSpec.class);
		setValue(result);
	}

	@Override
	public void onCancelled(DatabaseError databaseError) {
		// don't care
	}
}
