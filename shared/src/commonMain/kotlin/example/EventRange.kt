@file:OptIn(ExperimentalJsExport::class)

import com.wbd.beam.kmp.annotations.JsSuspendFunctionPropertyExport

@JsExport
class EventRange(
    val startTimeMs: Double,
    val durationMs: Double,
    val isDynamic: Boolean = false,
    @JsSuspendFunctionPropertyExport val getData: suspend () -> Double,
)
