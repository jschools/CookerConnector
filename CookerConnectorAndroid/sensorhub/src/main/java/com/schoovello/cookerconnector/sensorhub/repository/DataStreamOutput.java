package com.schoovello.cookerconnector.sensorhub.repository;

import com.schoovello.cookerconnector.datamodels.DataModel;
import com.schoovello.cookerconnector.datamodels.DataStreamConfig;

public interface DataStreamOutput {

	void onSensorEvent(DataStreamConfig config, DataModel data);

}
