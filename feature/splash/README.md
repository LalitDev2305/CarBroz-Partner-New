# CarBroz Partner — Static Splash, Bootstrap & Dynamic Handoff

Status: **FROZEN IMPLEMENTATION CONTRACT**

This document defines the exact production architecture for the exceptional static startup path and is the frontend source of truth for Partner bootstrap integration. It is subordinate to `docs/architecture/MASTER-ARCHITECTURE-CONSTITUTION.md`; when they conflict, the Master Constitution wins.

The backend Configuration bounded context has its own freeze documentation. This frontend document describes only the client ownership, transport adaptation, startup policy, dynamic handoff, host configuration, local testing, and change rules required to consume the frozen Partner bootstrap contract safely.

---

## 1. Quick mental model

CarBroz has exactly two frontend execution paths:

1. **Static startup/bootstrap** — Splash restores the canonical local session, calls the server-authoritative Partner bootstrap endpoint, evaluates startup policy, and resolves the first trusted dynamic destination.
2. **Generic dynamic SDUI runtime** — every normal screen and backend-driven interaction after startup uses the existing generic dynamic runtime.

```text
STATIC STARTUP EXCEPTION
Splash
  -> ApplicationRuntime
  -> StartupCoordinator
  -> SessionRestoreStartupTask
  -> BootstrapStartupTask
  -> ResolveBootstrapUseCase
  -> BootstrapRepository
  -> RemoteBootstrapRepository
  -> NetworkDataSource
  -> NetworkExecutor
  -> KtorNetworkTransport
  -> GET /api/v1/partner/bootstrap
  -> BootstrapSnapshot
  -> trusted StartupPayload
  -> Ready / Blocked / Failed

NORMAL APPLICATION FLOW
DynamicScreenInstruction
  -> DynamicFeature
  -> SDUI runtime
  -> binding/action runtime
  -> NetworkActionExecutor
  -> same NetworkDataSource / NetworkExecutor / KtorNetworkTransport
```

There are no Login/OTP/Dashboard/Profile-specific network stacks, repositories, stores or feature modules merely because those screens exist.

---

## 2. Non-negotiable ownership

- `feature:splash` owns static presentation only: intents, state, effects, store and Splash UI.
- `runtime:application` is the sole startup-state owner and owns startup orchestration plus transport-independent bootstrap application policy.
- `foundation:session` is the sole local session/authentication owner, including persisted-session restoration and corrupt-session cleanup.
- `foundation:configuration` owns immutable process configuration: environment, API base URL, client platform and build information.
- `data:network` owns the one canonical REST execution stack, global metadata headers, auth integration, retry/timeout/connectivity/cache/security/observability policy and typed response decoding mechanism.
- `data:bootstrap` owns only the Partner bootstrap HTTP adapter: endpoint, DTOs and `RemoteBootstrapRepository`.
- `feature:dynamic` owns conversion of the opaque trusted bootstrap payload into a validated `DynamicScreenInstruction`.
- `foundation:navigation` owns stack mechanics.
- `app:composition` wires owners together and must not become another startup/network/session owner.
- `app:startup` must not exist.

No second bootstrap store, second REST client, second session snapshot, second navigation stack or second generic dynamic-screen flow is permitted.

---

## 3. Exact frontend structure for this slice

```text
feature/splash/
├── README.md
└── src/commonMain/kotlin/com/carbroz/feature/splash/
    ├── SplashContract.kt
    ├── SplashStore.kt
    └── SplashScreen.kt

runtime/application/
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
└── src/commonMain/kotlin/com/carbroz/data/bootstrap/
    ├── BootstrapApiContract.kt
    ├── PartnerBootstrapDto.kt
    └── RemoteBootstrapRepository.kt

data/network/src/commonMain/kotlin/com/carbroz/data/network/
├── ConfigurationNetworkHeaderProvider.kt
├── NetworkDataSource.kt
├── NetworkExecutor.kt
├── KtorNetworkTransport.kt
└── ...canonical shared network infrastructure

feature/dynamic/src/commonMain/kotlin/com/carbroz/feature/dynamic/
├── DynamicScreenInstructionCodec.kt
├── DynamicStartupPayloadDecoder.kt
└── ...generic dynamic runtime

foundation/session/src/commonMain/kotlin/com/carbroz/foundation/session/
└── canonical session infrastructure

foundation/configuration/src/commonMain/kotlin/com/carbroz/foundation/configuration/
├── AppConfiguration.kt
├── ConfigurationValidator.kt
└── ...immutable process configuration

app/composition/
└── DI and cross-owner wiring only

androidApp/
└── Android host configuration and lifecycle bridge

desktopApp/
└── Desktop host configuration and lifecycle bridge
```

