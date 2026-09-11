# CarBroz Partner — Static Splash, Bootstrap & Dynamic Handoff

Status: **FROZEN IMPLEMENTATION CONTRACT**

This document is the frontend source of truth for the exceptional Partner startup/bootstrap path. It is subordinate to `docs/architecture/MASTER-ARCHITECTURE-CONSTITUTION.md` and must remain consistent with `docs/architecture/18-dynamic-runtime-architecture.md`.

The design is intentionally small: Clean Architecture, MVI/UDF, dependency inversion and single responsibility without a generic startup workflow framework.

---

## 1. Frozen mental model

CarBroz has two frontend execution paths:

1. **Static startup/bootstrap** — Splash owns presentation state while one application use case restores session state, obtains the server-authoritative bootstrap response, applies startup policy and returns a typed startup result.
2. **Generic dynamic runtime** — after startup, every normal backend-driven screen uses the existing Dynamic + SDUI runtime.

```text
STATIC STARTUP
SplashScreen
    ↓ intent
SplashStore                         # single startup presentation state owner
    ↓
ResolveStartupUseCase               # application orchestration + startup policy
    ├── SessionStore.restore()      # canonical session owner
    ├── BootstrapRepository.load()  # inward application port
    └── PartnerConfigStore.update() # process-scoped reusable Partner config
            ↓
RemoteBootstrapRepository           # data:bootstrap
            ↓
NetworkDataSource
            ↓
NetworkExecutor
            ↓
KtorNetworkTransport
            ↓
GET /api/v1/partner/config/bootstrap
            ↓
Bootstrap DTO -> mapper -> application model
            ↓
StartupResult
    ├── RequiredUpdate
    ├── Maintenance
    ├── Failure
    └── Ready(StartupDestination)
            ↓
SplashEffect.Navigate
            ↓
app:composition
            ↓
DynamicDestination
            ↓
NavigationProcessStateBridge
            ↓
foundation:navigation
            ↓
feature:dynamic
            ↓
nextScreen.endpoint
            ↓
runtime:sdui
```

There are no Login/OTP/Dashboard/Profile-specific frontend feature modules, repositories, stores or network clients merely because those screens exist on the backend.

---

## 2. Non-negotiable ownership

### `feature:splash`
Owns only:

- `SplashIntent`;
- immutable `SplashState`;
- one-shot `SplashEffect`;
- `SplashStore`;
- static Splash Compose UI;
- lifecycle/user event handling for the static startup feature.

It does **not** own:

- Ktor/network implementation;
- bootstrap DTOs/endpoints;
- session persistence;
- authorization headers;
- bootstrap mapping;
- persisted bootstrap cache;
- navigation mechanics;
- SDUI rendering.

`SplashStore` is the one MVI/UDF state owner for startup presentation. No `ApplicationRuntimeState`, `BootstrapStore` or second observable startup state machine may exist in parallel.

### `runtime:application`
Owns transport-independent application startup semantics:

- `ResolveStartupUseCase`;
- `StartupResult`;
- `StartupDestination`;
- `PartnerConfig` / `PartnerFeatures`;
- `PartnerConfigStore`;
- `BootstrapRepository` inward port and normalized bootstrap application models/failures.

It does not own HTTP DTOs, Ktor, Compose, Navigation 3 or Dynamic feature implementation types.

### `foundation:session`
Owns:

- persisted session restoration;
- invalid/corrupt session repair/cleanup;
- authenticated/signed-out truth;
- refresh/invalidation/logout semantics;
- secure credential persistence.

Startup calls `SessionStore.restore()`; it does not duplicate session repair policy.

### `data:bootstrap`
Owns only the Partner bootstrap transport adapter:

- canonical bootstrap route;
- serializable wire DTOs;
- envelope/field validation at the transport boundary;
- DTO -> application bootstrap mapping;
- `RemoteBootstrapRepository`, implementing the application-owned `BootstrapRepository`.

It does not own startup policy, UI, navigation, session state or a second network stack.

### `data:network`
Owns the single canonical REST execution stack:

- request execution;
- global metadata headers;
- Authorization integration;
- authentication recovery;
- timeout/retry/connectivity policy;
- HTTP cache policy;
- typed response decoding;
- observability/security hooks.

Partner bootstrap DTOs/routes do not belong here.

