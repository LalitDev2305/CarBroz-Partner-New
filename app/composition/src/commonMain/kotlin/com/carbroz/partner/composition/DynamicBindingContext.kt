package com.carbroz.partner.composition

import com.carbroz.foundation.configuration.ConfigurationProvider
import com.carbroz.foundation.session.SessionProvider
import com.carbroz.foundation.session.SessionState
import com.carbroz.runtime.binding.BindingContext
import com.carbroz.runtime.binding.BindingNamespace
import com.carbroz.runtime.binding.BindingValueSource
import com.carbroz.runtime.form.FormStore
import com.carbroz.runtime.form.asBindingValueSource
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.rendering.SduiRenderEvent
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class DynamicBindingSnapshot(
    val screen: Screen?,
    val form: FormStore?,
    val event: SduiRenderEvent?,
    val externalEvent: JsonElement?,
    val result: JsonElement?,
    val runtimeValues: Map<String, JsonElement>,
    val navigationId: String?,
)

fun interface DynamicBindingContextFactory {
    suspend fun create(snapshot: DynamicBindingSnapshot): BindingContext
}

class DefaultDynamicBindingContextFactory(
    private val sessionProvider: SessionProvider,
    private val configurationProvider: ConfigurationProvider,
) : DynamicBindingContextFactory {
    override suspend fun create(snapshot: DynamicBindingSnapshot): BindingContext {
        val sources = mutableListOf<Pair<BindingNamespace, BindingValueSource>>()
        snapshot.screen?.let { sources += BindingNamespace.SCREEN to jsonSource(it.toBindingJson()) }
        snapshot.form?.let { sources += BindingNamespace.FORM to it.asBindingValueSource() }
        sources += BindingNamespace.SESSION to jsonSource(sessionProvider.current().toBindingJson())
        sources += BindingNamespace.CONFIG to jsonSource(configurationProvider.get().let { configuration ->
            JsonObject(
                mapOf(
                    "environment" to JsonPrimitive(configuration.environment.name),
                    "versionName" to JsonPrimitive(configuration.buildInformation.versionName),
                    "versionCode" to JsonPrimitive(configuration.buildInformation.versionCode),
                    "applicationId" to JsonPrimitive(configuration.buildInformation.applicationId),
                ),
            )
        })
        val event = snapshot.event?.toBindingJson() ?: snapshot.externalEvent
        event?.let { sources += BindingNamespace.EVENT to jsonSource(it) }
        snapshot.result?.let { sources += BindingNamespace.RESULT to jsonSource(it) }
        sources += BindingNamespace.RUNTIME to jsonSource(
            JsonObject(
                buildMap {
                    putAll(snapshot.runtimeValues)
                    snapshot.navigationId?.let { put("navigationId", JsonPrimitive(it)) }
                },
            ),
        )
        return BindingContext.of(*sources.toTypedArray())
    }

    private fun Screen.toBindingJson(): JsonObject = JsonObject(
        mapOf(
            "id" to JsonPrimitive(id.value),
            "version" to JsonPrimitive(version),
            "template" to JsonObject(
                mapOf(
                    "id" to JsonPrimitive(template.id.value),
                    "type" to JsonPrimitive(template.type.value),
                ),
            ),
        ),
    )

    private fun SessionState.toBindingJson(): JsonObject = when (this) {
        SessionState.SignedOut -> JsonObject(mapOf("authenticated" to JsonPrimitive(false)))
        is SessionState.Authenticated -> JsonObject(
            mapOf(
                "authenticated" to JsonPrimitive(true),
                "subject" to JsonPrimitive(subject.value),
            ),
        )
    }

    private fun SduiRenderEvent.toBindingJson(): JsonObject = when (this) {
        is SduiRenderEvent.Activated -> JsonObject(
            mapOf("type" to JsonPrimitive("ACTIVATED"), "path" to JsonPrimitive(path.toString())),
        )
        is SduiRenderEvent.ValueChanged -> JsonObject(
            buildMap {
                put("type", JsonPrimitive("VALUE_CHANGED"))
                put("path", JsonPrimitive(path.toString()))
                put("value", JsonPrimitive(value))
                fieldId?.let { put("fieldId", JsonPrimitive(it)) }
            },
        )
    }

    private fun jsonSource(root: JsonElement): BindingValueSource = BindingValueSource { path ->
        path.fold(root as JsonElement?) { current, segment -> (current as? JsonObject)?.get(segment) }
    }
}
