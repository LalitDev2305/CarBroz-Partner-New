package com.carbroz.partner.composition

import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.JsonObject

class DynamicNavigationTest {
    @Test
    fun dynamicDestinationHasStableBusinessAgnosticIdentity() {
        val instruction = DynamicScreenInstruction(
            destination = ScreenDestination("screen-1", "template-7", NodeType("FORM")),
            request = DynamicScreenRequest(RequestMethod.GET, "/api/v1/screen/next", JsonObject(emptyMap())),
            transition = DynamicTransition.PUSH,
            backStackKey = "instance-9",
        )
        assertEquals("dynamic:instance-9:screen-1:template-7", DynamicDestination(instruction).navigationId)
    }

    @Test
    fun dynamicRequestRejectsAbsoluteOrProtocolRelativeEndpoint() {
        assertFailsWith<IllegalArgumentException> { DynamicScreenRequest(RequestMethod.GET, "https://example.com/screen") }
        assertFailsWith<IllegalArgumentException> { DynamicScreenRequest(RequestMethod.GET, "//example.com/screen") }
    }
}
