package example

import EventRange
import kotlin.Double
import kotlin.OptIn
import kotlin.Suppress
import kotlin.Throwable
import kotlin.Unit
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.Promise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@JsExport
@OptIn(
  ExperimentalJsExport::class,
  ExperimentalCoroutinesApi::class,
)
public class EventManagerImplJsWrapper(
  config: ThresholdConfig,
) {
  private val instance: EventManagerImpl = EventManagerImpl(config = config)

  private val job: Job = SupervisorJob()

  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + job)

  public fun load(): Unit {
    instance.load()
  }

  public fun eventHeadChanged(playheadMs: Double): Unit {
    instance.eventHeadChanged(playheadMs)
  }

  @Suppress("NON_EXPORTABLE_TYPE")
  public fun eventStateChanged(): Promise<Unit> = Promise { resolve, reject ->
      scope.launch {
          try {
              instance.eventStateChanged()
              resolve(Unit)
          } catch (exception: Throwable) {
              reject(exception)
          }
      }
  }

  public fun destroy(): Unit {
    instance.destroy()
  }

  @Suppress("NON_EXPORTABLE_TYPE")
  public fun getEventRangeStateFlow(success: (EventRange?) -> Unit, error: (Throwable) -> Unit):
      Promise<Unit> = Promise { _, reject ->
      scope.launch {
          try {
              instance.eventRangeStateFlow.collect { value ->
                  success(value)
              }
          } catch (exception: Throwable) {
              error(exception)
              reject(exception)
          }
      }
  }

  @Suppress("NON_EXPORTABLE_TYPE")
  public fun getProcessSeekStream(success: (Double?) -> Unit, error: (Throwable) -> Unit):
      Promise<Unit> = Promise { _, reject ->
      scope.launch {
          try {
              instance.processSeekStream.collect { value ->
                  success(value)
              }
          } catch (exception: Throwable) {
              error(exception)
              reject(exception)
          }
      }
  }

  public fun clear(): Unit {
    job.cancel()
  }
}
