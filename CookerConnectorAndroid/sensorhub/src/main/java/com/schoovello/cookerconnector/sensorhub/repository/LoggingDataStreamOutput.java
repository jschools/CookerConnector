package com.schoovello.cookerconnector.sensorhub.repository;

import android.util.Log;

import com.schoovello.cookerconnector.datamodels.DataModel;
import com.schoovello.cookerconnector.datamodels.DataStreamConfig;

public class LoggingDataStreamOutput implements DataStreamOutput {
	@Override
	public void onSensorEvent(DataStreamConfig config, DataModel data) {
		Log.d("SensorEvent", config + " :: " + data);
	}
}
