# CarBroz Compose Multiplatform — Master Architecture Constitution

Status: **FROZEN SOURCE OF TRUTH**

This document consolidates the architecture finalized before Phase 1, including the later redesign that superseded older Voyager-era decisions. It is mandatory reading before starting or changing any implementation phase.

## Governance rule

Before every phase, PR, module, package, class, interface, provider, registry, factory, repository, use case, adapter or platform implementation:

1. Read this document.
2. Identify the exact owning responsibility.
3. Verify the dependency direction.
4. Verify whether the responsibility is shared, product-specific, technical, business, SDUI, runtime, data or platform.
5. Compare the current repository against this constitution.
6. Classify existing code as KEEP / MOVE / RENAME / MERGE / DELETE / REPLACE / CREATE.
7. Do not create a second source of truth.
8. Before creating anything, search the complete repository for an existing equivalent or overlapping module, package, type, contract, implementation, test fixture, resource, dependency or configuration.
9. After every implementation/migration, delete superseded code, duplicate implementations, obsolete packages/modules, stale tests/resources/configuration and unused dependencies. Temporary compatibility copies are allowed only during an actively verified migration and MUST be removed before that slice is frozen.
10. Run a post-implementation simplification/optimization review: verify the solution is the smallest clear production-safe design, remove unnecessary abstraction/indirection/allocation/dependency, prefer common KMP code where valid, and preserve correctness, readability, testability and architectural ownership over clever micro-optimization.
11. Verify there is one canonical owner/source of truth for every responsibility and no old/new implementation remains in parallel unintentionally.
12. Build and test Android, iOS Kotlin targets and Desktop where applicable before freezing the slice.
13. Re-run repository-wide duplicate/stale-code and dependency checks after the build passes; a green build alone is not a freeze gate.
14. Implement each infrastructure phase to production-complete readiness, not placeholder readiness. If a technology or subsystem is part of the frozen architecture, its reusable contracts, production implementation, environment wiring, failure handling, tests and integration path must be completed in its owning phase even if the first visible screen does not consume every capability yet.

Older architecture notes remain historical context only. When they conflict with this constitution, **this constitution wins**.

---

# Part A — 77 Frozen Architecture Decisions

## 1. Product-neutral foundation
CarBroz Foundation is reusable architecture, not Partner business code. Partner is implemented first; Customer later consumes the same neutral foundation where semantically correct. Product business logic is not shared merely for reuse.

## 2. Architectural style
The system combines Clean Architecture, MVI, UDF, SDUI, modular architecture and dependency inversion. Each concern has one canonical owner.

## 3. Server-driven product model
After minimal static startup UI, product screens are predominantly SDUI. Do not create Login/Home/OTP/Booking/Profile feature modules merely because those screens exist on the backend.

## 4. Static feature policy
Splash is the intentional primary static feature. Other static features require a proven native/static reason.

## 5. Kotlin Multiplatform first
Maximum reusable `commonMain`; minimum platform-specific code. Android, iOS and Desktop should share architecture and Compose UI whenever technically valid.

## 6. Thin platform hosts
`androidApp`, `iosApp` and `desktopApp` only bootstrap the platform, mount Compose and host unavoidable platform SDK initialization. They contain no business logic, SDUI engine, repositories or generic MVI runtime.

## 7. Final technology baseline
Kotlin, Kotlin Multiplatform, Compose Multiplatform, Material 3 where appropriate, Navigation 3, AndroidX/Compose Multiplatform lifecycle, Koin, Ktor Client, kotlinx.serialization, Coroutines/Flow, Coil 3, Compose Resources, Room 3 KMP, DataStore Preferences KMP, secure platform storage, Ktor WebSockets, WorkManager/Foreground Service on Android where semantically correct, Apple BackgroundTasks/native modes on iOS, explicit Desktop policies, Detekt, formatting enforcement, Kover and Gradle convention plugins.

## 8. Navigation 3 supersedes Voyager
Voyager and Voyager ScreenModel are superseded. Navigation 3 is the navigation implementation. SDUI and domain code never depend directly on Navigation 3 framework types.

## 9. Application-owned navigation state
The application owns destination and back-stack semantics. There must not be a second CarBroz stack beside Navigation 3.

## 10. Canonical MVI/UDF state owner
One state owner per responsibility. Do not duplicate the same screen/runtime state in Store, ViewModel and renderer state.

## 11. MVI kernel remains small
`foundation:architecture` owns only reusable Store/Reducer/Effect/state-machine primitives. No BaseViewModel/BaseUseCase/BaseRepository hierarchy unless repository evidence proves a need.

## 12. Lifecycle semantics are shared
Platform lifecycle is translated into common semantic lifecycle state. Product/runtime code observes the common lifecycle contract rather than native APIs.

## 13. Application runtime owns startup state
`runtime:application` owns bootstrap state and ordered startup tasks. Startup is serialized, cancellation-safe, retryable where appropriate and observable.

## 14. Startup is ordered and lazy
Initialize only systems required for the current startup path, but distinguish lazy initialization from incomplete implementation. Expensive capabilities such as maps, camera or payment are not initialized eagerly; their production-ready contracts/adapters/wiring are still completed in their owning phases so later activation does not require architectural implementation work.

