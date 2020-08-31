package com.schoovello.cookerconnector.util

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Query.fetchValueSnapshot(): DataSnapshot {
    return suspendCancellableCoroutine { cont ->
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) {
                    cont.resume(snapshot)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) {
                    cont.resumeWithException(error.toException())
                }
            }
        }

        addListenerForSingleValueEvent(listener)

        cont.invokeOnCancellation {
            removeEventListener(listener)
        }
    }
}