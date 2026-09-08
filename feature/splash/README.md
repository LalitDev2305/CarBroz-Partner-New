# CarBroz Partner — Static Splash, Bootstrap & Dynamic Handoff

Status: **FROZEN IMPLEMENTATION CONTRACT**

This document defines the exact production architecture for the exceptional static startup path. It is subordinate to `docs/architecture/MASTER-ARCHITECTURE-CONSTITUTION.md`; when they conflict, the Master Constitution wins. Code, tests, Gradle dependencies and DI are incomplete until they match this contract exactly.

## 1. Two application execution paths only

CarBroz has exactly two frontend execution paths:

1. **Static startup/bootstrap** — used only while the static Splash starts the application, restores the canonical session, fetches current startup policy and resolves the first trusted dynamic destination.
2. **Generic dynamic SDUI runtime** — used by all normal product screens and normal backend-driven actions after bootstrap.

There are no screen-specific Login/OTP/Dashboard/Profile/etc. networking stacks, repositories, use cases, stores or feature modules merely because those screens exist. Dedicated domain code is introduced only for genuine business invariants/operations.

```text
STATIC EXCEPTION
Splash -> ApplicationRuntime -> StartupCoordinator
       -> SessionRestoreStartupTask
       -> BootstrapStartupTask -> ResolveBootstrapUseCase
       -> BootstrapRepository -> RemoteBootstrapRepository
       -> canonical NetworkDataSource -> NetworkExecutor -> KtorNetworkTransport
       -> BootstrapSnapshot -> trusted StartupPayload -> Ready/Blocked/Failed

NORMAL APP
DynamicScreenInstruction -> generic DynamicFeature -> SDUI runtime
       -> binding/action runtime -> generic NetworkActionExecutor
       -> same canonical NetworkDataSource -> NetworkExecutor -> KtorNetworkTransport
```

## 2. Non-negotiable ownership

- `feature:splash` owns static presentation only: `SplashIntent`, `SplashState`, `SplashEffect`, `SplashStore`, `SplashScreen`.
- `runtime:application` is the sole startup-state owner and owns startup orchestration plus application-level bootstrap policy/contracts/use case.
- `foundation:session` is the sole session/authentication owner, including persisted-session restoration and corrupt-session cleanup.
- `foundation:configuration` owns immutable process configuration: environment, API base URL, client platform, build information and neutral feature-flag contracts. It does not own Partner bootstrap DTOs or HTTP routes.
- `data:network` owns the single canonical REST execution stack, typed response decoding mechanism, global metadata headers, auth integration, retry/timeout/connectivity/cache/security/observability policy.
- `data:bootstrap` owns only the Partner bootstrap transport adapter: route contract, serializable backend DTOs and `RemoteBootstrapRepository`.
- `feature:dynamic` owns the implementation that converts the opaque trusted bootstrap payload into `DynamicScreenInstruction`; `runtime:application` must not depend on `feature:dynamic`.
- `foundation:navigation` owns stack mechanics; saved dynamic navigation is applied only after a fresh bootstrap resolves the server-authoritative root.
- `app:composition` wires these owners together. It does not become another startup, session or network owner.
- `app:startup` does not exist after this migration.

No second bootstrap store, second REST client, second session snapshot, second navigation stack or second generic dynamic-screen flow is permitted.

## 3. Exact production structure for this slice

```text
feature/splash/
├── README.md
└── src/commonMain/kotlin/com/carbroz/feature/splash/
    ├── SplashContract.kt
    ├── SplashStore.kt
    └── SplashScreen.kt

runtime/application/
├── build.gradle.kts
└── src/commonMain/kotlin/com/carbroz/runtime/application/
    ├── ApplicationRuntime.kt
    ├── bootstrap/
    │   ├── BootstrapContract.kt
    │   └── ResolveBootstrapUseCase.kt
    └── startup/
        ├── StartupTask.kt
        ├── StartupCoordinator.kt
        ├── SessionRestoreStartupTask.kt
        └── BootstrapStartupTask.kt

data/bootstrap/
├── build.gradle.kts
└── src/commonMain/kotlin/com/carbroz/data/bootstrap/
    ├── BootstrapApiContract.kt
    ├── PartnerBootstrapDto.kt
    └── RemoteBootstrapRepository.kt

data/network/src/commonMain/kotlin/com/carbroz/data/network/
├── NetworkDataSource.kt
├── NetworkExecutor.kt
├── KtorNetworkTransport.kt
├── NetworkPolicy.kt
├── ConfigurationNetworkHeaderProvider.kt
└── ...existing canonical network infrastructure

feature/dynamic/src/commonMain/kotlin/com/carbroz/feature/dynamic/
├── DynamicScreenInstructionCodec.kt
├── DynamicStartupPayloadDecoder.kt
└── ...existing generic dynamic feature

foundation/session/src/commonMain/kotlin/com/carbroz/foundation/session/
├── SessionStore.kt
└── ...existing canonical session infrastructure

app/composition/
└── DI/cross-owner wiring only
```

