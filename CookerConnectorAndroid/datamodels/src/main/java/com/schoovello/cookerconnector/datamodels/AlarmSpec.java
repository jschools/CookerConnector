package com.schoovello.cookerconnector.datamodels;

public class AlarmSpec {

	public static final String TYPE_LESSER_OR_EQUAL = "LESSER_OR_EQUAL";
	public static final String TYPE_GREATER_OR_EQUAL = "GREATER_OR_EQUAL";

	private boolean active;
	private String pushToken;
	private int calibratedThreshold;
	private String type;

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getPushToken() {
		return pushToken;
	}

	public void setPushToken(String pushToken) {
		this.pushToken = pushToken;
	}

	public int getCalibratedThreshold() {
		return calibratedThreshold;
	}

	public void setCalibratedThreshold(int calibratedThreshold) {
		this.calibratedThreshold = calibratedThreshold;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
