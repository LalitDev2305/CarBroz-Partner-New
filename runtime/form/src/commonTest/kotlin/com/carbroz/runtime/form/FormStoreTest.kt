package com.carbroz.runtime.form

import com.carbroz.runtime.binding.BindingContext
import com.carbroz.runtime.binding.BindingNamespace
import com.carbroz.runtime.binding.BindingResolutionResult
import com.carbroz.runtime.binding.BindingResolver
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FormStoreTest {
    private val phoneId = FormFieldId("phone")

    @Test
    fun updateTracksCurrentDirtyAndTouchedState() {
        val store = FormStore(listOf(FormFieldDefinition(phoneId, JsonPrimitive(""))))

        assertTrue(store.update(phoneId, JsonPrimitive("9876543210")))

        val field = store.state.value[phoneId]!!
        assertEquals(JsonPrimitive("9876543210"), field.value)
        assertTrue(field.dirty)
        assertTrue(field.touched)
        assertTrue(store.state.value.dirty)
    }

    @Test
    fun validationIsOwnedByFormRuntime() {
        val required = FormValidator { value, _ ->
            val text = (value as? JsonPrimitive)?.content.orEmpty()
            if (text.isBlank()) listOf(FormValidationError("required")) else emptyList()
        }
        val store = FormStore(listOf(FormFieldDefinition(phoneId, JsonPrimitive(""), listOf(required))))

        assertFalse(store.validate())
        assertEquals(listOf(FormValidationError("required")), store.state.value[phoneId]!!.errors)
        assertEquals(1, store.state.value.submitAttempts)

        store.update(phoneId, JsonPrimitive("9876543210"))
        assertTrue(store.validate())
    }

    @Test
    fun bindingReadsLatestValueAtResolutionTime() {
        val store = FormStore(listOf(FormFieldDefinition(phoneId, JsonPrimitive("old"))))
        val context = BindingContext.of(BindingNamespace.FORM to store.asBindingValueSource())
        val resolver = BindingResolver()

        store.update(phoneId, JsonPrimitive("9876543210"))

        val result = resolver.resolve(
            JsonObject(mapOf("phone" to JsonPrimitive("\$form.phone"))),
            context,
        )
        val success = assertIs<BindingResolutionResult.Success>(result)
        assertEquals(
            JsonObject(mapOf("phone" to JsonPrimitive("9876543210"))),
            success.value,
        )
    }

    @Test
    fun resetRestoresInitialState() {
        val store = FormStore(listOf(FormFieldDefinition(phoneId, JsonPrimitive("initial"))))
        store.update(phoneId, JsonPrimitive("changed"))
        store.validate()

        store.reset()

        val state = store.state.value
        assertEquals(JsonPrimitive("initial"), state[phoneId]!!.value)
        assertFalse(state.dirty)
        assertFalse(state[phoneId]!!.touched)
        assertEquals(0, state.submitAttempts)
    }
}
