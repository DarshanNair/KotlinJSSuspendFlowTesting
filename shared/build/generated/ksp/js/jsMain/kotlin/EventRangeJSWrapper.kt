@file:OptIn(ExperimentalJsExport::class)

import kotlin.Double
import kotlin.OptIn
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.Promise

@JsExport
public fun EventRange.eventRangeGetDataAsPromise(): Promise<Double> = Promise { resolve, reject ->
    val completion = Continuation(EmptyCoroutineContext) {
        it.onSuccess(resolve).onFailure(reject)
    }
    this.getData.startCoroutine(completion)
}
