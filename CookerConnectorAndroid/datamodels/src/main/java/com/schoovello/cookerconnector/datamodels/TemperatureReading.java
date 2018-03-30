package com.schoovello.cookerconnector.datamodels;

public class TemperatureReading {

	public final double f;
	public final double c;
	public final long timestamp;
	public final int channel;
	public final double rawAdcValue;

	public TemperatureReading(double temperature, boolean isFahrenheit, long timestamp, int channel, double rawAdcValue) {
		this.rawAdcValue = rawAdcValue;
		this.timestamp = timestamp;
		this.channel = channel;

		if (isFahrenheit) {
			this.f = temperature;
			this.c = fToC(temperature);
		} else {
			this.f = cToF(temperature);
			this.c = temperature;
		}
	}

	public static double fToC(double fahrenheit) {
		return (fahrenheit - 32.0) * (5.0 / 9.0);
	}

	public static double cToF(double celsius) {
		return celsius * (9.0 / 5.0) + 32.0;
	}

}
