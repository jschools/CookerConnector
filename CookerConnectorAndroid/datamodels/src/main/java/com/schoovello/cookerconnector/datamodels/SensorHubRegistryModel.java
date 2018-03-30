package com.schoovello.cookerconnector.datamodels;

public class SensorHubRegistryModel {

	private String name;
	private String model;
	private String description;
	private SensorHubCapabilities capabilities;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public SensorHubCapabilities getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(SensorHubCapabilities capabilities) {
		this.capabilities = capabilities;
	}
}