### `foundation:navigation`
Owns navigation stack mechanics, navigation commands, restoration rules and Navigation 3 adaptation.

### `app:composition`
Owns only cross-owner wiring and application-specific adaptation. It maps a trusted `StartupDestination` to the existing `DynamicDestination`, consumes Splash effects and coordinates `NavigationProcessStateBridge`.

### `feature:dynamic` / `runtime:sdui`
They begin **after** bootstrap resolves a destination. They do not decide maintenance, required update, session restoration or bootstrap policy.

`app:startup` must not exist.

---

## 3. Frozen target structure

The focused refactor converges toward this structure; exact filenames may be adjusted only where current repository naming proves a smaller equivalent without changing ownership.

```text
feature/splash/
└── src/commonMain/kotlin/com/carbroz/feature/splash/
    ├── SplashContract.kt
    ├── SplashStore.kt
    └── SplashScreen.kt

runtime/application/
└── src/commonMain/kotlin/com/carbroz/runtime/application/startup/
    ├── BootstrapRepository.kt
    ├── PartnerConfig.kt
    ├── PartnerConfigStore.kt
    ├── ResolveStartupUseCase.kt
    ├── StartupDestination.kt
    └── StartupResult.kt

data/bootstrap/
└── src/commonMain/kotlin/com/carbroz/data/bootstrap/
    ├── BootstrapApiContract.kt
    ├── PartnerBootstrapDto.kt
    ├── BootstrapMapper.kt
    └── RemoteBootstrapRepository.kt

data/network/
└── existing canonical NetworkDataSource / NetworkExecutor / Ktor transport

foundation/session/
└── existing canonical SessionStore/session infrastructure

foundation/navigation/
└── existing canonical NavigationStore/navigation infrastructure

app/composition/
├── DI/composition wiring
└── NavigationProcessStateBridge
```

Do not create one class per trivial mapping merely to mirror layers. `BootstrapMapper` may be a focused mapper function/object if that is the smallest clear implementation.

---

## 4. Superseded startup abstractions

The following current startup abstractions are superseded by this freeze and are migration/removal targets after reference and test migration is proven:

```text
ApplicationRuntime
ApplicationRuntimeState
StartupCoordinator
StartupTask
StartupTaskResult
StartupResolution
SessionRestoreStartupTask
BootstrapStartupTask
StartupPayload
StartupPayloadDecoder
DynamicStartupPayloadDecoder
StartupNotice
```

`ResolveBootstrapUseCase` is superseded by the broader and clearer `ResolveStartupUseCase` because startup orchestration includes session restoration, bootstrap loading and startup policy.

Do not retain compatibility wrappers by default. Keep an old API temporarily only if an actual current consumer requires a staged migration, and remove it before this slice is frozen.

---

## 5. Canonical backend bootstrap transport contract

Current backend routing registers Partner bootstrap under `/config`:

```http
GET /api/v1/partner/config/bootstrap
```

Frontend must not use the stale path:

```text
/api/v1/partner/bootstrap
```

### Request metadata

Bootstrap declares:

```text
method         = GET
authentication = OPTIONAL_SESSION
```

Global network ownership supplies client metadata such as platform/app/build information. Authorization is supplied only by canonical session/network integration. `RemoteBootstrapRepository` does not manually concatenate Bearer tokens or duplicate global headers.

### Canonical response envelope

Backend uses the common API envelope:

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "Partner bootstrap completed",
  "data": {
    "config": {
      "version": "1",
      "maintenance": {
        "enabled": false,
        "title": null,
        "message": null
      },
      "update": {
        "required": false,
        "optional": false,
        "minimumVersion": "1.0.0",
        "latestVersion": "1.0.0",
        "storeUrl": null
      },
      "features": {
        "registrationEnabled": true,
        "individualPartnerEnabled": true,
        "organizationPartnerEnabled": true
      }
    },
    "startup": {
      "authenticated": false,
      "nextScreen": {
        "screenId": "...",
        "templateId": "...",
        "templateType": "...",
        "endpoint": "/api/v1/...",
        "method": "GET",
        "authentication": "NONE"
      }
    }
  },
  "traceId": "req-x"
}
```

The old frontend `success: Boolean` envelope assumption is superseded.

Success validation is based on the canonical transport/HTTP contract and required data, not a synthetic `success` field.

Destination field values are server-authoritative. Frontend architecture must not hardcode Login or Dashboard routing values. Backend bootstrap configuration must itself stay consistent with the backend screen contracts.

---

## 6. Typed application boundary

Do not carry `JsonElement` or serialize decoded bootstrap JSON back into a String only to decode it again.

Transport DTO:

```text
PartnerBootstrapEnvelopeDto
  -> PartnerBootstrapDataDto
  -> PartnerStartupScreenDto
