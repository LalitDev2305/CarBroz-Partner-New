package com.carbroz.partner.core.mvi.internal

import com.carbroz.partner.core.mvi.Store
import com.carbroz.partner.core.mvi.StoreProcessor
import com.carbroz.partner.core.mvi.StoreScope
import com.carbroz.partner.core.observability.logger.BoundLogger
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val EFFECT_BUFFER_CAPACITY = 64

/**
 * Internal canonical implementation of [Store] and [StoreScope].
 *
 * Enforces sequential intent processing, atomic state updates via [MutableStateFlow.update],
 * single-consumer bounded effect emission with non-blocking overflow, and single processor-job lifecycle teardown.
 */
internal class DefaultStore<State, Intent, Effect>(
    scope: CoroutineScope,
    initialState: State,
    storeId: String,
    logger: StructuredLogger,
    private val processor: StoreProcessor<State, Intent, Effect>
) : Store<State, Intent, Effect>, StoreScope<State, Effect> {

    private val boundLogger: BoundLogger = logger.withSource(storeId)

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<State> = _state.asStateFlow()

    private val intentChannel = Channel<Intent>(Channel.RENDEZVOUS)
    private val effectChannel = Channel<Effect>(EFFECT_BUFFER_CAPACITY)

    override val effects: Flow<Effect> = effectChannel.receiveAsFlow()

    override val currentState: State
        get() = _state.value

    private val processorJob = scope.launch {
        var terminalCause: Throwable? = null
        try {
            for (intent in intentChannel) {
                try {
                    boundLogger.debug(
                        sourceFunction = "processIntent",
                        category = LogCategory.MVI,
                        event = "INTENT_PROCESSING_STARTED",
                        message = "Started processing intent"
                    )
                    processor.invoke(this@DefaultStore, intent)
                    boundLogger.debug(
                        sourceFunction = "processIntent",
                        category = LogCategory.MVI,
                        event = "INTENT_PROCESSING_COMPLETED",
                        message = "Completed processing intent"
                    )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    boundLogger.error(
                        sourceFunction = "processIntent",
                        category = LogCategory.MVI,
                        event = "INTENT_PROCESSING_FAILED",
                        message = "Uncaught error during intent processing",
                        throwable = failure
                    )
                    terminalCause = failure
                    break
                }
            }
        } catch (cancellation: CancellationException) {
            if (terminalCause == null) {
                terminalCause = cancellation
            }
            throw cancellation
        } finally {
            intentChannel.close(terminalCause)
            effectChannel.close(terminalCause)
            boundLogger.info(
                sourceFunction = "onProcessorCompletion",
                category = LogCategory.MVI,
                event = "STORE_TERMINATED",
                message = "Store processing terminated and channels closed"
            )
        }
    }

    init {
        boundLogger.info(
            sourceFunction = "init",
            category = LogCategory.MVI,
            event = "STORE_CREATED",
            message = "Store initialized"
        )
    }

    override suspend fun dispatch(intent: Intent) {
        intentChannel.send(intent)
    }

    override fun updateState(transform: (State) -> State) {
        _state.update { current -> transform(current) }
        boundLogger.debug(
            sourceFunction = "updateState",
            category = LogCategory.MVI,
            event = "STATE_UPDATED",
            message = "State updated atomically"
        )
    }

    override suspend fun emitEffect(effect: Effect) {
        val result = effectChannel.trySend(effect)
        if (result.isSuccess) {
            boundLogger.debug(
                sourceFunction = "emitEffect",
                category = LogCategory.MVI,
                event = "EFFECT_EMITTED",
                message = "Effect emitted to flow"
            )
        } else if (result.isFailure) {
            if (result.isClosed) {
                val cause = result.exceptionOrNull()
                if (cause != null) throw cause
                throw ClosedSendChannelException("Store effect channel is closed")
            } else {
                boundLogger.log(
                    level = LogLevel.WARN,
                    category = LogCategory.MVI,
                    sourceFunction = "emitEffect",
                    event = "EFFECT_DROPPED_BUFFER_FULL",
                    message = "Transient effect dropped because effect buffer is full"
                )
            }
        }
    }
}
