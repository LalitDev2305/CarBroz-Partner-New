package com.carbroz.foundation.session

import com.carbroz.foundation.security.Secret
import com.carbroz.foundation.security.SecureKey
import com.carbroz.foundation.security.SecureStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SecureSessionPersistenceTest {
    @Test
    fun `missing secure snapshot restores signed out state`() = runTest {
        val persistence = SecureSessionPersistence(FakeSecureStorage(), FakeCodec())

        assertEquals(SessionRestoreResult.NoSession, persistence.restore())
    }

    @Test
    fun `authenticated session is saved as one encoded secure snapshot and restored`() = runTest {
        val storage = FakeSecureStorage()
        val session = authenticatedSession()
        val codec = FakeCodec(encodedSession = session)
        val persistence = SecureSessionPersistence(storage, codec)

        assertEquals(SessionPersistenceResult.Success, persistence.save(session))
        assertEquals(1, storage.values.size)
        assertEquals("encoded-session", storage.values[SecureKey("session.snapshot")])
        assertEquals(SessionRestoreResult.Restored(session), persistence.restore())
    }

    @Test
    fun `malformed persisted snapshot is rejected without exposing payload`() = runTest {
        val storage = FakeSecureStorage().apply {
            values[SecureKey("session.snapshot")] = "malformed-sensitive-payload"
        }
        val persistence = SecureSessionPersistence(
            storage,
            FakeCodec(decodeResult = SessionSnapshotDecodeResult.Malformed),
        )

        assertEquals(
            SessionRestoreResult.Rejected(SessionRestoreFailure.MalformedSnapshot),
            persistence.restore(),
        )
    }

    @Test
    fun `unsupported persisted snapshot version is distinguished from malformed data`() = runTest {
        val storage = FakeSecureStorage().apply {
            values[SecureKey("session.snapshot")] = "future-version"
        }
        val persistence = SecureSessionPersistence(
            storage,
            FakeCodec(decodeResult = SessionSnapshotDecodeResult.UnsupportedVersion),
        )

        assertEquals(
            SessionRestoreResult.Rejected(SessionRestoreFailure.UnsupportedSnapshotVersion),
            persistence.restore(),
        )
    }

    @Test
    fun `encoding failure never writes partial session state`() = runTest {
        val storage = FakeSecureStorage()
        val persistence = SecureSessionPersistence(
            storage,
            FakeCodec(encodeResult = SessionSnapshotEncodeResult.Failed),
        )

        assertEquals(
            SessionPersistenceResult.Failed(SessionPersistenceFailure.EncodingFailed),
            persistence.save(authenticatedSession()),
        )
        assertEquals(emptyMap(), storage.values)
    }

    @Test
    fun `storage failures are mapped to privacy safe results`() = runTest {
        val persistence = SecureSessionPersistence(
            FakeSecureStorage(failure = IllegalStateException("secret-storage-details")),
            FakeCodec(),
        )

        assertEquals(
            SessionRestoreResult.Rejected(SessionRestoreFailure.StorageUnavailable),
            persistence.restore(),
        )
        assertEquals(
            SessionPersistenceResult.Failed(SessionPersistenceFailure.StorageUnavailable),
            persistence.save(authenticatedSession()),
        )
        assertEquals(
            SessionPersistenceResult.Failed(SessionPersistenceFailure.StorageUnavailable),
            persistence.clear(),
        )
    }

    @Test
    fun `clearing removes only the owned session snapshot`() = runTest {
        val sessionKey = SecureKey("session.snapshot")
        val otherKey = SecureKey("other.secret")
        val storage = FakeSecureStorage().apply {
            values[sessionKey] = "session"
            values[otherKey] = "must-remain"
        }
        val persistence = SecureSessionPersistence(storage, FakeCodec())

        assertEquals(SessionPersistenceResult.Success, persistence.clear())
        assertEquals(mapOf(otherKey to "must-remain"), storage.values)
    }

    @Test
    fun `coroutine cancellation from storage is never swallowed`() = runTest {
        val persistence = SecureSessionPersistence(
            FakeSecureStorage(failure = CancellationException("cancelled")),
            FakeCodec(),
        )

        assertFailsWith<CancellationException> { persistence.restore() }
        assertFailsWith<CancellationException> { persistence.save(authenticatedSession()) }
        assertFailsWith<CancellationException> { persistence.clear() }
    }

    private fun authenticatedSession() = SessionState.Authenticated(
        subject = SessionSubject("subject-123"),
        tokens = AuthTokens(
            accessToken = Secret.of("access-token"),
            refreshToken = Secret.of("refresh-token"),
            accessTokenExpiresAtEpochMilliseconds = 123_456L,
        ),
    )

    private class FakeCodec(
        private val encodedSession: SessionState.Authenticated? = null,
        private val encodeResult: SessionSnapshotEncodeResult? = null,
        private val decodeResult: SessionSnapshotDecodeResult? = null,
    ) : SessionSnapshotCodec {
        override fun encode(session: SessionState.Authenticated): SessionSnapshotEncodeResult =
            encodeResult ?: SessionSnapshotEncodeResult.Encoded("encoded-session")

        override fun decode(encoded: String): SessionSnapshotDecodeResult =
            decodeResult ?: encodedSession?.let(SessionSnapshotDecodeResult::Decoded)
            ?: SessionSnapshotDecodeResult.Malformed
    }

    private class FakeSecureStorage(
        private val failure: Throwable? = null,
    ) : SecureStorage {
        val values = mutableMapOf<SecureKey, String>()

        override suspend fun read(key: SecureKey): String? {
            failure?.let { throw it }
            return values[key]
        }

        override suspend fun write(key: SecureKey, value: String) {
            failure?.let { throw it }
            values[key] = value
        }

        override suspend fun remove(key: SecureKey) {
            failure?.let { throw it }
            values.remove(key)
        }

        override suspend fun clear() {
            failure?.let { throw it }
            values.clear()
        }
    }
}
