package com.schoovello.cookerconnector.model;

public class TimeDataPoint {

	private long timeMillis;

	private double rawValue;

	private double calibratedValue;

	public long getTimeMillis() {
		return timeMillis;
	}

	public double getRawValue() {
		return rawValue;
	}

	public double getCalibratedValue() {
		return calibratedValue;
	}

	public void setTimeMillis(long timeMillis) {
		this.timeMillis = timeMillis;
	}

	public void setRawValue(double rawValue) {
		this.rawValue = rawValue;
	}

	public void setCalibratedValue(double calibratedValue) {
		this.calibratedValue = calibratedValue;
	}
}
