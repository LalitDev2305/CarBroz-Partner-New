package com.carbroz.partner.composition

import com.carbroz.feature.dynamic.DynamicDestination
import com.carbroz.feature.dynamic.DynamicRestorePolicy
import com.carbroz.feature.dynamic.DynamicScreenInstruction
import com.carbroz.feature.dynamic.DynamicScreenInstructionCodec
import com.carbroz.feature.dynamic.DynamicScreenRequest
import com.carbroz.foundation.navigation.RestoredDestination
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.ScreenTransition
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DynamicNavigationTest {
    @Test
    fun safeDynamicDestinationRoundTripsThroughProcessRestorationContract() {
        val persistence = DynamicNavigationPersistence()
        val original = DynamicDestination(instruction())
        val persisted = persistence.persist(original) ?: error("destination must be persistable")
        val restored = assertIs<DynamicDestination>(persistence.restore(persisted))

        assertEquals(original, restored)
        assertEquals(original.navigationId, restored.navigationId)
    }

    @Test
    fun cacheOnlyDestinationIsNeverPersistedBecauseRestorationCouldReplayMutation() {
        val persistence = DynamicNavigationPersistence()
        val destination = DynamicDestination(
            instruction().copy(
                request = DynamicScreenRequest(RequestMethod.POST, "/api/v1/action"),
                restorePolicy = DynamicRestorePolicy.CACHE_ONLY,
            ),
        )
        assertNull(persistence.persist(destination))
    }

    @Test
    fun restorationFailsClosedWhenIdentityDoesNotMatchPayload() {
        val persistence = DynamicNavigationPersistence()
        val payload = DynamicScreenInstructionCodec().encode(instruction())
        assertNull(
            persistence.restore(
                RestoredDestination(
                    navigationId = "dynamic:tampered:screen-1:template-7",
                    payload = payload,
                ),
            ),
        )
    }

    @Test
    fun processStateCodecRoundTripsSemanticRestorationData() {
        val codec = DynamicNavigationProcessStateCodec()
        val expected = listOf(
            RestoredDestination("splash"),
            RestoredDestination(
                navigationId = "dynamic:instance-9:screen-1:template-7",
                payload = DynamicScreenInstructionCodec().encode(instruction()),
            ),
        )
        assertEquals(expected, codec.decode(codec.encode(expected)))
    }

    @Test
    fun malformedProcessStateFailsClosedBeforeDestinationRestoration() {
        assertNull(DynamicNavigationProcessStateCodec().decode("not-json"))
    }

    private fun instruction() = DynamicScreenInstruction(
        destination = ScreenDestination("screen-1", "template-7", NodeType("FORM")),
        request = DynamicScreenRequest(RequestMethod.GET, "/api/v1/screen/next", JsonObject(emptyMap())),
        transition = ScreenTransition.PUSH,
        backStackKey = "instance-9",
        restorePolicy = DynamicRestorePolicy.CACHE_FIRST,
    )
}
