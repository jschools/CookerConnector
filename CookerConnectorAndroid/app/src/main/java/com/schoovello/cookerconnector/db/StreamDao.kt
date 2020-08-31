package com.schoovello.cookerconnector.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StreamDao {

    @Query("SELECT * FROM stream WHERE session_id_fb = :fbSessionId AND stream_id_fb = :fbStreamId")
    suspend fun find(fbSessionId: String, fbStreamId: String): Stream?

    @Query("SELECT * FROM stream WHERE session_id_fb = :fbSessionId")
    suspend fun findBySessionId(fbSessionId: String): List<Stream>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(stream: Stream): Long

}