package com.carbroz.partner.core.mvi

import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.core.observability.model.LogCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * Internal canonical implementation of [Store] and [IntentScope].
 *
 * Enforces sequential intent processing, atomic state updates via [MutableStateFlow.update],
 * single-consumer rendezvous effect emission, and parent [CoroutineScope] lifecycle teardown.
 */
internal class DefaultStore<State, Intent, Effect>(
    scope: CoroutineScope,
    initialState: State,
    private val storeId: String,
    private val logger: StructuredLogger?,
    private val processor: IntentProcessor<State, Intent, Effect>
) : Store<State, Intent, Effect>, IntentScope<State, Effect> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<State> = _state.asStateFlow()

    private val intentChannel = Channel<Intent>(Channel.RENDEZVOUS)
    private val effectChannel = Channel<Effect>(Channel.RENDEZVOUS)

    override val effects: Flow<Effect> = effectChannel.receiveAsFlow()

    override val currentState: State
        get() = _state.value

    init {
        logger?.withSource(storeId)?.info(
            sourceFunction = "init",
            category = LogCategory.MVI,
            event = "STORE_CREATED",
            message = "Store initialized"
        )

        scope.launch {
            for (intent in intentChannel) {
                try {
                    logger?.withSource(storeId)?.debug(
                        sourceFunction = "processIntent",
                        category = LogCategory.MVI,
                        event = "INTENT_PROCESSING_STARTED",
                        message = "Started processing intent"
                    )
                    processor.invoke(this@DefaultStore, intent)
                    logger?.withSource(storeId)?.debug(
                        sourceFunction = "processIntent",
                        category = LogCategory.MVI,
                        event = "INTENT_PROCESSING_COMPLETED",
                        message = "Completed processing intent"
                    )
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    logger?.withSource(storeId)?.error(
                        sourceFunction = "processIntent",
                        category = LogCategory.MVI,
                        event = "INTENT_PROCESSING_FAILED",
                        message = "Uncaught error during intent processing",
                        throwable = t
                    )
                    intentChannel.close(t)
                    effectChannel.close(t)
                    return@launch
                }
            }
        }




        scope.coroutineContext.job.invokeOnCompletion {
            intentChannel.close()
            effectChannel.close()
            logger?.withSource(storeId)?.info(
                sourceFunction = "onCompletion",
                category = LogCategory.MVI,
                event = "STORE_SCOPE_COMPLETED",
                message = "Store scope completed and channels closed"
            )
        }
    }

    override suspend fun dispatch(intent: Intent) {
        intentChannel.send(intent)
    }

    override suspend fun updateState(transform: (State) -> State) {
        _state.update { current -> transform(current) }
        logger?.withSource(storeId)?.debug(
            sourceFunction = "updateState",
            category = LogCategory.MVI,
            event = "STATE_UPDATED",
            message = "State updated atomically"
        )
    }

    override suspend fun emitEffect(effect: Effect) {
        effectChannel.send(effect)
        logger?.withSource(storeId)?.debug(
            sourceFunction = "emitEffect",
            category = LogCategory.MVI,
            event = "EFFECT_EMITTED",
            message = "Effect emitted to flow"
        )
    }
}