`data:bootstrap` is intentional. It prevents Partner-specific transport code from contaminating reusable `data:network`.

---

## 4. Backend Partner bootstrap contract consumed by frontend

The canonical endpoint is:

```http
GET /api/v1/partner/bootstrap
```

The backend Configuration domain owns the actual startup policy and persisted `partner.bootstrap` document. The frontend does not call a separate `/config` API.

### Request

```http
GET /api/v1/partner/bootstrap
X-CarBroz-Platform: ANDROID
X-CarBroz-App-Version: 1.0.0
X-CarBroz-Build-Number: 1
Authorization: Bearer <token>   # optional
```

The frontend does not manually add those headers inside `RemoteBootstrapRepository`.

- platform/version/build come from `ConfigurationNetworkHeaderProvider`;
- Authorization comes from the canonical session/network integration;
- bootstrap declares only `NetworkAuthentication.OPTIONAL_SESSION`.

### Guest response shape

```json
{
  "success": true,
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
        "screenId": "partner_login",
        "templateId": "partner_login_template",
        "templateType": "form_template",
        "endpoint": "/api/v1/partner/sdui/registry/partner_login",
        "method": "GET",
        "authentication": "NONE"
      }
    }
  },
  "traceId": "req-x"
}
```

Authenticated startup uses the configured authenticated destination instead of the guest destination.

---

## 5. Complete startup request-to-response flow

```text
Android/Desktop host starts
        ↓
initializeCarBroz...Application(...)
        ↓
AppConfiguration created and validated
        ↓
DI graph created
        ↓
Splash rendered
        ↓
host lifecycle -> Foreground
        ↓
SplashStore -> ApplicationRuntime.start()
        ↓
StartupCoordinator
        ↓
SessionRestoreStartupTask
        ↓
SessionStore.restore()
        ├── valid persisted session -> Authenticated
        ├── no session -> SignedOut
        └── corrupt unsupported session -> canonical cleanup -> SignedOut
        ↓
BootstrapStartupTask
        ↓
ResolveBootstrapUseCase
        ↓
BootstrapRepository.load()
        ↓
RemoteBootstrapRepository
        ↓
NetworkRequest(
    GET,
    /api/v1/partner/bootstrap,
    OPTIONAL_SESSION
)
        ↓
NetworkDataSource.executeTyped(...)
        ↓
NetworkExecutor
        ├── resolve trusted API base URL
        ├── add X-CarBroz-Platform
        ├── add X-CarBroz-App-Version
        ├── add X-CarBroz-Build-Number
        ├── add Authorization only when a canonical session exists
        ├── enforce auth-recovery policy
        ├── retry/timeout/connectivity/cache/observability policy
        └── create final absolute URL
        ↓
KtorNetworkTransport
        ↓
Backend /api/v1/partner/bootstrap
        ↓
PartnerBootstrapEnvelopeDto
        ↓
RemoteBootstrapRepository validates/maps
        ↓
BootstrapSnapshot(
    authenticated,
    maintenance,
    update,
    nextPayload
)
        ↓
ResolveBootstrapUseCase policy
        ├── backend/local auth mismatch -> fail closed
        ├── required update -> Blocked
        ├── maintenance -> Blocked
        ├── invalid nextPayload -> Failed
        ├── optional update -> Ready + notice
        └── valid payload -> Ready
        ↓
ApplicationRuntime.Ready
        ↓
SplashEffect.Navigate(startupPayload)
        ↓
NavigationProcessStateBridge.applyAfterBootstrap(...)
        ↓
DynamicDestination
        ↓
DynamicFeature
        ↓
nextScreen.endpoint
        ↓
SDUI decode/validate/normalize/render
```

