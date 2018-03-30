package com.schoovello.cookerconnector.datamodels;

public class DataModel {

	public final double rawValue;
	public final double calibratedValue;
	public final long timeMillis;

	public DataModel(TemperatureReading reading) {
		this(reading.rawAdcValue, reading.f, reading.timestamp);
	}

	public DataModel(double rawValue, double calibratedValue, long timeMillis) {
		this.rawValue = rawValue;
		this.calibratedValue = calibratedValue;
		this.timeMillis = timeMillis;
	}

	@Override
	public String toString() {
		return "DataModel{" +
				"rawValue=" + rawValue +
				", calibratedValue=" + calibratedValue +
				", timeMillis=" + timeMillis +
				'}';
	}
}
