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
import com.carbroz.runtime.sdui.model.BackgroundCommand
import com.carbroz.runtime.sdui.model.BackgroundOperation
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.CommandKind
import com.carbroz.runtime.sdui.model.NavigationOperation
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestCommand
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.RequestResponseMode
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.SduiNavigationCommand
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ActionPreparerTest {
    @Test
    fun screenRequestValidatesFormAndResolvesLatestValue() {
        val fieldId = FormFieldId("value")
        val form = FormStore(listOf(FormFieldDefinition(fieldId, JsonPrimitive("old"))))
        form.update(fieldId, JsonPrimitive("latest"))
        val context = ActionPreparationContext(
            BindingContext.of(BindingNamespace.FORM to form.asBindingValueSource()),
            form,
        )
        val success = assertIs<ActionPreparationResult.Success>(ActionPreparer(coreRegistry()).prepare(screenRequest(), context))
        val prepared = assertIs<PreparedAction.Request>(success.action)
        val destination = assertNotNull(prepared.destination)
        assertEquals(RequestMethod.POST, prepared.method)
        assertEquals("/api/v1/action", prepared.endpoint)
        assertEquals("next-screen", destination.screenId)
        assertEquals(JsonPrimitive("latest"), prepared.payload["value"])
        assertEquals(1, form.state.value.submitAttempts)
    }

    @Test
    fun noScreenRequestDoesNotRequireDestination() {
        val command = RequestCommand(
            method = RequestMethod.POST,
            endpoint = "/api/v1/state",
            destination = null,
            payload = emptyMap(),
            responseMode = RequestResponseMode.NONE,
            validateForm = false,
        )
        val prepared = assertIs<PreparedAction.Request>(
            assertIs<ActionPreparationResult.Success>(
                ActionPreparer(coreRegistry()).prepare(command, ActionPreparationContext(BindingContext.of())),
            ).action,
        )
        assertEquals(null, prepared.destination)
        assertEquals(RequestResponseMode.NONE, prepared.responseMode)
    }

    @Test
    fun invalidFormStopsBeforeRequestPreparation() {
        val fieldId = FormFieldId("value")
        val required = FormValidator { value, _ ->
            if ((value as? JsonPrimitive)?.content.orEmpty().isBlank()) listOf(FormValidationError("required")) else emptyList()
        }
        val form = FormStore(listOf(FormFieldDefinition(fieldId, JsonPrimitive(""), listOf(required))))
        val context = ActionPreparationContext(BindingContext.of(BindingNamespace.FORM to form.asBindingValueSource()), form)
        assertIs<ActionPreparationResult.FormInvalid>(ActionPreparer(coreRegistry()).prepare(screenRequest(), context))
    }

    @Test
    fun missingBindingFailsClosed() {
        val result = ActionPreparer(coreRegistry()).prepare(screenRequest(), ActionPreparationContext(BindingContext.of()))
        val failure = assertIs<ActionPreparationResult.BindingFailure>(result)
        assertIs<BindingResolutionError.MissingSource>(failure.error)
    }

    @Test
    fun navigationAndBackgroundCommandsPrepareWithoutNetworkAssumptions() {
        val preparer = ActionPreparer(coreRegistry())
        val navigation = assertIs<PreparedAction.Navigation>(
            assertIs<ActionPreparationResult.Success>(
                preparer.prepare(SduiNavigationCommand(NavigationOperation.POP), ActionPreparationContext(BindingContext.of())),
            ).action,
        )
        assertEquals(NavigationOperation.POP, navigation.operation)

        val background = assertIs<PreparedAction.Background>(
            assertIs<ActionPreparationResult.Success>(
                preparer.prepare(
                    BackgroundCommand(BackgroundOperation.CANCEL, "generic-task"),
                    ActionPreparationContext(BindingContext.of()),
                ),
            ).action,
        )
        assertEquals("generic-task", background.id)
    }

    @Test
    fun unsupportedCommandDoesNotRequireDispatcherChanges() {
        val command = object : Command { override val kind: CommandKind = CommandKind("FUTURE_COMMAND") }
        assertIs<ActionPreparationResult.UnsupportedCommand>(
            ActionPreparer(coreRegistry()).prepare(command, ActionPreparationContext(BindingContext.of())),
        )
    }

    @Test
    fun registryRejectsDuplicateCommandKinds() {
        val definition = CoreActionDefinitions.all.first()
        val builder = ActionRegistry.builder().register(definition)
        assertFailsWith<IllegalArgumentException> { builder.register(definition) }
    }

    private fun coreRegistry(): ActionRegistry = ActionRegistry.builder().registerAll(CoreActionDefinitions.all).build()

    private fun screenRequest() = RequestCommand(
        method = RequestMethod.POST,
        endpoint = "/api/v1/action",
        destination = ScreenDestination("next-screen", "form-template", NodeType("FORM_TEMPLATE")),
        payload = mapOf("value" to JsonPrimitive("\$form.value")),
    )
}
