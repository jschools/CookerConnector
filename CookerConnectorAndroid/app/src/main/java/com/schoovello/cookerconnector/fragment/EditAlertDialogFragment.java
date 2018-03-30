package com.schoovello.cookerconnector.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.schoovello.cookerconnector.R;
import com.schoovello.cookerconnector.data.FirebaseDbInstance;
import com.schoovello.cookerconnector.datamodels.AlarmSpec;
import com.schoovello.cookerconnector.model.EditAlertViewModel;

public class EditAlertDialogFragment extends AppCompatDialogFragment implements OnClickListener,
		OnCheckedChangeListener, OnSeekBarChangeListener, TextWatcher {

	private static final String ARG_SESSION_ID = "sessionId";
	private static final String ARG_DATA_STREAM_ID = "dataStreamId";
	private static final String ARG_DATA_STREAM_TITLE = "dataStreamTitle";
	private static final String ARG_ALARM_ID = "alarmId";

	private static final int MAX_TEMP_F = 550;
	private static final int MIN_TEMP_F = -50;

	private String mSessionId;
	private String mDataStreamId;
	private String mDataStreamTitle;
	private String mAlarmId;

	private EditAlertViewModel mViewModel;

	private Views mViews;

	private static class Views {
		final TextView channelName;
		final RadioGroup directionGroup;
		final RadioButton exceedsButton;
		final RadioButton fallsBelowButton;
		final SeekBar thresholdSeekBar;
		final EditText thresholdInput;
		final View loading;

		Views(View v) {
			channelName = v.findViewById(R.id.channelName);
			directionGroup = v.findViewById(R.id.directionGroup);
			exceedsButton = v.findViewById(R.id.exceedsButton);
			fallsBelowButton = v.findViewById(R.id.fallsBelowButton);
			thresholdSeekBar = v.findViewById(R.id.thresholdSeekBar);
			thresholdInput = v.findViewById(R.id.thresholdInput);
			loading = v.findViewById(R.id.loading);
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		mSessionId = args.getString(ARG_SESSION_ID);
		mDataStreamId = args.getString(ARG_DATA_STREAM_ID);
		mDataStreamTitle = args.getString(ARG_DATA_STREAM_TITLE);
		mAlarmId = args.getString(ARG_ALARM_ID);

		mViewModel = ViewModelProviders.of(this).get(EditAlertViewModel.class);
		mViewModel.getAlarmSpecLiveData(mSessionId, mDataStreamId, mAlarmId).observe(this, new Observer<AlarmSpec>() {
			@Override
			public void onChanged(@Nullable AlarmSpec alarmSpec) {
				MutableLiveData<Boolean> loadedLiveData = mViewModel.getLoaded();
				Boolean loadedState = loadedLiveData.getValue();
				if (loadedState != null && loadedState) {
					return;
				}

				if (alarmSpec == null) {
					mViewModel.getExceedsSelected().setValue(true);
					mViewModel.getThresholdFahrenheit().setValue(250);
				} else {
					String type = alarmSpec.getType();
					mViewModel.getExceedsSelected().setValue(type.equals(AlarmSpec.TYPE_GREATER_OR_EQUAL));
					mViewModel.getThresholdFahrenheit().setValue(alarmSpec.getCalibratedThreshold());
				}

				loadedLiveData.setValue(true);
			}
		});

		mViewModel.getLoaded().observe(this, new Observer<Boolean>() {
			@Override
			public void onChanged(@Nullable Boolean value) {
				boolean loaded = value != null && value.booleanValue();
				mViews.loading.setVisibility(loaded ? View.GONE : View.VISIBLE);
				mViews.thresholdInput.setEnabled(loaded);
				mViews.thresholdSeekBar.setEnabled(loaded);
			}
		});

		mViewModel.getExceedsSelected().observe(this, new Observer<Boolean>() {
			@Override
			public void onChanged(@Nullable Boolean value) {
				if (value == null) {
					return;
				}

				mViews.directionGroup.check(value ? R.id.exceedsButton : R.id.fallsBelowButton);
			}
		});

		mViewModel.getThresholdFahrenheit().observe(this, new Observer<Integer>() {
			@SuppressLint("SetTextI18n")
			@Override
			public void onChanged(@Nullable Integer value) {
				if (value == null) {
					return;
				}

				final int range = MAX_TEMP_F - MIN_TEMP_F;
				final int progress = value - MIN_TEMP_F;

				if (mViews.thresholdSeekBar.getMax() != range || mViews.thresholdSeekBar.getProgress() != progress) {
					mViews.thresholdSeekBar.setMax(range);
					mViews.thresholdSeekBar.setProgress(progress);
				}

				final String valueString = value.toString();
				if (!mViews.thresholdInput.getText().toString().equals(valueString)) {
					mViews.thresholdInput.setText(valueString);
				}
			}
		});
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		final LayoutInflater inflater = LayoutInflater.from(builder.getContext());

		final View inflatedView = inflater.inflate(R.layout.dialog_configure_alert, null, false);
		mViews = new Views(inflatedView);

		builder.setTitle("Configure Alert")
				.setView(inflatedView)
				.setPositiveButton("Save", this)
				.setNegativeButton("Cancel", this);

		mViews.channelName.setText(mDataStreamTitle);
		mViews.directionGroup.setOnCheckedChangeListener(this);
		mViews.thresholdSeekBar.setOnSeekBarChangeListener(this);
		mViews.thresholdInput.addTextChangedListener(this);

		return builder.create();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		mViews = null;
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		mViewModel.getExceedsSelected().setValue(checkedId == R.id.exceedsButton);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		try {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				FirebaseDbInstance.get()
						.getReference("sessions").child(mSessionId)
						.child("dataStreams").child(mDataStreamId)
						.child("alarms").child(mAlarmId)
						.setValue(buildAlarmSpec());
			}
		} catch (Throwable t) {
			Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_LONG).show();
		}

		dismissAllowingStateLoss();
	}

	private AlarmSpec buildAlarmSpec() {
		final AlarmSpec spec = new AlarmSpec();

		spec.setActive(false);
		Integer thresholdF = mViewModel.getThresholdFahrenheit().getValue();
		Boolean exceedsSelected = mViewModel.getExceedsSelected().getValue();

		if (thresholdF == null || exceedsSelected == null) {
			throw new IllegalStateException("Alarm data not ready");
		}
		spec.setType(exceedsSelected ? AlarmSpec.TYPE_GREATER_OR_EQUAL : AlarmSpec.TYPE_LESSER_OR_EQUAL);
		spec.setCalibratedThreshold(thresholdF);

		spec.setPushToken(FirebaseInstanceId.getInstance().getToken());

		return spec;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			mViewModel.getThresholdFahrenheit().setValue(MIN_TEMP_F + progress);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void afterTextChanged(Editable s) {
		try {
			mViewModel.getThresholdFahrenheit().setValue(Integer.parseInt(s.toString()));
		} catch (NumberFormatException e) {
			// junk input is ignored
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// don't care
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// don't care
	}

	public static EditAlertDialogFragment newInstance(@NonNull String sessionId,
													  @NonNull String dataStreamId,
													  @NonNull String dataStreamTitle,
													  @NonNull String alarmId) {
		Bundle args = new Bundle();
		args.putString(ARG_SESSION_ID, sessionId);
		args.putString(ARG_DATA_STREAM_ID, dataStreamId);
		args.putString(ARG_DATA_STREAM_TITLE, dataStreamTitle);
		args.putString(ARG_ALARM_ID, alarmId);

		EditAlertDialogFragment frag = new EditAlertDialogFragment();
		frag.setArguments(args);
		return frag;
	}

}
