package com.schoovello.cookerconnector.util;

import android.support.annotation.NonNull;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseReference.CompletionListener;

public class CompletionBarrier {

	private int mOperationsStarted;
	private int mOperationsCompleted;
	private boolean mWaiting;
	private boolean mNotified;

	private CompletionBarrierListener mListener;

	public CompletionBarrier() {
		mOperationsStarted = 0;
		mOperationsCompleted = 0;
		mWaiting = false;
		mNotified = false;
	}

	public CompletionListener startNewOperation() {
		if (mWaiting) {
			throw new IllegalStateException("Can't start a new operation after calling startWaiting()");
		}

		mOperationsStarted++;
		return new CompletionListener() {
			@Override
			public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
				if (databaseError == null) {
					completeOperation();
				} else {
					failOperation(databaseError, databaseReference);
				}
			}
		};
	}

	public void startWaiting(@NonNull CompletionBarrierListener listener) {
		mListener = listener;
		mWaiting = true;
		update();
	}

	private void completeOperation() {
		mOperationsCompleted++;
		update();
	}

	private void failOperation(@NonNull DatabaseError databaseError, DatabaseReference databaseReference) {
		mListener.onOperationFailed(databaseError, databaseReference);
		mNotified = true;
	}

	private void update() {
		if (mWaiting && !mNotified && mOperationsCompleted == mOperationsStarted) {
			mNotified = true;
			mListener.onAllOperationsCompleted();
		}
	}

	public interface CompletionBarrierListener {
		void onOperationFailed(@NonNull DatabaseError databaseError, DatabaseReference databaseReference);
		void onAllOperationsCompleted();
	}


}