## 15. Configuration is centralized
Development/Staging/Production environment data, build information, network configuration and feature flags are centralized. No scattered base URLs or environment switches.

## 16. Time is injectable
Current time/time-zone behavior is accessed through deterministic abstractions. Direct system-time calls must not spread through business/runtime logic.

## 17. Localization is architectural
Locale state, localized text, number/currency/date formatting, long text and RTL readiness are foundation responsibilities.

## 18. Security is cross-cutting but isolated
Sensitive values, redaction, trusted host/URI rules, secure randomness and security policies belong in the security foundation. Vendor/security implementation details do not leak inward.

## 19. Session/authentication is canonical
Secure credential persistence, restoration, expiry, single-flight refresh, logout and invalidation have one session ownership model. Cancellation propagates correctly.

## 20. Privacy-safe diagnostics
Tokens, credentials, payment secrets and PII must never be exposed by logging, analytics or crash reporting.

## 21. Adaptive UI is first-class
Adaptive behavior is based on actual available width/height, posture, safe area, container size and input mode — not marketing device categories.

## 22. Never branch on device name
No generic `if tablet`, `if iPad`, `if foldable`, `if desktop` layout architecture. Layout responds to current space and environment.

## 23. Adaptive state must survive resize
Continuous Desktop resize, split-screen, orientation changes and fold/unfold transitions must not destroy canonical application/screen state.

## 24. Design system and adaptive policy are not SDUI
The design system owns semantic visual tokens, styling, accessibility and true visual primitives. `foundation:adaptive` owns responsive environment/classification and reusable layout policy. SDUI describes semantic UI intent and consumes these foundations during rendering; it does not duplicate them or hardcode device categories. Backend semantic tokens such as typography/spacing/size variants resolve through design-system/adaptive policy. Raw numeric values, when protocol-supported, remain logical values and are not blindly multiplied by screen size.

## 25. Accessibility is mandatory
Large fonts, semantics, keyboard focus, pointer input, touch targets, IME, safe areas and platform accessibility behavior are architecture concerns, not later polish.

## 26. SDUI is a protocol pipeline
Backend payload -> transport decode -> schema validation -> compatibility -> normalization -> runtime IR -> binding -> rendering. Transport DTOs are not the rendering model.

## 27. SDUI hierarchy is preserved with semantic names
Logical hierarchy: `Screen -> Template -> Component -> Section -> Group -> Element`. These are protocol/runtime structural concepts, not feature modules. `Section` supersedes the earlier `SubComponent` terminology, `Group` supersedes `Child`, and terminal `Element` supersedes `ChildData`/`ChildrenData`. New code must use the semantic names; old names are migration-only and must be deleted after the Phase 8 migration passes.

## 28. SDUI depth is variable but structurally constrained
`Template` owns Components. A Component may terminate in Elements or continue through Sections. A Section may terminate in Elements or continue through Groups. A Group terminates in one or more Elements. Do not manufacture empty intermediate objects. Invalid combinations are rejected before trusted runtime IR is created. Slots such as leading/content/trailing are constrained presentation slots, not another recursive SDUI hierarchy.

## 29. Element is terminal
`Element` is the only terminal dynamic UI node family for text/image/icon/button/input/etc. No second public hierarchy may independently model these dynamic primitives. Renderers translate normalized Elements into Compose/design-system primitives and emit interaction; terminal Elements do not recursively own arbitrary SDUI nodes.

## 30. Server screen is immutable
Backend canonical screen data is immutable. Client interaction state lives in a separate runtime overlay.

## 31. Runtime IR is canonical
Rendering and action execution consume safe normalized/runtime models, not arbitrary raw JSON DTOs. Raw `JsonObject`/`JsonElement` property payloads may exist only at the untrusted transport/definition-decoding boundary; Compose renderers receive typed normalized properties.

## 32. SDUI is untrusted instruction data
Only allow-listed template/component/section/group/element/action/capability/business-operation identifiers may execute. No reflection-based arbitrary execution, dynamic Kotlin evaluation, classpath scanning for executable behavior or remote executable code.

## 33. Protocol limits are mandatory
Validate nesting depth, collection size, sibling/path identity, mandatory fields, URLs, bindings, capabilities and version compatibility to prevent malformed/pathological payload crashes. `NodeId` is local semantic identity; normalized `NodePath` is canonical screen-tree identity. Sibling IDs must be unique, while reusable IDs in different branches are permitted where their canonical paths differ.

## 34. Version compatibility is explicit
Support protocol/schema/screen versions, minimum client compatibility and required definition/capability metadata. Required unsupported definitions fail before normal rendering; optional omission/fallback is allowed only where the protocol explicitly declares it safe.

## 35. Renderer responsibility is pure UI
Renderer receives normalized runtime data/context, renders Compose and emits interaction. It does not call Ktor, repositories, navigation framework APIs, payment SDKs or platform APIs directly. `RenderContext` remains intentionally small and must not become a service locator.

