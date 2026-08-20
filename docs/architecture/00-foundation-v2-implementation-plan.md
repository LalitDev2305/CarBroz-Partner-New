# CarBroz Foundation V2 — Frozen Implementation Plan

## Purpose

This branch implements the product-neutral architecture foundation used as the engineering baseline for the independent CarBroz Partner and CarBroz Customer applications. Product-specific business rules, workflows, and SDUI contracts must not be introduced into the foundation.

## Frozen implementation phases

1. Repository, Gradle & Build Engineering
2. Architecture Kernel & Dependency Governance
3. Application Runtime, Lifecycle & Bootstrap
4. Configuration, Time, Localization & Feature Control
5. Security, Authentication, Session & Privacy Foundation
6. Adaptive UI, Design System & Accessibility
7. Navigation 3 & Destination Runtime
8. SDUI Protocol & Rendering Runtime
9. Binding, Dynamic Forms & Action Runtime
10. Networking, Persistence & Data Infrastructure
11. Offline, Sync, Realtime & Resilience
12. Platform Capability Layer
13. Background & Foreground Execution
14. Observability, Analytics, Performance & Operational Quality
15. Static Splash + Neutral Reference Vertical Slice
16. Verification, Documentation, CI/Release & Final Architecture Freeze

## Non-negotiable architecture laws

- `commonMain` owns reusable product-neutral behavior.
- Android, iOS and Desktop hosts remain thin.
- Partner and Customer product business logic is not shared by default.
- MVI/UDF has one canonical state owner per feature/runtime scope.
- SDUI transport DTOs are never rendered directly: decode -> validate -> compatibility -> normalize -> runtime IR -> bind -> render.
- Renderers never call repositories, navigation libraries, or vendor SDKs directly.
- SDUI actions are validated and authorized before dispatch.
- UI adapts to available window/container space, posture, safe area and input mode; it does not branch on marketing device names.
- State is independent of layout so resize, rotation, folding and adaptive navigation cannot destroy state.
- Platform/vendor APIs are behind product-neutral ports.
- WorkManager is an Android adapter, not the common background-work architecture.
- Continuous user-visible work is distinct from reliable deferred background work.
- Sensitive data is redacted from logs, analytics, traces and generic SDUI bindings.
- Offline writes are explicitly opted in per operation; failed writes are never blindly queued.
- Retry behavior respects idempotency and operation semantics.
- Public architectural APIs require meaningful KDoc describing responsibility, ownership, invariants, lifecycle/failure semantics and extension points.
- Relevant foundation production code targets >=90% meaningful line and branch coverage; critical pure logic targets effectively complete behavioral coverage.
- Positive, negative, boundary, cancellation, timeout, retry, concurrency, recovery and malformed-input tests are required where applicable.
- A bug fix adds a regression test whenever reproducible.
- CI must compile/test all feasible supported targets and enforce architecture/static-analysis/coverage gates.

## Implementation rule

Do not create placeholder abstractions merely to satisfy this document. A class is added when its responsibility is implemented or required by a compiling vertical slice. The responsibility and dependency boundary are frozen; incidental implementation details may evolve through ADRs.

## First vertical slice

The first executable slice is intentionally product-neutral:

`native host -> application bootstrap -> DI -> lifecycle -> static adaptive Splash -> Navigation 3 -> neutral SDUI fixture -> protocol validation/normalization -> renderer -> intent -> action dispatcher -> controlled result`

This slice is the acceptance proof that the foundation components compose correctly before Partner-specific development begins.
