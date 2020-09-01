package com.schoovello.cookerconnector.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.FirebaseDatabase
import com.schoovello.cookerconnector.R
import com.schoovello.cookerconnector.data.FirebaseDbInstance
import com.schoovello.cookerconnector.db.DbStreamSynchronizer
import com.schoovello.cookerconnector.db.StreamRepository
import kotlinx.android.synthetic.main.activity_test_sync.*
import kotlin.coroutines.coroutineContext

class TestSyncActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_test_sync)

        start_button.setOnClickListener {
            DbStreamSynchronizer(
                coroutineScope = lifecycleScope,
                firebaseDb = FirebaseDbInstance.instance,
                roomDb = StreamRepository.database,
                fbSessionId = "-L8NtmMM2ndRyNlkyWC2",
                fbStreamId = "-L8NtmMNjf7WwlIHQWjg"
            ).start()
        }
    }
}