## 36. Registry/Strategy extension model
New Template/Component/Section/Group/Element/action/validation/capability/business operation follows `CREATE -> REGISTER -> TEST -> USE`. A normal SDUI UI type owns typed properties plus one definition; a separate renderer file is created only when rendering complexity justifies it. Adding an extension must not require modifying decoder core, hierarchy models, normalizer engine, dispatcher, existing renderers or unrelated engines.

## 37. One immutable generic SDUI definition registry
Do not create five independent registry engines or parallel property/mapper/renderer registries. SDUI uses one generic immutable registry keyed by `(NodeKind, NodeType)`. `NodeKind` is the closed structural set `TEMPLATE / COMPONENT / SECTION / GROUP / ELEMENT`; `NodeType` is an open server-defined string value. A definition atomically owns its type metadata, typed-property decoding/normalization contract and renderer strategy. Duplicate keys fail during registry construction. Application composition explicitly registers supported definitions; no reflection/classpath auto-discovery.

## 38. Dynamic screen state uses MVI
Renderer interaction -> DynamicScreenIntent -> Store -> Effect/Action Runtime -> Result -> Reducer -> DynamicScreenState.

## 39. Typed binding engine
Bindings are typed, validated and redaction-aware across runtime/session/screen/form/navigation/action-result/environment scopes. Static values normalize eagerly; dynamic values remain typed literal/binding expressions and resolve against current runtime state rather than being frozen during normalization.

## 40. Form engine is generic
Dynamic forms own field state, validation, async validation, cross-field rules, visibility and submission state without feature-specific rewrites.

## 41. Action runtime is generic
Action dispatcher/registry owns API/navigation/dialog/sheet/external-URI/capability execution contracts. Product-specific business handlers are added later.

## 42. Dynamic API actions are constrained
Server actions cannot call arbitrary URLs. Trusted services, endpoint policy, request binding, authorization, timeout, retry and idempotency rules validate execution.

## 43. Navigation actions are isolated
SDUI emits semantic navigation intent/commands. Navigation 3 framework classes remain inside navigation adapter/presentation boundaries.

## 44. Overlay actions are not automatically navigation
Dialog, bottom sheet and snackbar behavior is MVI/runtime presentation behavior unless explicitly modeled as navigation.

## 45. Business modules exist for business invariants
Create Booking/Payment/etc. domains because behavior, rules and invariants exist — not because a screen has that name.

## 46. Technical capabilities are not business domains
Camera/location/map/media/file/permissions/sharing/connectivity are technical capabilities. Booking/payment/earnings/payout/etc. are business domains.

## 47. Vendor isolation is mandatory
Payment != Razorpay, map != Google Maps, push != Firebase. Neutral contracts sit above vendor adapters.

## 48. Koin is the single DI mechanism
Modules define dependencies close to ownership; application composition aggregates them. Do not maintain a giant manual AppGraph/ServiceLocator in parallel.

## 49. Networking foundation is canonical
Ktor Client is hidden behind typed network contracts, request context, serialization, error mapping, timeout/retry/idempotency/security and observability policies.

## 50. Single-flight token refresh
Concurrent authentication failures coordinate through one refresh operation rather than launching duplicate refresh requests.

## 51. Technical errors are normalized
External exceptions such as Ktor/SQLite/vendor exceptions do not cross architecture boundaries directly. Translate into canonical failures/errors.

## 52. Room 3 KMP structured persistence is production infrastructure
Room 3 KMP is the chosen relational/structured persistence technology. Its database factory, platform drivers, schema/versioning, migrations, transactions, health/error mapping, test strategy and DI integration are implemented as production-ready infrastructure in the data phase rather than postponed until a particular screen happens to need a table. Do not create meaningless product tables merely for architecture completeness; create the complete reusable persistence mechanism and add real entities when their data contracts exist.

## 53. DataStore for non-sensitive preferences
Theme, locale and non-sensitive settings use DataStore Preferences KMP. Credentials never use preferences.

## 54. Secure storage is platform-backed
Android secure storage uses platform-backed protection, iOS uses Keychain, Desktop has an explicit declared security policy/provider.

## 55. Sync/outbox is operation-specific
Offline writes are not automatically queued. Sync/outbox/conflict resolution is explicit, idempotent and operation-specific.

## 56. Realtime transport is abstracted
Ktor WebSockets may be the initial transport, but consumers depend on `RealtimeTransport`, allowing WebSocket/SSE/polling evolution.

## 57. Background work is semantic
Common code expresses semantic task requirements. It never exposes WorkManager or BGTaskScheduler concepts.

## 58. Android work separation
Coroutines = in-process async work; WorkManager = reliable deferred/persistent work; Foreground Service = genuine continuous user-visible Android work.

## 59. iOS background semantics remain native
Use BackgroundTasks/BGTaskScheduler/native modes according to Apple constraints behind common semantic contracts.

## 60. Continuous execution is explicit
Tracking and other genuinely continuous operations use a `ContinuousExecutionController`-style semantic boundary rather than abusing generic background work.

## 61. Capability availability is modeled
Supported/Unsupported/Unavailable/Restricted/PermissionRequired states protect Desktop and device/platform differences.

