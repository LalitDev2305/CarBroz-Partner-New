package com.carbroz.foundation.architecture.store

import kotlinx.coroutines.CoroutineScope

/**
 * Creates a [Store] whose asynchronous work is owned by an explicit lifecycle scope.
 *
 * The architecture kernel never creates a global scope. The caller supplies the
 * [scope] so cancellation follows the lifecycle of the feature or runtime that owns
 * the Store. Implementations must not retain the scope after the Store is no longer
 * reachable from its owner.
 */
fun interface StoreFactory<in Intent : Any, out State : Any> {
    fun create(scope: CoroutineScope): Store<Intent, State>
}
