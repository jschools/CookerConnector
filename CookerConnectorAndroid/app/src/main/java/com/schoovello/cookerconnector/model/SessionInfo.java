package com.schoovello.cookerconnector.model;

import com.schoovello.cookerconnector.datamodels.SessionDataStream;

import java.util.Map;

public class SessionInfo {

	private String title;

	private long startTimeMillis;

	private String comments;

	private Map<String, SessionDataStream> dataStreams;

	private boolean paused;

	private boolean stopped;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public long getStartTimeMillis() {
		return startTimeMillis;
	}

	public void setStartTimeMillis(long startTimeMillis) {
		this.startTimeMillis = startTimeMillis;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public Map<String, SessionDataStream> getDataStreams() {
		return dataStreams;
	}

	public void setDataStreams(Map<String, SessionDataStream> dataStreams) {
		this.dataStreams = dataStreams;
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public boolean isStopped() {
		return stopped;
	}

	public void setStopped(boolean stopped) {
		this.stopped = stopped;
	}
}
