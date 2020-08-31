package com.schoovello.cookerconnector.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Stream::class,
        DataPoint::class
    ],
    version = 1
)
abstract class StreamDatabase : RoomDatabase() {
    abstract fun streamDao(): StreamDao
    abstract fun dataPointDao(): DataPointDao
}