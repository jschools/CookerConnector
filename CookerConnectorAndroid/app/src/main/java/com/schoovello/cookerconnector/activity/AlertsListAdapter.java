package com.schoovello.cookerconnector.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.Query;
import com.schoovello.cookerconnector.R;
import com.schoovello.cookerconnector.data.FirebaseDbInstance;
import com.schoovello.cookerconnector.datamodels.AlarmSpec;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AlertsListAdapter extends FirebaseRecyclerAdapter<AlarmSpec, AlertsListAdapter.ViewHolder> {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d, yyyy - h:mm a z", Locale.getDefault());

	private final Context mContext;
	private final String mDataStreamTitle;

	private OnAlertActionListener mListener;

	private static FirebaseRecyclerOptions<AlarmSpec> buildOptions(LifecycleOwner lifecycleOwner, String sessionId, String dataStreamId) {
		Query query = FirebaseDbInstance.get()
				.getReference("sessions").child(sessionId)
				.child("dataStreams").child(dataStreamId)
				.child("alarms");

		return new FirebaseRecyclerOptions.Builder<AlarmSpec>()
				.setQuery(query, AlarmSpec.class)
				.setLifecycleOwner(lifecycleOwner)
				.build();
	}

	public AlertsListAdapter(Context context, LifecycleOwner lifecycleOwner, String sessionId,
							 String dataStreamId, String dataStreamTitle, OnAlertActionListener listener) {
		super(buildOptions(lifecycleOwner, sessionId, dataStreamId));
		mContext = context;
		mDataStreamTitle = dataStreamTitle;
		mListener = listener;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(mContext).inflate(R.layout.alert_list_item, parent, false);
		return new ViewHolder(v);
	}

	@Override
	protected void onBindViewHolder(ViewHolder holder, int position, AlarmSpec model) {
		@StringRes int formatResId;
		if (model.getType().equals(AlarmSpec.TYPE_GREATER_OR_EQUAL)) {
			formatResId = R.string.alert_name_exceeds_f;
		} else {
			formatResId = R.string.alert_name_fallsbelow_f;
		}

		String alertSummary = mContext.getString(formatResId, mDataStreamTitle, model.getCalibratedThreshold());
		holder.alertTitle.setText(alertSummary);
	}

	public class ViewHolder extends RecyclerView.ViewHolder implements OnClickListener {

		final View clickReceiver;
		final TextView alertTitle;
		final View deleteButton;

		ViewHolder(View v) {
			super(v);

			clickReceiver = v.findViewById(R.id.clickReceiver);
			alertTitle = v.findViewById(R.id.alertTitle);
			deleteButton = v.findViewById(R.id.deleteButton);

			clickReceiver.setOnClickListener(this);
			deleteButton.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			if (mListener != null) {
				int adapterPosition = getAdapterPosition();
				String alertId = getRef(adapterPosition).getKey();

				if (v == clickReceiver) {
					mListener.onEditAlertSelected(alertId);
				} else if (v == deleteButton) {
					mListener.onDeleteAlertSelected(alertId);
				}
			}
		}
	}

	public interface OnAlertActionListener {
		void onEditAlertSelected(@NonNull String alertId);
		void onDeleteAlertSelected(@NonNull String alertId);
	}

}
