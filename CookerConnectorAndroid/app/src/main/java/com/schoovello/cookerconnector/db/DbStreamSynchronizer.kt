package com.schoovello.cookerconnector.db

import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.database.*
import com.schoovello.cookerconnector.util.fetchValueSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DbStreamSynchronizer(
    private val coroutineScope: CoroutineScope,
    private val firebaseDb: FirebaseDatabase,
    private val roomDb: StreamDatabase,
    private val fbSessionId: String,
    private val fbStreamId: String
) {

    fun start() {
        coroutineScope.launch {
            synchronizeAndMonitor()
        }
    }

    private suspend fun synchronizeAndMonitor() {
        try {
            val streamRef = firebaseDb.reference
                .child("sessionData")
                .child(fbSessionId)
                .child(fbStreamId)

            // synchronize data in bulk
            synchronize(streamRef)

            // begin monitoring for changes
            monitor()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private suspend fun synchronize(streamRef: DatabaseReference) {
        Log.d("ME", "Synchronize: $streamRef")

        // get the max timestamp in the Firebase db
        val maxFbTimestamp = fetchMaxTimestampFromFirebase(streamRef)

        Log.d("ME", "maxFbTimestamp: $maxFbTimestamp")

        // get the max timestamp in the Room db
        val roomStreamRow = findOrInsertStream()
        val maxRoomTimestamp = roomDb.dataPointDao().getLastTimestampForStream(roomStreamRow.rowId) ?: 0L

        Log.d("ME", "maxRoomTs: $maxRoomTimestamp")

        // pull down data between the timestamps
        if (maxFbTimestamp != null) {
            synchronize(roomStreamRow.rowId, streamRef, maxRoomTimestamp, maxFbTimestamp)
        }
    }

    private suspend fun synchronize(
        streamId: Long,
        streamRef: DatabaseReference,
        startTimestampExclusive: Long,
        endTimestampIncl: Long
    ) {
        Log.d("ME", "Synchronize start: $startTimestampExclusive end: $endTimestampIncl")

        var batchStartTsExclusive: Long? = startTimestampExclusive
        while (batchStartTsExclusive != null && batchStartTsExclusive < endTimestampIncl) {
            // synchronize a batch
            val lastTimestampInBatch = synchronizeBatch(streamId, streamRef, batchStartTsExclusive, endTimestampIncl)

            // next batch starts at the end of the previous batch
            batchStartTsExclusive = lastTimestampInBatch
        }
    }

    /**
     * @return the timestamp of the last inserted data point
     */
    private suspend fun synchronizeBatch(
        streamId: Long,
        streamRef: DatabaseReference,
        startTimestampExclusive: Long,
        endTimestampIncl: Long
    ): Long? {
        Log.d("ME", "Synchronize batch start: $startTimestampExclusive end: $endTimestampIncl")

        // create query for all data points starting after startTimestampExclusive, limiting to BATCH_SIZE
        val query = streamRef.orderByChild("timeMillis")
            .startAt((startTimestampExclusive + 1).toDouble())
            .endAt(endTimestampIncl.toDouble())
            .limitToFirst(BATCH_SIZE)

        // fetch from Firebase
        val snapshot = query.fetchValueSnapshot()

        // create Room objects for the results
        val dataPoints = snapshot.children.map { childSnapshot ->
            DataPoint(
                timestamp = (childSnapshot.child("timeMillis").value as Number).toLong(),
                calibratedValue = (childSnapshot.child("calibratedValue").value as Number).toFloat(),
                streamId = streamId
            )
        }.toList()

        Log.d("ME", "Batch size: ${dataPoints.size}")

        if (dataPoints.isEmpty()) {
            return null
        }

        // insert into Room
        roomDb.dataPointDao().insertAll(dataPoints)

        // return last timestamp
        return dataPoints.last().timestamp
    }

    private suspend fun findOrInsertStream(): Stream {
        val streamDao = roomDb.streamDao()

        return roomDb.withTransaction {
            // find existing row
            val existingRow = streamDao.find(fbSessionId, fbStreamId)
            if (existingRow == null) {
                // create and insert it
                val newRow = Stream(
                    fbSessionId = fbSessionId,
                    fbStreamId = fbStreamId
                )
                val id = streamDao.insert(newRow)

                // the insert produced an auto-id, so the returned result must have that ID
                newRow.copy(rowId = id)
            } else {
                existingRow
            }
        }
    }

    /**
     * Performs a query on the given stream in the Firebase DB and returns its timestamp.
     *
     * @throws DatabaseException if an error occurs
     * @return the timestamp in milliseconds since epoch
     */
    private suspend fun fetchMaxTimestampFromFirebase(streamRef: DatabaseReference): Long? {
        val query = streamRef.orderByChild("timeMillis").limitToLast(1)
        val value = query.fetchValueSnapshot().children.firstOrNull()?.child("timeMillis")?.value

        Log.d("ME", "Fetched $value (${value?.javaClass})")

        return (value as? Number)?.toLong()
    }

    private suspend fun monitor() {

    }

    companion object {
        private const val BATCH_SIZE = 1024
    }
}