```

maps once into transport-independent application models:

```text
BootstrapSnapshot
  ├── authenticated
  ├── maintenance
  ├── update
  ├── features/config
  └── nextScreen: StartupDestination
```

`StartupDestination` represents only trusted transport-independent startup navigation data, for example:

```text
screenId
templateId
templateType
endpoint
method
authentication
```

The application layer must use typed method/authentication semantics rather than arbitrary unchecked strings after mapping.

`runtime:application` does not depend on `feature:dynamic`. `app:composition` performs the thin adaptation:

```text
StartupDestination
   -> DynamicScreenInstruction / DynamicDestination
```

using the existing Dynamic contract and its validation requirements.

---

## 7. Complete startup request-to-response flow

```text
platform host starts
        ↓
AppConfiguration validated
        ↓
Koin graph/composition initialized
        ↓
Splash rendered
        ↓
common lifecycle enters Foreground
        ↓
SplashIntent.LifecycleChanged(Foreground)
        ↓
SplashStore
        ├── ignores duplicate start while an attempt is active
        └── invokes ResolveStartupUseCase
                ↓
        SessionStore.restore()
                ├── valid persisted session -> Authenticated
                ├── no persisted session -> SignedOut
                └── malformed/unsupported -> canonical SessionStore cleanup/repair
                ↓
        BootstrapRepository.load()
                ↓
        RemoteBootstrapRepository
                ↓
        NetworkRequest(
            GET,
            /api/v1/partner/config/bootstrap,
            OPTIONAL_SESSION
        )
                ↓
        NetworkDataSource / NetworkExecutor / Ktor
                ↓
        canonical backend envelope
                ↓
        DTO validation + one-way mapping
                ↓
        BootstrapSnapshot
                ↓
        PartnerConfigStore.update(reusable config)
                ↓
        ResolveStartupUseCase policy
                ├── required update -> StartupResult.RequiredUpdate
                ├── maintenance -> StartupResult.Maintenance
                ├── invalid/failure -> StartupResult.Failure
                └── ready -> StartupResult.Ready(StartupDestination)
                ↓
        SplashStore reducer/state transition
                ↓
        Ready -> SplashEffect.Navigate(destination)
                ↓
        app:composition maps to DynamicDestination
                ↓
        NavigationProcessStateBridge.applyAfterBootstrap(freshRoot)
                ↓
        foundation:navigation
                ↓
        feature:dynamic
                ↓
        nextScreen.endpoint
                ↓
        runtime:sdui decode -> validate -> compatibility -> normalize -> render
```

Bootstrap completes before normal SDUI screen acquisition begins.

---

## 8. Splash MVI/UDF

Canonical UDF:

```text
Lifecycle / User
      ↓ intent
SplashStore
      ↓ ResolveStartupUseCase
StartupResult
      ↓ reducer/state transition
SplashState
      ↓ UI
SplashScreen
      ↓ one-shot effect when required
app:composition
```

### Intents

At minimum:

```text
LifecycleChanged(state)
RetryClicked
UpdateClicked
```

### States

At minimum:

```text
Loading
RequiredUpdate(title, message, updateUri)
Maintenance(title, message, retryEnabled)
Error(message, retryEnabled)
Ready
```

### Effects

At minimum:

```text
Navigate(StartupDestination)
OpenUpdateUri(uri)
```

The Store owns attempt de-duplication for the feature. A second runtime state machine is prohibited.

Cancellation must propagate; cancellation is not converted into a generic error.

---

## 9. ResolveStartupUseCase responsibility

`ResolveStartupUseCase` is an application use case, not a framework.

It owns only orchestration/policy that spans canonical owners:

1. ask `SessionStore` to restore canonical session state;
2. stop with a stable startup failure only when restoration itself cannot complete safely;
3. load fresh Partner bootstrap through `BootstrapRepository`;
4. update the process-scoped `PartnerConfigStore` after successful validated mapping;
5. apply startup blocking policy;
6. return a typed `StartupResult`.

It does **not**:

- parse HTTP DTOs;
- create Ktor clients;
- manipulate NavigationStore directly;
- render Splash;
- duplicate session cleanup/refresh logic;
- persist bootstrap to Room/DataStore;
- execute arbitrary ordered task plugins.

### Policy order

Frozen order:

```text
restore canonical session
  ↓
