package com.schoovello.cookerconnector.fragment;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.schoovello.cookerconnector.R;
import com.schoovello.cookerconnector.data.FirebaseDbInstance;
import com.schoovello.cookerconnector.data.SensorHubRegistryLiveData;
import com.schoovello.cookerconnector.datamodels.DataStreamConfig;
import com.schoovello.cookerconnector.datamodels.SensorHubCapabilities;
import com.schoovello.cookerconnector.datamodels.SensorHubRegistryModel;
import com.schoovello.cookerconnector.fragment.NewSessionFragment.DataStreamSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SensorHubConfigFragment extends Fragment implements OnClickListener, OnCheckedChangeListener {

	private static final String ARG_SENSOR_HUB_ID = "sensorHubId";

	private static final String KEY_ENABLED_STREAM_SPECS = "enabledStreamSpecs";
	private static final String KEY_EXPANDED = "expanded";

	private String mSensorHubId;
	private SensorHubRegistryLiveData mRegistryLiveData;
	private boolean mExpanded;

	@Nullable
	private SensorHubRegistryModel mRegistryModel;

	private SparseArray<DataStreamSpec> mEnabledStreamSpecs;

	private Views mViews;

	private static final class Views {
		final TextView sensorHubName;
		final TextView activeChannelCount;
		final ImageButton toggleButton;
		final ViewGroup channelContainer;

		Views(@NonNull View v) {
			sensorHubName = v.findViewById(R.id.sensorHubName);
			activeChannelCount = v.findViewById(R.id.activeChannelCount);
			toggleButton = v.findViewById(R.id.sensorHubToggleButton);
			channelContainer = v.findViewById(R.id.channelContainer);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putSparseParcelableArray(KEY_ENABLED_STREAM_SPECS, mEnabledStreamSpecs);
		outState.putBoolean(KEY_EXPANDED, mExpanded);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Bundle args = getArguments();
		mSensorHubId = args.getString(ARG_SENSOR_HUB_ID);

		if (savedInstanceState != null) {
			mEnabledStreamSpecs = savedInstanceState.getSparseParcelableArray(KEY_ENABLED_STREAM_SPECS);
			mExpanded = savedInstanceState.getBoolean(KEY_EXPANDED);
		} else {
			mEnabledStreamSpecs = new SparseArray<>();
			mExpanded = false;
		}

		mRegistryLiveData = new SensorHubRegistryLiveData(FirebaseDbInstance.get(), mSensorHubId);
		mRegistryLiveData.observe(this, mSensorHubRegistryObserver);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_new_session_sensorhub, container, false);
		mViews = new Views(v);

		mViews.toggleButton.setOnClickListener(this);

		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		mViews = null;
	}

	public List<DataStreamSpec> getEnabledStreamSpecs() {
		final int count = mEnabledStreamSpecs.size();
		final List<DataStreamSpec> result = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			final DataStreamSpec spec = mEnabledStreamSpecs.valueAt(i);
			result.add(spec);
		}

		return result;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.sensorHubToggleButton:
				mExpanded = !mExpanded;
				updateChannelViews();
				break;
			default:
				break;
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		final Integer tag = (Integer) buttonView.getTag();
		if (tag == null) {
			return;
		}

		final int channelIdx = tag.intValue();
		final DataStreamSpec existingSpec = mEnabledStreamSpecs.get(channelIdx);

		if (isChecked) {
			if (existingSpec == null) {
				final String streamName = "Channel " + channelIdx;

				final DataStreamConfig dataStreamConfig = new DataStreamConfig();
				dataStreamConfig.setChannel(channelIdx);
				dataStreamConfig.setConversionFunction("TEMPERATURE_PROBE_DOT_5600_OHM_F");
				dataStreamConfig.setMeasurementIntervalMs(15_000);
				dataStreamConfig.setPaused(true);

				DataStreamSpec newSpec = new DataStreamSpec(channelIdx, streamName, dataStreamConfig);
				mEnabledStreamSpecs.put(channelIdx, newSpec);
			}
		} else {
			mEnabledStreamSpecs.remove(channelIdx);
		}

		updateChannelViews();
	}

	private Observer<SensorHubRegistryModel> mSensorHubRegistryObserver = new Observer<SensorHubRegistryModel>() {
		@Override
		public void onChanged(@Nullable SensorHubRegistryModel sensorHubRegistryModel) {
			mRegistryModel = sensorHubRegistryModel;
			if (mRegistryModel == null) {
				return;
			}

			onRegistryModelChanged();
		}
	};

	private void onRegistryModelChanged() {
		if (mRegistryModel == null) {
			return;
		}
		final SensorHubCapabilities capabilities = mRegistryModel.getCapabilities();
		if (capabilities == null) {
			return;
		}

		// remove any specs for channels that don't exist anymore
		final int channelCount = capabilities.getChannelCount();
		int idx = 0;
		while (idx < mEnabledStreamSpecs.size()) {
			final int enabledChannel = mEnabledStreamSpecs.keyAt(idx);
			if (enabledChannel >= channelCount) {
				mEnabledStreamSpecs.removeAt(idx);
			} else {
				idx++;
			}
		}

		updateChannelViews();
	}

	private void updateChannelViews() {
		if (mViews == null || mRegistryModel == null) {
			return;
		}

		mViews.sensorHubName.setText(mRegistryModel.getName());

		if (mExpanded) {
			mViews.channelContainer.setVisibility(View.VISIBLE);
			mViews.toggleButton.setImageResource(R.drawable.ic_expand_less_black_24dp);
		} else {
			mViews.channelContainer.setVisibility(View.GONE);
			mViews.toggleButton.setImageResource(R.drawable.ic_expand_more_black_24dp);
			return;
		}

		ensureCorrectChildCount();

		// at this point, the number of children in the container equals the number of channels
		final ViewGroup container = mViews.channelContainer;
		final int childCount = container.getChildCount();

		// update each child with its model data
		int enabledChannelCount = 0;
		for (int channelIdx = 0; channelIdx < childCount; channelIdx++) {
			final DataStreamSpec channelSpec = mEnabledStreamSpecs.get(channelIdx);
			final View child = container.getChildAt(channelIdx);
			final ChannelViewHolder holder = ((ChannelViewHolder) child.getTag());
			holder.enabledCheckbox.setText("Channel " + channelIdx);

			final Integer tag = Integer.valueOf(channelIdx);
			holder.enabledCheckbox.setTag(tag);

			if (channelSpec == null) {
				holder.enabledCheckbox.setChecked(false);
				holder.enabledDetails.setVisibility(View.GONE);
			} else {
				enabledChannelCount++;

				holder.enabledCheckbox.setChecked(true);
				holder.enabledDetails.setVisibility(View.VISIBLE);

				holder.channelName.setText(channelSpec.streamName);
			}
		}

		// set the enabled channel count
		if (enabledChannelCount > 0) {
			mViews.activeChannelCount.setText(String.format(Locale.getDefault(), "(%1$d enabled)", enabledChannelCount));
		} else {
			mViews.activeChannelCount.setText("");
		}
	}

	private void ensureCorrectChildCount() {
		if (mViews == null || mRegistryModel == null) {
			return;
		}
		final SensorHubCapabilities capabilities = mRegistryModel.getCapabilities();
		if (capabilities == null) {
			return;
		}

		final int channelCount = capabilities.getChannelCount();
		final int existingChildren = mViews.channelContainer.getChildCount();
		if (existingChildren < channelCount) {
			final int childrenToAdd = channelCount - existingChildren;
			for (int i = 0; i < childrenToAdd; i++) {
				addChannelView(mViews.channelContainer.getChildCount());
			}
		} else if (existingChildren > channelCount) {
			mViews.channelContainer.removeViews(channelCount, existingChildren - channelCount);
		}
	}

	private void addChannelView(int channelIdx) {
		if (mViews == null) {
			return;
		}

		final LayoutInflater inflater = LayoutInflater.from(mViews.channelContainer.getContext());
		final View child = inflater.inflate(R.layout.frag_sensorhub_channel_config, mViews.channelContainer, false);
		final ChannelViewHolder holder = new ChannelViewHolder(child);
		child.setTag(holder);

		holder.enabledCheckbox.setOnCheckedChangeListener(this);
		holder.channelName.addTextChangedListener(new ChannelNameWatcher(channelIdx));

		mViews.channelContainer.addView(child);
	}

	private class ChannelNameWatcher implements TextWatcher {

		private final int mChannel;

		public ChannelNameWatcher(int channel) {
			mChannel = channel;
		}

		@Override
		public void afterTextChanged(Editable s) {
			final DataStreamSpec streamSpec = mEnabledStreamSpecs.get(mChannel);
			if (streamSpec != null) {
				streamSpec.streamName = s.toString();
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
	}

	private static final class ChannelViewHolder {
		final CheckBox enabledCheckbox;
		final View enabledDetails;
		final EditText channelName;
		final Spinner functionSpinner;
		final Spinner intervalSpinner;

		ChannelViewHolder(View v) {
			enabledCheckbox = v.findViewById(R.id.enabledCheckbox);
			enabledDetails = v.findViewById(R.id.enabledDetails);
			channelName = v.findViewById(R.id.channelName);
			functionSpinner = v.findViewById(R.id.functionSpinner);
			intervalSpinner = v.findViewById(R.id.intervalSpinner);
		}
	}

	public static SensorHubConfigFragment newInstance(@NonNull String sensorHubId) {
		SensorHubConfigFragment frag = new SensorHubConfigFragment();
		Bundle args = new Bundle();
		args.putString(ARG_SENSOR_HUB_ID, sensorHubId);
		frag.setArguments(args);
		return frag;
	}

}
