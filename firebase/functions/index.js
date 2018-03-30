const ALARM_TYPE_EXCEEDS = 'GREATER_OR_EQUAL';
const ALARM_TYPE_FALLS_BELOW = 'LESSER_OR_EQUAL';

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp(functions.config().firebase);

exports.alarmMonitor = functions.database
		.ref('sessions/{sessionId}/dataStreams/{dataStreamId}/alarms/{alarmId}/active')
		.onUpdate((deltaSnapshot) => {
			var previousSnapshot = deltaSnapshot.data.previous;
			var currentSnapshot = deltaSnapshot.data;

			// fail if the data or previous data doesn't exist
			if (!previousSnapshot.exists() || !currentSnapshot.exists()) {
				console.log('Previous or current data is missing');
				return null;
			}

			var previous = previousSnapshot.val();
			var current = currentSnapshot.val();

			if (!previous && current) {
				// the alarm has switched from inactive to active. send a notification.
				var sessionId = deltaSnapshot.params.sessionId;
				var dataStreamId = deltaSnapshot.params.dataStreamId;
				var alarmId = deltaSnapshot.params.alarmId;

				return lookUpSession(sessionId).then((session) => {
					return sendNotification(sessionId, dataStreamId, alarmId, session);
				});
			} else {
				return null;
			}
		});

// returns a promise
function lookUpSession(sessionId) {
	return admin.database()
								.ref('sessions').child(sessionId)
								.once('value').then((dataSnapshot) => {
									return dataSnapshot.val();
								});
}

// returns a promise
function sendNotification(sessionId, dataStreamId, alarmId, session) {
	// get the dataStream and alarm objects from the session
	var dataStream = session.dataStreams[dataStreamId];
	var alarm = dataStream.alarms[alarmId];

	// check for a push token
	var pushToken = alarm.pushToken;
	if (pushToken === null || pushToken.length === 0) {
		// token is missing. can't push.
		console.log('Alarm', alarmId, 'missing pushToken');
		return null;
	}

	// determine the verb
	var action;
	if (alarm.type === ALARM_TYPE_EXCEEDS) {
		action = "exceeded";
	} else if (alarm.type === ALARM_TYPE_FALLS_BELOW) {
		action = "fell below";
	} else {
		console.log('Unknown alarm type:', alarm.type);
		action = alarm.type;
	}

	// create the message
	var title = `${dataStream.title} ${action} ${alarm.calibratedThreshold}`;
	var body = session.title;

	var message = {
		data: {
			sessionId: sessionId,
			dataStreamId: dataStreamId,
			alarmId: alarmId,
			alarm: JSON.stringify(alarm)
		},
		android: {
			priority: 'high',
			notification: {
				title: title,
				body: body,
				icon: 'ic_notifications_active_white_24dp',
				color: '#B71C1C',
				sound: 'default',
				tag: 'alarm:' + alarmId
			}
		},
		token: pushToken
	};

	return admin.messaging().send(message).then((response) => {
		console.log('Successfully sent message for alarmId:', alarmId);
		return null;
	}).catch((error) => {
		console.log('Error sending message for alarmId:', alarmId, error);
		return null;
	});
}
