# CARBROZ PARTNER — ENGINEERING & ARCHITECTURE CONSTITUTION

## 1. ARCHITECTURAL PRINCIPLES & BOUNDARIES
- **Core Multiplatform Stack**: Kotlin Multiplatform (KMP), Compose Multiplatform (CMP), JDK 21 LTS.
- **Architectural Paradigm**: Clean Architecture with MVI + Unidirectional Data Flow (UDF), immutable state, and `StateFlow`-based observation.
- **Dependency Direction & Ownership Rules**:
  - **High-Level Policy Integrity**: High-level policy and business rules MUST NEVER depend on low-level infrastructure, UI frameworks, or network/database drivers.
  - **Contract Ownership**: Contracts (interfaces) are owned by the domain/boundary that requires them, not by the implementing infrastructure.
  - **Implementation Inversion**: Implementations depend strictly on contracts (Dependency Inversion Principle).
  - **Platform Hosts**: Platform hosts (`androidApp`, `iosApp`, `desktopApp`) compose the application and provide platform contexts, but DO NOT own business or application state logic.
  - **Source vs Runtime Distinction**: Do not confuse control flow / runtime invocation (UI -> Store -> Network) with source-code dependency direction (UI -> Store Contract <- Store Impl -> Network Contract <- Ktor Impl).
  - **Zero Circular Dependencies**: Strict acyclic dependency graph across all source sets and Gradle modules.
- **Modularity Policy**: A Gradle module exists ONLY when a strict binary, compilation, or dependency boundary is required. Packages are preferred for internal logical isolation within a module.
- **Prohibition of Dumping Grounds**: Absolutely no generic dumping grounds (`Helper`, `Utils`, `Common`, `Misc`, `BaseManager`). Every class and function MUST have a single, explicitly named domain responsibility.

## 2. PRODUCT MODEL & EQUAL PLATFORM TARGETS
- **Independent Repositories**: CarBroz Partner is a standalone multiplatform project. (CarBroz Customer exists in a separate repository; Admin is a separate web/backend system).
- **First-Class Platform Parity**: Android, iOS, and Desktop (JVM) are equal first-class targets.
- **Zero Platform Branching in Common Code**: Common code MUST NEVER contain platform conditional branches (`if (android)`, `if (ios)`). Platform variations MUST be isolated behind multiplatform abstraction contracts and instantiated at dependency injection boundaries.

## 3. MVI + UNIDIRECTIONAL DATA FLOW (UDF) CONSTITUTION
- **State & Intent Flow**:
  - `UI -> Intent/Event -> Store -> State Transition -> Immutable State -> UI`
  - `Store -> One-Time Effect -> UI/Host/Navigation/Capability Handler`
- **Ownership & Lifecycle Rules**:
  - Exactly one State Owner per context exposing an immutable `StateFlow<State>`.
  - No dual ViewModel + Store or Presenter + Store duplication.
  - Store manages state transitions natively. Reducers exist only when complex state transition logic justifies separate, pure function testing.
  - Common architecture MUST NOT depend on `androidx.lifecycle.ViewModel` or any Android/JVM-specific lifecycle classes.

## 4. SERVER-DRIVEN UI (SDUI) CONSTITUTION & OBJECT GRAPH
- **Canonical Non-Recursive Hierarchy**:
  - `Screen -> Template -> Component -> SubComponent -> Child -> ChildrenData`
  - `Screen` owns root Theme & metadata.
  - `ChildrenData` is ALWAYS terminal and cannot contain any further hierarchy node.
  - **Recursion Restrictions**: No `Child -> Child` recursion; no `SubComponent -> SubComponent` recursion; no arbitrary recursive trees.
- **SDUI Object Graph Lifecycle**:
  - `Raw Response -> Parse -> Validate -> Normalize -> Apply Theme/Screen Context -> Assemble Hierarchy Nodes -> Attach Terminal ChildrenData -> Immutable Screen Graph -> Render`.
  - Compose MUST NEVER render directly from raw/unchecked JSON. Monolithic `ScreenBuilder` classes are prohibited.
  - Hierarchy construction uses `Composite` + `Assembler`. Construction operates via `Assembler` rather than Builders unless progressive conditional building is genuinely required.
