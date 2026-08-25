# Phase 14 — Observability, Analytics, Performance & Operational Quality

Status: **IN PROGRESS**

The frozen architecture keeps operational diagnostics and product analytics as separate responsibilities.

## Canonical ownership

- `:foundation:observability` owns structured operational logs, crash abstraction, correlation IDs, traces, performance metrics, responsiveness incidents, resource diagnostics, privacy policy, and vendor-neutral sink contracts.
- `:foundation:analytics` owns allow-listed product analytics events and analytics-specific sanitization. It does not receive operational logs or crashes.
- Platform/application composition supplies native diagnostic sinks, resource samplers, and a main-thread dispatcher. Runtime/data/background modules depend only on `:foundation:observability` contracts.

## Privacy and production policy

Sensitive diagnostic attributes are redacted before any sink receives them. Raw request bodies, headers, credentials, URLs containing dynamic data, background task input, and business payloads are not emitted by the Phase 14 instrumentation. Raw Throwable details are disabled by the default application policy; platform sinks intentionally do not render Throwable messages.

Environment log thresholds are explicit: Development=`DEBUG`, Staging=`INFO`, Production=`WARN`. Crash, performance, trace, responsiveness, and resource diagnostics remain enabled through the neutral sink boundary. Analytics remains disabled until an explicit product event allow-list is introduced.

## Correlation and tracing

Correlation IDs are opaque random identifiers containing no user/session/device/business identity. Startup, network executions, and background task executions use one correlation ID per logical operation. Retries and authentication recovery remain part of the same network correlation.

## Operational quality

A common main-thread responsiveness watchdog posts platform heartbeats and records bounded incidents when the main/UI thread exceeds the configured threshold. Android uses the main `Looper`, Desktop uses the AWT event queue used by Compose Desktop, and iOS uses the Darwin main dispatch queue.

Resource sampling is explicit per platform. Android/Desktop report JVM heap usage/limit and processor count. iOS reports portable process information available through public Foundation APIs and leaves unavailable process-heap measurements unknown rather than guessing.

## Instrumented boundaries

- application startup: task and total duration, outcome, crash boundary, trace
- network: logical request duration/outcome/correlation across retries and auth recovery
- Room database health: health-check duration/outcome
- background handler execution: duration/outcome/crash boundary/trace
- application composition: first-render metric only (not every recomposition)
- process operational quality: startup resource sample and main-thread responsiveness watchdog

The final Phase 14 freeze still requires impact validation, repository hygiene/privacy/dependency audit, and one final full repository gate.