## 62. Location, tracking and map are separate
Location obtains coordinates, tracking manages tracking lifecycle/remote updates, map presents spatial UI. They do not own one another.

## 63. Media/camera/notification/share/URI are isolated
Each technical capability owns neutral contracts with vendor/platform adapters beneath it.

## 64. Observability and analytics are separate
Structured operational logging/tracing/crash/performance diagnostics are not analytics. Analytics has its own sanitization and event policy.

## 65. Package/module ownership is explicit
No generic dumping grounds such as `utils`, `helpers`, `misc`, `managers` or broad `models`. A type belongs to the subsystem that owns its meaning.

## 66. `expect/actual` is sparing
Prefer injectable interfaces when DI/testing makes sense. Use `expect/actual` only for true compile-time platform differences.

## 67. Testing mirrors architecture
Pure logic, state machines, reducers, protocol validation, bindings, security, session, retry/cancellation/concurrency and platform adapters are independently testable.

## 68. Meaningful coverage target
Relevant foundation production code targets >=90% meaningful line and branch coverage where reliable tooling supports it. Critical pure logic targets near-complete behavioral coverage.

## 69. Failure and recovery tests are mandatory
Test valid, invalid, boundary, timeout, cancellation, retry, concurrency, race, idempotency, malformed protocol, unsupported capability and failure->recovery behavior where applicable.

## 70. Architecture tests enforce forbidden dependencies
Automate rules such as SDUI protocol -> no Compose, pure architecture/domain -> no Ktor/Room/Navigation 3, commonMain -> no accidental platform SDK, renderer -> no direct repository/network calls.

## 71. CI is a merge gate
Formatting, static analysis, architecture checks, tests, coverage, Android/iOS/Desktop builds and applicable protocol/adaptive/security suites must pass before a phase is frozen.

## 72. Documentation is architecture
Public architectural APIs require meaningful KDoc. Architecture docs, ADRs, diagrams, testing/security guidance and extension rules are maintained with implementation.

## 73. Foundation reuse is versioned long-term
Partner and Customer remain independent products but should consume versioned neutral foundation libraries rather than permanently copy/paste architecture source if avoidable.

## 74. Exact dependency law
Platform hosts -> application composition -> features/runtime -> contracts; data/adapters implement inward-owned contracts; external SDK/platform APIs remain outermost. Product domains follow Clean Architecture independently.

## 75. Extension experience is a quality metric
A new Template/Component/Section/Group/Element/action/validation/capability/business operation should normally require only its isolated implementation plus explicit registration and tests, not edits to unrelated central engines. SDUI concrete UI types are vertically sliced under `template/`, `component/`, `section/`, `group/` and `element/`; generic protocol/model/extension/registry/normalization/rendering infrastructure exists once.

## 76. Constitution-before-code rule
This file is the canonical architecture constitution. Before every new phase, implementation must first re-read this document, compare the current repository to the target ownership/dependency rules, document deviations, repair completed-phase drift when necessary, and only then start new code. Any architecture change requires updating this constitution first with the reason and migration impact. Every implementation or migration also requires a repository-wide hygiene and simplification pass: no superseded/duplicate code, module, resource, configuration or dependency may remain after the slice is frozen, and the resulting design must be reviewed for a simpler, clearer or more efficient implementation without weakening correctness, maintainability, testability or multiplatform ownership.

## 77. Production-complete infrastructure before product adoption
The foundation is built as a production-ready application platform, not as a sequence of placeholders that are finished only when a future screen asks for them. Development, Staging and Production configuration must be wired explicitly; Debug and release behavior, secrets boundaries, diagnostics policy and environment-specific endpoints must be supported without source edits. When an owning phase introduces networking, persistence, caching, authentication, SDUI, actions, realtime, background work or another frozen subsystem, complete the reusable production path in that phase: contracts, implementation, DI, configuration, error/failure handling, cancellation, retries/timeouts/idempotency where applicable, security/redaction, caching/persistence policy, tests and platform integration. The static Splash/bootstrap path must be able to call a configured bootstrap/config endpoint through the canonical network stack and use the response to determine the next semantic destination. After bootstrap, dynamic screens must use one generic SDUI screen-fetch/action pipeline: server-defined actions and bindings trigger the generic action runtime/network path, responses update runtime state/navigation according to protocol semantics, and adding a normal backend-driven screen or button/API action must not require screen-specific Kotlin networking code. Production endpoint values and backend contracts may change through configuration/protocol data; the client architecture must already be complete. "Implement when required" is acceptable only for genuinely product-specific entities, business invariants or vendor capabilities whose concrete contract does not yet exist—not for infrastructure already frozen in this constitution.

---

# Part B — Target Ownership / Repository Direction

These are architectural ownership boundaries. A boundary becomes a separate Gradle module when it materially improves dependency enforcement, platform isolation, testability, build ownership or replacement. Do not collapse independent responsibilities into catch-all modules; do not create microscopic modules without value.

