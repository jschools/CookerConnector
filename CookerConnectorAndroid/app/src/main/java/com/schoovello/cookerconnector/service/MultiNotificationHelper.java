package com.schoovello.cookerconnector.service;

import android.app.Notification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MultiNotificationHelper {

	private final int mNotificationId;

	private String mForegroundTag;

	private final Set<String> mOtherNotifications = new HashSet<>();

	private final Map<String, Notification> mNotificationsToAdd = new HashMap<>();
	private final Set<String> mTagsToRemove = new HashSet<>();

	public MultiNotificationHelper(int notificationId) {
		mNotificationId = notificationId;
	}

	@NonNull
	public Set<String> getAllTags() {
		final Set<String> result = new HashSet<>();

		if (mForegroundTag != null) {
			result.add(mForegroundTag);
		}
		result.addAll(mOtherNotifications);

		return result;
	}

	public void put(@NonNull String tag, @NonNull Notification notification) {
		mNotificationsToAdd.put(tag, notification);
		mTagsToRemove.remove(tag);
	}

	public void remove(@NonNull String tag) {
		mNotificationsToAdd.remove(tag);
		mTagsToRemove.add(tag);
	}

	@Nullable
	public Notification sync(@NonNull NotificationManagerCompat notificationManager) {
		// first remove
		for (String id : mTagsToRemove) {
			if (id.equals(mForegroundTag)) {
				mForegroundTag = null;
			} else {
				// cancel the notification
				notificationManager.cancel(id, mNotificationId);

				// remove from the Others list
				mOtherNotifications.remove(id);
			}
		}

		// then add
		Notification foregroundNotification = null;
		for (Map.Entry<String, Notification> entry : mNotificationsToAdd.entrySet()) {
			final String tag = entry.getKey();
			final Notification notification = entry.getValue();

			if (mForegroundTag == null) {
				// if there is no foreground notification, then this one will be the new foreground one
				mForegroundTag = tag;
			}

			if (tag.equals(mForegroundTag)) {
				// if this is the current foreground notification, then save it for later
				foregroundNotification = notification;
			} else {
				notificationManager.notify(tag, mNotificationId, notification);
				mOtherNotifications.add(tag);
			}
		}

		// we are synchronized now
		mNotificationsToAdd.clear();
		mTagsToRemove.clear();

		return foregroundNotification;
	}

}