load fresh bootstrap
  ↓
required update?
  ├── yes -> block
  ↓ no
maintenance enabled?
  ├── yes -> block
  ↓ no
validate/map startup destination
  ↓
ready
```

Backend already evaluates version comparisons. Frontend consumes `required` / `optional`; it must not implement a second semantic-version policy engine.

`optional == true` is stored as runtime config metadata for whichever real UI consumer is later approved. It does not justify a generic `StartupNotice` framework.

---

## 10. PartnerConfigStore

The bootstrap response intentionally contains small app-wide Partner configuration:

```text
config.version
features.registrationEnabled
features.individualPartnerEnabled
features.organizationPartnerEnabled
update metadata
```

Those values must not be decoded and discarded.

`PartnerConfigStore` is a small process-scoped application owner for the latest successfully validated reusable Partner configuration. It is not another MVI feature Store and does not own startup presentation state.

It may expose a minimal read-only snapshot/flow required by real consumers. Do not turn it into a generic remote-config framework.

### Do not persist bootstrap by default

Bootstrap is server-authoritative for startup blockers and destination selection. Therefore this freeze does **not** add Room or DataStore persistence for the bootstrap document.

Specifically, stale persisted values must not bypass fresh:

```text
maintenance
required update
authenticated truth
nextScreen
```

If offline bootstrap behavior is approved later, it requires an explicit freshness/security product contract before persistence is introduced.

---

## 11. Session restoration and OPTIONAL_SESSION recovery

`SessionStore.restore()` remains the sole restore/repair owner.

Bootstrap uses `OPTIONAL_SESSION`:

```text
SignedOut
  -> no Authorization
  -> backend evaluates guest startup

Authenticated
  -> Authorization: Bearer <access token>
  -> backend validates supplied bearer
```

Backend deliberately rejects an invalid supplied bearer instead of silently treating the same request as anonymous.

Canonical generic network recovery for `OPTIONAL_SESSION` is:

```text
request with bearer
   ↓
401
   ↓
authentication recovery
   ├── Recovered
   │     -> retry once with refreshed Authorization
   │
   ├── SessionInvalidated
   │     -> canonical session is now SignedOut
   │     -> OPTIONAL_SESSION only: retry once without Authorization
   │
   └── Unavailable
         -> return original failure
```

For `SESSION`, `SessionInvalidated` must **not** cause anonymous retry.

This policy belongs in the generic network/session boundary, not in `RemoteBootstrapRepository`.

The final successful bootstrap response is the server-authoritative startup authentication result. A lightweight invariant check may remain as defense in depth, but local/backend auth mismatch must not become a second parallel session model.

---

## 12. Bootstrap repository/data adapter rules

`BootstrapRepository` is owned inward by `runtime:application`. `RemoteBootstrapRepository` implements it in `data:bootstrap`.

The adapter owns:

- `/api/v1/partner/config/bootstrap`;
- `GET`;
- `OPTIONAL_SESSION` declaration;
- Partner bootstrap DTOs;
- canonical response-envelope validation;
- required wire-field validation;
- DTO -> application mapping;
- normalized transport/data failure mapping.

It must not:

- instantiate another HttpClient;
- add global client metadata manually;
- add Authorization manually;
- implement token refresh;
- decide required-update vs maintenance precedence;
- own navigation;
- render UI;
- persist bootstrap configuration;
- decode the returned startup destination through JSON a second time.

---

## 13. Navigation restoration is retained

`NavigationProcessStateBridge` has a valid process-restoration responsibility and remains in `app:composition`.

Saved navigation must never bypass fresh server-authoritative startup:

```text
capture restored navigation state
    ↓
keep pending only
    ↓
show Splash
    ↓
fresh session restore + bootstrap
    ↓
