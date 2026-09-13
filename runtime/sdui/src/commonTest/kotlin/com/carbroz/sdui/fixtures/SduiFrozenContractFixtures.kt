package com.carbroz.sdui.fixtures

object SduiFrozenContractFixtures {
    val login: String = screen(
        screenId = "partner_login",
        templateId = "partner_login_template",
        templateType = "form_template",
        body = """
            "elements": [
              {"id":"title","type":"text","properties":{"text":"Partner Login"}},
              {"id":"phone","type":"input","binding":{"key":"mobileNumber"},"validation":{"required":true,"pattern":"^[0-9]{10}$","message":"Enter mobile number"},"properties":{"value":""}},
              {"id":"continue","type":"button","properties":{"text":"Continue"},"actions":{"click":{"type":"request","payload":{"method":"POST","endpoint":"/api/v1/auth/login","authentication":"NONE","validate":true,"body":{"phoneNumber":{"__REF_BINDING__":"mobileNumber"},"deviceId":{"__REF_CONTEXT__":"deviceId"}},"responseMode":"destination","navigationMode":"push","contextUpdates":{"authFlow":{"phoneNumber":{"__REF_BINDING__":"mobileNumber"}}}}}}}
            ]
        """.trimIndent().wireRefs(),
    )

    val otp: String = screen(
        screenId = "auth_otp",
        templateId = "auth_otp_template",
        templateType = "form_template",
        body = """
            "elements": [
              {"id":"otp","type":"input","binding":{"key":"otp"},"validation":{"required":true,"pattern":"^[0-9]{6}$","message":"Enter OTP"},"properties":{"value":"","maxLength":6,"keyboardType":"number","presentation":{"type":"segmented","count":6,"spacing":8,"segmentWidth":44,"segmentHeight":52}}},
              {"id":"verify","type":"button","properties":{"text":"Verify"},"actions":{"click":{"type":"request","payload":{"method":"POST","endpoint":"/api/v1/auth/verify","authentication":"NONE","validate":true,"body":{"otp":{"__REF_BINDING__":"otp"},"phoneNumber":{"__REF_CONTEXT__":"authFlow.phoneNumber"},"challengeId":{"__REF_RESPONSE__":"data.challengeId"}},"responseMode":"destination","navigationMode":"reset"}}}}
            ]
        """.trimIndent().wireRefs(),
    )

    val dashboard: String = screen(
        screenId = "partner_dashboard",
        templateId = "dashboard_template",
        templateType = "stack_template",
        body = """
            "elements": [
              {"id":"hero","type":"image","properties":{"url":"https://cdn.example.com/dashboard.png","width":320,"height":160}},
              {"id":"heading","type":"text","properties":{"text":"Dashboard"}}
            ]
        """.trimIndent(),
    )

    val bookingDetails: String = screen(
        screenId = "booking_details",
        templateId = "booking_details_template",
        templateType = "default_template",
        body = """
            "sections": [
              {
                "id":"booking_section","type":"stack_section","properties":{"axis":"vertical"},
                "elements":[{"id":"booking_title","type":"text","properties":{"text":"Booking #1"}}],
                "groups":[{"id":"actions","type":"stack_group","properties":{"axis":"horizontal"},"elements":[{"id":"accept","type":"button","properties":{"text":"Accept"}}]}]
              }
            ]
        """.trimIndent(),
    )

    val allNodeTypes: String = """
        {
          "screenId":"all_nodes",
          "schemaVersion":"3.0",
          "targetApp":"PARTNER",
          "template":{
            "id":"all_nodes_template","type":"stack_template","properties":{"axis":"vertical"},
            "components":[{
              "id":"root","type":"stack_component","properties":{"axis":"vertical"},
              "elements":[
                {"id":"text","type":"text","properties":{"spans":[{"text":"Read "},{"text":"Terms","underline":true,"onClick":{"type":"external_uri","payload":{"uri":{"__REF_CONTEXT__":"legal.termsUri"}}}}],"leading":{"type":"divider","properties":{"orientation":"vertical"}},"trailing":{"type":"icon","properties":{"name":"arrow_forward"}}}},
                {"id":"image","type":"image","properties":{"url":"https://cdn.example.com/image.png","width":120,"height":80}},
                {"id":"input","type":"input","binding":{"key":"value"},"properties":{"value":"","maxLength":6,"leading":{"type":"icon","properties":{"name":"edit"}},"presentation":{"type":"segmented","count":6}}},
                {"id":"button","type":"button","properties":{"text":"Continue","trailing":{"type":"icon","properties":{"name":"arrow_forward"}}}}
              ],
              "sections":[{
                "id":"section","type":"stack_section","properties":{"axis":"vertical"},
                "elements":[{"id":"section_text","type":"text","properties":{"text":"Section"}}],
                "groups":[{"id":"group","type":"stack_group","properties":{"axis":"horizontal"},"elements":[{"id":"group_text","type":"text","properties":{"text":"Group"}}]}]
              }]
            }]
          }
        }
    """.trimIndent().wireRefs()