- **Context Ownership & Extension Law**:
  - `Screen` establishes `ThemeContext` once; child nodes inherit context without re-parsing theme metrics for every leaf.
  - Adding a new `Template`, `Component`, `SubComponent`, `Child`, or `ChildrenData` renderer MUST NOT require modifying the core rendering pipeline. Renderers use `Registry` + `Strategy` patterns to eliminate giant `when(type)` blocks.
- **Node Interaction & Declarative Action Model**:
  - Visually terminal `ChildrenData` nodes emit semantic `NodeEvents`. Renderers detect interactions; the execution layer processes actions via `NodeEvent -> EventRouter -> ActionDispatcher -> ActionRegistry -> ActionExecutor -> ExecutionResult`.
  - Backend-driven actions describe `type`, `id`, `parameters`, `target`, `execution policy`, `guard`, and `result binding`. Multiple actions support explicit policies (`SEQUENTIAL`, `PARALLEL`, `FIRST_SUCCESS`, `STOP_ON_FAILURE`).

## 5. DYNAMIC ACTION, EXECUTION & WORKFLOW ENGINE
- **Action Execution Architecture**:
  - Uniform execution flow: `Action -> ActionExecutor -> ActionRegistry -> ExecutionContext -> ExecutionResult` using Command + Registry/Executor patterns.
  - Adding a new action type (API call, GraphQL, Navigation, Form Update, Dialogs, Payments, Camera, BLE, Location, Workflows) requires registering a new `ActionExecutor`—never modifying the execution engine core.
- **Action Safety & Interaction Guards**:
  - Cross-cutting policies (rapid double-click prevention, duplicate submission guards, disabled state, debounce, throttle, idempotency, authentication requirement, network checks, retry, timeout) are composed around execution (Chain of Responsibility / Pipeline) rather than duplicated inside individual ActionExecutors.
- **Parent/Child Coordination & State Binding**:
  - Nodes MUST NOT couple tightly using parent references (`child.parent.doSomething()`). Target resolution relies on context scope IDs (`SELF`, `PARENT`, `SCREEN`, `NodeId`, `StateScopeId`).
  - Dynamic content uses `BindingResolver` to resolve scoped runtime state to render values without custom native screen code.
- **Workflow Subsystem**:
  - Sequential steps, branching, retries, timeouts, pause/resume, and recovery are handled in a dedicated Workflow engine boundary built on top of the Execution engine.

## 6. NAVIGATION CONSTITUTION
- Multiplatform-compatible stateful navigation stack (single source of truth for `push`, `replace`, `pop`, `popTo`, `reset`, deep links, state restoration, system back, and desktop keyboard shortcuts).
- Navigation MUST NOT fetch SDUI screens or make business network calls directly. Execution engine requests navigation operations through contracts.

## 7. COMMUNICATION & TRANSPORT CONSTITUTION
- **Multi-Transport Mapping**:
  - **Dynamic SDUI Documents**: REST / HTTP
  - **Structured Business Data**: GraphQL
  - **Realtime Operations**: WebSockets / Streaming
  - **Media & Files**: Multipart HTTP
- Transport implementations (Ktor, Apollo, etc.) are strictly hidden behind protocol-agnostic domain request contracts.

## 8. PERSISTENCE & CAPABILITY PROVIDERS
- **Persistence Decoupling**: Separate abstractions for `Preferences`, `SecureStorage`, `StructuredDatabase`, `Cache`, `FileStorage`, and `OfflineQueue`. Database details MUST NOT leak outside their persistence module.
- **Secure Storage**: Exposes one multiplatform `SecureStorage` contract implemented via platform-native security (KeyStore on Android, Keychain on iOS, Keyring on Desktop).
- **Capability Providers**: Platform & vendor capabilities (Payment, Camera, Location, Maps, BLE, Biometric, QR, Notifications, File Picker, Share) are accessed via `CapabilityProvider` contracts. Changing vendors (e.g., Razorpay -> Stripe) requires zero changes to business execution or SDUI code.