fresh startup root
    ↓
compare restored root navigationId
    ├── exact compatible root -> restore validated saved stack
    └── mismatch/malformed/unsafe -> discard and reset to fresh root
```

The bridge does not decide bootstrap policy and does not become a second navigation engine.

`foundation:navigation` remains the only owner of stack mutation/mechanics.

---

## 14. Dynamic handoff and SDUI boundary

Startup only selects the first trusted semantic destination.

After `StartupResult.Ready`:

```text
StartupDestination
    ↓ thin app:composition adaptation
DynamicDestination
    ↓
NavigationProcessStateBridge
    ↓
NavigationStore
    ↓
DynamicFeature
    ↓ screen request
runtime:sdui
```

SDUI has zero responsibility for:

- bootstrap transport;
- session restoration;
- maintenance;
- required update;
- feature-config persistence;
- initial startup policy.

Normal Login, OTP, Dashboard, KYC, Profile, Booking and Earnings screens remain backend-driven and use the one Dynamic/SDUI runtime.

---

## 15. Required dependency direction

Allowed:

```text
feature:splash       -> runtime:application + lifecycle/presentation foundations
runtime:application  -> foundation:session + foundation-neutral contracts
                         (no data implementation, Compose, Navigation3 or feature dependency)
data:bootstrap       -> runtime:application + data:network
app:composition      -> feature/runtime/data/foundation for wiring/adaptation
feature:dynamic      -> its existing runtime/foundation owners
```

Forbidden:

```text
feature:splash       -> data:network / data:bootstrap / bootstrap DTOs
feature:splash       -> secure session persistence implementation
runtime:application  -> feature:dynamic
runtime:application  -> Ktor / Partner transport DTOs / Navigation3
data:network         -> runtime:application Partner bootstrap models
data:network         -> Partner bootstrap DTOs/routes
runtime:sdui         -> bootstrap policy
foundation:*         -> Partner bootstrap transport code
app:startup          -> anything; module must not exist
```

`runtime:application` may depend on the canonical session abstraction/owner required to invoke restoration; do not invert the dependency by making session depend on Partner startup.

---

## 16. Focused refactor classification

The implementation phase that follows this documentation freeze must first re-inspect current references and tests, then apply the smallest safe migration.

### KEEP

```text
feature:splash module
SplashContract / SplashStore / SplashScreen
runtime:application module
BootstrapRepository concept
foundation:session / SessionStore
Partner data:bootstrap module
canonical data:network stack
foundation:navigation / NavigationStore
NavigationProcessStateBridge responsibility
feature:dynamic
runtime:sdui
```

### REPLACE / MERGE

```text
ApplicationRuntime + startup task state machine
    -> SplashStore + ResolveStartupUseCase

ResolveBootstrapUseCase
    -> ResolveStartupUseCase

opaque nextPayload JSON round-trip
    -> typed StartupDestination mapping

throw-away config/features decoding
    -> PartnerConfigStore
