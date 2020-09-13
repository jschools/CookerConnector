package com.schoovello.cookerconnector.db

import android.util.Log
import androidx.room.Room
import com.schoovello.cookerconnector.CookerConnectorApp
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

    suspend fun getStreamId(fbSessionId: String, fbStreamId: String): Long? {
        return database.streamDao().find(fbSessionId, fbStreamId)?.rowId
    }

    fun getAverages(
        fbSessionId: String,
        fbStreamId: String,
        startTs: Long,
        endTs: Long,
        windowSizeMs: Long
    ): Flow<List<SummarizedDataPoint>> {
        return flow {
            // look up the stream
            val streamId = getStreamId(fbSessionId, fbStreamId)
            if (streamId == null) {
                emit(emptyList())
                return@flow
            }

            val dataDao = database.dataPointDao()
            emitAll(dataDao.getAverages(streamId, startTs, endTs, windowSizeMs))
        }
    }

    suspend fun deleteStreamData(fbSessionId: String, fbStreamId: String) {
        val deleteCount = getStreamId(fbSessionId, fbStreamId)?.let {
            database.dataPointDao().deleteAll(it)
        } ?: 0

        Log.d("ME", "Deleted $deleteCount rows from $fbSessionId/$fbStreamId")
    }
}