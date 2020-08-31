package com.schoovello.cookerconnector.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stream",
    indices = [
        Index(
            value = ["session_id_fb", "stream_id_fb"],
            unique = true
        )
    ]
)
data class Stream(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val rowId: Long = 0,

    @ColumnInfo(name = "session_id_fb", index = true)
    val fbSessionId: String,

    @ColumnInfo(name = "stream_id_fb", index = true)
    val fbStreamId: String
)