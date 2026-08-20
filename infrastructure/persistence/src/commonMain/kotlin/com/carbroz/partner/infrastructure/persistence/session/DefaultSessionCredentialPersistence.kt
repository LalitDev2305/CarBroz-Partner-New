package com.carbroz.partner.infrastructure.persistence.session

import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.infrastructure.persistence.secure.SecureKeyValueStorage
import com.carbroz.partner.infrastructure.persistence.secure.SecureStorageResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Infrastructure implementation of [SessionCredentialPersistence] managing JSON serialization and key-value persistence.
 */
internal class DefaultSessionCredentialPersistence(
    private val secureStorage: SecureKeyValueStorage,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SessionCredentialPersistence {

    private companion object {
        private const val KEY_SESSION_CREDENTIALS: String = "session_credentials"
    }

    override suspend fun load(): CredentialLoadResult {
        return try {
            when (val result = secureStorage.read(KEY_SESSION_CREDENTIALS)) {
                is SecureStorageResult.Success -> {
                    try {
                        val creds = json.decodeFromString<SessionCredentials>(result.value)
                        CredentialLoadResult.Found(creds)
                    } catch (_: Exception) {
                        CredentialLoadResult.Failure
                    }
                }
                is SecureStorageResult.NotFound -> CredentialLoadResult.NotFound
                is SecureStorageResult.Failure -> CredentialLoadResult.Failure
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            CredentialLoadResult.Failure
        }
    }

    override suspend fun save(credentials: SessionCredentials): Boolean {
        return try {
            val payload = json.encodeToString(SessionCredentials.serializer(), credentials)
            when (secureStorage.write(KEY_SESSION_CREDENTIALS, payload)) {
                is SecureStorageResult.Success -> true
                else -> false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun clear(): Boolean {
        return try {
            when (secureStorage.remove(KEY_SESSION_CREDENTIALS)) {
                is SecureStorageResult.Success -> true
                is SecureStorageResult.NotFound -> true
                is SecureStorageResult.Failure -> false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }
}
