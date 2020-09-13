package com.schoovello.cookerconnector.activity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.DiffResult
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.schoovello.cookerconnector.R
import com.schoovello.cookerconnector.activity.TestSyncActivity.Adapter.Holder
import com.schoovello.cookerconnector.data.FirebaseDbInstance
import com.schoovello.cookerconnector.db.AveragedDataPoint
import com.schoovello.cookerconnector.db.DbStreamSynchronizer
import com.schoovello.cookerconnector.db.StreamRepository
import kotlinx.android.synthetic.main.activity_test_sync.decrease_size_button
import kotlinx.android.synthetic.main.activity_test_sync.increase_size_button
import kotlinx.android.synthetic.main.activity_test_sync.recycler_view
import kotlinx.android.synthetic.main.activity_test_sync.reset_button
import kotlinx.android.synthetic.main.activity_test_sync.start_button
import kotlinx.android.synthetic.main.activity_test_sync.window_size
import kotlinx.android.synthetic.main.list_item_data_point.view.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TestSyncActivity : AppCompatActivity() {

    private lateinit var adapter: Adapter

    private val viewModel: TestViewModel by viewModels()

    class TestViewModel : ViewModel() {

        private val windowStepIndexLd: MutableLiveData<Int> = MutableLiveData(DEFAULT_WINDOW_STEP_INDEX)

        val windowStepLd: LiveData<Pair<Long, String>> = windowStepIndexLd.map { step ->
            step.coerceIn(WINDOW_STEPS.indices).let { WINDOW_STEPS[it] }
        }

        val dataPointsLd: LiveData<List<AveragedDataPoint>> = windowStepLd.switchMap { (windowMs, _) ->
            StreamRepository.getAverages(SESSION_ID, STREAM_ID, 0, Long.MAX_VALUE, windowMs).asLiveData()
        }

        fun changeWindowStep(difference: Int) {
            val prevIndex = windowStepIndexLd.value ?: DEFAULT_WINDOW_STEP_INDEX
            windowStepIndexLd.value = (prevIndex + difference).coerceIn(WINDOW_STEPS.indices)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_test_sync)

        start_button.setOnClickListener {
            DbStreamSynchronizer(
                coroutineScope = lifecycleScope,
                firebaseDb = FirebaseDbInstance.instance,
                roomDb = StreamRepository.database,
                fbSessionId = SESSION_ID,
                fbStreamId = STREAM_ID
            ).start()
        }

        reset_button.setOnClickListener {
            lifecycle.coroutineScope.launch {
                StreamRepository.deleteStreamData(SESSION_ID, STREAM_ID)
            }
        }

        decrease_size_button.setOnClickListener {
            viewModel.changeWindowStep(-1)
        }

        increase_size_button.setOnClickListener {
            viewModel.changeWindowStep(1)
        }

        adapter = Adapter(this, lifecycleScope)
        recycler_view.adapter = adapter

        viewModel.windowStepLd.observe(this) {
            window_size.text = it.second
        }

        viewModel.dataPointsLd.observe(this) {
            adapter.setData(it)
        }
    }

    private class Adapter(
        context: Context,
        private val coroutineScope: CoroutineScope
    ) : RecyclerView.Adapter<Holder>() {

        private val inflater = LayoutInflater.from(context)

        private val formatter = DateTimeFormatter.ISO_LOCAL_TIME

        private var data: List<AveragedDataPoint> = emptyList()

        private var strings: List<String> = emptyList()

        private val processingChannel = Channel<ProcessInput>(Channel.CONFLATED)

        private var nextInputId = AtomicInteger(0)

        init {
            coroutineScope.launch {
                @Suppress("EXPERIMENTAL_API_USAGE")
                processingChannel.consumeAsFlow()
                    .mapLatest {
                        process(it)
                    }.collect {
                        onProcessingResult(it)
                    }
            }
        }

        private class ProcessInput(
            val oldData: List<AveragedDataPoint>,
            val newData: List<AveragedDataPoint>,
            val id: Int
        )

        private class ProcessOutput(
            val newData: List<AveragedDataPoint>,
            val strings: List<String>,
            val diff: DiffResult,
            val id: Int
        )

        /**
         * Transforms an input into an output on a background thread
         */
        private suspend fun process(input: ProcessInput): ProcessOutput {
            // launch within the current CoroutineScope
            return withContext(currentCoroutineContext()) {
                with(input) {
                    // perform diff on background thread
                    val diffResult = async(Dispatchers.Default) {
                        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                            override fun getOldListSize() = oldData.size

                            override fun getNewListSize() = newData.size

                            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                                // cooperate with cancellation
                                ensureActive()

                                return oldData[oldItemPosition].timeMillis == newData[newItemPosition].timeMillis
                            }

                            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = true
                        }, false)
                    }

                    val stringsResult = async(Dispatchers.Default) {
                        newData.map {
                            // cooperate with cancellation
                            yield()

                            val zonedTime = Instant.ofEpochMilli(it.timeMillis).atZone(ZoneId.systemDefault())
                            "${formatter.format(zonedTime)} : ${it.averageValue}"
                        }
                    }

                    ProcessOutput(
                        newData = newData,
                        strings = stringsResult.await(),
                        diff = diffResult.await(),
                        id = input.id
                    )
                }
            }
        }

        fun onProcessingResult(result: ProcessOutput) {
            data = result.newData
            strings = result.strings

            try {
                result.diff.dispatchUpdatesTo(this)
            } catch (t: Throwable) {
                t.printStackTrace()

                notifyDataSetChanged()
            }
        }

        fun setData(newData: List<AveragedDataPoint>) {
            val id = nextInputId.getAndIncrement()

            coroutineScope.launch {
                processingChannel.send(ProcessInput(
                    oldData = data,
                    newData = newData,
                    id = id
                ))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = inflater.inflate(R.layout.list_item_data_point, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.text.text = strings[position]
        }

        override fun getItemCount() = data.size

        class Holder(itemView: View) : ViewHolder(itemView) {
            val text = itemView.text
        }
    }

    companion object {
        private const val SESSION_ID = "-L8NtmMM2ndRyNlkyWC2"
        private const val STREAM_ID = "-L8NtmMNjf7WwlIHQWjg"

        private const val DEFAULT_WINDOW_STEP_INDEX = 6

        private val WINDOW_STEPS = listOf(
            TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS) to "1 sec",
            TimeUnit.MILLISECONDS.convert(15, TimeUnit.SECONDS) to "15 sec",
            TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS) to "30 sec",
            TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES) to "1 min",
            TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES) to "5 min",
            TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES) to "10 min",
            TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES) to "15 min",
            TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES) to "30 min",
            TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS) to "1 hr",
            TimeUnit.MILLISECONDS.convert(2, TimeUnit.HOURS) to "2 hr",
            TimeUnit.MILLISECONDS.convert(3, TimeUnit.HOURS) to "3 hr",
            TimeUnit.MILLISECONDS.convert(6, TimeUnit.HOURS) to "6 hr",
            TimeUnit.MILLISECONDS.convert(12, TimeUnit.HOURS) to "12 hr",
            TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS) to "24 hr"
        )
    }

}