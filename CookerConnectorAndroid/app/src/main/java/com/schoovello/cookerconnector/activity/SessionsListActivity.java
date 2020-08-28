package com.schoovello.cookerconnector.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.schoovello.cookerconnector.R;
import com.schoovello.cookerconnector.activity.SessionsListAdapter.OnSessionSelectedListener;
import com.schoovello.cookerconnector.model.SessionInfo;

public class SessionsListActivity extends AppCompatActivity implements OnSessionSelectedListener, OnClickListener {

	private RecyclerView mRecyclerView;
	private SessionsListAdapter mAdapter;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.recycler_with_add);

		mRecyclerView = findViewById(R.id.recyclerView);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		mAdapter = new SessionsListAdapter(this, this, this);
		mRecyclerView.setAdapter(mAdapter);

		findViewById(R.id.addButton).setOnClickListener(this);
	}

	@Override
	public void onSessionSelected(@NonNull String id, @NonNull SessionInfo info) {
		Intent intent = SessionActivity.buildIntent(id);
		startActivity(intent);
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
		Intent intent = new Intent(this, NewSessionActivity.class);
		startActivity(intent);
	}

}
