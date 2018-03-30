package com.schoovello.cookerconnector.data;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class BackgroundDataProcessor<Input, Output> {

	private static final int INPUT_BATCH_SIZE = 100;
	private static final long INPUT_BATCH_TIMEOUT_MS = 250;

	private static final int DELIVERY_DEADLINE_DURATION_MS = 15;

	private Thread mProcessingThread;
	private volatile boolean mRunning;

	private final BlockingQueue<Input> mInput;
	private final BlockingQueue<Output> mOutput;
	private final ArrayList<Output> mBatchBuffer;

	private final Handler mOutputHandler;

	public BackgroundDataProcessor(@NonNull Looper outputLooper) {
		mOutputHandler = new Handler(outputLooper);

		mInput = new LinkedBlockingQueue<>();
		mOutput = new LinkedBlockingQueue<>();

		mBatchBuffer = new ArrayList<>(INPUT_BATCH_SIZE);
	}

	@WorkerThread
	protected abstract Output processDataInBackground(@NonNull Input input);

	protected abstract void onDataProcessed(@NonNull Output output);

	@MainThread
	public void start() {
		if (mProcessingThread == null) {
			mRunning = true;
			mProcessingThread = new Thread(mProcessingRunnable);
			mProcessingThread.start();
		}
	}

	@MainThread
	public void stopAndClearUndeliveredResults() {
		if (mProcessingThread != null) {
			mRunning = false;
			mProcessingThread.interrupt();
			mProcessingThread = null;
		}

		mInput.clear();
		mOutput.clear();

		mOutputHandler.removeCallbacksAndMessages(null);
	}

	public final void enqueueData(@NonNull Input input) {
		mInput.add(input);
	}

	private Runnable mProcessingRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				while (mRunning) {
					for (int i = 0; i < INPUT_BATCH_SIZE; i++) {
						final Input input = mInput.poll(INPUT_BATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
						if (input == null) {
							break;
						}

						final Output output = processDataInBackground(input);
						mBatchBuffer.add(output);
					}

					mOutput.addAll(mBatchBuffer);
					mBatchBuffer.clear();

					mOutputHandler.removeCallbacks(mDeliveryRunnable);
					mOutputHandler.post(mDeliveryRunnable);
				}
			} catch(InterruptedException e){
				// quit if interrupted
			}
		}
	};

	private Runnable mDeliveryRunnable = new Runnable() {
		@Override
		public void run() {
			int deliveryCount = 0;

			final long startTime = SystemClock.elapsedRealtime();
			final long deadlineTime = startTime + DELIVERY_DEADLINE_DURATION_MS;

			boolean haveTime = true;
			while (!mOutput.isEmpty() && haveTime) {
				final Output output = mOutput.poll();
				if (output == null) {
					break;
				}

				onDataProcessed(output);

				deliveryCount++;

				haveTime = SystemClock.elapsedRealtime() < deadlineTime;
			}

			if (!mOutput.isEmpty()) {
				mOutputHandler.post(this);
			}

			if (deliveryCount > 0) {
				onBatchCompleted();
			}
		}
	};

	protected void onBatchCompleted() {
		// optional
	}


}