A successful bootstrap call is finished before normal SDUI screen loading starts.

---

## 6. Static Splash MVI/UDF

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

Splash knows nothing about Ktor, endpoint strings, JSON, DTOs, database/cache mechanics or persisted-session format.

### Intents

```text
LifecycleChanged(state)
RetryClicked
UpdateClicked
```

### States

```text
Loading
RequiredUpdate(title, message, updateUri)
Maintenance(title, message, retryEnabled)
Error(message, retryEnabled)
Ready
```

### Effects

```text
Navigate(startupPayload)
OpenUpdateUri(uri)
```

Startup begins when common lifecycle enters Foreground.

---

## 7. Startup orchestration

`ApplicationRuntime` is the only observable startup-state owner:

```text
Idle
Starting(attempt)
Ready(attempt, payload, notices)
Blocked(attempt, blocker)
Failed(taskId, failure, attempt)
```

`StartupCoordinator` executes exactly:

```text
1. SessionRestoreStartupTask
2. BootstrapStartupTask
```

Task results:

```text
Continue
Resolved(StartupResolution.Ready | StartupResolution.Blocked)
Failure(StartupFailure)
```

All tasks returning `Continue` must fail closed with `startup_missing_resolution`. Cancellation propagates.

There is no third `ConfigStartupTask`.

---

## 8. Session restoration and bootstrap authentication

`foundation:session` owns session restoration and session truth.

`SessionStore.restore()` must:

- restore a valid authenticated snapshot;
- return signed-out success when no persisted session exists;
- clear malformed/unsupported local snapshots through canonical persistence;
- report storage/cleanup failures;
- propagate cancellation.

After restore, bootstrap uses `OPTIONAL_SESSION`:

```text
SignedOut
  -> no Authorization header
  -> backend should return startup.authenticated = false

Authenticated
  -> Authorization: Bearer <access token>
  -> backend should return startup.authenticated = true
```

If local and backend authentication truth disagree, `ResolveBootstrapUseCase` fails closed.

An invalid supplied bearer must not silently downgrade the same request into guest bootstrap.

---

## 9. Bootstrap application boundary

`runtime:application/bootstrap` contains only transport-independent application semantics.

`BootstrapRepository` is an inward contract. `data:bootstrap` implements it.

The normalized `BootstrapSnapshot` contains only:

```text
authenticated
maintenance(enabled, title, message)
update(required, optional, minimumVersion, latestVersion, updateUri)
nextPayload
```

The following do not cross into the runtime application boundary:

```text
success
message
traceId
Partner transport DTOs
JsonElement
raw HTTP response types
```

`nextPayload` is opaque serialized startup data until `StartupPayloadDecoder` validates it.

The backend `features` object is decoded for wire compatibility but is not currently copied into `BootstrapSnapshot`. That is intentional until a real application-wide client consumer exists.

---

## 10. ResolveBootstrapUseCase policy order

The order is part of the frozen contract:

1. Load the remote bootstrap snapshot.
2. Map transport/data failures into stable startup failures.
3. Compare backend authentication truth with canonical local session state.
4. Required update has first precedence.
5. Required update requires a non-blank update URI; otherwise fail closed.
6. Maintenance is a valid blocked state, not a transport failure.
7. Decode and validate `nextPayload`.
8. Optional update becomes a non-blocking notice.
9. Return `Ready` with a trusted startup payload.

Do not move this policy into Splash, transport code or DTO mapping.

---

## 11. Partner bootstrap data adapter

`data:bootstrap` contains exactly one remote implementation: `RemoteBootstrapRepository`.

It owns:

- `GET /api/v1/partner/bootstrap` through `BootstrapApiContract`;
- `NetworkAuthentication.OPTIONAL_SESSION`;
- Partner serializable DTOs;
- success-envelope validation;
- required-field validation;
- DTO -> `BootstrapSnapshot` mapping;
- network/decode failure -> `BootstrapRepositoryFailure` mapping.

It does not:

