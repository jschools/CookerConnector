package com.schoovello.cookerconnector.model;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.schoovello.cookerconnector.data.AlarmSpecLiveData;
import com.schoovello.cookerconnector.datamodels.AlarmSpec;

public class EditAlertViewModel extends ViewModel {

	private MutableLiveData<Boolean> mLoaded = new MutableLiveData<>();
	private MutableLiveData<Boolean> mExceedsSelected = new MutableLiveData<>();
	private MutableLiveData<Integer> mThresholdFahrenheit = new MutableLiveData<>();
	private LiveData<AlarmSpec> mAlarmSpecLiveData;

	public MutableLiveData<Boolean> getLoaded() {
		return mLoaded;
	}

	public MutableLiveData<Boolean> getExceedsSelected() {
		return mExceedsSelected;
	}

	public MutableLiveData<Integer> getThresholdFahrenheit() {
		return mThresholdFahrenheit;
	}

	public LiveData<AlarmSpec> getAlarmSpecLiveData(@NonNull String sessionId, @NonNull String dataStreamId, @NonNull String alarmId) {
		if (mAlarmSpecLiveData == null) {
			mAlarmSpecLiveData = new AlarmSpecLiveData(sessionId, dataStreamId, alarmId);
		}
		return mAlarmSpecLiveData;
	}
}
