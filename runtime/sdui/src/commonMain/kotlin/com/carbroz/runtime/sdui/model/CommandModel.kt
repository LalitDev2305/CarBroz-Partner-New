package com.carbroz.runtime.sdui.model

import kotlinx.serialization.json.JsonElement

@kotlin.jvm.JvmInline
value class CommandKind(val value: String)

interface Command { val kind: CommandKind }

enum class RequestMethod { GET, POST, PUT, PATCH, DELETE }
enum class RequestAuthentication { NONE, SESSION, OPTIONAL_SESSION }
enum class RequestResponseMode { SCREEN, NONE }
enum class ScreenTransition { PUSH, REPLACE, RESET, STAY }

data class ScreenDestination(
    val screenId: String,
    val templateId: String,
    val templateType: NodeType,
) {
    init {
        require(screenId.isNotBlank()) { "Screen destination screenId must not be blank" }
        require(templateId.isNotBlank()) { "Screen destination templateId must not be blank" }
    }
}

data class RequestCommand(
    val method: RequestMethod,
    val endpoint: String,
    val destination: ScreenDestination? = null,
    val payload: Map<String, JsonElement>,
    val authentication: RequestAuthentication = RequestAuthentication.SESSION,
    val responseMode: RequestResponseMode = RequestResponseMode.SCREEN,
    val transition: ScreenTransition = ScreenTransition.PUSH,
    val backStackKey: String? = null,
    val validateForm: Boolean = true,
) : Command {
    init {
        require(endpoint.startsWith('/')) { "Request endpoint must be relative" }
        require(!endpoint.startsWith("//")) { "Request endpoint must not be protocol-relative" }
        require("://" !in endpoint) { "Request endpoint must not contain an absolute URL" }
        require(responseMode != RequestResponseMode.SCREEN || destination != null) {
            "SCREEN response mode requires a destination"
        }
        require(backStackKey == null || backStackKey.isNotBlank()) { "Back-stack key must not be blank" }
    }
    override val kind: CommandKind = KIND
    companion object { val KIND = CommandKind("REQUEST") }
}

data class CapabilityCommand(
    val capability: String,
    val operation: String,
    val arguments: Map<String, JsonElement> = emptyMap(),
) : Command {
    init {
        require(capability.isNotBlank()) { "Capability command capability must not be blank" }
        require(operation.isNotBlank()) { "Capability command operation must not be blank" }
    }
    override val kind: CommandKind = KIND
    companion object { val KIND = CommandKind("CAPABILITY") }
}

enum class NavigationOperation { POP, POP_TO }

data class SduiNavigationCommand(
    val operation: NavigationOperation,
    val targetNavigationId: String? = null,
) : Command {
    init {
        require(operation != NavigationOperation.POP_TO || !targetNavigationId.isNullOrBlank()) {
            "POP_TO requires targetNavigationId"
        }
    }
    override val kind: CommandKind = KIND
    companion object { val KIND = CommandKind("NAVIGATION") }
}

enum class PresentationKind { MESSAGE, DIALOG, SHEET }
enum class PresentationOperation { SHOW, DISMISS }

data class PresentationCommand(
    val operation: PresentationOperation,
    val presentationKind: PresentationKind,
    val id: String,
    val properties: Map<String, JsonElement> = emptyMap(),
) : Command {
    init { require(id.isNotBlank()) { "Presentation id must not be blank" } }
    override val kind: CommandKind = KIND
    companion object { val KIND = CommandKind("PRESENTATION") }
}

data class LocalStateCommand(val values: Map<String, JsonElement>) : Command {
    init { require(values.keys.all { it.isNotBlank() }) { "Local-state keys must not be blank" } }
    override val kind: CommandKind = KIND
    companion object { val KIND = CommandKind("LOCAL_STATE") }
}

enum class FormOperation { VALIDATE, RESET }

data class FormCommand(val operation: FormOperation) : Command {
    override val kind: CommandKind = KIND
    companion object { val KIND = CommandKind("FORM") }
}

enum class BackgroundOperation { SCHEDULE, CANCEL, START_CONTINUOUS, STOP_CONTINUOUS }
enum class BackgroundWorkKind { REFRESH, PROCESSING }
enum class BackgroundNetworkRequirement { NOT_REQUIRED, CONNECTED, UNMETERED }

data class BackgroundCommand(
    val operation: BackgroundOperation,
    val id: String,
    val workKind: BackgroundWorkKind = BackgroundWorkKind.PROCESSING,
    val earliestStartDelayMillis: Long = 0L,
    val networkRequirement: BackgroundNetworkRequirement = BackgroundNetworkRequirement.NOT_REQUIRED,
    val requiresCharging: Boolean = false,
    val title: String? = null,
    val description: String? = null,
    val input: Map<String, JsonElement> = emptyMap(),
) : Command {
    init {
        require(id.isNotBlank()) { "Background command id must not be blank" }
        require(earliestStartDelayMillis >= 0L) { "Background delay must be non-negative" }
        if (operation == BackgroundOperation.START_CONTINUOUS) {
            require(!title.isNullOrBlank() && !description.isNullOrBlank()) {
                "Continuous execution requires title and description"
            }
        }
    }
    override val kind: CommandKind = KIND
    companion object { val KIND = CommandKind("BACKGROUND") }
}

/** Executes child commands in order. Renderers remain unaware of composition semantics. */
data class SequenceCommand(val commands: List<Command>) : Command {
    init { require(commands.isNotEmpty()) { "Sequence command must contain at least one command" } }
    override val kind: CommandKind = KIND
    companion object { val KIND = CommandKind("SEQUENCE") }
}

/** Resolves [condition] through the binding runtime and prepares one branch only. */
data class ConditionalCommand(
    val condition: JsonElement,
    val whenTrue: Command,
    val whenFalse: Command? = null,
) : Command {
    override val kind: CommandKind = KIND
    companion object { val KIND = CommandKind("CONDITIONAL") }
}
