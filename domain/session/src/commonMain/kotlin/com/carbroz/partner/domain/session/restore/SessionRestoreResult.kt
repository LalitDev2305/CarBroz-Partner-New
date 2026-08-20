package com.carbroz.partner.domain.session.restore

/**
 * Explicit domain outcome for session startup restoration.
 */
public sealed interface SessionRestoreResult {
    public data object Authenticated : SessionRestoreResult
    public data object Unauthenticated : SessionRestoreResult
    public data object Unavailable : SessionRestoreResult
}
