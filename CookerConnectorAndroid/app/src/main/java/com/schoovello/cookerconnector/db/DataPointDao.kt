package com.schoovello.cookerconnector.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DataPointDao {

    @Query("SELECT * FROM data_point WHERE stream_id = :streamId ORDER BY ts")
    suspend fun getAllForStream(streamId: Long): List<DataPoint>

    @Query("SELECT MAX(ts) FROM data_point WHERE stream_id = :streamId")
    suspend fun getLastTimestampForStream(streamId: Long): Long?

    @Query("SELECT * FROM data_point WHERE stream_id = :streamId AND ts BETWEEN :startTs AND :endTs ORDER BY ts")
    fun getAllInRange(streamId: Long, startTs: Long, endTs: Long): Flow<List<DataPoint>>

    @Query("SELECT (chunk_time * :windowSizeMs + (:windowSizeMs / 2)) as ts, avg_value FROM ( SELECT (ts / :windowSizeMs) as chunk_time, avg(calibrated_val) as avg_value FROM data_point WHERE stream_id = :streamId AND ts BETWEEN :startTs AND :endTs GROUP BY chunk_time)")
    fun getAverages(streamId: Long, startTs: Long, endTs: Long, windowSizeMs: Long): Flow<List<SummarizedDataPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dataPoint: DataPoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dataPoints: List<DataPoint>)

    @Query("DELETE FROM data_point WHERE stream_id = :streamId")
    suspend fun deleteAll(streamId: Long): Int
}