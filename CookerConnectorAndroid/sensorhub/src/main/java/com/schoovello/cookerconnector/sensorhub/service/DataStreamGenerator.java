package com.schoovello.cookerconnector.sensorhub.service;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.schoovello.cookerconnector.datamodels.DataModel;
import com.schoovello.cookerconnector.datamodels.DataStreamConfig;
import com.schoovello.cookerconnector.datamodels.TemperatureReading;
import com.schoovello.cookerconnector.sensorhub.BuildConfig;
import com.schoovello.cookerconnector.sensorhub.peripheral.TemperatureLiveData;
import com.schoovello.cookerconnector.sensorhub.repository.DataStreamOutput;

public class DataStreamGenerator implements Observer<TemperatureReading> {
	private static final String LOG_TAG = DataStreamGenerator.class.getSimpleName();

	private final TemperatureLiveData mLiveData;
	private final DataStreamOutput mOutput;

	private DataStreamConfig mConfig;

	private boolean mRunning;

	public DataStreamGenerator(DataStreamConfig config, DataStreamOutput output) {
		mConfig = config;
		mOutput = output;
		mLiveData = new TemperatureLiveData(mConfig);
	}

	/**
	 * @return {@code false} if the new config was rejected
	 */
	public boolean tryUpdateConfig(DataStreamConfig newConfig) {
		if (mConfig != null && (
					!mConfig.isValid() ||
					!mConfig.isSameStream(newConfig) ||
					!mConfig.getConversionFunction().equals(newConfig.getConversionFunction())
				)
			) {
			return false;
		}

		updateConfig(newConfig);
		return true;
	}

	private void updateConfig(DataStreamConfig newConfig) {
		mConfig = newConfig;

		mLiveData.setPollInterval(newConfig.getMeasurementIntervalMs());
	}

	public void start() {
		mLiveData.observeForever(this);
		mRunning = true;

		if (BuildConfig.DEBUG) {
			@SuppressLint("DefaultLocale")
			String message = String.format("Started ch:%1$d %2$s/%3$s", mConfig.getChannel(), mConfig.getSessionId(), mConfig.getDataStreamId());
			Log.d(LOG_TAG, message);
		}
	}

	public void stop() {
		mLiveData.removeObserver(this);
		mRunning = false;

		if (BuildConfig.DEBUG) {
			@SuppressLint("DefaultLocale")
			String message = String.format("Stopped ch:%1$d %2$s/%3$s", mConfig.getChannel(), mConfig.getSessionId(), mConfig.getDataStreamId());
			Log.d(LOG_TAG, message);
		}
	}

	public boolean isRunning() {
		return mRunning;
	}

	@Override
	public void onChanged(@Nullable TemperatureReading temperatureReading) {
		mOutput.onSensorEvent(mConfig, new DataModel(temperatureReading));
	}


}
