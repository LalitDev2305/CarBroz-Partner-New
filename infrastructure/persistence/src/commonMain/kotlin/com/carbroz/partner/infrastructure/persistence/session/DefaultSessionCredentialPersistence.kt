package com.carbroz.partner.infrastructure.persistence.session

import com.carbroz.partner.domain.session.credential.CredentialLoadResult
import com.carbroz.partner.domain.session.credential.CredentialPersistenceResult
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.infrastructure.persistence.secure.SecureKeyValueStorage
import com.carbroz.partner.infrastructure.persistence.secure.SecureStorageResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SessionCredentialsDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null
) {
    fun toDomain(): SessionCredentials = SessionCredentials(accessToken, refreshToken)

    companion object {
        fun fromDomain(domain: SessionCredentials): SessionCredentialsDto =
            SessionCredentialsDto(domain.accessToken, domain.refreshToken)
    }
}

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
                        val dto = json.decodeFromString<SessionCredentialsDto>(result.value)
                        CredentialLoadResult.Found(dto.toDomain())
                    } catch (_: Exception) {
                        CredentialLoadResult.Unavailable
                    }
                }
                is SecureStorageResult.NotFound -> CredentialLoadResult.NotFound
                is SecureStorageResult.Failure -> CredentialLoadResult.Unavailable
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            CredentialLoadResult.Unavailable
        }
    }

    override suspend fun save(credentials: SessionCredentials): CredentialPersistenceResult {
        return try {
            val dto = SessionCredentialsDto.fromDomain(credentials)
            val payload = json.encodeToString(SessionCredentialsDto.serializer(), dto)
            when (secureStorage.write(KEY_SESSION_CREDENTIALS, payload)) {
                is SecureStorageResult.Success -> CredentialPersistenceResult.Success
                else -> CredentialPersistenceResult.Failure
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            CredentialPersistenceResult.Failure
        }
    }

    override suspend fun clear(): CredentialPersistenceResult {
        return try {
            when (secureStorage.remove(KEY_SESSION_CREDENTIALS)) {
                is SecureStorageResult.Success -> CredentialPersistenceResult.Success
                is SecureStorageResult.NotFound -> CredentialPersistenceResult.Success
                is SecureStorageResult.Failure -> CredentialPersistenceResult.Failure
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            CredentialPersistenceResult.Failure
        }
    }
}
