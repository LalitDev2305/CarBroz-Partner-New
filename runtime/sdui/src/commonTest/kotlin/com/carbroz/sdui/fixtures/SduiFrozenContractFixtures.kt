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
              {"id":"continue","type":"button","properties":{"text":"Continue"},"actions":{"click":{"type":"request","payload":{"method":"POST","endpoint":"/api/v1/auth/login","authentication":"NONE","validate":true,"body":{"phoneNumber":{"${'$'}binding":"mobileNumber"},"deviceId":{"${'$'}context":"deviceId"}},"responseMode":"destination","navigationMode":"push","contextUpdates":{"authFlow":{"phoneNumber":{"${'$'}binding":"mobileNumber"}}}}}}}
            ]
        """.trimIndent(),
    )

    val otp: String = screen(
        screenId = "auth_otp",
        templateId = "auth_otp_template",
        templateType = "form_template",
        body = """
            "elements": [
              {"id":"otp","type":"input","binding":{"key":"otp"},"validation":{"required":true,"pattern":"^[0-9]{6}$","message":"Enter OTP"},"properties":{"value":"","maxLength":6,"keyboardType":"number","presentation":{"type":"segmented","count":6,"spacing":8,"segmentWidth":44,"segmentHeight":52}}},
              {"id":"verify","type":"button","properties":{"text":"Verify"},"actions":{"click":{"type":"request","payload":{"method":"POST","endpoint":"/api/v1/auth/verify","authentication":"NONE","validate":true,"body":{"otp":{"${'$'}binding":"otp"},"phoneNumber":{"${'$'}context":"authFlow.phoneNumber"},"challengeId":{"${'$'}response":"data.challengeId"}},"responseMode":"destination","navigationMode":"reset"}}}},
              {"id":"resend","type":"text","properties":{"text":"Resend OTP"},"actions":{"click":{"type":"request","payload":{"method":"POST","endpoint":"/api/v1/auth/resend","authentication":"NONE","body":{"phoneNumber":{"${'$'}context":"authFlow.phoneNumber"}},"responseMode":"none"}}}}
            ]
        """.trimIndent(),
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
                {"id":"text","type":"text","properties":{"text":"Hello","leading":{"type":"divider","properties":{"orientation":"vertical"}},"trailing":{"type":"icon","properties":{"name":"arrow_forward"}}}},
                {"id":"image","type":"image","properties":{"url":"https://cdn.example.com/image.png","width":120,"height":80}},
                {"id":"input","type":"input","binding":{"key":"value"},"properties":{"value":"","maxLength":6,"presentation":{"type":"segmented","count":6}}},
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
    """.trimIndent()

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
                  "request":{"type":"request","payload":{"method":"POST","endpoint":"/api/v1/jobs/1/accept","authentication":"SESSION","validate":false,"body":{"jobId":{"${'$'}literal":"1"}},"responseMode":"none","navigationMode":"push"}},
                  "navigate":{"type":"navigate","payload":{"screenId":"booking_details","templateId":"booking_details_template","templateType":"default_template","endpoint":"/api/v1/screens/booking/1","method":"GET","authentication":"SESSION"},"navigationMode":"push"},
                  "present":{"type":"present","targetId":"cancel_sheet","payload":{"presentation":"bottom_sheet"}},
                  "dismiss":{"type":"dismiss","targetId":"cancel_sheet"},
                  "state":{"type":"state","targetId":"action_host","payload":{"operation":"set","property":"loading","value":true}},
                  "external":{"type":"external_uri","payload":{"uri":{"${'$'}context":"legal.termsUri"}}},
                  "sequence":{"type":"sequence","payload":{"actions":[{"type":"state","targetId":"action_host","payload":{"operation":"set","property":"enabled","value":false}},{"type":"dismiss","targetId":"cancel_sheet"}]}}
                }
              }]
            }]
          }
        }
    """.trimIndent()

    val allValueReferences: String = """
        {
          "binding":{"${'$'}binding":"phone"},
          "context":{"${'$'}context":"authFlow.phoneNumber"},
          "response":{"${'$'}response":"data.challengeId"},
          "literal":{"${'$'}literal":{"source":"server"}},
          "nested":[{"phone":{"${'$'}binding":"phone"}},{"challenge":{"${'$'}response":"data.challengeId"}}]
        }
    """.trimIndent()

    val unsupportedVocabulary: String = """
        {
          "screenId":"unsupported",
          "schemaVersion":"3.0",
          "targetApp":"PARTNER",
          "template":{"id":"unsupported_template","type":"unknown_template","components":[]}
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
}
