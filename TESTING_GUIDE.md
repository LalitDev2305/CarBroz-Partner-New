# CarBroz Partner — Executable Testing Guide & Verification Catalog

This document defines the canonical execution commands, quality gates, and verification matrices for all modules in CarBroz Partner.

---

## 1. Testing Philosophy & Quality Gates

A task or feature in CarBroz Partner is **NOT DONE** until it satisfies **3 Quality Gates**:
1. **BUILD SUCCESS**: Code compiles cleanly across Android, Desktop, and iOS KMP targets.
2. **TEST SUCCESS**: 100% of unit tests pass in `commonTest` without failures or swallowed errors.
3. **RUNTIME SMOKE SUCCESS**: Desktop JVM host runs without crash (`./gradlew :desktopApp:run`).

---

## 2. Platform Verification Constraints

- **Windows Host Capabilities**: Development on Windows supports automated compilation and testing for **Android**, **Desktop (JVM)**, and KMP **iOS compilation** (`compileKotlinIosArm64`, `compileKotlinIosSimulatorArm64`).
- **iOS Runtime Requirement**: iOS native framework linking, Xcode execution, and iOS simulator runtime testing require a **macOS host environment with Xcode**. iOS runtime functionality is explicitly marked **UNVERIFIED ON WINDOWS**.

---

## 3. Module Verification Catalog

--------------------------------------------------
### MODULE: `:core:observability`
--------------------------------------------------
- **Purpose**: Structured logging, category classification, log level management, and automatic PII/financial/credential redaction.
- **Current Implementation Status**: **IMPLEMENTED** (Committed `e48d7d4`)
- **Test Types**: Common Multiplatform Unit Tests (`commonTest`)
- **Exact Commands**:
  ```bash
  ./gradlew :core:observability:allTests --no-daemon
  ```
- **Expected Result**: `BUILD SUCCESSFUL` with 100% unit tests passing.
- **Contracts Verified**:
  - `StructuredLogger` log event formatting and sink distribution.
  - Category routing (`AUTH`, `NETWORK`, `UI`, `STORAGE`, `SYSTEM`).
  - Redaction engine masking sensitive fields (`PII`, `FINANCIAL`, `CREDENTIALS`).
- **Edge Cases Covered**: Null attributes, unredacted public fields, nested map redaction.
- **Platform Verification**: Android (PASS), Desktop JVM (PASS), iOS Compilation (PASS), iOS Runtime (NOT VERIFIED ON WINDOWS).
- **Known Limitations**: None for logging logic.
- **Failure Interpretation**: Failure indicates a breach of log formatting or security redaction rules.

--------------------------------------------------
### MODULE: `:core:mvi`
--------------------------------------------------
- **Purpose**: Generic multiplatform State owner contract and default implementation (`Store<State, Intent, Effect>`).
- **Current Implementation Status**: **IMPLEMENTED** (Committed `2a84bcd`)
- **Test Types**: Common Multiplatform Unit Tests (`commonTest`)
- **Exact Commands**:
  ```bash
  ./gradlew :core:mvi:allTests --no-daemon
  ```
- **Expected Result**: `BUILD SUCCESSFUL` with 100% unit tests passing.
- **Contracts Verified**:
  - `DefaultStore` intent processing and atomic `StateFlow` emission.
  - Sequential intent execution and effect flow distribution.
  - Exception classification: Domain failures vs. severe programmer defects.
  - Concurrency safety and cancellation scope handling.
- **Edge Cases Covered**: Intent queue overflow, unhandled store exception propagation, rapid intent dispatching.
- **Platform Verification**: Android (PASS), Desktop JVM (PASS), iOS Compilation (PASS), iOS Runtime (NOT VERIFIED ON WINDOWS).
- **Known Limitations**: None.
- **Failure Interpretation**: Failure indicates non-deterministic state mutations or unhandled coroutine exceptions.

--------------------------------------------------
### MODULE: `:core:ui`
--------------------------------------------------
- **Purpose**: Platform-neutral adaptive resolution engine for dynamic Server-Driven UI specifications.
- **Current Implementation Status**: **IMPLEMENTED** (Committed `a1708e6`)
- **Test Types**: Common Multiplatform Unit Tests (`commonTest`)
- **Exact Commands**:
  ```bash
  ./gradlew :core:ui:allTests --no-daemon
  ```
