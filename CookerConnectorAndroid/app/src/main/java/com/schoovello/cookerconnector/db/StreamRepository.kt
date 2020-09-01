package com.schoovello.cookerconnector.db

import androidx.room.Room
import androidx.room.RoomDatabase
import com.schoovello.cookerconnector.CookerConnectorApp

object StreamRepository {

    private const val DB_NAME = "stream_db"

    val database: StreamDatabase by lazy {
        Room.databaseBuilder(
            CookerConnectorApp.getInstance(),
            StreamDatabase::class.java,
            DB_NAME
        ).build()
    }
}