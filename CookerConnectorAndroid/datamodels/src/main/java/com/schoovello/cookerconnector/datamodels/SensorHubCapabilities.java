package com.schoovello.cookerconnector.datamodels;

import java.util.Map;

public class SensorHubCapabilities {

	private int channelCount;

	private Map<String, ConversionFunctionCapability> conversionFunctions;

	public int getChannelCount() {
		return channelCount;
	}

	public void setChannelCount(int channelCount) {
		this.channelCount = channelCount;
	}

	public Map<String, ConversionFunctionCapability> getConversionFunctions() {
		return conversionFunctions;
	}

	public void setConversionFunctions(Map<String, ConversionFunctionCapability> conversionFunctions) {
		this.conversionFunctions = conversionFunctions;
	}

	public static class ConversionFunctionCapability {

		private String description;
		private String name;

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