- create another HttpClient;
- add metadata headers manually;
- add Authorization manually;
- own retries;
- own session state;
- decide maintenance/update precedence;
- navigate;
- render UI;
- cache bootstrap in a local business-data cache.

Bootstrap remains network-authoritative.

---

## 12. Canonical network stack and headers

All REST traffic uses:

```text
NetworkDataSource
      ↓
NetworkExecutor
      ↓
KtorNetworkTransport
      ↓
process-owned REST HttpClient
```

`ConfigurationNetworkHeaderProvider` supplies:

```text
X-CarBroz-Platform
X-CarBroz-App-Version
X-CarBroz-Build-Number
```

Values come from immutable `AppConfiguration`.

Example Android development values currently resolve conceptually to:

```text
X-CarBroz-Platform: ANDROID
X-CarBroz-App-Version: 1.0.0-dev
X-CarBroz-Build-Number: 1
```

The backend version comparison supports the development suffix; frontend must still send the real build identity rather than hardcoding `1.0.0` in request code.

---

## 13. Dynamic handoff

After `ApplicationRuntime.Ready`, bootstrap is complete.

```text
ApplicationRuntime.Ready(DynamicScreenInstruction)
      ↓
SplashEffect.Navigate
      ↓
NavigationProcessStateBridge.applyAfterBootstrap
      ↓
DynamicDestination
      ↓
DynamicFeature
      ↓
SDUI screen fetch
      ↓
Decode -> Validate -> Compatibility -> Normalize -> Runtime IR
      ↓
Dynamic MVI Store / renderer
      ↓
Interaction
      ├── generic API -> NetworkActionExecutor -> canonical network stack
      ├── navigation
      ├── capability
      └── registered domain operation only when genuine client semantics exist
```

Login, OTP, Dashboard, Profile, Booking and Earnings remain backend-driven screens.

---

## 14. Navigation restoration

Saved dynamic navigation cannot bypass fresh startup:

```text
capture saved state
  -> keep pending
  -> show Splash
  -> fresh session restore
  -> fresh bootstrap
  -> fresh server-authoritative root
  -> compare saved root
      ├── compatible -> restore validated stack
      └── incompatible/malformed -> discard and ResetTo fresh root
```

---

## 15. Required dependency direction

Allowed:

```text
feature:splash       -> runtime:application + presentation/lifecycle foundations
runtime:application  -> foundation owners only
feature:dynamic      -> runtime:application to implement StartupPayloadDecoder
data:bootstrap       -> runtime:application + data:network
app:composition      -> runtime/feature/data/foundation for wiring
```

Forbidden:

```text
runtime:application -> feature:dynamic
data:network        -> runtime:application
data:network        -> Partner bootstrap DTOs
feature:splash      -> data:network / data:bootstrap / session persistence / bootstrap DTOs
foundation:*        -> Partner bootstrap transport code
app:startup         -> anything; the module must not exist
```

---

## 16. Current implementation status

The architecture implementation for this slice is present:

```text
✅ static Splash presentation path
✅ ApplicationRuntime
✅ StartupCoordinator
✅ SessionRestoreStartupTask
✅ BootstrapStartupTask
✅ ResolveBootstrapUseCase
✅ BootstrapRepository application contract
✅ data:bootstrap module
✅ GET /api/v1/partner/bootstrap endpoint contract
✅ Partner bootstrap DTOs
✅ RemoteBootstrapRepository
✅ canonical NetworkDataSource / NetworkExecutor / Ktor transport
✅ global platform/version/build headers
✅ OPTIONAL_SESSION integration
✅ fail-closed invalid bearer recovery
✅ DynamicStartupPayloadDecoder
✅ dynamic SDUI handoff wiring
✅ Android host lifecycle initialization
✅ Desktop host lifecycle initialization
```

Focused startup/network tests and `verifyStartupArchitecture` were green at the last verified development baseline.

This means no new bootstrap/config architecture is required before real API testing.

---

## 17. Platform host configuration

### Desktop host

Desktop reads:

```text
system property: carbroz.environment
or environment variable: CARBROZ_ENVIRONMENT

system property: carbroz.apiBaseUrl
or environment variable: CARBROZ_API_BASE_URL
```

