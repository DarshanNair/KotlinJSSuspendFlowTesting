package example

import EventRange
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class EventSequenceProviderFactory {

    val eventRangeUpdates: Flow<EventRange?> = flowOf(
        EventRange(
            startTimeMs = 0.0,
            durationMs = 1.0,
            isDynamic = true,
            getData = { delay(100); 42.0 }
        )
    )

    fun create() {
        // Creating a list of mock events
        val events = listOf(
            "Event 1: Description for event 1 at 2024-07-01T10:00:00Z",
            "Event 2: Description for event 2 at 2024-07-02T14:00:00Z",
            "Event 3: Description for event 3 at 2024-07-03T18:00:00Z"
        )

        // Print the events
        events.forEach { event ->
            println(event)
        }
    }
}
