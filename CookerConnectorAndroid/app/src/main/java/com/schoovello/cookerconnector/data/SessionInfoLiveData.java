package com.schoovello.cookerconnector.data;

import androidx.lifecycle.LiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.schoovello.cookerconnector.model.SessionInfo;

public class SessionInfoLiveData extends LiveData<SessionInfo> implements ValueEventListener {

	private final String mSessionId;

	private DatabaseReference mReference;

	public SessionInfoLiveData(String sessionId) {
		mSessionId = sessionId;
	}

	@Override
	protected void onActive() {
		mReference = FirebaseDbInstance.INSTANCE.getInstance()
				.getReference("sessions")
				.child(mSessionId);
		mReference.addValueEventListener(this);
	}

	@Override
	protected void onInactive() {
		mReference.removeEventListener(this);
	}

	@Override
	public void onDataChange(DataSnapshot dataSnapshot) {
		if (dataSnapshot == null) {
			postValue(null);
		} else {
			SessionInfo value = dataSnapshot.getValue(SessionInfo.class);
			postValue(value);
		}
	}

	@Override
	public void onCancelled(DatabaseError databaseError) {

	}
}
