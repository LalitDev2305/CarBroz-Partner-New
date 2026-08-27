package com.carbroz.feature.dynamic

import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.ScreenTransition
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DynamicNavigationTest {
    @Test
    fun dynamicDestinationHasStableBusinessAgnosticIdentity() {
        assertEquals("dynamic:instance-9:screen-1:template-7", DynamicDestination(instruction()).navigationId)
    }

    @Test
    fun dynamicRequestRejectsAbsoluteOrProtocolRelativeEndpoint() {
        assertFailsWith<IllegalArgumentException> { DynamicScreenRequest(RequestMethod.GET, "https://example.com/screen") }
        assertFailsWith<IllegalArgumentException> { DynamicScreenRequest(RequestMethod.GET, "//example.com/screen") }
    }

    @Test
    fun instructionCodecRoundTripsFeatureOwnedDestinationContract() {
        val original = instruction()
        val decoded = assertIs<DynamicInstructionDecodeResult.Success>(
            DynamicScreenInstructionCodec().decode(DynamicScreenInstructionCodec().encode(original)),
        )
        assertEquals(original, decoded.instruction)
    }

    private fun instruction() = DynamicScreenInstruction(
        destination = ScreenDestination("screen-1", "template-7", NodeType("FORM")),
        request = DynamicScreenRequest(RequestMethod.GET, "/api/v1/screen/next", JsonObject(emptyMap())),
        transition = ScreenTransition.PUSH,
        backStackKey = "instance-9",
        restorePolicy = DynamicRestorePolicy.CACHE_FIRST,
    )
}
