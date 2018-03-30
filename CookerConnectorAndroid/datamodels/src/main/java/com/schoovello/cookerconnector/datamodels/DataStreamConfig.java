package com.schoovello.cookerconnector.datamodels;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.firebase.database.Exclude;

public class DataStreamConfig implements Parcelable {

	private int channel;
	private String conversionFunction;
	private String sessionId;
	private String dataStreamId;
	private long measurementIntervalMs;
	private boolean paused;

	public DataStreamConfig() {
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}

	public String getConversionFunction() {
		return conversionFunction;
	}

	public void setConversionFunction(String conversionFunction) {
		this.conversionFunction = conversionFunction;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getDataStreamId() {
		return dataStreamId;
	}

	public void setDataStreamId(String dataStreamId) {
		this.dataStreamId = dataStreamId;
	}

	public long getMeasurementIntervalMs() {
		return measurementIntervalMs;
	}

	public void setMeasurementIntervalMs(long measurementIntervalMs) {
		this.measurementIntervalMs = measurementIntervalMs;
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public boolean isSameStream(@NonNull DataStreamConfig other) {
		if (this.sessionId == null || this.dataStreamId == null) {
			return false;
		}

		return this.sessionId.equals(other.sessionId) && this.dataStreamId.equals(other.dataStreamId);
	}

	@Exclude
	public boolean isValid() {
		return this.sessionId != null && this.dataStreamId != null && this.conversionFunction != null && this.measurementIntervalMs > 0;
	}

	@Override
	public String toString() {
		return "DataStreamConfig{" +
				"ch=" + channel +
				", fn='" + conversionFunction + '\'' +
				", sessionId='" + sessionId + '\'' +
				", dataStreamId='" + dataStreamId + '\'' +
				", intervalMs=" + measurementIntervalMs +
				", paused=" + paused +
				'}';
	}

	protected DataStreamConfig(Parcel in) {
		channel = in.readInt();
		conversionFunction = in.readString();
		sessionId = in.readString();
		dataStreamId = in.readString();
		measurementIntervalMs = in.readLong();
		paused = in.readByte() != 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(channel);
		dest.writeString(conversionFunction);
		dest.writeString(sessionId);
		dest.writeString(dataStreamId);
		dest.writeLong(measurementIntervalMs);
		dest.writeByte((byte) (paused ? 1 : 0));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<DataStreamConfig> CREATOR = new Creator<DataStreamConfig>() {
		@Override
		public DataStreamConfig createFromParcel(Parcel in) {
			return new DataStreamConfig(in);
		}

		@Override
		public DataStreamConfig[] newArray(int size) {
			return new DataStreamConfig[size];
		}
	};
}
