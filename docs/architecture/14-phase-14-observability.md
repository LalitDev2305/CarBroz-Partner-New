# Phase 14 — Observability, Analytics, Performance & Operational Quality

Status: **IN PROGRESS — FREEZE AUDIT**

The frozen architecture keeps operational diagnostics and product analytics as separate responsibilities.

## Canonical ownership

- `:foundation:observability` owns structured operational logs, crash abstraction, typed correlation IDs, traces, performance metrics, responsiveness incidents, resource diagnostics, privacy policy, and vendor-neutral sink contracts.
- `:foundation:analytics` owns allow-listed product analytics events and analytics-specific sanitization. It does not receive operational logs or crashes.
- Platform/application composition supplies native diagnostic sinks, resource samplers, and a main-thread dispatcher. Runtime/data/background modules depend only on `:foundation:observability` contracts.

## Privacy and production policy

Sensitive diagnostic attributes are redacted before any sink receives them. Public diagnostic and analytics attributes are length-bounded at their sink boundary. Raw request bodies, headers, credentials, URLs containing dynamic data, background task input, and business payloads are not emitted by the Phase 14 instrumentation. Raw Throwable details are disabled by the default application policy; platform sinks intentionally do not render Throwable messages.

Operational and analytics sink failures are fail-isolated: diagnostics can never change application control flow. Coroutine cancellation remains application control flow and is propagated by startup, network, database-health, and background execution boundaries rather than being normalized as a failure.

Environment behavior is explicit. Development uses `DEBUG` local platform diagnostics and Staging uses `INFO`. Production uses a `WARN` policy but defaults to neutral/no-op local sinks and does not start the local heartbeat watchdog or local resource sampler. A future production crash/performance provider plugs into the same vendor-neutral sink contracts at composition without changing runtime/data modules. Analytics remains disabled until an explicit product event allow-list is introduced.

## Correlation and tracing

`CorrelationId` is the single typed correlation contract for logs, crash events, performance metrics, and traces. IDs are opaque random identifiers containing no user/session/device/business identity. Startup, network executions, and background task executions use one correlation ID per logical operation. Retries and authentication recovery remain part of the same network correlation. Background retry is modeled separately from terminal trace failure.

Startup observability follows the frozen focused-use-case architecture: one logical startup operation is traced end-to-end. Named sub-step spans such as session restoration and bootstrap acquisition may be emitted when diagnostically useful, but observability must not require or recreate a generic `StartupTask` framework.

## Operational quality

A common main-thread responsiveness watchdog posts platform heartbeats and records bounded incidents when the main/UI thread exceeds the configured threshold. Android uses the main `Looper`, Desktop uses the AWT event queue used by Compose Desktop, and iOS uses the Darwin main dispatch queue. One continuous stall emits one incident; the monitor waits for heartbeat recovery before starting another interval. A caller-supplied coroutine scope remains caller-owned and is never cancelled when the monitor closes.

Resource sampling is explicit per platform. Android/Desktop report JVM heap usage/limit and processor count. iOS reports physical memory and processor count through public Foundation APIs and leaves unavailable process-heap measurements unknown rather than guessing.

## Instrumented boundaries

- application startup: total duration, outcome, crash boundary and trace; optional named session-restore/bootstrap spans only where they add diagnostic value
- network: logical request duration/outcome/correlation across retries and auth recovery
- Room database health: health-check duration/outcome with cancellation propagation
- background handler execution: duration/outcome/crash boundary/trace
- application composition: first rendered frame metric only (not every recomposition)
- process operational quality: non-production startup resource sample and main-thread responsiveness watchdog

The final Phase 14 freeze still requires post-audit impact validation and one final full repository gate.