- **Expected Result**: `BUILD SUCCESSFUL` with 17 test methods passing.
- **Contracts Verified**:
  - `DimensionSpec` resolution (`Fixed`, `Adaptive`, `Fraction`, `Fill`, `Wrap`, `Token`).
  - `ResolvedDimension` semantic preservation (`Exact(Dp)`, `Fill`, `Wrap`).
  - `AdaptiveScaleCalculator` damped exponential scaling ($V = B \cdot \text{clamp}((A/R)^{\text{damp}}, \text{minScale}, \text{maxScale})$).
  - Axis-aware fraction resolution against immediate parent container bounds.
  - Responsive layout constraints (`minDp`, `maxDp`) preserving design intent for parent overflow layers.
  - Non-double-scaling of typography `sp` under accessibility system `fontScale`.
  - Minimum touch target hit area calculation ($48\text{dp} \times 48\text{dp}$) via `InteractionTargetPolicy`.
- **Edge Cases Covered**: `NaN`, `Infinity`, negative dimensions, invalid policy factors, contradictory min/max constraints (`minDp > maxDp`), unknown token fallbacks (`Unsupported`).
- **Platform Verification**: Android (PASS), Desktop JVM (PASS), iOS Compilation (PASS), iOS Runtime (NOT VERIFIED ON WINDOWS).
- **Known Limitations**: None.
- **Failure Interpretation**: Failure indicates mathematical inaccuracy or violation of responsive adaptive contracts.

--------------------------------------------------
### MODULE: `:core:navigation`
--------------------------------------------------
- **Purpose**: Stateful multiplatform router foundation, stack entries, atomic commands, and mutex-serialized navigation state management.
- **Current Implementation Status**: **IMPLEMENTED** (Phase 010)
- **Test Types**: Common Multiplatform Unit Tests (`commonTest`)
- **Exact Commands**:
  ```bash
  ./gradlew :core:navigation:allTests --no-daemon
  ```
- **Expected Result**: `BUILD SUCCESSFUL` with 30 test methods passing.


- **Contracts Verified**:
  - `NavDestination` parameter immutability and defensive map copying.
  - `NavStack` non-empty stack invariant enforcement.
  - `DefaultRouter` atomic stack mutations (`Push`, `Replace`, `Pop`, `PopTo`, `ResetTo`).
  - `PopToTarget` last-match route resolution and entry ID targeting.
  - `NavResult` typed rejection handling (`CannotPopRoot`, `TargetNotFound`, `InvalidRoute`).
  - `Mutex` command serialization preventing race conditions during concurrent execution.
  - Telemetry emitting `LogCategory.NAVIGATION` structured log events.
- **Edge Cases Covered**: Pop on root, popTo inclusive root protection, blank/whitespace route rejection, duplicate route instances, concurrent pushes, parameter map mutation after construction.
- **Platform Verification**: Android (PASS), Desktop JVM (PASS), iOS Compilation (PASS), iOS Runtime (NOT VERIFIED ON WINDOWS).
- **Known Limitations**: None.
- **Failure Interpretation**: Failure indicates non-deterministic navigation state mutations or broken stack invariants.

--------------------------------------------------
### MODULE: `:domain:actions`
--------------------------------------------------
- **Purpose**: Pure declarative domain model foundation for dynamic Server-Driven UI actions, parameters, binding expressions, and metadata.
- **Current Implementation Status**: **IMPLEMENTED** (Phase 011)
- **Test Types**: Common Multiplatform Unit Tests (`commonTest`)
- **Exact Commands**:
  ```bash
  ./gradlew :domain:actions:allTests --no-daemon
  ```
- **Expected Result**: `BUILD SUCCESSFUL` with 18 test methods passing.

- **Contracts Verified**:
  - `ActionId` non-blank invariant validation and value equality.
  - `ActionType` open backend-extensible raw value wrapping and constant matching.
  - `BindingExpression` syntax verification (`${...}`).
  - `ActionValue` sealed value tree (`Text`, `Integer`, `Decimal`, `Flag`, `Binding`, `Object`, `List`, `Null`).
  - `ActionValue.Decimal` exact text precision preservation for financial/numeric values.
  - `ActionParameters` defensive map copying and `toString()` parameter log value redaction.
  - `ActionMetadata` positive version validation.
  - `ActionSpec` pure declarative data composition and adversarial backend action key representation.
- **Edge Cases Covered**: Blank action IDs/types, malformed binding syntax, negative decimals, invalid metadata versions, mutable map/list source mutation post-construction, log payload leaking.
- **Platform Verification**: Android (PASS), Desktop JVM (PASS), iOS Compilation (PASS), iOS Runtime (NOT VERIFIED ON WINDOWS).
- **Known Limitations**: None.
- **Failure Interpretation**: Failure indicates non-deterministic action value tree representation or broken defensive collection copying.

