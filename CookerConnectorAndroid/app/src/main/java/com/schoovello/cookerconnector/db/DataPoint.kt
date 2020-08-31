package com.schoovello.cookerconnector.db

import androidx.room.*

@Entity(
    tableName = "data_point",
    indices = [
        Index(
            value = ["ts", "stream_id"],
            unique = true
        )
    ],
    foreignKeys = [ForeignKey(
        entity = Stream::class,
        parentColumns = ["_id"],
        childColumns = ["stream_id"],
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )]
)
class DataPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "ts")
    val timestamp: Long,

    @ColumnInfo(name = "stream_id", index = true)
    val streamId: Long,

    @ColumnInfo(name = "calibrated_val")
    val calibratedValue: Float
)