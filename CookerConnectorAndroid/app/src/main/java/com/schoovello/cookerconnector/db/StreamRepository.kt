package com.schoovello.cookerconnector.db

import android.util.Log
import androidx.room.Room
import com.schoovello.cookerconnector.CookerConnectorApp
import com.schoovello.cookerconnector.data.FirebaseStreamIdentifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

object StreamRepository {

    private const val DB_NAME = "stream_db"

    val database: StreamDatabase by lazy {
        Room.databaseBuilder(
            CookerConnectorApp.getInstance(),
            StreamDatabase::class.java,
            DB_NAME
        ).build()
    }

    private suspend fun getStreamId(streamIdentifier: FirebaseStreamIdentifier): Long? {
        return database.streamDao().find(streamIdentifier.sessionId, streamIdentifier.streamId)?.rowId
    }

    fun getAverages(
        streamIdentifier: FirebaseStreamIdentifier,
        startTs: Long,
        endTs: Long,
        windowSizeMs: Long
    ): Flow<List<SummarizedDataPoint>> {
        return flow {
            // look up the stream
            val streamId = getStreamId(streamIdentifier)
            if (streamId == null) {
                emit(emptyList())
                return@flow
            }

            val dataDao = database.dataPointDao()
            emitAll(dataDao.getAverages(streamId, startTs, endTs, windowSizeMs))
        }
    }

    suspend fun deleteStreamData(streamIdentifier: FirebaseStreamIdentifier) {
        val deleteCount = getStreamId(streamIdentifier)?.let {
            database.dataPointDao().deleteAll(it)
        } ?: 0

        Log.d("ME", "Deleted $deleteCount rows from $streamIdentifier")
    }
}