If not provided, development currently falls back to:

```text
https://development.invalid
```

Therefore real local testing must override the base URL.

### Android host

Android reads generated BuildConfig values:

```text
BuildConfig.CARBROZ_ENVIRONMENT
BuildConfig.CARBROZ_API_BASE_URL
BuildConfig.VERSION_NAME
BuildConfig.VERSION_CODE
BuildConfig.APPLICATION_ID
```

Development base URL comes from Gradle property:

```text
carbroz.api.development
```

and currently defaults to:

```text
https://development.invalid
```

So a real local backend URL must be supplied for development builds.

---

## 18. Local backend testing — Desktop

Backend example:

```text
http://127.0.0.1:3000
```

Desktop development configuration already allows local HTTP for `localhost` / `127.0.0.1`.

### PowerShell

```powershell
$env:CARBROZ_ENVIRONMENT="development"
$env:CARBROZ_API_BASE_URL="http://127.0.0.1:3000"
.\gradlew.bat :desktopApp:run
```

Expected request:

```http
GET http://127.0.0.1:3000/api/v1/partner/bootstrap
```

Expected startup behavior for a fresh signed-out desktop app:

```text
Splash
 -> bootstrap guest response
 -> partner_login DynamicScreenInstruction
 -> navigation to dynamic destination
 -> GET /api/v1/partner/sdui/registry/partner_login
```

### Desktop readiness

```text
Bootstrap implementation: READY
Local HTTP configuration: READY
Base URL override mechanism: READY
Can test against local backend: YES
```

No frontend source-code change is required merely to point Desktop at `127.0.0.1:3000`.

---

## 19. Local backend testing — Android Emulator

Android Emulator cannot use host-machine `127.0.0.1` for the backend. The normal emulator host-loopback alias is:

```text
10.0.2.2
```

Target base URL:

```text
http://10.0.2.2:3000
```

However two development-host gaps currently block direct emulator testing:

### Gap A — configuration validation

`ConfigurationValidator` currently permits development HTTP only when the base URL begins with:

```text
http://localhost
http://127.0.0.1
```

It does not currently permit:

```text
http://10.0.2.2
```

Therefore emulator development configuration would fail before the request is executed.

Required change:

```text
foundation/configuration/ConfigurationValidator.kt
```

Allow `http://10.0.2.2` only for `AppEnvironment.Development`.

Do not relax staging/production HTTPS requirements.

### Gap B — Android cleartext HTTP

The shared Android manifest currently has INTERNET permission but does not enable cleartext HTTP.

For local emulator development, add a **development-only** Android manifest override/network-security rule that permits local HTTP.

Preferred ownership:

```text
androidApp/src/development/AndroidManifest.xml
```

or an equivalent development-only network security config.

Do not enable cleartext globally for staging/production.

### Development base URL

After those two gaps are fixed, supply:

```text
carbroz.api.development=http://10.0.2.2:3000
```

For example through a local/uncommitted Gradle property or command-line `-P` value.

Expected call:

```http
GET http://10.0.2.2:3000/api/v1/partner/bootstrap
```

### Android Emulator readiness

```text
Bootstrap implementation: READY
Endpoint/DTO mapping: READY
Headers/auth integration: READY
Development base URL override: READY
10.0.2.2 configuration validation: PENDING
Development-only cleartext allowance: PENDING
Can directly test local HTTP today without those fixes: NO
```

Once those two small host-level changes are applied, no bootstrap architecture work remains before emulator testing.

---

## 20. Local backend testing — Physical Android device

For a real device on the same LAN, use the development machine's LAN IP, for example:

```text
http://192.168.x.x:3000
```

Additional requirements:

- backend must listen on an address reachable from the LAN, not only an inaccessible local binding;
- Windows firewall/network rules must allow the port;
- device and machine must be on the same reachable network;
- `ConfigurationValidator` must explicitly permit the chosen development-only LAN HTTP policy if local physical-device testing is required;
- Android development cleartext policy must permit it.

Do not broaden production URL validation just to support local device testing.