--------------------------------------------------
### MODULE: `:domain:capabilities`
--------------------------------------------------
- **Purpose**: Pure declarative platform-neutral domain model foundation for platform capabilities, permissions, location, camera, media/document pickers, connectivity, clipboard, external launcher, and app settings.
- **Current Implementation Status**: **IMPLEMENTED** (Phase 012)
- **Test Types**: Common Multiplatform Unit Tests (`commonTest`)
- **Exact Commands**:
  ```bash
  ./gradlew :domain:capabilities:allTests --no-daemon
  ```
- **Expected Result**: `BUILD SUCCESSFUL` with 23 test methods passing.
- **Contracts Verified**:
  - `CapabilityResult` sealed variants (`Success`, `Cancelled`, `Unavailable`, `Unsupported`, `Failure`).
  - `CapabilityFailure` code evaluation and non-blank message invariant.
  - `GeoCoordinate` boundary range validation (-90..90, -180..180) and non-finite number (`NaN`, `+Infinity`, `-Infinity`) rejection.
  - `LocationSnapshot` accuracy finiteness and non-negative check, timestamp epoch milliseconds positivity.
  - `CapturedImage`, `SelectedMedia`, and `SelectedDocument` non-blank URI/fileName/MIME invariants and non-negative sizeBytes validation.
  - `PermissionGateway`, `LocationGateway`, `CameraGateway`, `MediaPickerGateway`, `DocumentPickerGateway`, `ConnectivityGateway`, `ClipboardGateway`, `ExternalUriLauncher`, and `SettingsLauncher` domain contracts.
- **Edge Cases Covered**: `NaN`/`Infinity` coordinates/accuracies, negative timestamps, negative file sizes, blank URIs/MIMEs, user cancellation, hardware unavailability, system permission recovery states.
- **Platform Verification**: Android (PASS), Desktop JVM (PASS), iOS Compilation (PASS), iOS Runtime (NOT VERIFIED ON WINDOWS).
- **Known Limitations**: None.
- **Failure Interpretation**: Failure indicates broken numeric invariant guards or non-deterministic capability outcome hierarchy mapping.

--------------------------------------------------
### MODULE: `:domain:storage`
--------------------------------------------------
- **Purpose**: Pure platform-neutral domain model foundation for key-value preferences, strongly-typed preference keys, sensitive string persistence contracts, and unified storage outcome results.

- **Current Implementation Status**: **IMPLEMENTED** (Phase 013)
- **Test Types**: Common Multiplatform Unit Tests (`commonTest`)
- **Exact Commands**:
  ```bash
  ./gradlew :domain:storage:allTests --no-daemon
  ```
- **Expected Result**: `BUILD SUCCESSFUL` with 15 test methods passing across `StorageResultTest` and `PreferenceKeyTest`.
- **Contracts Verified**:
  - `StorageResult` sealed variants (`Success`, `NotFound`, `Failure`).
  - `StorageFailure` code classification (`READ_FAILED`, `WRITE_FAILED`, `DELETE_FAILED`, `SECURITY_HARDWARE_UNAVAILABLE`, `UNKNOWN`) and non-blank error message guard.
  - `PreferenceKey` sealed variants (`StringKey`, `BooleanKey`, `IntKey`, `LongKey`) non-blank key name validation, default value preservation, and key type identity distinction.
  - `PreferenceGateway` and `SecureStorageGateway` suspend CRUD domain contracts.
- **Edge Cases Covered**: Blank/whitespace key name rejection, blank error message rejection, default value fallback preservation, unicode key names, key type mismatch isolation.
- **Platform Verification**: Android (PASS), Desktop JVM (PASS), iOS Compilation (PASS), iOS Runtime (NOT VERIFIED ON WINDOWS).
- **Known Limitations**: `SecureStorageGateway` input validation is enforced by concrete infrastructure adapters or calling contracts.
- **Failure Interpretation**: Failure indicates broken preference key invariants or non-deterministic storage result hierarchy handling.

--------------------------------------------------
### MODULE: `:engine:execution`
--------------------------------------------------
- **Purpose**: Pure platform-neutral dynamic action execution engine foundation providing immutable strategy lookup, recursive parameter binding resolution, structured outcome handling, and safe diagnostic tracing.
- **Current Implementation Status**: **IMPLEMENTED** (Phase 014)
- **Test Types**: Common Multiplatform Unit Tests (`commonTest`)
- **Exact Commands**:
  ```bash
  ./gradlew :engine:execution:allTests --no-daemon
  ```
