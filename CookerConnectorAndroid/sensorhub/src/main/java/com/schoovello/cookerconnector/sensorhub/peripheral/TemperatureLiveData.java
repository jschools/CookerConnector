package com.schoovello.cookerconnector.sensorhub.peripheral;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import androidx.lifecycle.LiveData;

import com.schoovello.cookerconnector.datamodels.DataStreamConfig;
import com.schoovello.cookerconnector.datamodels.TemperatureReading;
import com.schoovello.cookerconnector.sensorhub.conversion.ConversionFunction;

import java.io.IOException;

public class TemperatureLiveData extends LiveData<TemperatureReading> {

	private static final String THREAD_NAME = "TemperatureLiveData";

	public static final long DEFAULT_POLL_INTERVAL_MS = 1000;

	private SpiAdcDevice mAdc;

	private final int mChannel;
	private final ConversionFunction mConversionFunction;

	private long mPollIntervalMs;

	private HandlerThread mHandlerThread;
	private Handler mBackgroundHandler;

	public TemperatureLiveData(DataStreamConfig config) {
		this(config.getChannel(), ConversionFunction.getInstance(config.getConversionFunction()), config.getMeasurementIntervalMs());
	}

	public TemperatureLiveData(int channel, ConversionFunction conversionFunction, long pollIntervalMs) {
		mChannel = channel;
		mConversionFunction = conversionFunction;
		mPollIntervalMs = pollIntervalMs;
	}

	public void setPollInterval(long pollIntervalMs) {
		mPollIntervalMs = pollIntervalMs;
	}

	@Override
	protected void onActive() {
		try {
			mAdc = SpiAdcDevice.acquireOpenSharedDevice();
		} catch (IOException e) {
			e.printStackTrace();
		}

		mHandlerThread = new HandlerThread(THREAD_NAME);
		mHandlerThread.start();
		mBackgroundHandler = new Handler(mHandlerThread.getLooper());
		mBackgroundHandler.post(mPollRunnable);
	}

	@Override
	protected void onInactive() {
		try {
			SpiAdcDevice.releaseSharedDevice();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mAdc = null;

		mBackgroundHandler.removeCallbacksAndMessages(null);
		mHandlerThread.quitSafely();
		mHandlerThread = null;
	}

	private final Runnable mPollRunnable = new Runnable() {
		@Override
		public void run() {
			mBackgroundHandler.removeCallbacks(this);

			final long startTime = SystemClock.elapsedRealtime();

			// read the ADC
			final double sampledValue;
			try {
				sampledValue = mAdc.getSampledValue(mChannel);

				// convert to temperature
				final double temperatureF = mConversionFunction.convert(sampledValue);
				postValue(new TemperatureReading(temperatureF, true, System.currentTimeMillis(), mChannel, sampledValue));
			} catch (IOException e) {
				e.printStackTrace();
			}

			final long endTime = SystemClock.elapsedRealtime();

			// re-schedule self
			final long runTimeMs = endTime - startTime;
			final long delayMs = mPollIntervalMs - runTimeMs;
			if (delayMs > 0) {
				mBackgroundHandler.postDelayed(this, delayMs);
			} else {
				mBackgroundHandler.post(this);
			}
		}
	};
}
