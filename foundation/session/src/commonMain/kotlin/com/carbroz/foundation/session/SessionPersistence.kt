package com.carbroz.foundation.session

import com.carbroz.foundation.security.SecureKey
import com.carbroz.foundation.security.SecureStorage
import kotlinx.coroutines.CancellationException

/**
 * Persistence boundary for the authenticated session snapshot.
 *
 * Implementations must replace the complete persisted snapshot atomically from
 * the session layer's point of view. Callers must not persist token fields as
 * independently managed preferences because partial updates can create an
 * inconsistent authenticated session after process death.
 */
interface SessionPersistence {
    suspend fun restore(): SessionRestoreResult
    suspend fun save(session: SessionState.Authenticated): SessionPersistenceResult
    suspend fun clear(): SessionPersistenceResult
}

/** Result of restoring persisted authentication state. */
sealed interface SessionRestoreResult {
    data object NoSession : SessionRestoreResult
    data class Restored(val session: SessionState.Authenticated) : SessionRestoreResult
    data class Rejected(val reason: SessionRestoreFailure) : SessionRestoreResult
}

/** Result of mutating persisted authentication state. */
sealed interface SessionPersistenceResult {
    data object Success : SessionPersistenceResult
    data class Failed(val reason: SessionPersistenceFailure) : SessionPersistenceResult
}

/** Privacy-safe persistence failures; no payload or credential is included. */
enum class SessionPersistenceFailure {
    StorageUnavailable,
    EncodingFailed,
}

/** Privacy-safe restore failures; malformed payload contents are never exposed. */
enum class SessionRestoreFailure {
    StorageUnavailable,
    MalformedSnapshot,
    UnsupportedSnapshotVersion,
}

/**
 * Codec for one complete persisted authenticated-session envelope.
 *
 * Wire/storage serialization is intentionally separated from persistence so the
 * encoding can evolve without coupling session policy to a serialization library.
 */
interface SessionSnapshotCodec {
    fun encode(session: SessionState.Authenticated): SessionSnapshotEncodeResult
    fun decode(encoded: String): SessionSnapshotDecodeResult
}

sealed interface SessionSnapshotEncodeResult {
    data class Encoded(val value: String) : SessionSnapshotEncodeResult
    data object Failed : SessionSnapshotEncodeResult
}

sealed interface SessionSnapshotDecodeResult {
    data class Decoded(val session: SessionState.Authenticated) : SessionSnapshotDecodeResult
    data object Malformed : SessionSnapshotDecodeResult
    data object UnsupportedVersion : SessionSnapshotDecodeResult
}

/**
 * Secure-storage-backed persistence of one encoded session snapshot.
 *
 * One key is used deliberately so credential rotation never relies on multiple
 * independently persisted token fields being updated successfully in sequence.
 * Coroutine cancellation is always propagated and is never converted into a
 * storage or codec failure.
 */
class SecureSessionPersistence(
    private val secureStorage: SecureStorage,
    private val codec: SessionSnapshotCodec,
) : SessionPersistence {
    override suspend fun restore(): SessionRestoreResult {
        val encoded = try {
            secureStorage.read(SESSION_SNAPSHOT_KEY)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            return SessionRestoreResult.Rejected(SessionRestoreFailure.StorageUnavailable)
        } ?: return SessionRestoreResult.NoSession

        val decoded = try {
            codec.decode(encoded)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            SessionSnapshotDecodeResult.Malformed
        }

        return when (decoded) {
            is SessionSnapshotDecodeResult.Decoded -> SessionRestoreResult.Restored(decoded.session)
            SessionSnapshotDecodeResult.Malformed -> SessionRestoreResult.Rejected(SessionRestoreFailure.MalformedSnapshot)
            SessionSnapshotDecodeResult.UnsupportedVersion ->
                SessionRestoreResult.Rejected(SessionRestoreFailure.UnsupportedSnapshotVersion)
        }
    }

    override suspend fun save(session: SessionState.Authenticated): SessionPersistenceResult {
        val encoded = try {
            when (val result = codec.encode(session)) {
                is SessionSnapshotEncodeResult.Encoded -> result.value
                SessionSnapshotEncodeResult.Failed ->
                    return SessionPersistenceResult.Failed(SessionPersistenceFailure.EncodingFailed)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            return SessionPersistenceResult.Failed(SessionPersistenceFailure.EncodingFailed)
        }

        return try {
            secureStorage.write(SESSION_SNAPSHOT_KEY, encoded)
            SessionPersistenceResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            SessionPersistenceResult.Failed(SessionPersistenceFailure.StorageUnavailable)
        }
    }

    override suspend fun clear(): SessionPersistenceResult = try {
        secureStorage.remove(SESSION_SNAPSHOT_KEY)
        SessionPersistenceResult.Success
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        SessionPersistenceResult.Failed(SessionPersistenceFailure.StorageUnavailable)
    }

    private companion object {
        val SESSION_SNAPSHOT_KEY = SecureKey("session.snapshot")
    }
}