## 9. DESIGN PATTERN SELECTION CONSTITUTION & REUSE LAW
- **Pattern Selection Rule**: Design patterns are architectural tools, NOT mandatory ceremony. Evaluate the expected change axis and select the smallest pattern or combination that solves the concrete problem.
- **Prohibited Ceremonial Usage**: NEVER create `Factory`, `Builder`, `Strategy`, `Registry`, `Manager`, `Mediator`, `Observer`, `Command`, `Adapter`, or `Repository` merely because a pattern is common. Prefer composition over inheritance.
- **Canonical Design Pattern Guidance by Concern**:
  - **SDUI Hierarchy**: Composite
  - **Dynamic Renderer/Action/Provider Discovery**: Registry
  - **Dynamic Implementation Selection**: Factory Method / Registered Factory
  - **Contextual Metric Interpretation**: Resolver
  - **Interchangeable Rendering/Layout**: Strategy
  - **Dynamic Backend Actions**: Command
  - **Multi-Step Workflows**: State Machine + Interpreter
  - **Action Guard & Execution Pipeline**: Chain of Responsibility / Pipeline
  - **Platform/Vendor Integration**: Adapter + Provider
  - **Cross-System Event Routing**: Mediator / EventRouter
  - **State Observation**: StateFlow / Observer semantics
  - **Object Graph Assembly**: Assembler (Builder ONLY when progressive conditional construction is truly required)
- **Pattern Review Mandate**: All future major architecture implementation plans MUST include a `DESIGN PATTERN REVIEW` section detailing the problem, change axis, pattern selected, alternatives rejected, and verification that extending the subsystem requires editing **ZERO** existing code (only new registration/composition).
- **Reuse Law**: Search codebase before creating any class, interface, function, Store, or test utility. Decision hierarchy: `REUSE -> COMPOSE -> EXTEND -> CREATE`.
- **Cross-Cutting Single-Owner & Zero Semantic Duplication Law**:
  - If a behavior, calculation, or pipeline applies to multiple renderers, screens, nodes, actions, platforms, or subsystems, its algorithm MUST have exactly ONE canonical implementation. Duplicating semantic logic across multiple renderers or platform source sets is strictly prohibited.
  - Renderers MUST remain thin presentation bridges (`NodeEvent` emission + Compose primitive rendering). Renderers MUST NOT perform screen-ratio scaling, density calculations, raw JSON parsing, or direct network/database calls.
  - Adaptive resolution operates through a centralized pipeline (`AdaptiveEnvironment` snapshot -> Focused Resolvers for Dimension, Typography, Spacing, Color, Radius, Elevation -> Clamped Safety Bounds -> Resolved Compose Token). Renderers consume resolved style inputs without calculating layout mathematics independently.
  - Absolute Prohibition of God Utilities: Centralization MUST NOT result in generic dumping grounds (`Utils.kt`, `UiUtils.kt`, `CommonHelper.kt`). Reusable algorithms MUST be owned by focused, single-responsibility domain resolvers or engine services.


## 10. DEPENDENCY ADMISSION & CATALOG POLICY
- **Admission Checklist**: Before adding any external library, verify: actual requirement, existing capability, stable release status, Android/iOS/Desktop compatibility, KMP/CMP support, maintenance activity, licensing, security, binary size, build impact, and replacement complexity.
- **Strict Isolation**: Dependencies are introduced ONLY when their owning subsystem is implemented. Never add dependencies "for future use".
- **Single Catalog**: `gradle/libs.versions.toml` is the single source of truth for versions and dependencies. No hardcoded version literals in `.gradle.kts` files.