The `data:bootstrap` module is intentional: it prevents Partner transport DTO/route code from contaminating the reusable `data:network` module. It contains one remote repository implementation, not a ceremonial repository-impl -> remote-data-source chain.

## 4. Static Splash MVI/UDF

```text
Lifecycle / User
      ↓ SplashIntent
SplashStore
      ↓ delegates startup
ApplicationRuntime
      ↑ observable runtime state
SplashStore
      ↓ immutable SplashState / one-time SplashEffect
SplashScreen / composition effect consumer
```

Splash knows nothing about Ktor, JSON, endpoints, bootstrap DTOs, database/cache strategy or session persistence internals.

Intents remain:

```text
LifecycleChanged(state)
RetryClicked
UpdateClicked
```

States remain:

```text
Loading
RequiredUpdate(title, message, updateUri)
Maintenance(title, message, retryEnabled)
Error(message, retryEnabled)
Ready
```

Effects remain:

```text
Navigate(startupPayload)
OpenUpdateUri(uri)
```

Startup begins when common lifecycle enters Foreground. Background cancellation, retry and one-navigation-per-attempt behavior remain owned by the existing SplashStore/ApplicationRuntime contract.

## 5. Startup orchestration

`ApplicationRuntime` remains the only observable startup state owner:

```text
Idle
Starting(attempt)
Ready(attempt, payload, notices)
Blocked(attempt, blocker)
Failed(taskId, failure, attempt)
```

`StartupCoordinator` executes ordered tasks:

```text
1. SessionRestoreStartupTask
2. BootstrapStartupTask
```

Task results remain:

```text
Continue
Resolved(StartupResolution.Ready | StartupResolution.Blocked)
Failure(StartupFailure)
```

All tasks returning `Continue` must fail closed with `startup_missing_resolution`. Cancellation propagates and the runtime returns to the previous stable state according to the existing runtime contract.

## 6. Session restoration

`foundation:session` owns the complete restore policy.

`SessionStore.restore()` must:

- restore a valid persisted authenticated session;
- return signed-out success when no persisted session exists;
- treat malformed or unsupported persisted snapshots as invalid local session data, clear them through canonical session persistence, publish `SignedOut`, and return success when cleanup succeeds;
- report storage/cleanup failure without pretending restoration succeeded;
- propagate coroutine cancellation.

`SessionRestoreStartupTask` is intentionally thin: it calls the canonical session store and translates the semantic result to `Continue` or recoverable/non-recoverable `StartupFailure`. It does not inspect storage payloads and does not perform separate sign-out/cleanup policy.

## 7. Bootstrap application boundary

`runtime:application/bootstrap` defines only transport-independent application semantics.

`BootstrapRepository` is an inward contract. `data:bootstrap` implements it.

The normalized `BootstrapSnapshot` contains only what startup policy needs:

```text
authenticated
maintenance(enabled, title, message)
update(required, optional, minimumVersion, latestVersion, updateUri)
nextPayload   // opaque serialized startup payload; never raw JsonElement in runtime
```

Partner response-envelope fields, `traceId`, JSON types and transport field names do not cross this boundary.

`StartupPayloadDecoder` is also defined inward by `runtime:application`. It receives the opaque serialized payload and returns either a trusted `StartupPayload` or a stable validation failure code. `feature:dynamic` supplies `DynamicStartupPayloadDecoder` using the existing `DynamicScreenInstructionCodec`. Therefore `runtime:application` never depends on `feature:dynamic` and no circular dependency is introduced.

## 8. ResolveBootstrapUseCase

`ResolveBootstrapUseCase` owns the real application policy, so it is retained as a genuine use case rather than moving policy into networking or Splash.

Its ordered policy is:

1. Load `BootstrapSnapshot` through `BootstrapRepository`.
2. Map repository transport/data failure into stable startup failure semantics.
3. Compare backend `authenticated` with canonical local `SessionProvider` state; mismatch fails closed.
4. Required update has first precedence and requires a non-blank update URI.
5. Maintenance is a valid blocked startup result, not a failure.
6. Decode/validate `nextPayload` through `StartupPayloadDecoder`.
7. Optional update becomes a non-blocking `StartupNotice.OptionalUpdate`.
8. Return `StartupResolution.Ready` with the trusted payload.

`BootstrapStartupTask` only adapts `ResolveBootstrapUseCase` result into `StartupTaskResult`; it contains no duplicate policy.

## 9. Partner bootstrap data adapter

