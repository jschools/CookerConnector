package com.schoovello.cookerconnector.db

import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.room.withTransaction
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.schoovello.cookerconnector.data.FirebaseDbInstance
import com.schoovello.cookerconnector.data.FirebaseStreamIdentifier
import com.schoovello.cookerconnector.util.fetchValueSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resumeWithException

object FirebaseSyncManager {

    private val activeSynchronizers = HashMap<FirebaseStreamIdentifier, ManagedSynchronizer>()

    @MainThread
    fun synchronizeLifecycle(lifecycleOwner: LifecycleOwner, streamIdentifier: FirebaseStreamIdentifier) {
        // register lifecycle observer
        lifecycleOwner.lifecycle.addObserver(ActiveStreamObserver(streamIdentifier))
    }

    @MainThread
    fun synchronizeWhenActive(streamIdentifier: FirebaseStreamIdentifier): LiveData<Unit> {
        return object : LiveData<Unit>() {
            override fun onActive() {
                addSyncRequest(streamIdentifier)
                value = Unit
            }

            override fun onInactive() {
                removeSyncRequest(streamIdentifier)
            }
        }
    }

    @MainThread
    fun addSyncRequest(streamIdentifier: FirebaseStreamIdentifier) {
        val managed = activeSynchronizers[streamIdentifier] ?: run {
            // create and start a new synchronizer
            val synchronizer = DbStreamSynchronizer(FirebaseDbInstance.instance, StreamRepository.database, streamIdentifier)
            synchronizer.start()

            val managed = ManagedSynchronizer(synchronizer)
            activeSynchronizers[streamIdentifier] = managed

            managed
        }

        managed.refCount++
    }

    @MainThread
    fun removeSyncRequest(streamIdentifier: FirebaseStreamIdentifier) {
        val managed = activeSynchronizers[streamIdentifier] ?: return

        managed.refCount--

        if (managed.refCount == 0) {
            // stop and remove the synchronizer
            managed.synchronizer.stop()
            activeSynchronizers.remove(streamIdentifier)
        }
    }

    private class ActiveStreamObserver(
        private val streamIdentifier: FirebaseStreamIdentifier
    ) : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        @Keep
        fun onStart() {
            addSyncRequest(streamIdentifier)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        @Keep
        fun onStop() {
            removeSyncRequest(streamIdentifier)
        }
    }

    private class ManagedSynchronizer(
        val synchronizer: DbStreamSynchronizer,
    ) {
        var refCount = 0
    }
}

private class DbStreamSynchronizer(
    private val firebaseDb: FirebaseDatabase,
    private val roomDb: StreamDatabase,
    private val streamIdentifier: FirebaseStreamIdentifier
) : CoroutineScope {

    private val job: Job by lazy { Job() }

    private var started = false

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main.immediate

    fun start() {
        if (started) {
            throw IllegalStateException("Synchronizer was already started")
        }
        started = true

        launch {
            synchronizeAndMonitor()
        }
    }

    fun stop() {
        job.cancel()
    }

    private suspend fun synchronizeAndMonitor() {
        try {
            val streamRef = firebaseDb.reference
                .child("sessionData")
                .child(streamIdentifier.sessionId)
                .child(streamIdentifier.streamId)

            // synchronize data in bulk
            synchronize(streamRef)

            // begin monitoring for changes
            monitorForever(streamRef)
        } catch (e: CancellationException) {
            // normal cancellation: this is not an error
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
        while (coroutineContext.isActive && batchStartTsExclusive != null && batchStartTsExclusive < endTimestampIncl) {
            // synchronize a batch
            val lastTimestampInBatch = synchronizeBatch(streamId, streamRef, batchStartTsExclusive, endTimestampIncl, BATCH_SIZE)

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
        endTimestampIncl: Long,
        maxBatchSize: Int
    ): Long? {
        Log.d("ME", "Synchronize batch start: $startTimestampExclusive end: $endTimestampIncl")

        // create query for all data points starting after startTimestampExclusive, limiting to BATCH_SIZE
        val query = streamRef.orderByChild("timeMillis")
            .startAt((startTimestampExclusive + 1).toDouble())
            .endAt(endTimestampIncl.toDouble())
            .limitToFirst(maxBatchSize)

        // fetch from Firebase
        val snapshot = query.fetchValueSnapshot()

        // create Room objects for the results
        val dataPoints = snapshot.children.map {
            convertDataSnapshot(it, streamId)
        }.toList()

        Log.d("ME", "Batch size: ${dataPoints.size}")

        if (dataPoints.isEmpty()) {
            return null
        }

        // insert into Room
        roomDb.withTransaction {
            roomDb.dataPointDao().insertAll(dataPoints)
        }

        // return last timestamp
        return dataPoints.last().timestamp
    }

    private suspend fun findOrInsertStream(): Stream {
        val streamDao = roomDb.streamDao()

        return roomDb.withTransaction {
            // find existing row
            val existingRow = streamDao.find(streamIdentifier.sessionId, streamIdentifier.streamId)
            if (existingRow == null) {
                // create and insert it
                val newRow = Stream(
                    fbSessionId = streamIdentifier.sessionId,
                    fbStreamId = streamIdentifier.streamId
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

    /**
     * Attaches an observer to the Firebase DB and inserts all changes
     */
    private suspend fun monitorForever(
        streamRef: DatabaseReference
    ) {
        Log.d("ME", "monitor: $streamRef")

        // get the max timestamp in the Room db
        val roomStreamRow = findOrInsertStream()
        val streamId = roomStreamRow.rowId
        val maxRoomTimestamp = roomDb.dataPointDao().getLastTimestampForStream(streamId) ?: 0L

        // create query for all data points starting after startTimestampExclusive
        val query = streamRef.orderByChild("timeMillis")
            .startAt((maxRoomTimestamp + 1).toDouble())

        suspendCancellableCoroutine<Unit> { cont ->
            val childListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (cont.isActive) {
                        onNewDataPoint(snapshot, streamId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (cont.isActive) {
                        cont.resumeWithException(error.toException())
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    // don't care. children are required to be immutable
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    // don't care. children are required to be immutable
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // don't care. children are required to be immutable
                }
            }

            // attach observer
            query.addChildEventListener(childListener)

            cont.invokeOnCancellation {
                // remove observer
                query.removeEventListener(childListener)
            }
        }
    }

    private fun convertDataSnapshot(childSnapshot: DataSnapshot, streamId: Long): DataPoint {
        return DataPoint(
            timestamp = (childSnapshot.child("timeMillis").value as Number).toLong(),
            calibratedValue = (childSnapshot.child("calibratedValue").value as Number).toFloat(),
            streamId = streamId
        )
    }

    private fun onNewDataPoint(childSnapshot: DataSnapshot, streamId: Long) {
        launch {
            val dataPoint = convertDataSnapshot(childSnapshot, streamId)
            roomDb.dataPointDao().insert(dataPoint)

            Log.d("ME", "Inserted data point ts: ${dataPoint.timestamp} streamId: $streamId")
        }
    }

    companion object {
        private const val BATCH_SIZE = 1024
    }
}