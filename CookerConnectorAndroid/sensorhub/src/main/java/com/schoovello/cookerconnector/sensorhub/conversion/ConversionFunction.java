package com.schoovello.cookerconnector.sensorhub.conversion;

import androidx.annotation.Nullable;

public abstract class ConversionFunction {

	protected ConversionFunction() {
		// protected
	}

	public abstract double convert(double rawValue);

	public abstract String formatValue(double convertedValue);

	public static ConversionFunction getInstance(@Nullable String name) {
		if (name == null) {
			return IdentityFunction.INSTANCE;
		}

		switch (name) {
			case "TEMPERATURE_PROBE_DOT_5600_OHM_F":
				return ThermoWorksThermistorFahrenheitFunction.INSTANCE;
			default:
				return IdentityFunction.INSTANCE;
		}
	}

}