    val allActionTypes: String = """
        {
          "screenId":"all_actions",
          "schemaVersion":"3.0",
          "targetApp":"PARTNER",
          "template":{
            "id":"all_actions_template","type":"form_template",
            "components":[{
              "id":"root","type":"stack_component",
              "elements":[{
                "id":"action_host","type":"button","properties":{"text":"Actions"},
                "actions":{
                  "request":{"type":"request","payload":{"method":"POST","endpoint":"/api/v1/jobs/1/accept","authentication":"SESSION","validate":false,"body":{"jobId":{"__REF_LITERAL__":"1"}},"responseMode":"none","navigationMode":"push"}},
                  "navigate":{"type":"navigate","payload":{"screenId":"booking_details","templateId":"booking_details_template","templateType":"default_template","endpoint":"/api/v1/screens/booking/1","method":"GET","authentication":"SESSION"},"navigationMode":"push"},
                  "present":{"type":"present","targetId":"cancel_sheet","payload":{"presentation":"bottom_sheet"}},
                  "dismiss":{"type":"dismiss","targetId":"cancel_sheet"},
                  "state":{"type":"state","targetId":"action_host","payload":{"operation":"set","property":"loading","value":true}},
                  "external":{"type":"external_uri","payload":{"uri":{"__REF_CONTEXT__":"legal.termsUri"}}},
                  "sequence":{"type":"sequence","payload":{"actions":[{"type":"state","targetId":"action_host","payload":{"operation":"set","property":"enabled","value":false}},{"type":"dismiss","targetId":"cancel_sheet"}]}}
                }
              }]
            }]
          }
        }
    """.trimIndent().wireRefs()

    val allValueReferences: String = """
        {
          "binding":{"__REF_BINDING__":"phone"},
          "context":{"__REF_CONTEXT__":"authFlow.phoneNumber"},
          "response":{"__REF_RESPONSE__":"data.challengeId"},
          "literal":{"__REF_LITERAL__":{"source":"server"}},
          "nested":[{"phone":{"__REF_BINDING__":"phone"}},{"challenge":{"__REF_RESPONSE__":"data.challengeId"}}]
        }
    """.trimIndent().wireRefs()

    val unsupportedVocabulary: String = """
        {
          "screenId":"unsupported",
          "schemaVersion":"3.0",
          "targetApp":"PARTNER",
          "template":{"id":"unsupported_template","type":"unknown_template","components":[]}
        }
    """.trimIndent()

    val unsupportedSpanAction: String = """
        {
          "screenId":"unsupported_span_action",
          "schemaVersion":"3.0",
          "targetApp":"PARTNER",
          "template":{
            "id":"unsupported_span_template","type":"stack_template","properties":{"axis":"vertical"},
            "components":[{
              "id":"root","type":"stack_component","properties":{"axis":"vertical"},
              "elements":[{"id":"legal","type":"text","properties":{"spans":[{"text":"Terms","onClick":{"type":"future_action","payload":{}}}]}}]
            }]
          }
        }
    """.trimIndent()

    private fun screen(screenId: String, templateId: String, templateType: String, body: String): String = """
        {
          "screenId":"$screenId",
          "schemaVersion":"3.0",
          "targetApp":"PARTNER",
          "template":{
            "id":"$templateId","type":"$templateType","properties":{"axis":"vertical"},
            "components":[{"id":"root","type":"stack_component","properties":{"axis":"vertical"},$body}]
          }
        }
    """.trimIndent()

    private fun String.wireRefs(): String =
        replace("__REF_BINDING__", "\$binding")
            .replace("__REF_CONTEXT__", "\$context")
            .replace("__REF_RESPONSE__", "\$response")
            .replace("__REF_LITERAL__", "\$literal")
}
