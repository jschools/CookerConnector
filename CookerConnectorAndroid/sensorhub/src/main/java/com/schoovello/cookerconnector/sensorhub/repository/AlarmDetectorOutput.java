package com.schoovello.cookerconnector.sensorhub.repository;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.schoovello.cookerconnector.datamodels.AlarmSpec;
import com.schoovello.cookerconnector.datamodels.DataModel;
import com.schoovello.cookerconnector.datamodels.DataStreamConfig;
import com.schoovello.cookerconnector.datamodels.SessionDataStream;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class AlarmDetectorOutput implements DataStreamOutput {
	private static final String LOG_TAG = "AlarmDetector";

	@NonNull
	private final Map<String, Pair<DatabaseReference, SessionDataStreamListener>> mRefs = new ArrayMap<>();

	@NonNull
	private final Map<String, SessionDataStream> mDataStreams = new ArrayMap<>();

	public void onDataStreamEnabled(@NonNull DataStreamConfig config) {
		final DatabaseReference ref = FirebaseDbInstance.get()
				.getReference("sessions").child(config.getSessionId())
				.child("dataStreams").child(config.getDataStreamId());
		final SessionDataStreamListener listener = new SessionDataStreamListener(config);

		mRefs.put(getDataStreamKey(config), new Pair<>(ref, listener));

		ref.addValueEventListener(listener);
	}

	public void onDataStreamDisabled(@NonNull DataStreamConfig config) {
		final String key = getDataStreamKey(config);
		final Pair<DatabaseReference, SessionDataStreamListener> pair = mRefs.remove(key);
		if (pair == null) {
			return;
		}

		final DatabaseReference ref = pair.first;
		final SessionDataStreamListener listener = pair.second;

		ref.removeEventListener(listener);
	}

	@Override
	public void onSensorEvent(DataStreamConfig config, DataModel data) {
		final Map<String, AlarmSpec> alarmSpecs = getAlarmSpecsForDataStream(config);
		if (alarmSpecs == null) {
			Log.d(LOG_TAG, "No alarms for " + config.getSessionId() + '/' + config.getDataStreamId());
			return;
		}

		for (Entry<String, AlarmSpec> entry : alarmSpecs.entrySet()) {
			final String alarmId = entry.getKey();
			final AlarmSpec alarmSpec = entry.getValue();

			boolean active = testAlarm(alarmSpec, data);

			if (active != alarmSpec.isActive()) {
				updateAlarmState(alarmId, config, alarmSpec, active);
			}
		}
	}

	@Nullable
	private Map<String, AlarmSpec> getAlarmSpecsForDataStream(@NonNull DataStreamConfig config) {
		final SessionDataStream sessionDataStream = mDataStreams.get(getDataStreamKey(config));
		return sessionDataStream != null ? sessionDataStream.getAlarms() : null;
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private boolean testAlarm(@NonNull AlarmSpec alarmSpec, @NonNull DataModel data) {
		if (Objects.equals(alarmSpec.getType(), AlarmSpec.TYPE_GREATER_OR_EQUAL)) {
			return data.calibratedValue >= alarmSpec.getCalibratedThreshold();
		}

		if (Objects.equals(alarmSpec.getType(), AlarmSpec.TYPE_LESSER_OR_EQUAL)) {
			return data.calibratedValue <= alarmSpec.getCalibratedThreshold();
		}

		return false;
	}

	private void updateAlarmState(String alarmId, DataStreamConfig streamConfig, AlarmSpec alarmSpec, boolean newActive) {
		FirebaseDbInstance.get()
				.getReference("sessions").child(streamConfig.getSessionId())
				.child("dataStreams").child(streamConfig.getDataStreamId())
				.child("alarms").child(alarmId)
				.child("active").setValue(newActive);
	}

	private class SessionDataStreamListener implements ValueEventListener {

		private final String mKey;

		public SessionDataStreamListener(DataStreamConfig config) {
			mKey = getDataStreamKey(config);
		}

		@Override
		public void onDataChange(DataSnapshot dataSnapshot) {
			SessionDataStream sessionDataStream = dataSnapshot.getValue(SessionDataStream.class);
			mDataStreams.put(mKey, sessionDataStream);
		}

		@Override
		public void onCancelled(DatabaseError databaseError) {
			// don't care
		}
	};

	private static String getDataStreamKey(@NonNull DataStreamConfig config) {
		return config.getSessionId() + '/' + config.getDataStreamId();
	}

}
