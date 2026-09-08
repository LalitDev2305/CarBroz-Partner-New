package com.carbroz.foundation.observability

import java.awt.EventQueue

/** Desktop UI-thread heartbeat adapter for Compose Desktop's AWT event queue. */
class DesktopMainThreadDispatcher : MainThreadDispatcher {
    override fun dispatch(block: () -> Unit) {
        EventQueue.invokeLater(block)
    }
}

/** Lightweight Desktop process resource sampling backed by the JVM runtime. */
class DesktopResourceDiagnostics : ResourceDiagnostics {
    override fun sample(): ResourceDiagnosticResult = runCatching {
        val runtime = Runtime.getRuntime()
        ResourceDiagnosticResult.Available(
            ResourceSnapshot(
                heapUsedBytes = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L),
                heapLimitBytes = runtime.maxMemory().coerceAtLeast(0L),
                processorCount = runtime.availableProcessors().coerceAtLeast(1),
            ),
        )
    }.getOrElse {
        ResourceDiagnosticResult.Unavailable("desktop runtime resource sampling failed")
    }
}

/**
 * Curated Desktop diagnostic console for Development/Staging only.
 *
 * Operational metrics/traces/resources remain available through their neutral contracts but are intentionally
 * not printed here. The local console is reserved for readable startup flow and HTTP request/response blocks.
 */
class DesktopPlatformDiagnosticSink :
    LogSink,
    CrashSink,
    PerformanceSink,
    TraceSink,
    ResponsivenessSink,
    ResourceSink,
    DiagnosticBlockSink {

    override fun emit(event: LogEvent) {
        renderFlow(event)?.let(::println)
    }

    override fun emit(block: DiagnosticBlock) {
        val (color, label) = when (block.kind) {
            DiagnosticBlockKind.API_REQUEST -> CYAN to "🌐 API REQUEST"
            DiagnosticBlockKind.API_RESPONSE -> GREEN to "✅ API RESPONSE"
            DiagnosticBlockKind.API_ERROR -> RED to "❌ API ERROR"
        }
        val rendered = buildString {
            append(color)
            appendLine("╭─ $label  ${block.title}")
            block.content.lineSequence().forEach { line -> appendLine("│ $line") }
            append("╰────────────────────────────────────────────────────────")
            append(RESET)
        }
        if (block.kind == DiagnosticBlockKind.API_ERROR) System.err.println(rendered) else println(rendered)
    }

    override fun record(event: CrashEvent, throwable: Throwable?) {
        if (event.category != STARTUP_CATEGORY) return
        System.err.println(
            "$RED❌ FLOW ERROR  ${event.category}.${event.message}${correlationSuffix(event.correlationId)}$RESET",
        )
    }

    // Intentionally silent in the local console: the user-facing diagnostic stream contains FLOW + API only.
    override fun record(metric: PerformanceMetric) = Unit
    override fun record(span: TraceSpan) = Unit
    override fun record(incident: ResponsivenessIncident) = Unit
    override fun record(snapshot: ResourceSnapshot) = Unit

    private fun renderFlow(event: LogEvent): String? {
        if (event.category != STARTUP_CATEGORY) return null
        val taskId = event.attributes["task_id"]?.value
        val message = when (event.message) {
            "startup_started" ->
                "AppLifecycle.Foreground → SplashStore → ApplicationRuntime.start() → StartupCoordinator.run()"
            "startup_task_started" -> when (taskId) {
                "session.restore" -> "StartupCoordinator.run() → SessionRestoreStartupTask.execute()"
                "bootstrap" -> "StartupCoordinator.run() → BootstrapStartupTask.execute() → ResolveBootstrapUseCase.invoke() → RemoteBootstrapRepository.load()"
                else -> "StartupCoordinator.run() → StartupTask.execute(task=$taskId)"
            }
            "startup_ready" -> "Bootstrap resolved → ApplicationRuntime.Ready → SplashEffect.Navigate"
            "startup_blocked" -> "Bootstrap resolved → ApplicationRuntime.Blocked"
            "startup_task_failed" -> "Startup task failed(task=$taskId)"
            "startup_missing_resolution" -> "StartupCoordinator.run() → FAILED(startup_missing_resolution)"
            else -> return null
        }
        val color = when (event.level) {
            LogLevel.DEBUG -> DIM
            LogLevel.INFO -> MAGENTA
            LogLevel.WARN -> YELLOW
            LogLevel.ERROR -> RED
        }
        return "$color▶️ FLOW  $message${correlationSuffix(event.correlationId)}$RESET"
    }

    private fun correlationSuffix(correlationId: CorrelationId?): String =
        correlationId?.value?.takeIf(String::isNotBlank)?.let { "  [${it}]" }.orEmpty()

    private companion object {
        const val STARTUP_CATEGORY = "startup"
        const val RESET = "\u001B[0m"
        const val RED = "\u001B[31m"
        const val GREEN = "\u001B[32m"
        const val YELLOW = "\u001B[33m"
        const val MAGENTA = "\u001B[35m"
        const val CYAN = "\u001B[36m"
        const val DIM = "\u001B[2m"
    }
}
