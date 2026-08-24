package com.carbroz.runtime.binding

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class BindingResolverTest {
    private val resolver = BindingResolver()

    @Test
    fun parsesOnlyStrictSemanticReferences() {
        assertEquals(
            BindingReference(BindingNamespace.FORM, listOf("phone")),
            BindingReference.parse("${'$'}form.phone"),
        )
        assertEquals(
            BindingReference(BindingNamespace.SCREEN, listOf("booking", "id")),
            BindingReference.parse("${'$'}screen.booking.id"),
        )
        assertNull(BindingReference.parse("form.phone"))
        assertNull(BindingReference.parse("${'$'}unknown.value"))
        assertNull(BindingReference.parse("${'$'}form"))
        assertNull(BindingReference.parse("${'$'}form..phone"))
    }

    @Test
    fun resolvesFormValueAndPreservesPrimitiveType() {
        val context = BindingContext.of(
            BindingNamespace.FORM to BindingValueSource { path ->
                when (path) {
                    listOf("phone") -> JsonPrimitive("9876543210")
                    listOf("rememberMe") -> JsonPrimitive(true)
                    else -> null
                }
            },
        )

        val payload = JsonObject(
            mapOf(
                "phone" to JsonPrimitive("${'$'}form.phone"),
                "rememberMe" to JsonPrimitive("${'$'}form.rememberMe"),
            ),
        )

        val success = assertIs<BindingResolutionResult.Success>(resolver.resolve(payload, context))
        val objectValue = assertIs<JsonObject>(success.value)
        assertEquals(JsonPrimitive("9876543210"), objectValue["phone"])
        assertEquals(JsonPrimitive(true), objectValue["rememberMe"])
    }

    @Test
    fun resolvesNestedObjectsAndArraysRecursively() {
        val context = BindingContext.of(
            BindingNamespace.SCREEN to BindingValueSource { path ->
                if (path == listOf("bookingId")) JsonPrimitive(42) else null
            },
        )
        val payload = JsonObject(
            mapOf(
                "items" to JsonArray(
                    listOf(
                        JsonObject(mapOf("bookingId" to JsonPrimitive("${'$'}screen.bookingId"))),
                    ),
                ),
            ),
        )

        val success = assertIs<BindingResolutionResult.Success>(resolver.resolve(payload, context))
        val objectValue = assertIs<JsonObject>(success.value)
        val items = assertIs<JsonArray>(objectValue["items"])
        val item = assertIs<JsonObject>(items.single())
        assertEquals(JsonPrimitive(42), item["bookingId"])
    }

    @Test
    fun leavesLiteralStringsUntouched() {
        val literal = "Call ${'$'}form.phone now"
        val result = resolver.resolve(JsonPrimitive(literal), BindingContext.of())
        val success = assertIs<BindingResolutionResult.Success>(result)
        assertEquals(JsonPrimitive(literal), success.value)
    }

    @Test
    fun failsClosedWhenSourceOrValueIsUnavailable() {
        val noSource = resolver.resolve(JsonPrimitive("${'$'}form.phone"), BindingContext.of())
        assertIs<BindingResolutionError.MissingSource>(
            assertIs<BindingResolutionResult.Failure>(noSource).error,
        )

        val context = BindingContext.of(BindingNamespace.FORM to BindingValueSource { null })
        val noValue = resolver.resolve(JsonPrimitive("${'$'}form.phone"), context)
        assertIs<BindingResolutionError.MissingValue>(
            assertIs<BindingResolutionResult.Failure>(noValue).error,
        )
    }
}
