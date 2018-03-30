package com.schoovello.cookerconnector.sensorhub.registry;

import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.schoovello.cookerconnector.datamodels.SensorHubRegistryModel;
import com.schoovello.cookerconnector.sensorhub.BuildConfig;
import com.schoovello.cookerconnector.sensorhub.R;
import com.schoovello.cookerconnector.sensorhub.SensorHubApplication;
import com.schoovello.cookerconnector.sensorhub.repository.FirebaseDbInstance;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class SensorHubRegistryInitializer implements Observer<SensorHubRegistryModel> {

	private final SensorHubIdentity mIdentity;

	private RegistryLiveData mLiveData;

	public SensorHubRegistryInitializer() {
		mIdentity = SensorHubIdentity.getInstance();
	}

	public void onCreate() {
		mLiveData = new RegistryLiveData(mIdentity);
		mLiveData.observeForever(this);
	}

	public void onDestroy() {
		mLiveData.removeObserver(this);
	}

	@Override
	public void onChanged(@Nullable SensorHubRegistryModel sensorHubRegistryModel) {
		if (sensorHubRegistryModel == null) {
			createAndSetDefaultModel();
			onDestroy();
		}
	}

	private void createAndSetDefaultModel() {
		// parse the model from JSON asset
		InputStream inputStream = SensorHubApplication.getInstance().getResources().openRawResource(R.raw.default_registry_model);
		InputStreamReader reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
		SensorHubRegistryModel model = new Gson().fromJson(reader, SensorHubRegistryModel.class);

		String name = model.getName();
		model.setName(appendId(name));

		if (BuildConfig.DEBUG) {
			Log.d("SensorHubRegistry", "I'm a new SensorHub. " + model.getName());
		}

		// save it to the database
		FirebaseDbInstance.get()
				.getReference("sensorHubs")
				.child(mIdentity.getId())
				.child("registry")
				.setValue(model);
	}

	private static final String USER_FRIENDLY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private String appendId(String defaultName) {
		final int hash = mIdentity.getId().hashCode();

		String result = defaultName + " ";
		result += USER_FRIENDLY_CHARS.charAt(Math.abs(hash >>> 0) % USER_FRIENDLY_CHARS.length());
		result += USER_FRIENDLY_CHARS.charAt(Math.abs(hash >>> 8) % USER_FRIENDLY_CHARS.length());
		result += USER_FRIENDLY_CHARS.charAt(Math.abs(hash >>> 16) % USER_FRIENDLY_CHARS.length());
		result += USER_FRIENDLY_CHARS.charAt(Math.abs(hash >>> 24) % USER_FRIENDLY_CHARS.length());

		return result;
	}
}
