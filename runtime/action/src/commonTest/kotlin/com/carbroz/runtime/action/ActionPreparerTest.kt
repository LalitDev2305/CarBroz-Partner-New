package com.carbroz.runtime.action

import com.carbroz.runtime.binding.BindingContext
import com.carbroz.runtime.binding.BindingNamespace
import com.carbroz.runtime.binding.BindingResolutionError
import com.carbroz.runtime.form.FormFieldDefinition
import com.carbroz.runtime.form.FormFieldId
import com.carbroz.runtime.form.FormStore
import com.carbroz.runtime.form.FormValidationError
import com.carbroz.runtime.form.FormValidator
import com.carbroz.runtime.form.asBindingValueSource
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.CommandKind
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestCommand
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ActionPreparerTest {
    @Test
    fun requestPreparationValidatesFormAndResolvesLatestValue() {
        val phoneId = FormFieldId("phone")
        val form = FormStore(listOf(FormFieldDefinition(phoneId, JsonPrimitive("old"))))
        form.update(phoneId, JsonPrimitive("9876543210"))
        val context = ActionPreparationContext(
            bindings = BindingContext.of(BindingNamespace.FORM to form.asBindingValueSource()),
            form = form,
        )
        val preparer = ActionPreparer(coreRegistry())

        val success = assertIs<ActionPreparationResult.Success>(preparer.prepare(request(), context))
        val prepared = assertIs<PreparedAction.Request>(success.action)

        assertEquals(RequestMethod.POST, prepared.method)
        assertEquals("/auth/send-otp", prepared.endpoint)
        assertEquals("otp", prepared.destination.screenId)
        assertEquals("auth_otp", prepared.destination.templateId)
        assertEquals("FORM_TEMPLATE", prepared.destination.templateType.value)
        assertEquals(JsonPrimitive("9876543210"), prepared.payload["phone"])
        assertEquals(1, form.state.value.submitAttempts)
    }

    @Test
    fun invalidFormStopsBeforeRequestPreparation() {
        val phoneId = FormFieldId("phone")
        val required = FormValidator { value, _ ->
            if ((value as? JsonPrimitive)?.content.orEmpty().isBlank()) {
                listOf(FormValidationError("required"))
            } else {
                emptyList()
            }
        }
        val form = FormStore(listOf(FormFieldDefinition(phoneId, JsonPrimitive(""), listOf(required))))
        val context = ActionPreparationContext(
            bindings = BindingContext.of(BindingNamespace.FORM to form.asBindingValueSource()),
            form = form,
        )

        assertIs<ActionPreparationResult.FormInvalid>(ActionPreparer(coreRegistry()).prepare(request(), context))
    }

    @Test
    fun missingBindingFailsClosed() {
        val result = ActionPreparer(coreRegistry()).prepare(
            request(),
            ActionPreparationContext(bindings = BindingContext.of()),
        )

        val failure = assertIs<ActionPreparationResult.BindingFailure>(result)
        assertIs<BindingResolutionError.MissingSource>(failure.error)
    }

    @Test
    fun unsupportedCommandDoesNotRequireDispatcherChanges() {
        val command = object : Command {
            override val kind: CommandKind = CommandKind("FUTURE_COMMAND")
        }

        val result = ActionPreparer(coreRegistry()).prepare(
            command,
            ActionPreparationContext(BindingContext.of()),
        )

        assertIs<ActionPreparationResult.UnsupportedCommand>(result)
    }

    @Test
    fun registryRejectsDuplicateCommandKinds() {
        val definition = CoreActionDefinitions.all.first()
        val builder = ActionRegistry.builder().register(definition)

        assertFailsWith<IllegalArgumentException> {
            builder.register(definition)
        }
    }

    private fun coreRegistry(): ActionRegistry = ActionRegistry.builder()
        .registerAll(CoreActionDefinitions.all)
        .build()

    private fun request() = RequestCommand(
        method = RequestMethod.POST,
        endpoint = "/auth/send-otp",
        destination = ScreenDestination(
            screenId = "otp",
            templateId = "auth_otp",
            templateType = NodeType("FORM_TEMPLATE"),
        ),
        payload = mapOf("phone" to JsonPrimitive(binding("form.phone"))),
    )

    private fun binding(path: String): String = buildString {
        append('$')
        append(path)
    }
}