```text
CarBroz Partner
├── androidApp/                 # thin Android host
├── iosApp/                     # Swift/Xcode host only
├── desktopApp/                 # thin Desktop host
├── app/
│   └── composition/            # Compose/application/Koin composition root
├── foundation/
│   ├── architecture/           # MVI/UDF kernel
│   ├── lifecycle/
│   ├── navigation/
│   ├── adaptive/
│   ├── design-system/
│   ├── configuration/
│   ├── localization/
│   ├── security/
│   ├── session/                # focused authentication/session ownership retained
│   ├── time/
│   ├── observability/          # introduced in its implementation phase
│   ├── analytics/              # introduced in its implementation phase
│   └── testing/                # reusable test infrastructure when justified
├── runtime/
│   ├── application/
│   ├── sdui/
│   ├── binding/
│   ├── form/
│   ├── action/
│   └── capability/
├── data/
│   ├── network/
│   ├── database/
│   ├── preferences/
│   ├── secure-storage/
│   ├── sync/
│   └── realtime/
├── capabilities/
│   ├── background/
│   ├── connectivity/
│   ├── permissions/
│   ├── location/
│   ├── tracking/
│   ├── map/
│   ├── camera/
│   ├── media/
│   ├── notification/
│   ├── sharing/
│   └── external-uri/
├── feature/
│   └── splash/
├── build-logic/
├── docs/
└── gradle/
```

Important: dynamic backend screens do **not** create Login/Home/Booking/Profile feature modules. Product business domains are introduced separately only when real business invariants exist.

---

# Part C — Canonical Runtime Mental Model

```text
Backend
  ↓
SDUI transport payload
  ↓
Decode
  ↓
Schema validation
  ↓
Compatibility
  ↓
Normalization
  ↓
Runtime IR / immutable canonical Screen
  ↓
Dynamic MVI Store + Runtime Overlay
  ├── Binding
  ├── Forms/Validation
  └── Actions
         ↓
      Action Runtime
      ├── API -> Network foundation -> Backend
      ├── Navigation -> semantic destination -> Navigation 3 adapter
      ├── Business -> product domain/use case
      └── Capability -> common capability -> platform adapter

Canonical Screen + Runtime Overlay
  ↓
SduiRegistry / NodeRendererDispatcher
  ↓
Template -> Component -> Section -> Group -> Element
  ↓
foundation:adaptive + foundation:design-system
  ↓
Compose Multiplatform
  ↓
Android / iOS / Desktop
```

### SDUI responsive rendering law

The backend sends semantic UI intent, not device-specific final pixels. Templates/Components/Sections/Groups/Elements may expose semantic layout, typography, spacing, size and style tokens. During rendering, `foundation:adaptive` supplies the current available-space environment and reusable responsive policy, while `foundation:design-system` resolves visual tokens/accessibility-aware primitives. Major layout adaptation belongs primarily to Template/Component policy; terminal Elements adapt through semantic design-system variants and accessibility. Do not scatter screen-width multipliers or `if tablet/desktop` branches through renderers. The same normalized screen must survive resize and recompose against the current adaptive environment without losing canonical MVI state.

### SDUI extension law

```text
CREATE isolated type package
  ├── <Type>Properties
  ├── <Type>Definition
  └── <Type>Renderer only when complexity justifies it
        ↓
REGISTER explicitly in application SDUI definition set
        ↓
TEST definition/property/renderer behavior and registry collision safety
        ↓
USE by backend NodeType
```

Adding a normal Template, Component, Section, Group or Element must not require editing the decoder core, hierarchy model, generic normalizer engine, dispatcher, existing definitions or action/network/navigation engines. The application registration set is the intentional controlled integration point.

### Production bootstrap and dynamic request path

```text
Static Splash
   ↓
ApplicationRuntime / StartupCoordinator
   ↓
Configured bootstrap/config request
   ↓
Canonical Network Foundation
(Ktor + auth + timeout/retry + typed errors + security + cache policy)
   ↓
Bootstrap/config response
   ↓
Semantic next destination
   ↓
Navigation 3 adapter
   ↓
Generic SDUI screen request
   ↓
Decode -> Validate -> Compatibility -> Normalize -> Runtime IR
   ↓
Dynamic MVI Store -> Renderer
   ↓
User interaction
   ↓
Binding -> Generic Action Runtime
   ├── API action -> canonical network pipeline
   ├── Navigation action -> semantic navigation
   ├── Overlay action -> runtime presentation
   ├── Capability action -> capability registry
   └── Business action -> registered business operation
   ↓
Result -> Reducer / binding result / semantic navigation / next SDUI request
```

Normal server-driven screens and actions must flow through this path without adding screen-specific API clients, repositories, ViewModels, navigation wiring or Kotlin button handlers.

---

# Part D — 17 Master Implementation Phases

The 77 decisions are architecture responsibilities. These 17 phases are the implementation sequence. Module count is independent from phase count.

## Phase 1 — Repository, Gradle & Build Engineering
KMP source sets and hosts; Gradle Kotlin DSL; version catalog; convention-plugin direction; dependency governance; Development/Staging/Production build/environment model; Debug/release behavior; secrets handling; static analysis/formatting; coverage; CI/reproducibility; warning cleanup; platform compile gates. Build/environment setup must be production-usable rather than a placeholder awaiting release work.

