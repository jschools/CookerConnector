package com.schoovello.cookerconnector.sensorhub.service;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.Pwm;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;

public class ServoController {

	private static final long PWM_TIMEOUT_MS = 1_000;
	private static final double PERIOD_MS = 20;
	private static final double FREQ = 1.0 / PERIOD_MS * 1_000;

	private DatabaseReference mValueReference;

	private Pwm mPwm;

	private Handler mHandler;

	public ServoController() {
		mHandler = new Handler(Looper.getMainLooper());
	}

	protected void start() {
		mValueReference = FirebaseDatabase.getInstance()
				.getReference("sensorHubs")
				.child("-KzvLiaqYMCHTQ0nXR4L")
				.child("control")
				.child("channels")
				.child("0")
				.child("value");

		mValueReference.addValueEventListener(mValueEventListener);

		try {
			mPwm = new PeripheralManagerService().openPwm("PWM1");
			mPwm.setPwmFrequencyHz(FREQ);
		} catch (IOException e) {
			e.printStackTrace();
		}

		setFractionOpen(0.5);
	}

	protected void stop() {
		if (mValueReference != null) {
			mValueReference.removeEventListener(mValueEventListener);
		}

		mHandler.removeCallbacksAndMessages(null);
	}

	private void setFractionOpen(double fractionOpen) {
		// 1.0 --> 1ms
		// 0.5 --> 1.5ms
		// 0.0 --> 2ms
		final double widthMs = 2.0 - fractionOpen;
		final double duty = widthMs / PERIOD_MS * 100;

		if (mPwm != null) {
			try {
				mPwm.setEnabled(true);
				mPwm.setPwmDutyCycle(duty);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Log.d("PWM", "Set fraction open: " + fractionOpen + " --> " + widthMs + "ms pulse");
		mHandler.postDelayed(mStopRunnable, PWM_TIMEOUT_MS);
	}

	private Runnable mStopRunnable = new Runnable() {
		@Override
		public void run() {
			mHandler.removeCallbacks(this);

			Log.d("PWM", "Stop");

			if (mPwm != null) {
				try {
					mPwm.setEnabled(false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};

	private ValueEventListener mValueEventListener = new ValueEventListener() {
		@Override
		public void onDataChange(DataSnapshot dataSnapshot) {
			if (dataSnapshot == null) {
				return;
			}

			final Number value = (Number) dataSnapshot.getValue();
			if (value == null) {
				return;
			}

			setFractionOpen(value.doubleValue());
		}

		@Override
		public void onCancelled(DatabaseError databaseError) {
			// don't care
		}
	};

}
