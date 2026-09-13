package com.carbroz.sdui.value

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SduiValueResolverTest {
    private val resolver = SduiValueResolver()
    private val context = SduiExecutionContext(
        bindings = mapOf("phone" to JsonPrimitive("9999999999")),
        context = JsonObject(
            mapOf(
                "flow" to JsonObject(mapOf("id" to JsonPrimitive("flow-1"))),
            ),
        ),
        response = JsonObject(
            mapOf(
                "data" to JsonObject(
                    mapOf(
                        "challengeId" to JsonPrimitive("challenge-2"),
                        "items" to JsonArray(listOf(JsonPrimitive("first"))),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun resolvesBindingContextResponseAndLiteralReferences() {
        assertResolved(reference("\$binding", "phone"), JsonPrimitive("9999999999"))
        assertResolved(reference("\$context", "flow.id"), JsonPrimitive("flow-1"))
        assertResolved(reference("\$response", "data.challengeId"), JsonPrimitive("challenge-2"))
        assertResolved(JsonObject(mapOf("\$literal" to JsonPrimitive("literal-value"))), JsonPrimitive("literal-value"))
    }

    @Test
    fun resolvesNestedObjectsAndArraysRecursively() {
        val input = JsonObject(
            mapOf(
                "phone" to reference("\$binding", "phone"),
                "nested" to JsonObject(
                    mapOf("flowId" to reference("\$context", "flow.id")),
                ),
                "values" to JsonArray(
                    listOf(
                        reference("\$response", "data.items.0"),
                        JsonObject(mapOf("\$literal" to JsonPrimitive(7))),
                    ),
                ),
            ),
        )

        val result = assertIs<SduiValueResolution.Success>(resolver.resolve(input, context))
        val resolved = assertIs<JsonObject>(result.value)

        assertEquals(JsonPrimitive("9999999999"), resolved["phone"])
        assertEquals(JsonPrimitive("flow-1"), (resolved["nested"] as JsonObject)["flowId"])
        assertEquals(JsonPrimitive("first"), (resolved["values"] as JsonArray)[0])
        assertEquals(JsonPrimitive(7), (resolved["values"] as JsonArray)[1])
    }

    @Test
    fun missingReferencesFailDeterministically() {
        assertFailure(reference("\$binding", "missing"), "binding_missing:missing")
        assertFailure(reference("\$context", "missing.path"), "context_missing:missing.path")
        assertFailure(reference("\$response", "missing.path"), "response_missing:missing.path")
    }

    private fun reference(key: String, value: String): JsonObject =
        JsonObject(mapOf(key to JsonPrimitive(value)))

    private fun assertResolved(input: JsonObject, expected: JsonPrimitive) {
        val result = assertIs<SduiValueResolution.Success>(resolver.resolve(input, context))
        assertEquals(expected, result.value)
    }

    private fun assertFailure(input: JsonObject, expectedReason: String) {
        val result = assertIs<SduiValueResolution.Failure>(resolver.resolve(input, context))
        assertEquals(expectedReason, result.reason)
    }
}