## Phase 2 — Architecture Kernel & Dependency Governance
Clean dependency boundaries; MVI/UDF Store/Reducer/Effect contracts; coroutine policies; architecture dependency laws/tests; product-neutral package policy; `expect/actual` policy; prevention of dumping-ground/base-class architecture.

## Phase 3 — Application Runtime, Lifecycle & Bootstrap
ApplicationRuntime; application scope; startup coordinator/tasks/registry; startup ordering; cancellation/retry/recovery; cold/warm launch semantics; lifecycle; process restoration strategy; platform bridges; Koin composition-root foundation. Bootstrap contracts must be capable of orchestrating the production config/bootstrap request once the network implementation is supplied in Phase 10.

## Phase 4 — Configuration, Time, Localization & Feature Control
Development/Staging/Production environment/build/network configuration; endpoint/service configuration without source edits; time/time-zone; localization/formatting/RTL; runtime feature flags and configuration contracts.

## Phase 5 — Security, Authentication, Session & Privacy Foundation
Secure storage boundary; credentials/tokens; restoration; expiry; single-flight refresh; logout/invalidation; trusted hosts/URIs; secure randomness; redaction/privacy; cancellation/concurrency/security tests.

## Phase 6 — Adaptive UI, Design System & Accessibility
Dedicated adaptive ownership; window/container classification; responsive layout policy; design tokens/theme/typography; safe interaction targets; accessibility semantics; large fonts/RTL/IME/pointer/keyboard readiness; Android/iOS/Desktop integration.

## Phase 7 — Navigation 3 & Destination Runtime
Semantic destinations and commands; application-owned back stack; Navigation 3 adapter; deep links; guards/prerequisite redirects; pending destinations; adaptive navigation; restoration; SDUI isolation from framework types.

## Phase 8 — SDUI Protocol & Rendering Runtime
Envelope/contracts; decoder; strict validation; protocol limits; compatibility; normalization; immutable trusted runtime IR; path-scoped node identity; variable-depth `Screen -> Template -> Component -> Section -> Group -> Element`; one immutable generic `(NodeKind, NodeType)` definition registry; typed property definitions; centralized renderer dispatch/traversal; adaptive/design-system rendering bridge; fallback/unsupported behavior; architecture tests proving protocol/normalization remain Compose-free and renderers remain side-effect-free. Concrete UI types are vertically sliced under `template/`, `component/`, `section/`, `group/`, `element/` and follow CREATE -> REGISTER -> TEST -> USE. The protocol/runtime must be production-complete for generic backend-driven screen delivery, not only a fixture decoder.

## Phase 9 — Binding, Dynamic Forms & Action Runtime
Typed/redaction-aware binding scopes; dynamic form state/validation; action dispatcher/registry; validation/security/authorization; API/navigation/dialog/sheet/URI/capability semantics; trusted endpoint/idempotency policy. API actions must be generic and data-driven so ordinary server-defined button/form actions execute without screen-specific Kotlin networking code.

## Phase 10 — Networking, Persistence, Caching & Data Infrastructure
Implement the complete production data platform: Ktor client/engines and lifecycle; serialization; typed request/response/error contracts; request context; Development/Staging/Production endpoint resolution; headers/authentication integration; timeout/retry/backoff/idempotency; connectivity-aware behavior where applicable; security/trusted-host/redaction hooks; HTTP/cache policy and reusable cache abstraction; Room 3 KMP database factory/platform drivers/schema/versioning/migrations/transactions/health/error mapping/testing; DataStore Preferences KMP; secure-storage data adapters; repository/data-source boundaries; Koin wiring and observability hooks. Provide the canonical bootstrap/config API execution path and the generic SDUI screen/action API execution path. After this phase, changing configured backend URLs/contracts should not require inventing networking architecture, database infrastructure or caching infrastructure later.

## Phase 11 — Offline, Sync, Realtime & Resilience
Connectivity; operation-specific offline policy; outbox; sync coordinator; conflicts; retries/backoff/dedup/idempotency; `RealtimeTransport`; Ktor WebSockets adapter; reconnect/order/dedup/recovery. Complete the reusable production mechanisms in this phase; only operation-specific queue/conflict rules wait for actual business semantics.

## Phase 12 — Platform Capability Layer
Capability registry/availability; permissions; location; tracking; maps; camera; media; notifications; sharing; external URI; provider isolation and explicit unsupported/restricted behavior across platforms. Frozen generic capability infrastructure is completed here even when a specific product flow has not activated every capability.

## Phase 13 — Background & Foreground Execution
Semantic common scheduler; Android WorkManager; Android foreground execution boundary; iOS BackgroundTasks/native modes; Desktop scheduler; constraints/cancellation/uniqueness/recovery and continuous-execution semantics.

## Phase 14 — Observability, Analytics, Performance & Operational Quality
Structured logging/correlation/tracing; crash abstraction; analytics separation/sanitization; performance instrumentation; startup/render/network/database/background metrics; freeze/ANR awareness; resource/memory diagnostics. Production and debug/staging diagnostics behavior must be explicitly configured and privacy-safe.

