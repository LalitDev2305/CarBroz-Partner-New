package com.carbroz.foundation.session

import com.carbroz.foundation.security.Secret
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Versioned JSON codec for the single encrypted session snapshot.
 *
 * This codec is intentionally strict: unknown fields are rejected, snapshot size is bounded, and
 * credential lengths are validated before materializing a session. The encoded JSON is not safe for
 * plaintext persistence; [SecureSessionPersistence] is the required storage boundary.
 */
class JsonSessionSnapshotCodec(
    private val json: Json = DEFAULT_JSON,
) : SessionSnapshotCodec {
    override fun encode(session: SessionState.Authenticated): SessionSnapshotEncodeResult = try {
        val snapshot = PersistedSessionSnapshot(
            version = CURRENT_VERSION,
            subject = session.subject.value,
            accessToken = session.tokens.accessToken.reveal(),
            refreshToken = session.tokens.refreshToken?.reveal(),
            accessTokenExpiresAtEpochMilliseconds = session.tokens.accessTokenExpiresAtEpochMilliseconds,
        )
        if (!snapshot.hasValidCredentialLengths()) return SessionSnapshotEncodeResult.Failed

        val encoded = json.encodeToString(PersistedSessionSnapshot.serializer(), snapshot)
        if (encoded.length > MAX_SNAPSHOT_CHARACTERS) {
            SessionSnapshotEncodeResult.Failed
        } else {
            SessionSnapshotEncodeResult.Encoded(encoded)
        }
    } catch (_: IllegalArgumentException) {
        SessionSnapshotEncodeResult.Failed
    } catch (_: SerializationException) {
        SessionSnapshotEncodeResult.Failed
    }

    override fun decode(encoded: String): SessionSnapshotDecodeResult {
        if (encoded.isEmpty() || encoded.length > MAX_SNAPSHOT_CHARACTERS) {
            return SessionSnapshotDecodeResult.Malformed
        }

        val snapshot = try {
            json.decodeFromString(PersistedSessionSnapshot.serializer(), encoded)
        } catch (_: IllegalArgumentException) {
            return SessionSnapshotDecodeResult.Malformed
        } catch (_: SerializationException) {
            return SessionSnapshotDecodeResult.Malformed
        }

        if (snapshot.version != CURRENT_VERSION) return SessionSnapshotDecodeResult.UnsupportedVersion
        if (!snapshot.hasValidCredentialLengths()) return SessionSnapshotDecodeResult.Malformed

        return try {
            SessionSnapshotDecodeResult.Decoded(
                SessionState.Authenticated(
                    subject = SessionSubject(snapshot.subject),
                    tokens = AuthTokens(
                        accessToken = Secret.of(snapshot.accessToken),
                        refreshToken = snapshot.refreshToken?.let(Secret::of),
                        accessTokenExpiresAtEpochMilliseconds = snapshot.accessTokenExpiresAtEpochMilliseconds,
                    ),
                ),
            )
        } catch (_: IllegalArgumentException) {
            SessionSnapshotDecodeResult.Malformed
        }
    }

    private fun PersistedSessionSnapshot.hasValidCredentialLengths(): Boolean =
        subject.length <= MAX_SUBJECT_CHARACTERS &&
            accessToken.length in 1..MAX_TOKEN_CHARACTERS &&
            (refreshToken == null || refreshToken.length in 1..MAX_TOKEN_CHARACTERS)

    @Serializable
    private data class PersistedSessionSnapshot(
        val version: Int,
        val subject: String,
        val accessToken: String,
        val refreshToken: String? = null,
        val accessTokenExpiresAtEpochMilliseconds: Long? = null,
    )

    private companion object {
        const val CURRENT_VERSION = 1
        const val MAX_SNAPSHOT_CHARACTERS = 65_536
        const val MAX_SUBJECT_CHARACTERS = 256
        const val MAX_TOKEN_CHARACTERS = 16_384

        val DEFAULT_JSON = Json {
            ignoreUnknownKeys = false
            explicitNulls = true
            encodeDefaults = true
            isLenient = false
        }
    }
}
