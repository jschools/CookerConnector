package com.schoovello.cookerconnector.sensorhub.conversion;

import java.util.Locale;
import java.util.Map.Entry;
import java.util.TreeMap;

public class ThermoWorksThermistorFahrenheitFunction extends ConversionFunction {

	public static final ThermoWorksThermistorFahrenheitFunction INSTANCE = new ThermoWorksThermistorFahrenheitFunction();

	private ThermoWorksThermistorFahrenheitFunction() {
		// use INSTANCE
	}

	@Override
	public double convert(double rawValue) {
		final Double key = Double.valueOf(rawValue);

		Entry<Double, Double> lower;
		Entry<Double, Double> upper;

		if (key < sTable.firstKey()) {
			lower = sTable.firstEntry();
			upper = sTable.higherEntry(lower.getKey());
		} else if (key > sTable.lastKey()) {
			upper = sTable.lastEntry();
			lower = sTable.lowerEntry(upper.getKey());
		} else {
			lower = sTable.floorEntry(key);
			upper = sTable.ceilingEntry(key);
		}

		if (lower.getKey().equals(upper.getKey())) {
			return lower.getValue();
		}

		return getLinearInterpolation(rawValue, lower, upper);
	}

	@Override
	public String formatValue(double convertedValue) {
		return String.format(Locale.getDefault(), "%1$.1f°F", convertedValue);
	}

	private static double getLinearInterpolation(double value, Entry<Double, Double> lower, Entry<Double, Double> upper) {
		final double xLower = lower.getKey();
		final double yLower = lower.getValue();
		final double dx = upper.getKey() - xLower;
		final double dy = upper.getValue() - yLower;
		final double slope = dy / dx;
		final double intercept = yLower - (slope * xLower);

		return slope * value + intercept;
	}

	private static final TreeMap<Double, Double> sTable;

	static {
		sTable = new TreeMap<>();
		sTable.put(22.809865, -5.0);
		sTable.put(27.782946, 2.0);
		sTable.put(33.456243, 8.0);
		sTable.put(40.554455, 14.0);
		sTable.put(48.228764, 20.0);
		sTable.put(57.981800, 27.0);
		sTable.put(68.348033, 32.0);
		sTable.put(83.227866, 39.0);
		sTable.put(101.673759, 47.0);
		sTable.put(123.586207, 56.0);
		sTable.put(147.413882, 61.0);
		sTable.put(182.624204, 70.0);
		sTable.put(217.212121, 78.0);
		sTable.put(261.844749, 85.0);
		sTable.put(311.652174, 93.0);
		sTable.put(372.363636, 102.0);
		sTable.put(436.076046, 109.0);
		sTable.put(514.295964, 118.0);
		sTable.put(594.238342, 126.0);
		sTable.put(703.607362, 135.0);
		sTable.put(831.072464, 145.0);
		sTable.put(971.932203, 155.0);
		sTable.put(1113.475728, 164.0);
		sTable.put(1303.272727, 176.0);
		sTable.put(1470.358974, 186.0);
		sTable.put(1662.144928, 196.0);
		sTable.put(1849.806452, 207.0);
		sTable.put(2048.000000, 219.0);
		sTable.put(2226.951456, 230.0);
		sTable.put(2414.484211, 242.0);
		sTable.put(2577.258427, 253.0);
		sTable.put(2763.566265, 266.0);
		sTable.put(2940.717949, 280.0);
		sTable.put(3099.675676, 295.0);
		sTable.put(3230.647887, 309.0);
		sTable.put(3373.176471, 326.0);
		sTable.put(3475.393939, 341.0);
		sTable.put(3572.834891, 357.0);
		sTable.put(3652.484076, 373.0);
		sTable.put(3723.636364, 391.0);
		sTable.put(3778.846787, 407.0);
		sTable.put(3829.315526, 425.0);
		sTable.put(3868.060708, 441.0);
		sTable.put(3907.597956, 464.0);
		sTable.put(3941.168385, 484.0);
		sTable.put(3968.442907, 509.0);
	}

}