## Phase 15 — Static Splash + Neutral Reference Vertical Slice
Real static Splash using MVI/DI/lifecycle/adaptive UI/Navigation 3. Splash/bootstrap executes the configured bootstrap/config request through the canonical network stack, handles loading/failure/retry/cancellation/cache policy as defined, and resolves the next semantic destination from response/runtime state. Then a neutral SDUI fixture/integration path proves protocol->normalize->binding->render->generic action->network/navigation without Partner business logic or screen-specific API wiring.

## Phase 16 — Verification, Documentation, CI/Release & Architecture Freeze
Meaningful >=90% coverage where appropriate; positive/negative/boundary/concurrency/cancellation/protocol/migration/UI/adaptive/accessibility/security/performance suites; KDoc; ADRs; dependency diagrams; extension/testing guides; full Development/Staging/Production and applicable Debug/release verification; Android/iOS/Desktop build matrix; final dependency/security/performance audit. Verify that no frozen production subsystem remains a TODO, placeholder, fake implementation or "implement when needed" architecture gap.

## Phase 17 — Foundation Governance, Versioning & Product Adoption
Version the neutral foundation; establish compatibility/deprecation policy; publish/consume foundation internally for Partner and later Customer; prevent architecture drift; maintain constitution/ADRs; perform periodic dependency/technology/security upgrades without leaking product assumptions into foundation.

### Mandatory phase-entry gate
Before beginning **any** phase above:

```text
READ THIS FILE
   ↓
Audit current repository against the phase ownership
   ↓
Search repository for existing/overlapping implementation
   ↓
List deviations from completed phases
   ↓
Repair deviations first
   ↓
Define exact modules/dependencies for the phase
   ↓
Define production-complete acceptance criteria for every frozen subsystem owned by the phase
   ↓
Implement complete reusable production path (not placeholder/future TODO)
   ↓
Delete superseded/duplicate/stale code, modules, resources and dependencies
   ↓
Review for simpler/clearer/more efficient production-safe implementation
   ↓
Tests + Android/iOS/Desktop gates
   ↓
Repository-wide hygiene/dependency audit
   ↓
Architecture + production-readiness audit
   ↓
Freeze phase
```

A phase/slice is **not frozen merely because it compiles**. Freeze requires proof that the new implementation is the canonical owner, superseded code has been removed, dependencies are minimal, no unnecessary parallel abstraction remains, and every infrastructure responsibility assigned to that phase has a complete production-usable path. A TODO/placeholder or "we will implement it when a screen needs it" is not acceptable for frozen infrastructure.

---

# Part E — Reconciliation of Completed Phases 1–6

Current repository implementation has useful production work, but the following completed-phase structure must be reconciled before Phase 7 begins.

## Keep
- `foundation:architecture` — matches the small MVI/UDF kernel direction.
- `foundation:lifecycle` — correct independent lifecycle ownership.
- `runtime:application` — correct startup/runtime ownership.
- `foundation:configuration` — correct configuration owner.
- `foundation:time` — correct deterministic time owner.
- `foundation:localization` — correct localization owner.
- `foundation:security` — correct security owner.
- `foundation:session` — retained as a focused session/authentication responsibility because Phase 5 explicitly requires canonical session ownership and the implementation is already isolated/tested.
- `foundation:design-system` — keep styling/theme/accessibility responsibility, but remove adaptive ownership from it.

## Must change before Phase 7
1. **Create `foundation:adaptive` and move adaptive/window/content-layout policy code out of `foundation:design-system`.** The final architecture makes adaptive UI a first-class foundation boundary; design-system consumes it rather than owning it. Migration is complete only after old design-system adaptive code/tests are deleted and repository search confirms no stale package/import remains.
2. **Migrate `app:shared` toward `app:composition`.** The final architecture requires a composition root rather than a generic shared catch-all. Shared Compose root, Koin assembly and the Kotlin iOS `MainViewController` bridge belong here. Delete the superseded module/path/configuration after all consumers migrate; do not leave both module identities in parallel.
3. **Merge `foundation:feature-control` into `foundation:configuration` unless an independent dependency/build reason is proven.** The frozen final direction places FeatureFlag/FeatureFlagProvider under configuration ownership. Remove the old module and all stale dependency declarations after migration.
4. **Add Koin to the composition/bootstrap architecture.** Koin is the single DI owner and was part of the pre-Phase-1 frozen stack; completed startup/bootstrap work is not structurally complete until composition is Koin-based rather than relying on future/manual wiring. Remove any superseded manual graph/service-locator wiring rather than retaining two DI systems.
5. **Keep platform hosts thin while performing the above moves.** No reusable architecture should migrate into Android/iOS/Desktop hosts.
6. **Preserve all current tests and cross-platform compilation while restructuring.** Migration is not permission to rewrite working behavior. Once behavior is proven equivalent, delete the superseded implementation and migrate tests to the canonical owner.
7. **After all Phase 1–6 reconciliation work, run a full repository hygiene and optimization audit.** Search for duplicate classes/contracts/packages/modules, stale imports/resources/configuration, unused dependencies, obsolete compatibility code, unnecessary abstractions and avoidable platform-specific implementations. Resolve findings before Phase 7.
8. **Re-audit completed Phases 1–6 against Decision 77.** Anything already declared as frozen infrastructure must be production-complete for its current responsibility rather than a placeholder. Missing production configuration, DI integration, security/session behavior, environment wiring or platform implementation that belongs to Phases 1–6 must be corrected before Phase 7; responsibilities explicitly owned by later phases remain implemented in those later phases, but may not be deferred beyond their owning phase.

