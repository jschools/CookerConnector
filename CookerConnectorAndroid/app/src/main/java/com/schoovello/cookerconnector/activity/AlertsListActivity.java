package com.schoovello.cookerconnector.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.View.OnClickListener;

import com.schoovello.cookerconnector.CookerConnectorApp;
import com.schoovello.cookerconnector.R;
import com.schoovello.cookerconnector.activity.AlertsListAdapter.OnAlertActionListener;
import com.schoovello.cookerconnector.data.FirebaseDbInstance;
import com.schoovello.cookerconnector.fragment.EditAlertDialogFragment;

public class AlertsListActivity  extends AppCompatActivity implements OnAlertActionListener, OnClickListener {

	private static final String KEY_SESSION_ID = "sessionId";
	private static final String KEY_DATA_STREAM_ID = "dataStreamId";
	private static final String KEY_DATA_STREAM_TITLE = "dataStreamTitle";

	private static final String FRAG_TAG_EDIT_ALERT = "editAlert";

	private RecyclerView mRecyclerView;
	private AlertsListAdapter mAdapter;

	private String mSessionId;
	private String mDataStreamId;
	private String mDataStreamTitle;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		mSessionId = extras.getString(KEY_SESSION_ID);
		mDataStreamId = extras.getString(KEY_DATA_STREAM_ID);
		mDataStreamTitle = extras.getString(KEY_DATA_STREAM_TITLE);

		setContentView(R.layout.recycler_with_add);

		setTitle(getString(R.string.alerts_list_title, mDataStreamTitle));

		mRecyclerView = findViewById(R.id.recyclerView);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		mAdapter = new AlertsListAdapter(this, this, mSessionId, mDataStreamId, mDataStreamTitle, this);
		mRecyclerView.setAdapter(mAdapter);

		findViewById(R.id.addButton).setOnClickListener(this);
	}

	@Override
	public void onEditAlertSelected(@NonNull String alertId) {
		EditAlertDialogFragment frag = EditAlertDialogFragment.newInstance(mSessionId, mDataStreamId, mDataStreamTitle, alertId);
		frag.show(getSupportFragmentManager(), FRAG_TAG_EDIT_ALERT);
	}

	@Override
	public void onDeleteAlertSelected(@NonNull String alertId) {
		FirebaseDbInstance.get()
				.getReference("sessions").child(mSessionId)
				.child("dataStreams").child(mDataStreamId)
				.child("alarms").child(alertId)
				.removeValue();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.addButton:
				onAddClicked();
				break;
		}
	}

	private void onAddClicked() {
		String alertId = FirebaseDbInstance.get().getReference().push().getKey();
		EditAlertDialogFragment frag = EditAlertDialogFragment.newInstance(mSessionId, mDataStreamId, mDataStreamTitle, alertId);
		frag.show(getSupportFragmentManager(), FRAG_TAG_EDIT_ALERT);
	}

	public static Intent buildIntent(@NonNull String sessionId, @NonNull String dataStreamId, @NonNull String dataStreamTitle) {
		Intent intent = new Intent(CookerConnectorApp.getInstance(), AlertsListActivity.class);
		intent.putExtra(KEY_SESSION_ID, sessionId);
		intent.putExtra(KEY_DATA_STREAM_ID, dataStreamId);
		intent.putExtra(KEY_DATA_STREAM_TITLE, dataStreamTitle);
		return intent;
	}

}