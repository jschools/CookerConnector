package com.schoovello.cookerconnector.datamodels;

import androidx.annotation.NonNull;

public class DataModel {

	public double rawValue;
	public double calibratedValue;
	public long timeMillis;

	public DataModel() {

	}

	public DataModel(TemperatureReading reading) {
		this(reading.rawAdcValue, reading.f, reading.timestamp);
	}

	public DataModel(double rawValue, double calibratedValue, long timeMillis) {
		this.rawValue = rawValue;
		this.calibratedValue = calibratedValue;
		this.timeMillis = timeMillis;
	}

	public double getRawValue() {
		return rawValue;
	}

	public void setRawValue(double rawValue) {
		this.rawValue = rawValue;
	}

	public double getCalibratedValue() {
		return calibratedValue;
	}

	public void setCalibratedValue(double calibratedValue) {
		this.calibratedValue = calibratedValue;
	}

	public long getTimeMillis() {
		return timeMillis;
	}

	public void setTimeMillis(long timeMillis) {
		this.timeMillis = timeMillis;
	}

	@NonNull
	@Override
	public String toString() {
		return "DataModel{" +
				"rawValue=" + rawValue +
				", calibratedValue=" + calibratedValue +
				", timeMillis=" + timeMillis +
				'}';
	}
}
