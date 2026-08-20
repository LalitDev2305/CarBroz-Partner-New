package com.carbroz.partner.domain.session.refresh

import com.carbroz.partner.domain.session.credential.CredentialLoadResult
import com.carbroz.partner.domain.session.credential.CredentialPersistenceResult
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.operation.ClearSession
import com.carbroz.partner.domain.session.store.SessionStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates single-flight token refresh across concurrent requests and manages runtime/persisted state transitions.
 */
public class SessionRefreshCoordinator(
    private val credentialPersistence: SessionCredentialPersistence,
    private val refreshGateway: SessionRefreshGateway,
    private val clearSession: ClearSession,
    private val sessionStore: SessionStore? = null
) {
    private val mutex = Mutex()

    public suspend fun refresh(failedToken: String?): SessionRefreshOutcome {
        return mutex.withLock {
            val loadResult = credentialPersistence.load()
            if (loadResult !is CredentialLoadResult.Found) {
                clearSession.execute()
                return SessionRefreshOutcome.Rejected
            }

            val currentCredentials = loadResult.credentials

            if (failedToken != null && currentCredentials.accessToken != failedToken) {
                // Another concurrent request already refreshed credentials.
                sessionStore?.markAuthenticated()
                return SessionRefreshOutcome.AlreadyRefreshed
            }

            val refreshToken = currentCredentials.refreshToken
            if (refreshToken.isNullOrBlank()) {
                clearSession.execute()
                return SessionRefreshOutcome.Rejected
            }

            return when (val result = refreshGateway.refreshToken(refreshToken)) {
                is SessionRefreshResult.Success -> {
                    when (credentialPersistence.save(result.credentials)) {
                        is CredentialPersistenceResult.Success -> {
                            sessionStore?.markAuthenticated()
                            SessionRefreshOutcome.Refreshed
                        }
                        is CredentialPersistenceResult.Failure -> {
                            clearSession.execute()
                            SessionRefreshOutcome.PersistenceFailure
                        }
                    }
                }
                is SessionRefreshResult.Rejected -> {
                    clearSession.execute()
                    SessionRefreshOutcome.Rejected
                }
                is SessionRefreshResult.Unavailable -> {
                    // DO NOT clear credentials/session on transient failure
                    SessionRefreshOutcome.Unavailable
                }
            }
        }
    }
}
