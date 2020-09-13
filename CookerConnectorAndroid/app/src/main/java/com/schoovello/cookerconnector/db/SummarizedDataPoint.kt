package com.schoovello.cookerconnector.db

import androidx.room.ColumnInfo

class SummarizedDataPoint(
    @ColumnInfo(name = "ts")
    val timeMillis: Long,

    @ColumnInfo(name = "avg_value")
    val averageValue: Float
)