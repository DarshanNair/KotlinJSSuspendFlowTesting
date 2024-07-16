@file:OptIn(ExperimentalJsExport::class)

package example

import kotlin.js.JsExport

@JsExport
data class ThresholdConfig(
    val maxIntervalMs: Double,
    val endRangeEpsilonMs: Double
)
