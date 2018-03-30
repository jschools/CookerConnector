package com.schoovello.cookerconnector.sensorhub.conversion;

import java.util.Locale;

public class IdentityFunction extends ConversionFunction {

	public static final IdentityFunction INSTANCE = new IdentityFunction();

	private IdentityFunction() {
		// use INSTANCE
	}

	@Override
	public double convert(double rawValue) {
		return rawValue;
	}

	@Override
	public String formatValue(double convertedValue) {
		final double volts = convertedValue / 4096 * 3.3;
		return String.format(Locale.getDefault(), "%1$.2fV", volts);
	}
}