## 11. QUALITY GATES, WARNINGS & ZERO SUPPRESSION
- **Three Quality Gates**: `1. BUILD SUCCESS` | `2. TEST SUCCESS` | `3. RUNTIME SMOKE SUCCESS`.
- **Zero Suppression Policy**: NEVER suppress compiler warnings, toolchain compatibility warnings (`android.suppressUnsupportedCompileSdk`), lint findings, or runtime errors to fake a green build. Fix the root cause or report technical incompatibility to obtain decision approval.

## 12. TESTING & PLATFORM VALIDATION
- **Testing Categories**: Unit, MVI/State, Reducer, Parser, Validator, Normalization, Serialization, Schema/Contract, Execution, Workflow, Navigation, Persistence, Cache, Communication, Capability, UI, Accessibility, Snapshot, Integration, and Regression.
- **Location & Coverage**: Common logic tested in `commonTest`. Platform-specific behavior tested in corresponding Android/iOS/Desktop test targets. Focus on high behavioral coverage for critical business/engine logic rather than superficial 100% line coverage. Every bug fix requires a regression test.
- **Platform Verification Reporting**:
  - `Android`: Build (PASS/FAIL), Tests (PASS/FAIL), Runtime (PASS/FAIL/NOT AVAILABLE).
  - `iOS`: Configuration (PASS/FAIL), Metadata/Commonization (PASS/FAIL), Native Compilation (PASS/FAIL/NOT AVAILABLE), Framework Linking (PASS/FAIL/NOT AVAILABLE), Xcode Build (PASS/FAIL/NOT AVAILABLE), Runtime (PASS/FAIL/NOT AVAILABLE).
  - `Desktop`: Build (PASS/FAIL), Tests (PASS/FAIL), Runtime (PASS/FAIL). Mandatory Desktop JVM smoke test (`./gradlew :desktopApp:run`) on Windows before completing any phase.

## 13. KDOC & DOCUMENTATION MANDATE
- **Public APIs**: All public interfaces, classes, methods, and types MUST have meaningful KDoc explaining purpose, responsibility, why it exists, expected caller, ownership, lifecycle, concurrency behavior, platform considerations, extension rules, and error behavior.
- **Intent Focus**: Documentation MUST explain *WHY* and architectural intent, not restate code syntax. Private code MUST be documented when non-obvious rationale exists.

## 14. OBSERVABILITY, DIAGNOSTICS & SECURITY
- **Structured Multi-Category Observability**: Zero raw console logging (`println`, `Log.*`, `NSLog`). Events include `timestamp`, `level`, `category`, `sourceClass`, `sourceFunction`, `event`, `message`, `traceId`, `operationId`, `requestId`, `screenId`, `actionId`, `workflowId`, `durationMs`, and `platform`.
- **Categories**: `APP`, `LIFECYCLE`, `UI`, `INTERACTION`, `MVI`, `SDUI`, `EXECUTION`, `WORKFLOW`, `NAVIGATION`, `COMMUNICATION`, `GRAPHQL`, `HTTP`, `WEBSOCKET`, `PERSISTENCE`, `CACHE`, `CAPABILITY`, `PAYMENT`, `LOCATION`, `BLE`, `PERFORMANCE`, `SECURITY`, `ERROR`.
- **Network & Realtime Diagnostics**: Human-readable JSON formatting in development. Clear GraphQL operation/variable tracing. WebSocket lifecycle state logging (`CONNECT`, `CONNECTED`, `SUBSCRIBE`, `MESSAGE_RECEIVED`, `RECONNECTING`, `DISCONNECTED`, `ERROR`).
- **End-to-End Flow Tracing**: Enable full flow reconstruction (e.g., `User Interaction -> Intent -> Store -> Action -> Executor -> Network -> State Transition -> Navigation` and `SDUI Parse -> Validate -> Normalize -> Template -> Render -> Action -> Workflow`).
- **Security & Mandatory Redaction**: ABSOLUTE PROHIBITION of logging sensitive credentials (tokens, Authorization headers, OTPs, passwords, PINs, bank secrets, secure storage data). PII (phones, emails, addresses, locations) MUST be masked/redacted automatically.

