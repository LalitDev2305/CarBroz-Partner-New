package com.carbroz.partner.domain.session.refresh

import com.carbroz.partner.domain.session.credential.CredentialLoadResult
import com.carbroz.partner.domain.session.credential.CredentialPersistenceResult
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.operation.ClearSession
import com.carbroz.partner.domain.session.operation.ClearSessionResult
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
    private val sessionStore: SessionStore
) {
    private val mutex = Mutex()

    public suspend fun refresh(failedToken: String?): SessionRefreshOutcome {
        return mutex.withLock {
            when (val loadResult = credentialPersistence.load()) {
                is CredentialLoadResult.NotFound -> {
                    sessionStore.markUnauthenticated()
                    SessionRefreshOutcome.Rejected
                }
                is CredentialLoadResult.Unavailable -> {
                    SessionRefreshOutcome.Unavailable
                }
                is CredentialLoadResult.Found -> {
                    val currentCredentials = loadResult.credentials

                    if (failedToken != null && currentCredentials.accessToken != failedToken) {
                        sessionStore.markAuthenticated()
                        return@withLock SessionRefreshOutcome.AlreadyRefreshed
                    }

                    val refreshToken = currentCredentials.refreshToken
                    if (refreshToken.isNullOrBlank()) {
                        return@withLock clearAndMapResult()
                    }

                    when (val refreshResult = refreshGateway.refreshToken(refreshToken)) {
                        is SessionRefreshResult.Success -> {
                            when (credentialPersistence.save(refreshResult.credentials)) {
                                is CredentialPersistenceResult.Success -> {
                                    sessionStore.markAuthenticated()
                                    SessionRefreshOutcome.Refreshed
                                }
                                is CredentialPersistenceResult.Failure -> {
                                    clearSession.execute()
                                    SessionRefreshOutcome.PersistenceFailure
                                }
                            }
                        }
                        is SessionRefreshResult.Rejected -> {
                            clearAndMapResult()
                        }
                        is SessionRefreshResult.Unavailable -> {
                            SessionRefreshOutcome.Unavailable
                        }
                    }
                }
            }
        }
    }

    private suspend fun clearAndMapResult(): SessionRefreshOutcome {
        return when (clearSession.execute()) {
            is ClearSessionResult.Cleared -> SessionRefreshOutcome.Rejected
            is ClearSessionResult.PersistenceFailure -> SessionRefreshOutcome.PersistenceFailure
        }
    }
}