- **Expected Result**: `BUILD SUCCESSFUL` with 20 test methods passing across `BindingResolverTest`, `ActionRegistryTest`, `ActionDispatcherTest`, and `ExecutionResultTest`.

- **Contracts Verified**:
  - `ActionDispatcher` & `DefaultActionDispatcher` dispatch orchestration, unregistered action type failure, parameter binding resolution before execution, recursive object/list parameter resolution, missing binding execution abort, immutable `ActionSpec` copy, executor exception safety, coroutine cancellation propagation, and raw parameter logging omission.
  - `ActionRegistry` immutable strategy registration, single-claim `ActionType` ownership, duplicate claim `IllegalArgumentException` enforcement, and defensive map copy immutability.
  - `BindingResolver` & `DefaultBindingResolver` `${path}` path normalization, `BindingScope` value lookup delegation, and explicit `BindingResult.Failure` on missing paths.
  - `ExecutionResult` sealed variants (`Success`, `Failure`) and `ExecutionFailure` non-blank message guard.
- **Edge Cases Covered**: Missing binding with null scope, nested object/list parameter bindings, missing nested key, duplicate executor registration, unexpected executor runtime exceptions, cancellation propagation, parameter value redaction in diagnostic logs.
- **Platform Verification**: Android (PASS), Desktop JVM (PASS), iOS Compilation (PASS), iOS Runtime (NOT VERIFIED ON WINDOWS).
- **Known Limitations**: Concrete action executors (e.g. navigation, network, capability) are implemented by future integration modules.
- **Failure Interpretation**: Failure indicates broken parameter binding resolution, non-deterministic action strategy lookup, or parameter mutation.

--------------------------------------------------
### MODULE: `:sdui:engine`
--------------------------------------------------
- **Purpose**: Pure platform-neutral Server-Driven UI (SDUI) schema parsing, structural validation, model normalization, template component assembly, and action execution payload bridging.
- **Current Implementation Status**: **IMPLEMENTED** (Phase 015.1)
- **Test Types**: Common Multiplatform Unit Tests (`commonTest`)
- **Exact Commands**:
  ```bash
  ./gradlew :sdui:engine:allTests --no-daemon
  ```
- **Expected Result**: `BUILD SUCCESSFUL` with 8 test methods passing across `SduiEngineTest.kt`.
- **Contracts Verified**:
  - `DefaultSduiParser` kotlinx.serialization JSON decoding into polymorphic `SduiScreenDto` / `SduiTemplateDto` / `SduiComponentDto` / `SduiSubComponentDto` / `SduiChildDto` / `SduiChildrenDataDto` models with `@SerialName` `snake_case` mapping.
  - `DefaultSduiValidator` structural validation enforcing mandatory schema version 1, unique element IDs, valid hierarchy types (`home`, `form`, `grid`, `list`, `card`, `section`, `row`, `column`, `cell`, `item`, `button`, `input`, `timer`, `badge`, `banner`, `image`), `ChildrenData` terminal depth invariant, and `parent_action` `target_id` controlled node existence and `accepts_parent_action` declaration.
  - `DefaultSduiNormalizer` property mapping converting dimension/spacing strings to `DimensionSpec` (`Fixed`, `Adaptive`, `Fraction`, `Fill`, `Wrap`, `Token`) and `SpacingSpec` (`Fixed`, `Adaptive`, `Token`).
  - `DefaultSduiScreenAssembler` full pipeline execution producing valid `SduiScreen` domain model and building fast O(1) `nodeIndex` map.
- **Edge Cases Covered**: Missing/blank screenId, invalid schema version, duplicate template/component/subcomponent/child/childrenData IDs, invalid hierarchy type strings, non-terminal `ChildrenData -> ChildrenData` violation, dangling `parent_action` `target_id`, target node missing `accepts_parent_action`, missing optional hierarchy levels (e.g. direct Template -> SubComponent or Template -> Child).
- **Platform Verification**: Android (PASS), Desktop JVM (PASS), iOS Compilation (PASS), iOS Runtime (NOT VERIFIED ON WINDOWS).
- **Known Limitations**: Component renderers (`:sdui:render`) and runtime state management are implemented in future phases.
- **Failure Interpretation**: Failure indicates JSON contract decoding mismatch, structural validation rule breach, or normalizer property map failure.

