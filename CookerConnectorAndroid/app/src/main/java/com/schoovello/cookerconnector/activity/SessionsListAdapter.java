package com.schoovello.cookerconnector.activity;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.Query;
import com.schoovello.cookerconnector.R;
import com.schoovello.cookerconnector.data.FirebaseDbInstance;
import com.schoovello.cookerconnector.model.SessionInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionsListAdapter extends FirebaseRecyclerAdapter<SessionInfo, SessionsListAdapter.ViewHolder> {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d, yyyy - h:mm a z", Locale.getDefault());

	private Context mContext;
	private OnSessionSelectedListener mListener;

	private Date mTempDate = new Date();

	private static FirebaseRecyclerOptions<SessionInfo> buildOptions(LifecycleOwner lifecycleOwner) {
		Query query = FirebaseDbInstance.get()
				.getReference("sessions");

		return new FirebaseRecyclerOptions.Builder<SessionInfo>()
				.setQuery(query, SessionInfo.class)
				.setLifecycleOwner(lifecycleOwner)
				.build();
	}

	public SessionsListAdapter(Context context, LifecycleOwner lifecycleOwner, OnSessionSelectedListener listener) {
		super(buildOptions(lifecycleOwner));
		mContext = context;
		mListener = listener;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(mContext).inflate(R.layout.session_list_item, parent, false);
		return new ViewHolder(v);
	}

	@Override
	protected void onBindViewHolder(ViewHolder holder, int position, SessionInfo model) {
		mTempDate.setTime(model.getStartTimeMillis());
		holder.date.setText(DATE_FORMAT.format(mTempDate));
		holder.title.setText(model.getTitle());
	}

	public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

		public final View clickReceiver;
		public final TextView date;
		public final TextView title;

		public ViewHolder(View v) {
			super(v);

			clickReceiver = v.findViewById(R.id.clickReceiver);
			date = v.findViewById(R.id.recordingDate);
			title = v.findViewById(R.id.recordingTitle);

			clickReceiver.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			if (mListener != null) {
				int adapterPosition = getAdapterPosition();
				mListener.onSessionSelected(getRef(adapterPosition).getKey(), getItem(adapterPosition));
			}
		}
	}

	public interface OnSessionSelectedListener {
		void onSessionSelected(@NonNull String id, @NonNull SessionInfo info);
	}

}
