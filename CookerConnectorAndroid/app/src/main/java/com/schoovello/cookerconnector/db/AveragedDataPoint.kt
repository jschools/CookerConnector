package com.schoovello.cookerconnector.db

import androidx.room.ColumnInfo

class AveragedDataPoint(
    @ColumnInfo(name = "ts")
    val ts: Long,

    @ColumnInfo(name = "avg_value")
    val averageValue: Float
)