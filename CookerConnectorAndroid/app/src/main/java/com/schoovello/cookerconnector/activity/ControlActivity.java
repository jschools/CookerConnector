package com.schoovello.cookerconnector.activity;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.schoovello.cookerconnector.R;
import com.schoovello.cookerconnector.data.FirebaseDbInstance;

public class ControlActivity extends AppCompatActivity implements OnClickListener, SeekBar.OnSeekBarChangeListener {

	private TextView mPercentView;
	private SeekBar mSeekBar;
	private Button mUpdateButton;

	private DatabaseReference mRef;

	private MutableLiveData<Double> mLiveData;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_control);

		mRef = getReference();

		mPercentView = findViewById(R.id.percent);
		mSeekBar = findViewById(R.id.seekBar);
		mUpdateButton = findViewById(R.id.updateButton);

		mUpdateButton.setOnClickListener(this);

		mSeekBar.setOnSeekBarChangeListener(this);

		mLiveData = new MutableLiveData<>();
		mLiveData.observe(this, new Observer<Double>() {
			@Override
			public void onChanged(@Nullable Double value) {
				double percent;
				if (value == null) {
					percent = 0;
				} else {
					percent = value.doubleValue();
				}

				int wholePercent = (int) (percent * 100);
				String string = wholePercent + "%";
				mPercentView.setText(string);
				mSeekBar.setProgress(wholePercent);
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();

		mRef.addValueEventListener(mListener);
	}

	@Override
	protected void onStop() {
		super.onStop();

		mRef.removeEventListener(mListener);
	}

	private ValueEventListener mListener = new ValueEventListener() {
		@Override
		public void onDataChange(DataSnapshot dataSnapshot) {
			Double percent = dataSnapshot.getValue(Double.class);
			mLiveData.postValue(percent);
		}

		@Override
		public void onCancelled(DatabaseError databaseError) {
			// don't care
		}
	};

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.updateButton) {
			update();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			mLiveData.setValue(progress / 100d);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// don't care
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// don't care
	}

	private void update() {
		int progress = mSeekBar.getProgress();
		double percent = progress / 100d;

		getReference().setValue(percent);
	}

	private DatabaseReference getReference() {
		return FirebaseDbInstance.get()
				.getReference("sensorHubs").child("-KzvLiaqYMCHTQ0nXR4L")
				.child("control")
				.child("channels").child("0")
				.child("value");
	}
}