```

### DELETE AFTER PROVEN ZERO REFERENCES

```text
StartupCoordinator
StartupTask
SessionRestoreStartupTask
BootstrapStartupTask
ApplicationRuntime/ApplicationRuntimeState
StartupPayload/StartupPayloadDecoder
DynamicStartupPayloadDecoder
StartupNotice
obsolete tests/DI bindings/imports for those abstractions
```

No deletion is performed until replacement tests and consumers are migrated.

---

## 17. Verification requirements for the refactor

Focused behavior tests must cover at least:

### Splash/MVI

- lifecycle Foreground starts one attempt;
- duplicate Foreground/start while loading does not launch a second request;
- retry launches a new attempt only after terminal retryable state;
- RequiredUpdate mapping;
- Maintenance mapping;
- Failure mapping;
- Ready emits one navigation effect;
- cancellation propagates.

### Application startup

- session restore occurs before bootstrap;
- corrupt session cleanup remains owned by SessionStore;
- required update precedes maintenance;
- maintenance blocks normal destination;
- valid ready response returns typed destination;
- optional update remains non-blocking configuration;
- config/features are retained in PartnerConfigStore;
- no config from one failed attempt incorrectly replaces last valid config.

### Data bootstrap

- exact `/api/v1/partner/config/bootstrap` route;
- `GET` + `OPTIONAL_SESSION`;
- canonical `status/code/message/data/traceId` envelope;
- no `success` dependency;
- required-field validation;
- typed `nextScreen` mapping;
- no JsonElement/String re-decode path.

### Network/session

- OPTIONAL_SESSION without token executes anonymously;
- valid token executes authenticated;
- 401 + successful refresh retries with refreshed token;
- 401 + definitive session invalidation retries OPTIONAL_SESSION once anonymously;
- SESSION never downgrades to anonymous;
- no infinite recovery loop.

### Navigation restoration

- pending state does not mutate NavigationStore before fresh bootstrap;
- matching restored root may restore stack;
- mismatched root is discarded/reset;
- malformed/unsafe state fails closed.

Architecture gates must verify:

- Splash has no data-network/bootstrap transport dependency;
- application startup has no Ktor/Compose/Navigation3/feature dependency;
- only data:bootstrap owns Partner bootstrap transport DTO/route;
- no superseded startup framework remains after migration;
- one canonical startup presentation state owner exists.

---

## 18. Local backend integration

The configured API base URL is platform/environment owned. The bootstrap adapter uses only the relative route:

```text
/api/v1/partner/config/bootstrap
```

Desktop local example:

```text
base URL: http://127.0.0.1:3000
final call: http://127.0.0.1:3000/api/v1/partner/config/bootstrap
```

Android emulator local example:

```text
base URL: http://10.0.2.2:3000
final call: http://10.0.2.2:3000/api/v1/partner/config/bootstrap
```

Development-only local HTTP allowances remain platform/configuration concerns. Do not relax staging/production trusted-host/HTTPS policy to support local testing.

---

## 19. Change guide

| Requirement | Canonical owner |
| --- | --- |
| Bootstrap endpoint/method/wire DTO | `data:bootstrap` |
| Bootstrap application result/policy | `runtime:application` |
| Splash state/UI/retry intent | `feature:splash` |
| Session restore/repair | `foundation:session` |
| Authorization/401 recovery | `data:network` + `foundation:session` |
| Global platform/app/build headers | canonical network/configuration provider |
| Reusable Partner feature config | `PartnerConfigStore` in `runtime:application` |
| API base URL/environment | `foundation:configuration` + platform host |
| Saved back-stack mechanics | `foundation:navigation` |
| Saved-state-to-fresh-root coordination | `NavigationProcessStateBridge` in `app:composition` |
| Dynamic screen loading | `feature:dynamic` |
| SDUI decode/validation/rendering | `runtime:sdui` |
| Backend startup destination values | backend Partner bootstrap configuration |

Do not create a new architectural layer merely because one requirement changes.

---

## 20. Prohibited regressions

Do not reintroduce:

- generic `StartupTask` pipelines without demonstrated multi-task need;
- a second observable startup runtime state beside Splash MVI state;
- `BootstrapStore` / `BootstrapDestinationStore`;
- `app:startup`;
- bootstrap-specific HttpClient/network executor;
- Partner bootstrap DTOs in `data:network`;
- navigation mechanics in Splash/application startup;
- JSON String/`JsonElement` round-trip for `nextScreen`;
- stale bootstrap persisted as authority for maintenance/update/auth/destination;
- Login/OTP/Dashboard-specific frontend networking/features solely because those screens exist;
- SDUI involvement in startup blockers;
- anonymous downgrade for required `SESSION` requests;
- old `/api/v1/partner/bootstrap` route;
- old `success: Boolean` bootstrap envelope assumption.

---

## 21. Freeze gate

Documentation is frozen before implementation.

The focused bootstrap refactor may start only from this ownership model:

```text
SplashStore
    -> ResolveStartupUseCase
        -> SessionStore.restore()
        -> BootstrapRepository
            -> RemoteBootstrapRepository
                -> canonical Network stack
        -> PartnerConfigStore
        -> StartupResult
    -> SplashState / SplashEffect
    -> app:composition
    -> NavigationProcessStateBridge
    -> foundation:navigation
    -> feature:dynamic
    -> runtime:sdui
```

The refactor is complete only when:

```text
README
== Master Constitution
== Dynamic Runtime Architecture
== module dependency graph
== DI graph
== implementation ownership
== tests
== current backend bootstrap contract
```

A green build with old/new startup frameworks in parallel is a failure, not a freeze.