--------------------------------------------------
### MODULE: `:androidApp`
--------------------------------------------------
- **Purpose**: Android Application Host (`MainActivity`, Android Manifest, Application Composition).
- **Current Implementation Status**: **IMPLEMENTED HOST**
- **Test Types**: Build & Assembly Verification
- **Exact Commands**:
  ```bash
  ./gradlew :androidApp:assembleDebug --no-daemon
  ```
- **Expected Result**: `BUILD SUCCESSFUL` producing debug APK at `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
- **Contracts Verified**: Android Manifest compilation, AGP packaging, dependency resolution.
- **Edge Cases Covered**: N/A.
- **Platform Verification**: Android (PASS).
- **Known Limitations**: None.
- **Failure Interpretation**: Failure indicates Android-specific build configuration or resource packaging issues.

--------------------------------------------------
### MODULE: `:desktopApp`
--------------------------------------------------
- **Purpose**: Desktop JVM Application Host (`Main.kt`, Compose Desktop Window).
- **Current Implementation Status**: **IMPLEMENTED HOST**
- **Test Types**: Build Assembly & JVM Runtime Smoke Test
- **Exact Commands**:
  ```bash
  ./gradlew :desktopApp:assemble --no-daemon
  ./gradlew :desktopApp:run --no-daemon
  ```
- **Expected Result**: `BUILD SUCCESSFUL` compiling desktop JAR and launching JVM window runtime cleanly.
- **Contracts Verified**: Compose Desktop window initialization and JVM desktop entry point.
- **Edge Cases Covered**: N/A.
- **Platform Verification**: Desktop JVM (PASS).
- **Known Limitations**: None.
- **Failure Interpretation**: Failure indicates Desktop JVM target incompatibility or window runtime crash.

--------------------------------------------------
### SKELETON MODULES CATALOG
--------------------------------------------------

The following modules exist as architectural subprojects in `settings.gradle.kts` but currently contain skeleton code reserved for upcoming implementation phases:

- **`:sdui:render`**: SKELETON — NO FUNCTIONAL TEST CONTRACT YET
- **`:feature:splash`**: SKELETON — NO FUNCTIONAL TEST CONTRACT YET
- **`:infrastructure:network`**: SKELETON — NO FUNCTIONAL TEST CONTRACT YET
- **`:infrastructure:persistence`**: SKELETON — NO FUNCTIONAL TEST CONTRACT YET
- **`:infrastructure:capabilities`**: SKELETON — NO FUNCTIONAL TEST CONTRACT YET

> [!IMPORTANT]
> **Skeleton Maintenance Mandate**: Whenever a skeleton module becomes implemented in a future phase, its skeleton entry above MUST be replaced in the SAME implementation phase with its full 11-field catalog entry (Purpose, Status, Test Types, Exact Commands, Expected Result, Contracts Verified, Edge Cases Covered, Platform Verification, Known Limitations, Failure Interpretation).

---

> [!IMPORTANT]
> **Master Command Synchronization Mandate**: Whenever a new module becomes IMPLEMENTED, the implementation phase MUST evaluate and update the Master Verification Commands below to include the new module's unit tests, host assembly tasks, and KMP iOS compilation targets.

---

## 4. Master Verification Commands

To execute the complete quality gate verification across all currently implemented modules (`:core:observability`, `:core:mvi`, `:core:ui`, `:core:navigation`, `:domain:actions`, `:domain:capabilities`, `:domain:storage`, `:engine:execution`, `:sdui:engine`) and platform targets:

### Standard Gradle Command (Linux / macOS / Windows Bash)
```bash
./gradlew :core:observability:allTests :core:mvi:allTests :core:ui:allTests :core:navigation:allTests :domain:actions:allTests :domain:capabilities:allTests :domain:storage:allTests :engine:execution:allTests :sdui:engine:allTests :androidApp:assembleDebug :desktopApp:assemble :sdui:engine:compileKotlinIosSimulatorArm64 --no-daemon
```

### Windows PowerShell Command Syntax
```powershell
& "./gradlew" :core:observability:allTests :core:mvi:allTests :core:ui:allTests :core:navigation:allTests :domain:actions:allTests :domain:capabilities:allTests :domain:storage:allTests :engine:execution:allTests :sdui:engine:allTests :androidApp:assembleDebug :desktopApp:assemble :sdui:engine:compileKotlinIosSimulatorArm64 --no-daemon
```