## 15. PERFORMANCE & CONCURRENCY CONSTITUTION
- **Performance Anti-Patterns**: Avoid main-thread blocking, uncontrolled recomposition, repeated JSON parsing, unnecessary network calls, unbounded caches, renderer reflection, and giant state copies.
- **Structured Concurrency**: No `GlobalScope`. Coroutine scopes are strictly tied to component lifecycles or managed execution engines. Structured cancellation propagation, race condition prevention, rapid double-click action deduplication, and thread-safe state update serialization are enforced.

## 16. COMPLETE DEFINITION OF DONE
A task is COMPLETE if and only if all applicable criteria pass:
1. Requirement & ownership confirmed.
2. Codebase searched; reuse/compose/extend decision documented.
3. Architecture boundaries & multiplatform parity respected.
4. Implementation complete with KDoc explaining intent.
5. `commonTest` / platform tests written and passing.
6. Build successful with ZERO actionable warnings/suppressions.
7. Security, redaction, and logging rules verified.
8. Android & iOS validated to environment capability.
9. Desktop JVM runtime smoke test passed locally (`./gradlew :desktopApp:run`).
10. `git diff` and `git status` reviewed; self-audit clean.

## 17. ANTIGRAVITY WORKFLOW (PERMANENT STEP-BY-STEP)
1. Inspect repository & verify environment.
2. Formulate explicit Implementation Plan & request approval. Include `DESIGN PATTERN REVIEW` for major architecture subsystems.
3. Implement approved scope only (KDoc + tests).
4. Run builds, tests, and Desktop JVM runtime smoke test.
5. Perform repository audit (`git diff`, `git status`).
6. Present factual report and stop. Never auto-advance.

## 18. MAPPING, SUBSYSTEM & MODEL-BOUNDARY CONSTITUTION
- **Data Flow Lifecycle**:
  - `External / Transport DTO -> Deserializer -> Validator -> Normalizer -> Mapper / Assembler -> Domain / Definition Model`
  - `Persistence Entity <-> Mapper <-> Domain Model`
- **Mapping Laws**:
  - Mapping has exactly one canonical owner. Mappers are pure, deterministic functions.
  - Mappers MUST NOT perform network calls, database queries, navigation operations, Compose UI rendering, or vendor SDK interactions.
  - DTOs MUST NOT leak into UI/runtime presentation layers. Database entities MUST NOT leak into domain/rendering. Runtime state MUST NOT be reused as transport DTOs. Renderers MUST NEVER manually parse or convert transport DTOs.
- **Model Classification**:
  - `TRANSPORT DTO`: External serialized representation.
  - `PERSISTENCE ENTITY`: Local database/storage representation.
  - `DOMAIN / CONTRACT MODEL`: Stable application business meaning.
  - `SDUI DEFINITION MODEL`: Immutable, validated backend screen definition.
  - `RUNTIME STATE MODEL`: Mutable/evolving application runtime state.
  - `RESOLVED PRESENTATION MODEL`: Final renderer-ready values after token, adaptive, and property resolution.
- **Product Model & Static vs SDUI Laws**:
  - **Single Native Screen**: Native product UI exists ONLY for the Launch/Splash Screen (`feature:splash`).
  - **Backend-Driven Screens**: EVERY screen after Splash (Login, OTP, KYC, Home, Bookings, Earnings, Wallet, Support, Profile, Dialogs, Sheets) is Server-Driven UI. Monolithic native feature screens (e.g., `AuthScreen`, `BookingScreen`) are strictly prohibited.
  - **Zero Static Visual Values**: Renderers MUST NOT hardcode text, font size, padding, margin, radius, color, icon, or dimensions. Visual attributes originate from backend definitions, theme tokens, and centralized adaptive resolvers.
