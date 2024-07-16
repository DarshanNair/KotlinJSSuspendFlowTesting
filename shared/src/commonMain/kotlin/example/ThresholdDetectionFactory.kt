package example

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ThresholdDetectionFactory(
    private val maxIntervalMs: Double,
    private val endRangeEpsilonMs: Double
) {
    val activeRangeState: Flow<Double> = flowOf(0.0)

    fun create() {
        // Your implementation here
    }
}
