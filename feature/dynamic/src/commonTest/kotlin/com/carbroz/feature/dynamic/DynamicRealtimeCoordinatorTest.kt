package com.carbroz.feature.dynamic

import com.carbroz.data.realtime.RealtimeMessage
import com.carbroz.runtime.sdui.model.RequestMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DynamicRealtimeCoordinatorTest {
    private val decoder = DynamicRealtimeEventDecoder()

    @Test
    fun dataEventRemainsProductNeutralRuntimeData() {
        val event = assertIs<DynamicRealtimeEvent.Data>(
            decoder.decode(RealtimeMessage("""{"type":"DATA","data":{"status":"updated"}}""")),
        )
        assertEquals("updated", event.values["status"]?.toString()?.trim('"'))
    }

    @Test
    fun refreshEventDoesNotCarryAReplayRequest() {
        assertEquals(
            DynamicRealtimeEvent.RefreshCurrent,
            decoder.decode(RealtimeMessage("""{"type":"REFRESH"}""")),
        )
    }

    @Test
    fun screenEventUsesCanonicalDynamicInstructionCodec() {
        val event = assertIs<DynamicRealtimeEvent.Navigate>(
            decoder.decode(
                RealtimeMessage(
                    """
                    {
                      "type":"SCREEN",
                      "nextScreen":{
                        "screenId":"screen-next",
                        "templateId":"template-form",
                        "templateType":"FORM_TEMPLATE",
                        "endpoint":"/api/v1/screen/next",
                        "method":"GET",
                        "transition":"PUSH",
                        "backStackKey":"next-instance"
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals("screen-next", event.instruction.destination.screenId)
        assertEquals("FORM_TEMPLATE", event.instruction.destination.templateType.value)
        assertEquals(RequestMethod.GET, event.instruction.request.method)
        assertEquals("/api/v1/screen/next", event.instruction.request.endpoint)
    }

    @Test
    fun malformedOrUntrustedScreenInstructionFailsClosed() {
        assertNull(
            decoder.decode(
                RealtimeMessage(
                    """{"type":"SCREEN","nextScreen":{"screenId":"x","templateId":"y","templateType":"FORM","endpoint":"https://evil.example/x"}}""",
                ),
            ),
        )
        assertNull(decoder.decode(RealtimeMessage("not-json")))
    }
}
