package com.schoovello.cookerconnector.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DataPointDao {

    @Query("SELECT * FROM data_point WHERE stream_id = :streamId ORDER BY ts")
    fun getAllForStream(streamId: Long): List<DataPoint>

    @Query("SELECT MAX(ts) FROM data_point WHERE stream_id = :streamId")
    fun getLastTimestampForStream(streamId: Long): Long?

    @Query("SELECT * FROM data_point WHERE stream_id = :streamId AND ts BETWEEN :startTs AND :endTs ORDER BY ts")
    fun getAllInRange(streamId: Long, startTs: Long, endTs: Long): Flow<List<DataPoint>>

    @Query("SELECT (ts / :chunkSizeMs) as chunk_time, avg(calibrated_val) as avg_value FROM data_point WHERE stream_id = :streamId AND ts BETWEEN :startTs AND :endTs GROUP BY chunk_time")
    fun getChunkedAverages(streamId: Long, startTs: Long, endTs: Long, chunkSizeMs: Long): Flow<List<AveragedDataPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dataPoint: DataPoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(dataPoints: List<DataPoint>)
}