`data:bootstrap` contains exactly one remote implementation: `RemoteBootstrapRepository`.

It owns:

- `GET /api/v1/partner/bootstrap` through `BootstrapApiContract`;
- `NetworkAuthentication.OPTIONAL_SESSION`;
- the serializable Partner response DTO;
- validation of the common success envelope and required bootstrap fields;
- mapping DTO -> `BootstrapSnapshot`;
- mapping canonical network failures into `BootstrapRepositoryFailure`.

It does not create an `HttpClient`, add Authorization/client-metadata headers manually, implement retry, manage session state, make update/maintenance decisions, navigate or render UI.

There is no bootstrap local/database cache in this slice. Bootstrap is network-authoritative because startup must obtain the current server policy. Application-data caching remains a repository decision for data that genuinely requires local/remote coordination; the network layer only owns technical HTTP/cache policy.

## 10. Canonical network stack

All REST calls — static bootstrap, dynamic screen fetching and dynamic API actions — use the same execution path:

```text
NetworkDataSource
      ↓
NetworkExecutor
      ↓
KtorNetworkTransport
      ↓
process-owned REST HttpClient
```

`NetworkDataSource` additionally provides typed response decoding using a caller-supplied kotlinx serializer. The serialization mechanism/error normalization belongs to `data:network`; the DTO serializer itself belongs to the API-specific owner. Existing raw `NetworkResult` execution remains available for generic dynamic runtime paths that require protocol-level JSON.

No bootstrap-specific Ktor client is permitted.

`data:realtime` remains a separate protocol abstraction and may own a WebSocket-configured Ktor client; the single-client rule here means one canonical REST client/path, not one physical client for unrelated protocols.

## 11. Configuration and global headers

`foundation:configuration` stays unchanged as the canonical immutable process configuration owner:

```text
AppEnvironment
apiBaseUrl
ClientPlatform
BuildInformation
```

Remote bootstrap JSON does not replace or mutate `AppConfiguration`.

`ConfigurationNetworkHeaderProvider` lives in `data:network` and reads `ConfigurationProvider` to supply globally:

```text
X-CarBroz-Platform
X-CarBroz-App-Version
X-CarBroz-Build-Number
```

`NetworkExecutor`/session integration owns Authorization. Request code cannot override provider-owned/reserved header names.

The Partner bootstrap `features` object remains a transport field for backend compatibility. Because the current client startup/dynamic runtime does not consume those values directly, this slice does not create an unused mutable feature-flag store. If a future application-wide client consumer is proven, it must map into the existing neutral `foundation:configuration/featureflag` ownership rather than remaining in bootstrap transport models.

## 12. Dynamic handoff and the one normal app flow

After bootstrap resolves a trusted `DynamicScreenInstruction`, bootstrap is finished.

```text
ApplicationRuntime.Ready(DynamicScreenInstruction)
      ↓ SplashEffect.Navigate
NavigationProcessStateBridge.applyAfterBootstrap
      ↓ DynamicDestination
Existing generic DynamicFeature
      ↓ generic SDUI screen fetch
Decode -> Validate -> Compatibility -> Normalize -> Runtime IR
      ↓ Dynamic MVI Store / renderer
Interaction -> generic Action Runtime
      ├── generic API -> NetworkActionExecutor -> canonical network stack
      ├── navigation
      ├── capability
      └── registered business operation only when real domain semantics exist
```

Login, OTP, Dashboard, Profile, Booking UI, Earnings UI, etc. remain backend-driven screens. Do not create screen-specific Kotlin network clients/repositories/use cases/stores merely for those screens.

Domain-specific code is allowed only when the operation has genuine client-owned business/security/state invariants, e.g. canonical session establishment, booking state-machine rules, payments/payouts or similar behavior.

## 13. Navigation restoration

Saved dynamic navigation never bypasses startup:

```text
capture saved state -> keep pending -> show Splash -> fresh session restore/bootstrap
-> fresh dynamic root -> compare saved root
-> compatible: restore validated stack
-> incompatible/malformed: discard and ResetTo fresh root
```

## 14. Required dependency direction

Allowed:

```text
feature:splash       -> runtime:application + presentation/lifecycle foundations
runtime:application  -> foundation:session/lifecycle/observability/time
feature:dynamic      -> runtime:application (implements StartupPayloadDecoder)
data:bootstrap       -> runtime:application + data:network
app:composition      -> runtime/feature/data/foundation modules for wiring
```

Forbidden:

```text
runtime:application -> feature:dynamic
data:network        -> runtime:application
data:network        -> Partner bootstrap DTOs
feature:splash      -> data:network / data:bootstrap / session persistence / bootstrap DTOs
foundation:*        -> data:bootstrap or Partner code
app:startup         -> any dependency, because the module must not exist
```