- **Cross-Cutting Subsystem Ownership Laws**:
  - **Resource Resolution**: `ResourceReference -> ResourceResolver -> ResolvedResource -> Renderer`. Single canonical owner for loading/error/fallback resource resolution. Renderers consume resolved resource states without executing transport or loading algorithms independently.
  - **Dynamic Form Runtime**: One generic dynamic form runtime (`FieldId`, value, validation, touched, dirty, enabled, visible, focus, keyboard action, submit state) manages post-Splash dynamic forms. Native Auth/Profile form managers are prohibited.
  - **Session Runtime**: Session management (access token, refresh token, expiry, restoration, logout, secure persistence) is owned by a single cross-cutting session runtime service. Creating native `AuthRepository` classes for Login/OTP is prohibited.
  - **Application Configuration**: Single read-only runtime configuration owner manages environment parameters, minimum app versions, maintenance flags, and supported SDUI schemas post-Splash bootstrap.
  - **Lifecycle & Capability States**: Common lifecycle signals (`ACTIVE`, `INACTIVE`, `FOREGROUND`, `BACKGROUND`) are decoupled from platform APIs. Capability states (`SUPPORTED`, `UNSUPPORTED`, `AVAILABLE`, `UNAVAILABLE`, `PERMISSION_REQUIRED`, `PERMISSION_DENIED`) are platform-neutral.
  - **Clock, ID & Concurrency**: Time providers, UUID generation, trace IDs, request IDs, workflow IDs, idempotency keys, and coroutine dispatchers are owned centrally by testable providers. `GlobalScope` is prohibited.
  - **Cache & Refresh Policy**: Centralized rules govern cache expiry, invalidation, stale data, SDUI document caching, offline queues, and reconnect sync. Individual repositories MUST NOT invent arbitrary caching logic.
  - **Screen & Overlay Coordination**: Navigation and Renderers do NOT fetch screens directly. Single Screen Runtime Coordinator manages `Route -> Screen Acquisition -> Parse -> Validate -> Normalize -> Definition -> State -> Render`. Single Overlay Coordinator handles dynamic dialogs, sheets, snackbars, and loading overlays.
  - **Realtime, Analytics & Security**: `WebSocket Frame -> Realtime Mapper -> App Event -> Execution`. Vendor analytics names are mapped strictly at the analytics provider boundary. Payload size limits, hierarchy depth limits, malformed resource rejection, and URL allow-policies are enforced at parsing boundaries.
  - **Test Fixtures & Visibility**: Representative SDUI JSON fixtures (all composite levels, actions, bindings, unknown types, malformed properties) are mandatory for contract testing. Modules default to `internal` visibility.

## 19. CACHE, MEMORY, BACKGROUND & RESOURCE CONSTITUTION
- **Cache Architecture & Centralized Policy**:
  - Caching is a first-class runtime concern. Repositories, renderers, and executors MUST NOT independently invent TTL, expiration, stale behavior, eviction, or refresh logic.
  - Multi-level caching (`Request -> Memory Cache -> Persistent Cache -> Network -> Mapper -> Cache Update -> Consumer`) must preserve schema/version validity. Incompatible cached SDUI screens MUST NEVER be rendered merely because data exists locally.
  - Memory caches MUST be bounded (defined maximum size/cost, eviction policy, lifetime, clear triggers). Unbounded mutable maps are strictly prohibited.
- **Memory & Resource Lifecycle**:
  - All long-lived objects (WebSockets, DB connections, streams, image resources, BLE/Location sessions) MUST define explicit release behavior (`Acquire -> Use -> Cancel/Close/Dispose`). Lifecycle cleanup MUST NOT rely solely on garbage collection.
  - Lifecycle, coroutine scope, listener, subscription, and platform context leaks are strictly prohibited.
- **Background Execution & Concurrency**:
  - Long-running or blocking work (I/O, parsing, mapping, crypto) MUST NOT execute on the UI/main thread. Dispatcher selection operates through testable context abstractions without scattering `Dispatchers.IO` indiscriminately.
  - Structured concurrency is mandatory. `GlobalScope` and unowned coroutine scopes are strictly prohibited.
  - Durable background tasks (surviving app process termination) operate behind platform-neutral background-work contracts adapted later to platform schedulers (e.g., WorkManager, Apple background tasks). Common code MUST NOT depend directly on platform APIs.

