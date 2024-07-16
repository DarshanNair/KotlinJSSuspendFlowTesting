package example

import EventRange
import com.wbd.beam.kmp.annotations.JsWrapperExport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.CoroutineContext

@JsWrapperExport
class EventManagerImpl(
    private val config: ThresholdConfig,
    private val eventSequenceProviderFactory: EventSequenceProviderFactory = EventSequenceProviderFactory(),
    private val thresholdDetectionFactory: ThresholdDetectionFactory = ThresholdDetectionFactory(
        maxIntervalMs = config.maxIntervalMs,
        endRangeEpsilonMs = config.endRangeEpsilonMs,
    ),
    private val coroutineContext: CoroutineContext = Dispatchers.Default,
) {

    private val eventSequenceManagerScope = CoroutineScope(coroutineContext)

    private var eventStreamJob: Job? = null

    val eventRangeStateFlow = MutableStateFlow<EventRange?>(null)
    val processSeekStream = MutableSharedFlow<Double>()

    private val activeRangeStateFlow = MutableStateFlow<Double?>(null)
    private val playheadMsSharedFlow = MutableSharedFlow<Double>()

    fun load() {
        resetForNewEventRange()

        val streamJob = eventSequenceManagerScope.launch {
            val timelineProvider = eventSequenceProviderFactory.create()

            val boundaryDetector = thresholdDetectionFactory.create()

            launch {
                eventSequenceProviderFactory.eventRangeUpdates.collect(eventRangeStateFlow)
            }

            launch {
                thresholdDetectionFactory.activeRangeState.collect(activeRangeStateFlow)
            }
        }

        this.eventStreamJob = streamJob
    }

    private fun processSeek() {
        // Your implementation here
    }

    fun eventHeadChanged(playheadMs: Double) {
        processSeek()
        eventSequenceManagerScope.launch {
            this@EventManagerImpl.playheadMsSharedFlow.emit(playheadMs)
        }
    }

    suspend fun eventStateChanged() {
        // Your implementation here
        delay(1000)
    }

    fun destroy() {
        eventSequenceManagerScope.cancel()
    }

    private fun resetForNewEventRange() {
        eventStreamJob?.cancel()
        eventRangeStateFlow.value = null
        activeRangeStateFlow.value = null
    }
}
