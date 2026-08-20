package com.carbroz.partner.domain.session.operation

/**
 * Domain outcome for clearing session state and credentials.
 */
public sealed interface ClearSessionResult {
    public data object Cleared : ClearSessionResult
    public data object PersistenceFailure : ClearSessionResult
}