## 20. RUNTIME CLEAN ARCHITECTURE & EXECUTION LAWS
- **Splash Clean Architecture Law**:
  - Native Splash UI operates through `SplashIntent -> Store<SplashState, SplashIntent, SplashEffect> -> Startup Orchestrator -> SplashState / SplashEffect -> First SDUI Route`.
  - Store is the single state owner. Dual `SplashViewModel` + `SplashStore` duplication is strictly prohibited. `SplashRepository` classes MUST NOT be created unless a genuine reusable data abstraction exists.
  - Splash MUST NOT directly depend on Ktor, database implementations, secure storage implementations, SDUI parser internals, or platform SDKs.
- **Post-Splash Dynamic Architecture Law**:
  - Post-Splash screens are fully backend-driven (SDUI). Endpoint- or screen-specific repositories/use-cases/viewmodels (e.g. `LoginRepository`, `OtpUseCase`, `WalletViewModel`) are strictly prohibited.
  - Runtime execution operates via `SDUI Renderer -> NodeEvent -> EventRouter -> ActionDispatcher -> ActionExecutor -> Transport -> Response Mapping -> Result Binding -> Store -> StateFlow -> Compose Recomposition`.
  - Domain UseCases and Repositories remain valid ONLY for genuine, reusable application/domain capabilities.
- **Request Model & Context Ownership Law**:
  - Raw backend JSON MUST NOT be passed directly to transport clients. Request creation follows `Backend Action Definition -> Parse / Validate -> Request Definition -> Request Resolution -> Typed Resolved Request -> Transport`.
  - Context ownership is strictly partitioned across canonical owners:
    1. `Device / Application Context`: Installation ID, app version, OS version, locale, timezone.
    2. `Session Context`: Access token, refresh token, user ID, partner ID, session ID.
    3. `Screen / Runtime State`: Screen-specific state variables (`selectedServiceId`, `bookingId`).
    4. `Form State`: Dynamic form input values owned by generic Form Runtime.
    5. `Workflow State`: Multi-step workflow variables owned by Workflow Runtime.
    6. `Action Result Context`: Values produced by prior action executions.
    7. `Action Constants`: Static backend action parameters.
- **Binding Resolution & Single-Owner Request Assembly**:
  - One canonical binding-resolution mechanism (`BindingResolver`) resolves scoped runtime values (`${device.id}`, `${session.partnerId}`, `${form.phone}`) for headers, query params, path params, request bodies, UI text, and conditions. Individual subsystems MUST NOT invent custom string interpolation.
  - Request assembly pipeline (`Request Definition -> Default Metadata -> Device Context -> Session Context -> Trace Context -> Action Values -> Security Policy -> Resolved Request`) is centralized. Screens and renderers MUST NOT manually construct headers (e.g., `Authorization`, `Device-Id`, `Trace-Id`).
- **Response Mapping & Result Binding Law**:
  - Reverse execution pipeline (`Raw Response -> Transport Model -> Mapper -> Execution Result -> Result Binding -> Runtime State -> Store -> StateFlow -> UI`) enforces boundary mapping.
  - Transport DTOs MUST NOT leak into Store states, SDUI renderers, or domain models. Meaningless 1:1 mappers without architectural boundaries are prohibited.
- **MVI vs Execution vs Network Responsibility Matrix**:
  - `MVI`: Intent, State, Effect, state transitions, state lifecycle.
  - `Execution`: Actions, action execution, workflows, capability invocation, navigation requests.
  - `Network`: HTTP, GraphQL, WebSockets, Multipart, auth headers, serialization.
  - `Runtime Context`: Session, Device/App Context, Screen State, Form State, Workflow State, Action Result Context.
