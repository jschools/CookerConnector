package com.schoovello.cookerconnector.sensorhub.repository;

import com.schoovello.cookerconnector.datamodels.DataModel;
import com.schoovello.cookerconnector.datamodels.DataStreamConfig;

import java.util.Arrays;
import java.util.List;

public class SyndicatorDataStreamOutput implements DataStreamOutput {

	private final List<DataStreamOutput> mOutputs;

	public SyndicatorDataStreamOutput(DataStreamOutput... outputs) {
		mOutputs = Arrays.asList(outputs);
	}

	@Override
	public void onSensorEvent(DataStreamConfig config, DataModel data) {
		for (DataStreamOutput output : mOutputs) {
			output.onSensorEvent(config, data);
		}
	}
}
