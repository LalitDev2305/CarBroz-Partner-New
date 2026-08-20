package com.carbroz.foundation.architecture.store

/**
 * Creates a [Store] whose asynchronous work is owned by an explicit [StoreScope].
 *
 * The architecture kernel never creates a global coroutine scope. The caller
 * supplies lifecycle ownership so cancellation follows the feature or runtime
 * that owns the Store.
 */
fun interface StoreFactory<in Intent : Any, out State : Any> {
    fun create(scope: StoreScope): Store<Intent, State>
}