---

## 21. Change guide — what to edit when requirements change

| Requirement | Correct frontend owner / files |
| --- | --- |
| Change Partner bootstrap path | `data/bootstrap/BootstrapApiContract.kt` + tests + README; coordinate with backend contract |
| Change request HTTP method | `BootstrapApiContract`/repository request construction + tests |
| Add/rename/remove bootstrap response field used only for wire decoding | `PartnerBootstrapDto.kt` + `RemoteBootstrapRepository` tests |
| Add a new startup application semantic | `runtime/application/bootstrap/BootstrapContract.kt` + `ResolveBootstrapUseCase.kt` + mapping/tests |
| Change maintenance/update precedence | `ResolveBootstrapUseCase.kt` + runtime tests |
| Change local/backend auth consistency policy | `ResolveBootstrapUseCase.kt` + session/runtime tests |
| Change global platform/version/build headers | `ConfigurationNetworkHeaderProvider.kt` + network tests; do not change bootstrap repository manually |
| Change Authorization behavior | canonical session/network ownership; never bootstrap DTO/repository-specific auth code |
| Change API base URL source | platform host configuration + `foundation:configuration`; not `RemoteBootstrapRepository` |
| Add development emulator host support | `ConfigurationValidator.kt` + tests + Android development host policy |
| Change backend startup destination values only | normally backend `partner.bootstrap`; frontend should not hardcode Login/Dashboard |
| Change SDUI screen structure/content | backend SDUI contract/runtime compatibility; not bootstrap application policy |
| Change dynamic payload validation rules | `feature:dynamic` codec/decoder + tests |
| Change common API envelope (`success/data/traceId`) | coordinated backend/global API contract + bootstrap DTO mapping; treat as versioned contract change |
| Add client-wide feature-flag consumption | prove consumer first, then map into existing `foundation:configuration/featureflag` ownership |
| Add Partner KYC/jobs/earnings business data | owning domain/client operation boundary, not generic bootstrap |

---

## 22. Add / rename / remove bootstrap fields

### A. Add a response field

First determine whether the new field is:

```text
transport-only
or
startup application semantic
```

#### Transport-only field

Example: backend adds metadata that the client decodes for compatibility but does not need for startup policy.

Change:

```text
PartnerBootstrapDto.kt
+ DTO/repository tests
```

Do not expand `BootstrapSnapshot` without a real application consumer.

#### Startup semantic field

Example: backend adds a new startup blocker that the client must act on.

Change flow:

```text
backend contract
  -> PartnerBootstrapDto.kt
  -> BootstrapContract.kt
  -> RemoteBootstrapRepository mapping
  -> ResolveBootstrapUseCase policy
  -> StartupResolution/State if necessary
  -> Splash mapping only if presentation must change
  -> tests at every changed boundary
```

### B. Rename a field

A rename is a client/server contract change.

Update together:

```text
backend response contract
 -> PartnerBootstrapDto
 -> repository mapping
 -> tests
 -> compatibility/versioning decision
 -> documentation
```

Do not rename only the Kotlin application model while leaving transport expectations inconsistent.

### C. Delete a field

Before deleting:

1. Confirm the backend has removed/versioned it.
2. Confirm no frontend mapping/runtime consumer uses it.
3. Remove it from DTOs.
4. Remove mapping/application semantics only if no longer required.
5. Update tests.
6. Consider older backend/client compatibility where applicable.

---

## 23. Change endpoint, headers or auth

### Endpoint change

Current:

```text
/api/v1/partner/bootstrap
```

Frontend owner:

```text
data/bootstrap/BootstrapApiContract.kt
```

A URL change must be coordinated with backend route tests and frontend integration tests.

Do not move endpoint strings into Splash or `runtime:application`.

### New request header

Ask first whether the header is global or bootstrap-specific.

#### Global client metadata

```text
ConfigurationNetworkHeaderProvider
 -> NetworkExecutor
 -> every canonical REST request
```

#### Bootstrap-only transport header

Only if genuinely bootstrap-specific, add it at the transport adapter/request boundary and test that it cannot override provider-owned reserved headers.

### Auth policy change