## 15. Migration tasks — one atomic slice

The implementation is incomplete until all four tasks are complete together:

### Task 1 — Contract
- replace this README first;
- keep it aligned with the Master Constitution;
- treat every structure/ownership/test statement here as acceptance criteria.

### Task 2 — Canonical infrastructure ownership
- move/rename global metadata header provider into `data:network`;
- add generic typed response decoding to `NetworkDataSource` without breaking generic raw JSON execution;
- move corrupt persisted-session cleanup into `foundation:session`;
- move `SessionRestoreStartupTask` into `runtime:application` and make it thin.

### Task 3 — Bootstrap clean-architecture migration
- add application bootstrap contracts + `ResolveBootstrapUseCase` in `runtime:application`;
- add `data:bootstrap` with Partner DTO, route contract and one `RemoteBootstrapRepository`;
- add `DynamicStartupPayloadDecoder` in `feature:dynamic`;
- move/replace `PartnerBootstrapStartupTask` with thin `BootstrapStartupTask`;
- remove `PartnerBootstrapClient` and `PartnerBootstrapPolicyEvaluator`;
- remove `app:startup` completely.

### Task 4 — Composition, dependencies, hygiene and verification
- update `settings.gradle.kts`, Gradle dependencies and Koin wiring;
- remove all stale imports/tests/references to `com.carbroz.partner.startup` and `:app:startup`;
- preserve the existing generic dynamic runtime unchanged except for the startup-payload adapter;
- run duplicate/stale-code and forbidden-dependency checks;
- run all required module/full verification gates before freeze.

## 16. Mandatory tests

### `foundation:session`
- no persisted session -> `SignedOut` success;
- valid snapshot restores authenticated state;
- malformed snapshot is cleared and becomes `SignedOut` success;
- unsupported snapshot version is cleared and becomes `SignedOut` success;
- cleanup/storage failure is surfaced;
- cancellation propagates;
- existing authentication/token-refresh/sign-out concurrency tests remain green.

### `data:network`
- global metadata headers for Android/iOS/Desktop and exact version/build values;
- request-specific code cannot override provider-owned headers;
- typed success body decodes with supplied serializer;
- missing/invalid typed body fails closed;
- raw generic JSON execution remains unchanged;
- existing auth recovery, timeout/retry, cache, connectivity and observability tests remain green.

### `data:bootstrap`
- exact GET bootstrap route;
- `OPTIONAL_SESSION` and no manually supplied metadata/Auth headers;
- valid guest and authenticated envelopes map to normalized snapshot;
- unsuccessful envelope, missing data, malformed/invalid body and invalid required fields fail closed;
- offline/timeout/transport/HTTP failures map correctly;
- config `features` may be decoded for compatibility but do not leak into application snapshot;
- request/data mapping is independent of Splash/navigation/session policy.

### `runtime:application`
- existing coordinator/runtime ordering, serialization, retry, cancellation and missing-resolution tests remain green;
- SessionRestore task success/failure mapping;
- required-update precedence;
- required update without URI fails closed;
- maintenance is blocked/retryable, not failure;
- optional update is Ready + notice;
- signed-out/backend-guest and authenticated/backend-authenticated are valid;
- local/backend authentication mismatch fails closed;
- repository failure mapping including recoverable HTTP statuses;
- valid payload decoder result -> Ready;
- invalid payload decoder result -> non-recoverable failure;
- BootstrapStartupTask is a thin result adapter.

### `feature:dynamic`
- valid serialized next-screen payload decodes to trusted `DynamicScreenInstruction`;
- invalid identity/endpoint/method/authentication/transition/restore-policy fails closed through `StartupPayloadDecoder`;
- existing dynamic screen/action tests remain green.

### `feature:splash`
- foreground/start, duplicate foreground, background cancellation, retry, blocker/error mapping and exactly-once navigation effects remain green;
- public Splash presentation contract contains no data/network/bootstrap/session-storage types.

### `app:composition` / architecture
- DI resolves one `BootstrapRepository`, one `StartupPayloadDecoder`, one `NetworkDataSource`, one `SessionStore` and one `ApplicationRuntime`;
- startup task order is session restore then bootstrap;
- no `:app:startup` dependency/module/reference remains;
- `runtime:application` has no dependency on `feature:dynamic`, `data:network` or `data:bootstrap`;
- `data:network` has no Partner bootstrap dependency;
- saved navigation is still applied only after fresh bootstrap;
- Android/iOS/Desktop compile/test targets pass.

## 17. Freeze gate

This slice is frozen only when:

```text
README == implementation == Gradle graph == DI graph == tests
```

and all superseded startup code is deleted. A green build with old/new ownership in parallel is a failure, not a freeze.