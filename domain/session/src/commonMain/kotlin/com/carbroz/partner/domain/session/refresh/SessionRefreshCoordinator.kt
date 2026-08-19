package com.carbroz.partner.domain.session.refresh

import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionRefreshResult
import com.carbroz.partner.domain.session.operation.ClearSession
import com.carbroz.partner.domain.session.storage.SessionCredentialStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class SessionRefreshCoordinator(
    private val credentialStorage: SessionCredentialStorage,
    private val refreshGateway: SessionRefreshGateway,
    private val clearSession: ClearSession
) {

    private val mutex = Mutex()

    public suspend fun refresh(failedToken: String?): Boolean {
        return mutex.withLock {
            val loadResult = credentialStorage.loadCredentials()
            if (loadResult !is CredentialLoadResult.Found) {
                clearSession.execute()
                return false
            }

            val currentCredentials = loadResult.credentials

            if (failedToken != null && currentCredentials.accessToken != failedToken) {
                // Another concurrent request already refreshed credentials.
                return true
            }

            val refreshToken = currentCredentials.refreshToken
            if (refreshToken.isNullOrBlank()) {
                clearSession.execute()
                return false
            }

            return when (val result = refreshGateway.refreshToken(refreshToken)) {
                is SessionRefreshResult.Success -> {
                    val saved = credentialStorage.saveCredentials(result.credentials)
                    if (saved) {
                        true
                    } else {
                        clearSession.execute()
                        false
                    }
                }
                is SessionRefreshResult.Failure -> {
                    clearSession.execute()
                    false
                }
            }
        }
    }
}
