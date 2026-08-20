package com.carbroz.foundation.architecture.store

import kotlinx.coroutines.CoroutineScope

/**
 * Explicit lifecycle owner supplied to Store implementations.
 *
 * This wrapper prevents the architecture kernel from inventing process-wide
 * coroutine scopes and makes lifecycle ownership visible at construction sites.
 */
@JvmInline
value class StoreScope(val coroutineScope: CoroutineScope)
