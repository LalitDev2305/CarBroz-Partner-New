package com.carbroz.partner.domain.session.storage

import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.domain.storage.core.StorageResult
import com.carbroz.partner.domain.storage.secure.SecureStorageGateway
import kotlinx.serialization.json.Json

public class SessionCredentialStorage(
    private val secureStorage: SecureStorageGateway,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    public companion object {
        public const val KEY_SESSION_CREDENTIALS: String = "session_credentials"
    }

    public suspend fun loadCredentials(): CredentialLoadResult {
        return when (val result = secureStorage.getSecret(KEY_SESSION_CREDENTIALS)) {
            is StorageResult.Success -> {
                try {
                    val creds = json.decodeFromString<SessionCredentials>(result.value)
                    CredentialLoadResult.Found(creds)
                } catch (_: Exception) {
                    CredentialLoadResult.Failure
                }
            }
            is StorageResult.NotFound -> CredentialLoadResult.NotFound
            is StorageResult.Failure -> CredentialLoadResult.Failure
        }
    }

    public suspend fun saveCredentials(credentials: SessionCredentials): Boolean {
        return try {
            val payload = json.encodeToString(SessionCredentials.serializer(), credentials)
            when (secureStorage.putSecret(KEY_SESSION_CREDENTIALS, payload)) {
                is StorageResult.Success -> true
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    public suspend fun clearCredentials(): Boolean {
        return when (secureStorage.removeSecret(KEY_SESSION_CREDENTIALS)) {
            is StorageResult.Success -> true
            is StorageResult.NotFound -> true
            is StorageResult.Failure -> false
        }
    }
}
