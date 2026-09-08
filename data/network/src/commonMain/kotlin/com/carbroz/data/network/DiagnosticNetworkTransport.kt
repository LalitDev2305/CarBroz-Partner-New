package com.carbroz.data.network

import com.carbroz.foundation.observability.DiagnosticBlock
import com.carbroz.foundation.observability.DiagnosticBlockKind
import com.carbroz.foundation.observability.DiagnosticBlockSink
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Development/staging diagnostic decorator for the canonical REST transport.
 *
 * It does not own an HttpClient, retry, auth, headers or request policy. It observes the already-built
 * [TransportRequest], delegates exactly once, and emits privacy-safe request/response blocks. Production
 * composition must use the underlying transport directly.
 */
class DiagnosticNetworkTransport(
    private val delegate: NetworkTransport,
    private val sink: DiagnosticBlockSink,
    private val json: Json = Json,
) : NetworkTransport {
    private val prettyJson = Json { prettyPrint = true }

    override suspend fun execute(request: TransportRequest): NetworkResult {
        emitRequest(request)
        val result = try {
            delegate.execute(request)
        } catch (cancellation: CancellationException) {
            throw cancellation
        }
        emitResult(request, result)
        return result
    }

    private fun emitRequest(request: TransportRequest) {
        emit(
            DiagnosticBlock(
                kind = DiagnosticBlockKind.API_REQUEST,
                title = "${request.method} ${sanitizeUrl(request.url)}",
                content = buildString {
                    appendLine("METHOD   ${request.method}")
                    appendLine("URL      ${sanitizeUrl(request.url)}")
                    appendLine("HEADERS")
                    appendLine(prettyHeaders(request.headers).prependIndent("  "))
                    append("BODY\n")
                    append(prettyBody(request.body).prependIndent("  "))
                },
            ),
        )
    }

    private fun emitResult(request: TransportRequest, result: NetworkResult) {
        when (result) {
            is NetworkResult.Success -> emit(
                DiagnosticBlock(
                    kind = DiagnosticBlockKind.API_RESPONSE,
                    title = "${result.response.statusCode} ${request.method} ${sanitizeUrl(request.url)}",
                    content = buildString {
                        appendLine("STATUS   ${result.response.statusCode}")
                        appendLine("URL      ${sanitizeUrl(request.url)}")
                        append("JSON\n")
                        append(prettyElement(result.response.body).prependIndent("  "))
                    },
                ),
            )

            is NetworkResult.Failure -> {
                val failure = result.error
                val status = (failure as? NetworkFailure.Http)?.statusCode
                val responseBody = (failure as? NetworkFailure.Http)?.body
                emit(
                    DiagnosticBlock(
                        kind = DiagnosticBlockKind.API_ERROR,
                        title = buildString {
                            status?.let { append("$it ") }
                            append("${request.method} ${sanitizeUrl(request.url)}")
                        },
                        content = buildString {
                            status?.let { appendLine("STATUS   $it") }
                            appendLine("URL      ${sanitizeUrl(request.url)}")
                            appendLine("ERROR    ${failureName(failure)}")
                            if (responseBody != null) {
                                append("JSON\n")
                                append(prettyElement(responseBody).prependIndent("  "))
                            }
                        }.trimEnd(),
                    ),
                )
            }
        }
    }

    private fun prettyHeaders(headers: Map<String, String>): String {
        if (headers.isEmpty()) return "{}"
        val sanitized = JsonObject(
            headers.toSortedMap(String.CASE_INSENSITIVE_ORDER).mapValues { (name, value) ->
                JsonPrimitive(if (isSensitiveKey(name)) REDACTED else value)
            },
        )
        return prettyJson.encodeToString(JsonElement.serializer(), sanitized)
    }

    private fun prettyBody(body: String?): String {
        if (body.isNullOrBlank()) return "<none>"
        val element = runCatching { json.parseToJsonElement(body) }.getOrNull()
            ?: return "<non-json body omitted>"
        return prettyElement(element)
    }

    private fun prettyElement(element: JsonElement?): String {
        if (element == null || element is JsonNull) return "<empty>"
        return prettyJson.encodeToString(JsonElement.serializer(), redact(element))
    }

    private fun redact(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.mapValues { (key, value) ->
                if (isSensitiveKey(key)) JsonPrimitive(REDACTED) else redact(value)
            },
        )
        is JsonArray -> JsonArray(element.map(::redact))
        else -> element
    }

    private fun sanitizeUrl(url: String): String {
        val queryIndex = url.indexOf('?')
        if (queryIndex < 0) return url
        val base = url.substring(0, queryIndex)
        val query = url.substring(queryIndex + 1)
        val sanitized = query.split('&').joinToString("&") { entry ->
            val delimiter = entry.indexOf('=')
            if (delimiter < 0) return@joinToString entry
            val key = entry.substring(0, delimiter)
            val value = entry.substring(delimiter + 1)
            "$key=${if (isSensitiveKey(key)) REDACTED else value}"
        }
        return "$base?$sanitized"
    }

    private fun isSensitiveKey(key: String): Boolean =
        key.lowercase().replace("-", "").replace("_", "") in SENSITIVE_KEYS

    private fun failureName(failure: NetworkFailure): String = when (failure) {
        NetworkFailure.Offline -> "OFFLINE"
        NetworkFailure.Timeout -> "TIMEOUT"
        NetworkFailure.Transport -> "TRANSPORT_FAILURE"
        is NetworkFailure.Http -> "HTTP_${failure.statusCode}"
        is NetworkFailure.InvalidRequest -> "INVALID_REQUEST"
    }

    private fun emit(block: DiagnosticBlock) {
        runCatching { sink.emit(block) }
    }

    private companion object {
        const val REDACTED = "[REDACTED]"
        val SENSITIVE_KEYS = setOf(
            "authorization", "cookie", "setcookie", "xapikey", "apikey",
            "password", "passcode", "otp", "mockotp", "token", "accesstoken", "refreshtoken", "idtoken",
            "secret", "clientsecret", "credential", "credentials",
            "phonenumber", "phone", "mobile", "email",
            "address", "formattedaddress", "latitude", "longitude", "coordinates", "location",
            "cardnumber", "cvv", "cvc", "upiid", "bankaccount", "ifsc", "documentnumber", "kyc",
        )
    }
}