## Deferred intentionally to later phases
- Navigation 3 -> Phase 7.
- SDUI protocol/runtime -> Phase 8, where the generic production screen pipeline must be completed.
- Binding/forms/actions -> Phase 9, where generic data-driven API/action execution semantics must be completed.
- Ktor/Room/DataStore/caching data foundation -> Phase 10, where the complete reusable production infrastructure is implemented even if only bootstrap/reference flows initially consume it.
- Sync/realtime -> Phase 11, with generic production mechanisms completed there and only business-specific policies deferred.
- Capabilities -> Phase 12.
- Background execution -> Phase 13.
- Observability/analytics/performance -> Phase 14.
- Splash/reference SDUI slice -> Phase 15, which must prove the real bootstrap/config and generic dynamic request/action paths rather than mocks standing in for missing infrastructure.
- final CI/docs/freeze -> Phase 16.
- foundation versioning/product adoption -> Phase 17.

Phase 7 MUST NOT begin until the completed Phase 1–6 reconciliation items above are implemented, tested, optimized, cleaned of superseded/duplicate artifacts and audited.

---

# Part F — Frozen Phase 8 SDUI Implementation Rules

1. **Canonical terminology:** `Screen -> Template -> Component -> Section -> Group -> Element`. Earlier `SubComponent`, `Child`, `ChildData` and `ChildrenData` names are superseded and must not survive the migration as parallel public models.
2. **One Gradle module first:** keep `:runtime:sdui` as one module until measured dependency/build/test evidence justifies a split. Inside it, package boundaries separate protocol, model, extension, registry, normalization, rendering and concrete type implementations.
3. **Generic infrastructure once:** use one `NodeKind`, open `NodeType`, one `SduiDefinition` family, one immutable registry implementation and one centralized renderer dispatcher/traversal. Do not duplicate five mini-frameworks.
4. **Strong structural runtime IR:** do not replace the hierarchy with arbitrary recursive `Node(children: List<Node>)`. Kotlin runtime types must make invalid parent/child combinations unrepresentable after normalization.
5. **Vertical concrete type packages:** concrete implementations live under `template/<type>`, `component/<type>`, `section/<type>`, `group/<type>`, `element/<type>`. A simple type normally needs `<Type>Properties` + `<Type>Definition`; split `<Type>Renderer` only when complexity warrants it.
6. **Atomic registration:** a definition owns its type metadata, property decoding/normalization and renderer strategy. There are no synchronized mapper/property/renderer registries. Production registry is immutable after construction; duplicate `(NodeKind, NodeType)` registration fails.
7. **Explicit registration:** application composition registers definition sets. No reflection/classpath scanning. The normal future extension flow is CREATE -> REGISTER -> TEST -> USE.
8. **Typed properties:** transport may hold raw JSON properties, but normalization converts them to typed properties before Compose. Static semantic tokens normalize once; dynamic bindings remain typed expressions for `runtime:binding`.
9. **Responsive law:** server JSON describes semantic intent. `foundation:adaptive` resolves current available-space policy; `foundation:design-system` resolves visual/accessibility tokens. Do not blindly scale every numeric value or branch on device marketing category.
10. **Pure rendering:** definitions/renderers render UI and emit `RenderEvent`; API, repository, navigation, business and platform side effects execute outside rendering through MVI/action/capability architecture.
11. **Central traversal:** container definitions choose layout but delegate descendant rendering to the centralized dispatcher so lookup, keys, diagnostics, unsupported handling and tracing remain consistent.
12. **Identity:** local `NodeId` plus normalized `NodePath` provide stable Compose/runtime identity. Never use collection index as canonical identity. Duplicate sibling IDs are invalid; reusable IDs in different branches are allowed when paths differ.
13. **Compatibility before render:** unsupported required definitions/capabilities and malformed properties fail before normal Compose traversal. Optional fallback/omission requires explicit protocol semantics.
14. **Constrained slots:** leading/content/trailing slots may exist for terminal presentation composition but are not another recursive SDUI hierarchy.
15. **Adaptive state safety:** resize/orientation/posture/container changes re-resolve layout without replacing canonical screen/MVI state.
16. **Architecture tests:** protocol/model/normalization must remain Compose-free; renderers must remain free of Ktor/repository/Navigation 3/platform side effects; registry duplicate/unsupported behavior and hierarchy normalization are tested.
17. **Migration hygiene:** Phase 8 is not frozen while old SubComponent/Child/ChildData public models, duplicate registries, giant type `when` dispatchers, raw JSON render paths or superseded tests remain.
