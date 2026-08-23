package com.carbroz.foundation.session

import com.carbroz.foundation.security.Secret
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsonSessionSnapshotCodecTest {
    private val codec = JsonSessionSnapshotCodec()

    @Test
    fun roundTripsAuthenticatedSession() {
        val original = SessionState.Authenticated(
            subject = SessionSubject("partner-42"),
            tokens = AuthTokens(
                accessToken = Secret.of("access-token"),
                refreshToken = Secret.of("refresh-token"),
                accessTokenExpiresAtEpochMilliseconds = 123_456L,
            ),
        )

        val encoded = assertIs<SessionSnapshotEncodeResult.Encoded>(codec.encode(original))
        val decoded = assertIs<SessionSnapshotDecodeResult.Decoded>(codec.decode(encoded.value))

        assertEquals(original, decoded.session)
    }

    @Test
    fun rejectsUnknownVersion() {
        val result = codec.decode(
            """{"version":2,"subject":"partner","accessToken":"access","refreshToken":null,"accessTokenExpiresAtEpochMilliseconds":null}""",
        )

        assertEquals(SessionSnapshotDecodeResult.UnsupportedVersion, result)
    }

    @Test
    fun rejectsUnknownFieldsAndMalformedPayloads() {
        assertEquals(
            SessionSnapshotDecodeResult.Malformed,
            codec.decode(
                """{"version":1,"subject":"partner","accessToken":"access","refreshToken":null,"accessTokenExpiresAtEpochMilliseconds":null,"unexpected":true}""",
            ),
        )
        assertEquals(SessionSnapshotDecodeResult.Malformed, codec.decode("not-json"))
        assertEquals(SessionSnapshotDecodeResult.Malformed, codec.decode(""))
    }

    @Test
    fun rejectsBlankOrOversizedCredentials() {
        assertEquals(
            SessionSnapshotDecodeResult.Malformed,
            codec.decode(
                """{"version":1,"subject":"partner","accessToken":"","refreshToken":null,"accessTokenExpiresAtEpochMilliseconds":null}""",
            ),
        )

        val oversized = "x".repeat(16_385)
        assertEquals(
            SessionSnapshotEncodeResult.Failed,
            codec.encode(
                SessionState.Authenticated(
                    subject = SessionSubject("partner"),
                    tokens = AuthTokens(
                        accessToken = Secret.of(oversized),
                        refreshToken = null,
                        accessTokenExpiresAtEpochMilliseconds = null,
                    ),
                ),
            ),
        )
    }
}
