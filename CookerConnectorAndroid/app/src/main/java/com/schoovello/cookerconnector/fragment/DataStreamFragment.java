package com.schoovello.cookerconnector.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.schoovello.cookerconnector.R;
import com.schoovello.cookerconnector.activity.AlertsListActivity;
import com.schoovello.cookerconnector.data.BackgroundDataProcessor;
import com.schoovello.cookerconnector.data.FirebaseDbInstance;
import com.schoovello.cookerconnector.data.SessionInfoLiveData;
import com.schoovello.cookerconnector.datamodels.SessionDataStream;
import com.schoovello.cookerconnector.model.SessionInfo;
import com.schoovello.cookerconnector.model.TimeDataPoint;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class DataStreamFragment extends Fragment implements OnClickListener {

	private static final String ARG_SESSION_ID = "sessionId";
	private static final String ARG_DATA_STREAM_ID = "dataStreamId";

	private String mSessionId;
	private String mDataStreamId;
	private Long mStartTimeMillis;

	private SessionInfoLiveData mSessionInfoLiveData;
	private DatabaseReference mDataRef;

	private Views mViews;

	private static final class Views {

		final LineChart chart;
		LineDataSet dataSet;
		LineData lineData;
		final TextView streamTitle;
		final TextView streamValue;
		final View alertsButton;

		Views(View v) {
			chart = v.findViewById(R.id.chart);
			streamTitle = v.findViewById(R.id.streamTitle);
			streamValue = v.findViewById(R.id.streamValue);
			alertsButton = v.findViewById(R.id.alertsButton);
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		mSessionId = args.getString(ARG_SESSION_ID);
		mDataStreamId = args.getString(ARG_DATA_STREAM_ID);

		mSessionInfoLiveData = new SessionInfoLiveData(mSessionId);
		mSessionInfoLiveData.observe(this, mSessionInfoObserver);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.frag_data_stream, container, false);
		mViews = new Views(v);

		mViews.alertsButton.setOnClickListener(this);

		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		mViews = null;
	}

	@Override
	public void onStart() {
		super.onStart();

		mViews.dataSet = new LineDataSet(new ArrayList<Entry>(), "Temperature");
		final LineDataSet dataSet = mViews.dataSet;
		mViews.lineData = new LineData(dataSet);

		dataSet.setAxisDependency(AxisDependency.LEFT);
		dataSet.setDrawCircles(false);
		dataSet.setColor(ColorTemplate.getHoloBlue());
		dataSet.setLineWidth(2f);
		dataSet.setHighLightColor(Color.rgb(244, 117, 117));
		dataSet.setDrawValues(false);
		mViews.chart.setAutoScaleMinMaxEnabled(true);

		mBackgroundDataProcessor.start();

		mDataRef = FirebaseDbInstance.INSTANCE.getInstance()
				.getReference("sessionData")
				.child(mSessionId)
				.child(mDataStreamId);
		mDataRef.addChildEventListener(mDataChildListener);
	}

	@Override
	public void onStop() {
		super.onStop();

		if (mDataRef != null) {
			mDataRef.removeEventListener(mDataChildListener);
		}

		mBackgroundDataProcessor.stopAndClearUndeliveredResults();
	}

	private void updateViews() {
		if (mViews == null) {
			return;
		}

		final SessionDataStream dataStreamInfo = getDataStreamInfo();
		final String title = dataStreamInfo != null ? dataStreamInfo.getTitle() : "Temperature";

		mViews.streamTitle.setText(title);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.alertsButton) {
			SessionDataStream dataStreamInfo = getDataStreamInfo();
			String title = dataStreamInfo != null ? dataStreamInfo.getTitle() : getString(R.string.temperature);
			Intent intent = AlertsListActivity.buildIntent(mSessionId, mDataStreamId, title);
			getActivity().startActivity(intent);
		}
	}

	@Nullable
	private SessionDataStream getDataStreamInfo() {
		SessionInfo session = mSessionInfoLiveData != null ? mSessionInfoLiveData.getValue() : null;
		Map<String, SessionDataStream> dataStreams = session != null ? session.getDataStreams() : null;
		return dataStreams != null ? dataStreams.get(mDataStreamId) : null;
	}

	private Observer<SessionInfo> mSessionInfoObserver = new Observer<SessionInfo>() {
		@Override
		public void onChanged(@Nullable SessionInfo sessionInfo) {
			updateViews();
		}
	};

	private void addDataPoint(TimeDataPoint value, boolean notify) {
		if (mStartTimeMillis == null) {
			mStartTimeMillis = value.getTimeMillis();
		}

		float x = (value.getTimeMillis() - mStartTimeMillis) / 60_000f;
		final Entry entry = new Entry(x, (float) value.getCalibratedValue());
		mViews.dataSet.addEntry(entry);

		if (mViews.dataSet.getEntryCount() == 2) {
			mViews.chart.setData(mViews.lineData);
		}

		if (notify) {
			notifyDataUpdated();
		}

		String formattedValue = String.format(Locale.getDefault(), "%1$.1f°F", value.getCalibratedValue());
		mViews.streamValue.setText(formattedValue);
	}

	private void notifyDataUpdated() {
		if (mViews == null) {
			return;
		}

		mViews.dataSet.notifyDataSetChanged();
		mViews.chart.notifyDataSetChanged();
		mViews.chart.invalidate();
	}

	private ChildEventListener mDataChildListener = new ChildEventListener() {
		@Override
		public void onChildAdded(DataSnapshot dataSnapshot, String s) {
			if (dataSnapshot != null) {
				mBackgroundDataProcessor.enqueueData(dataSnapshot);
			}
		}

		@Override
		public void onChildChanged(DataSnapshot dataSnapshot, String s) {

		}

		@Override
		public void onChildRemoved(DataSnapshot dataSnapshot) {

		}

		@Override
		public void onChildMoved(DataSnapshot dataSnapshot, String s) {

		}

		@Override
		public void onCancelled(DatabaseError databaseError) {

		}
	};

	private BackgroundDataProcessor<DataSnapshot, TimeDataPoint> mBackgroundDataProcessor =
			new BackgroundDataProcessor<DataSnapshot, TimeDataPoint>(Looper.getMainLooper()) {
				@Override
				protected TimeDataPoint processDataInBackground(@NonNull DataSnapshot snapshot) {
					return snapshot.getValue(TimeDataPoint.class);
				}

				@Override
				public void onDataProcessed(@NonNull TimeDataPoint processedResult) {
					addDataPoint(processedResult, false);
				}

				@Override
				protected void onBatchCompleted() {
					notifyDataUpdated();
				}
			};

	public static DataStreamFragment newInstance(String sessionId, String dataStreamId) {
		Bundle args = new Bundle();
		args.putString(ARG_SESSION_ID, sessionId);
		args.putString(ARG_DATA_STREAM_ID, dataStreamId);
		DataStreamFragment frag = new DataStreamFragment();
		frag.setArguments(args);
		return frag;
	}

}
