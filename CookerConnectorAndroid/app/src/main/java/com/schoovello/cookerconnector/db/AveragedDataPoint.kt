package com.schoovello.cookerconnector.db

import androidx.room.ColumnInfo

class AveragedDataPoint(
    @ColumnInfo(name = "chunk_time")
    val chunkTimeMillis: Long,

    @ColumnInfo(name = "avg_value")
    val averageValue: Float
)