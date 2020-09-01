package com.schoovello.cookerconnector.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;

import com.google.firebase.database.DatabaseReference;
import com.schoovello.cookerconnector.CookerConnectorApp;
import com.schoovello.cookerconnector.R;
import com.schoovello.cookerconnector.data.FirebaseDbInstance;
import com.schoovello.cookerconnector.data.SessionInfoLiveData;
import com.schoovello.cookerconnector.datamodels.SessionDataStream;
import com.schoovello.cookerconnector.fragment.DataStreamFragment;
import com.schoovello.cookerconnector.model.SessionInfo;
import com.schoovello.cookerconnector.service.MonitorNotificationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SessionActivity extends AppCompatActivity implements OnClickListener {
	private static final String EXTRA_SESSION_ID = "SESSION_ID";

	private String mSessionId;
	private SessionInfoLiveData mSessionInfoLiveData;
	private List<String> mActiveStreams;

	private Button mPauseButton;
	private Button mStopButton;
	private Button mNotificationButton;
	private Button mControlButton;
	private View mBottomBar;
	private View mBottomBarShadow;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);

		mActiveStreams = new ArrayList<>();

		setContentView(R.layout.activity_session);

		mPauseButton = findViewById(R.id.pauseButton);
		mStopButton = findViewById(R.id.stopButton);
		mNotificationButton = findViewById(R.id.notificationButton);
		mControlButton = findViewById(R.id.controlButton);

		mPauseButton.setOnClickListener(this);
		mStopButton.setOnClickListener(this);
		mNotificationButton.setOnClickListener(this);
		mControlButton.setOnClickListener(this);

		mBottomBar = findViewById(R.id.bottomBar);
		mBottomBarShadow = findViewById(R.id.bottomBarShadow);

		mSessionInfoLiveData = new SessionInfoLiveData(mSessionId);
		mSessionInfoLiveData.observe(this, mSessionInfoObserver);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.pauseButton:
				onPauseClicked();
				break;
			case R.id.stopButton:
				onStopClicked();
				break;
			case R.id.notificationButton:
				MonitorNotificationService.updateNow();
				break;
			case R.id.controlButton:
				Intent intent = new Intent(this, ControlActivity.class);
				startActivity(intent);
				break;
			default:
				break;
		}
	}

	private void onPauseClicked() {
		final SessionInfo sessionInfo = mSessionInfoLiveData != null ? mSessionInfoLiveData.getValue() : null;
		if (sessionInfo == null) {
			return;
		}

		final boolean newPaused = !sessionInfo.isPaused();
		final DatabaseReference pausedRef = FirebaseDbInstance.INSTANCE.getInstance()
				.getReference("sessions")
				.child(mSessionId)
				.child("paused");
		pausedRef.setValue(newPaused);
	}

	private void onStopClicked() {
		final SessionInfo sessionInfo = mSessionInfoLiveData != null ? mSessionInfoLiveData.getValue() : null;
		if (sessionInfo == null) {
			return;
		}

		final DatabaseReference stoppedRef = FirebaseDbInstance.INSTANCE.getInstance()
				.getReference("sessions")
				.child(mSessionId)
				.child("stopped");
		stoppedRef.setValue(true);
	}

	private Observer<SessionInfo> mSessionInfoObserver = new Observer<SessionInfo>() {
		@Override
		public void onChanged(@Nullable SessionInfo sessionInfo) {
			onSessionUpdated();
		}
	};

	private void updateButtons() {
		final SessionInfo sessionInfo = mSessionInfoLiveData.getValue();

		if (sessionInfo == null || sessionInfo.isStopped()) {
			mBottomBar.setVisibility(View.GONE);
			mBottomBarShadow.setVisibility(View.GONE);
		} else {
			mBottomBar.setVisibility(View.VISIBLE);
			mBottomBarShadow.setVisibility(View.VISIBLE);
			mPauseButton.setText(sessionInfo.isPaused() ? R.string.resume : R.string.pause);
		}
	}

	private void onSessionUpdated() {
		// get the list of streams
		SessionInfo sessionInfo = mSessionInfoLiveData.getValue();
		Map<String, SessionDataStream> dataStreams;
		if (sessionInfo == null) {
			dataStreams = Collections.emptyMap();
		} else {
			dataStreams = sessionInfo.getDataStreams();
		}

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		// add new ones
		for (String streamId : dataStreams.keySet()) {
			if (!mActiveStreams.contains(streamId)) {
				addStreamFragment(ft, streamId);
				mActiveStreams.add(streamId);
			}
		}

		// remove deleted ones
		Iterator<String> it = mActiveStreams.iterator();
		while (it.hasNext()) {
			String streamId = it.next();
			if (!dataStreams.containsKey(streamId)) {
				removeStreamFragment(ft, streamId);
				it.remove();
			}
		}

		if (!ft.isEmpty()) {
			ft.commitAllowingStateLoss();
		}

		updateSensorPausedStates();

		updateButtons();
	}

	private void updateSensorPausedStates() {
		final SessionInfo sessionInfo = mSessionInfoLiveData.getValue();
		if (sessionInfo == null) {
			return;
		}

		final Map<String, SessionDataStream> dataStreams = sessionInfo.getDataStreams();
		for (Map.Entry<String, SessionDataStream> entry : dataStreams.entrySet()) {
			final SessionDataStream dataStream = entry.getValue();
			DatabaseReference streamPausedRef = FirebaseDbInstance.INSTANCE.getInstance()
					.getReference("sensorHubs")
					.child(dataStream.getActiveDataStream())
					.child("paused");
			streamPausedRef.setValue(sessionInfo.isPaused() || sessionInfo.isStopped());
		}
	}

	private void addStreamFragment(FragmentTransaction ft, String streamId) {
		String fragTag = getStreamFragmentTag(streamId);
		Fragment existing = getSupportFragmentManager().findFragmentByTag(fragTag);
		if (existing == null) {
			DataStreamFragment frag = DataStreamFragment.newInstance(mSessionId, streamId);
			ft.add(R.id.graphs_container, frag, fragTag);
		}
	}

	private void removeStreamFragment(FragmentTransaction ft, String streamId) {
		String fragTag = getStreamFragmentTag(streamId);
		Fragment existing = getSupportFragmentManager().findFragmentByTag(fragTag);
		if (existing != null) {
			ft.remove(existing);
		}
	}

	private static String getStreamFragmentTag(String streamId) {
		return "stream:" + streamId;
	}

	public static Intent buildIntent(@NonNull String sessionId) {
		Intent intent = new Intent(CookerConnectorApp.getInstance(), SessionActivity.class);
		intent.putExtra(EXTRA_SESSION_ID, sessionId);
		return intent;
	}
}