Current:

```text
OPTIONAL_SESSION
```

Any change must remain within canonical network/session policy. Never manually concatenate `Bearer` inside `RemoteBootstrapRepository`.

---

## 24. Troubleshooting local bootstrap

### App remains on Splash / shows generic startup error

Check in this order:

```text
1. Is backend running?
2. Is platform base URL real or still *.invalid?
3. Does ConfigurationValidator accept the URL?
4. Does Android allow local cleartext if using HTTP?
5. Can the host reach the backend address?
6. Are X-CarBroz-Platform/App-Version/Build-Number present?
7. Is an Authorization header being sent unexpectedly?
8. Does backend return success=true and data?
9. Does backend startup.authenticated match local SessionProvider state?
10. Is nextScreen valid for DynamicStartupPayloadDecoder?
11. Does nextScreen.endpoint exist and return valid SDUI after navigation?
```

### Bootstrap succeeds but next screen fails

Bootstrap and SDUI loading are separate stages.

```text
/bootstrap success
  !=
nextScreen.endpoint success
```

If `partner_login` bootstrap succeeds but the screen cannot render, debug:

```text
/api/v1/partner/sdui/registry/partner_login
```

through the dynamic runtime path, not `RemoteBootstrapRepository`.

### Invalid bearer

An invalid bearer must not be retried anonymously as the same bootstrap request. The network/session path must fail closed after invalidation according to current recovery rules.

### 404

Confirm the final composed URL is exactly:

```text
<apiBaseUrl>/api/v1/partner/bootstrap
```

and `apiBaseUrl` does not end with `/`.

---

## 25. Mandatory verification

### Focused architecture tests

```powershell
.\gradlew.bat --no-daemon :foundation:session:allTests
.\gradlew.bat --no-daemon :data:network:allTests
.\gradlew.bat --no-daemon :data:bootstrap:allTests
.\gradlew.bat --no-daemon :runtime:application:allTests
.\gradlew.bat --no-daemon :feature:dynamic:allTests
.\gradlew.bat --no-daemon :feature:splash:allTests
.\gradlew.bat --no-daemon verifyStartupArchitecture
```

### Broader verification

```powershell
.\gradlew.bat --no-daemon :app:composition:allTests
.\gradlew.bat --no-daemon allTests
.\gradlew.bat --no-daemon assemble
```

Use the focused gates after startup/network changes and broader gates before merging/freezing.

### Manual backend sanity check

```powershell
$headers = @{
    "X-CarBroz-Platform" = "ANDROID"
    "X-CarBroz-App-Version" = "1.0.0"
    "X-CarBroz-Build-Number" = "1"
}

Invoke-RestMethod `
    -Uri "http://127.0.0.1:3000/api/v1/partner/bootstrap" `
    -Headers $headers `
    -Method GET |
    ConvertTo-Json -Depth 10
```

This verifies the backend contract independently of frontend host networking.

---

## 26. Current pending work before real platform testing

### Desktop

```text
Nothing architectural is pending.
```

Required operational steps only:

```text
1. Start backend on 127.0.0.1:3000.
2. Set CARBROZ_ENVIRONMENT=development.
3. Set CARBROZ_API_BASE_URL=http://127.0.0.1:3000.
4. Run :desktopApp:run.
5. Observe Splash -> bootstrap -> dynamic handoff.
```

### Android Emulator

Two source-level local-development changes remain:

```text
1. Allow development-only http://10.0.2.2 in ConfigurationValidator.
2. Allow development-only Android cleartext HTTP.
```

Then:

```text
3. Set carbroz.api.development=http://10.0.2.2:3000.
4. Install/run developmentDebug.
5. Observe Splash -> bootstrap -> dynamic handoff.
```

These are host-development integration changes only. They do not change the frozen startup architecture.

---

## 27. Freeze gate

This slice remains frozen only when:

```text
README == implementation == Gradle graph == DI graph == tests == backend client contract
```

A green build with old/new ownership in parallel is a failure, not a freeze.

Before any future bootstrap change, determine the correct owner first, update the contract and tests together, and avoid introducing parallel configuration/startup/network paths.