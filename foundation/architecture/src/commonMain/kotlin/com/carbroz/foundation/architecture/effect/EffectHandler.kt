package com.carbroz.foundation.architecture.effect

/**
 * Executes side effects outside a reducer and translates them into typed results.
 *
 * Implementations may call repositories, capabilities, or other ports owned by
 * higher-level modules. They must not mutate Store state directly; the returned
 * result is reduced through the owning state machine.
 */
fun interface EffectHandler<in Effect : Any, out Result : Any> {
    suspend fun handle(effect: Effect): Result
}
