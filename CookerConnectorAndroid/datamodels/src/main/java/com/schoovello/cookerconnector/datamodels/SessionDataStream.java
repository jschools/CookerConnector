package com.schoovello.cookerconnector.datamodels;

import java.util.Map;

public class SessionDataStream {

	private String title;
	private String description;
	private String activeDataStream;
	private Map<String, AlarmSpec> alarms;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getActiveDataStream() {
		return activeDataStream;
	}

	public void setActiveDataStream(String activeDataStream) {
		this.activeDataStream = activeDataStream;
	}

	public Map<String, AlarmSpec> getAlarms() {
		return alarms;
	}
}
