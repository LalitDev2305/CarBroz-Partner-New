# SDUI + Dynamic Frozen Convergence — Implementation Tracker

> **Status:** IMPLEMENTED — final configured CI proof and authority-document status update remain before declaring `FROZEN GREEN`.
>
> **Authority 1:** `runtime/SDUI_SIMPLIFICATION_IMPLEMENTATION_PLAN.md`
>
> **Authority 2:** `feature/dynamic/DYNAMIC_FEATURE_ARCHITECTURE.md`
>
> This file is the execution/evidence tracker only. It does not define a third architecture.

## 1. Scope and owner decisions

- Repository: `LalitDev2305/CarBroz-Partner-New`
- Branch: `feature/sdui-frozen-convergence`
- Base: `development`
- PR: `#11`
- Frontend only. Backend was not modified by this convergence.
- Login/OTP are **not** reimplemented as product-specific frontend flows. They are represented only as realistic generic SDUI fixtures/mock flow proof.
- Resend/cooldown/timer behavior is **deferred by owner decision** until the real OTP JSON/auth flow is implemented. It is not part of the generic-engine freeze.
- Real backend/manual visual/auth validation is **post-engine validation** to be performed when the owner runs the actual app. It must not be represented as completed here.
- Do not merge to `development` unless explicitly approved by the owner.

## 2. Frozen generic architecture

Canonical hierarchy:

```text
Screen
  -> Template
      -> Component
          -> Element
          OR
          -> Section
              -> Element
              OR
              -> Group
                  -> Element
```

Active SDUI package:

```text
runtime/sdui/src/commonMain/kotlin/com/carbroz/sdui/
  model/
  parser/
  value/
  runtime/
  registry/
  render/
```

Exact wire actions:

```text
request
navigate
present
dismiss
state
external_uri
sequence
```

Exact structured value references:

```text
$binding
$context
$response
$literal
```

Dynamic ownership:

```text
NavigationStore current DynamicDestination
  -> DynamicFeatureFactory
  -> DynamicScreenStore
  -> NetworkDataSource
  -> API envelope.data
  -> SduiDecoder
  -> SduiSupportChecker
  -> DynamicScreenState
  -> DynamicScreen
  -> SduiRenderer
  -> SduiInteraction
  -> DynamicScreenStore
  -> SduiActionExecutor
```

Rules frozen by the two authority documents:

- Full `DynamicDestination` is stored/restored: `screenId`, `templateId`, `templateType`, `endpoint`, `method`, `authentication`.
- `templateType` controls rendering only; it never selects navigation.
- `NavigationStore` is the only back-stack owner.
- Back dispatches `NavigationStore.Pop`.
- Refresh reloads the current full destination.
- Screen GET unwraps canonical API envelope `data` before decoding.
- Frontend performs capability/security support checks, not a second backend structural validator.
- `DynamicScreenState` is the one mutable owner of fields/runtime node state/overlay for a screen.
- `DynamicContextProvider` is a read-only projection, not a second state owner.
- `SduiActionExecutor` executes exactly the seven backend actions.
- `request` and `navigate` remain distinct; request navigation is explicit through `responseMode=destination`.
- A request commits `$response` and `contextUpdates` only after the requested response contract succeeds; destination-mode requests validate/construct the destination and satisfy SESSION requirements before committing flow state.
- Successful request response/context state is committed atomically inside `DynamicFlowContext`.
- A successful request returning a SESSION destination establishes canonical session state before navigation.
- Production Dynamic code contains no Login/OTP/Dashboard/Booking-specific routing branch.

## 3. Phase status

| Phase | Requirement | State | Executable / structural proof |
|---|---|---|---|
| 1 | Canonical realistic fixtures | VERIFIED | Login, OTP, Dashboard, Booking Details, all-node, all-action, all-value-reference, unsupported vocabulary fixtures exist. OTP fixture deliberately contains no resend/cooldown policy. |
| 2 | Destination consistency boundary | VERIFIED | Store checks `screenId + templateId + templateType`; regression tests cover all identity mismatch classes; endpoint safety is checked client-side. |
| 3 | Exact SDUI models/actions/value refs | VERIFIED | Typed immutable models use exact seven-action union and exact four structured reference forms. |
| 4 | API envelope + decoder | VERIFIED | Store unwraps `data`; decoder accepts `JsonElement`; malformed/unknown action tests fail predictably. |
| 5 | Capability/security support checker | VERIFIED | Schema + template/component/section/group/element support and request/navigate endpoint safety tests exist; legacy deep validator removed. |
| 6 | Separated hierarchy registry | VERIFIED | One `SduiNodeRegistration.createRegistry()` entry point; hierarchy-specific registration/lookup and duplicate-failure tests exist. |
| 7 | Common rendering/accessories/contracts | VERIFIED FOR ENGINE | Shared render context/interactions/runtime overrides/accessory path exists. Visual appearance remains part of later owner app validation. |
| 8 | Template/component/section/group/element renderers | VERIFIED FOR ENGINE | Frozen vocabulary covers registered hierarchy and Text/Image/Input/Button; segmented input is property-driven; full configured compilation is final CI gate. |
| 9 | One Dynamic screen-state owner | VERIFIED | Field updates preserve other fields; validation, runtime node state, overlay, immutability and cancellation tests exist; legacy `FormStore` removed. |
| 10 | Structured value resolver | VERIFIED | `$binding/$context/$response/$literal`, nested object/array and missing-path behavior covered. |
| 11 | Seven-action executor | VERIFIED | Exact actions covered; HTTP/destination/session failures do not commit transient flow state; success response/context commit is atomic; SESSION is established before navigation; sequence failure policy covered. |
| 12 | Full destination/navigation/lifecycle | VERIFIED | Serialization/navigationId/conversion, process restoration, Back/Refresh, background cancellation and repeated-action suppression covered. |
| 13 | Legacy convergence/removal | VERIFIED STRUCTURALLY | Entire `com.carbroz.runtime.sdui.*` production/test tree removed; temporary `runtime:binding` dependency removed; unrelated runtime action/binding/realtime modules preserved. |
| 14 | Full vocabulary regression | VERIFIED | All-node, all-action, all-value-reference and unsupported-vocabulary tests exist. |
| 15 | Dynamic + mock flow tests | VERIFIED FOR ENGINE | Login -> OTP -> Dashboard and Dashboard -> Booking Details -> Back generic flows covered; store/context/executor tests cover generic runtime behavior. |
| 16 | Real Desktop/backend/manual app integration | DEFERRED BY OWNER | Post-engine. Real production Login/OTP JSON/auth/UI/backend behavior will be validated when the owner runs the app. No fake E2E claim. |
| 17 | Configured multiplatform/architecture CI | WAITING ON RUNNER | Workflow has `jvm-android`, `published-foundation-boundary`, then dependent `ios`. Current runs fail before runner allocation (`runner_id=0`, zero steps); details below. |
| 18 | Documentation freeze | IN_PROGRESS | Tracker converged. Authority-doc status must change from review draft only after configured CI executes green. |

## 4. Executable proof inventory

### SDUI runtime

- `SduiNodeRegistryTest`
  - frozen hierarchy registration
  - renderer lookup
  - duplicate template rejection
  - duplicate element rejection
- `SduiValueResolverTest`
  - all four references
  - nested object/array resolution
  - deterministic missing-path failure
- `SduiDecoderSupportCheckerTest`
  - direct `JsonElement` decode
  - malformed required data
  - unknown action failure
  - supported/unsupported schema
  - unsupported template/element
  - unsafe request endpoint
- `SduiSupportCheckerHierarchyTest`
  - unsupported component
  - unsupported section
  - unsupported group
  - unsafe navigate endpoint
- `SduiFrozenVocabularyTest`
  - product-shaped fixtures decode/support-check
  - all registered hierarchy/node types represented
  - exactly seven wire actions
  - four reference forms recursively resolve
  - unsupported vocabulary is rejected

### Dynamic feature

- `DynamicDestinationFlowContextTest`
  - complete destination serialization round-trip
  - stable navigation identity
  - lossless SDUI destination conversion
  - unsafe/load method checks
  - flow-context merge/latest-response/clear behavior
- `DynamicContextProviderTest`
  - safe read-only dynamic context projection
- `SduiActionExecutorTest`
  - request value resolution and exact network request
  - HTTP failure commit policy
  - 2xx destination-contract failure leaves `$response`/context untouched
  - 2xx SESSION validation failure leaves `$response`/context untouched
  - successful response/context commits only after the entire action contract succeeds
  - response destination navigation
  - SESSION establishment before navigation
  - direct navigate mode/destination
  - present/dismiss/state
  - external URI resolution/safety
  - sequence order/stop-on-failure
  - latest successful generic request replaces `$response`
- `DynamicScreenStoreTest`
  - envelope load/identity/field initialization
  - malformed envelope/unsupported schema/network failure
  - binding updates
  - validation true/false policy
  - product-neutral state/present/dismiss reduction
  - retry/refresh/back
  - background cancellation
- `DynamicScreenStoreFrozenRegressionTest`
  - templateId mismatch
  - templateType mismatch
  - runtime node state does not mutate immutable server screen
  - repeated interaction suppressed while action is in flight
- `DynamicFrozenFlowIntegrationTest`
  - Login -> OTP -> Dashboard through generic request/response/destination/session contracts
  - Dashboard -> Booking Details -> Back through generic navigate + `NavigationStore.Pop`

### Composition/startup

- Navigation process-state tests were migrated to full `DynamicDestination` storage/restoration.
- Composition DI tests were migrated to the new decoder/support/registry/renderer/value/executor graph.
- Startup mapping constructs the full destination without template-type routing inference.

## 5. Legacy/stale-code proof

Cleanup commit:

```text
9667c828bdd6e64cc6f0ff53879871e22211d693
refactor(sdui): retire legacy runtime and prove frozen contracts
```

The commit removes the complete legacy production/test namespace under:

```text
runtime/sdui/src/commonMain/kotlin/com/carbroz/runtime/sdui/**
runtime/sdui/src/commonTest/kotlin/com/carbroz/runtime/sdui/**
```

The current `runtime/sdui/build.gradle.kts` no longer contains the temporary compatibility dependency on `runtime:binding`.

Preserved intentionally because they are not duplicate SDUI ownership:

```text
runtime/action
runtime/binding
runtime/application
realtime/platform/foundation infrastructure
```

Current PR-diff audit:

- no added `com.carbroz.runtime.sdui` import/reference;
- no `templateType` routing branch introduced;
- no hardcoded `screenId == "partner_..."` routing branch introduced;
- no resend implementation/test id remains in the current PR diff; resend/cooldown appears only in this tracker as an explicit deferred owner decision.

Latest generic-engine hardening commits:

```text
c2036c2e656ff79b36083d0cc063dca835acd6e1
fix(dynamic): commit request flow state only after action success

2aa4cd8c9936d39384e833ea966c6676eb959981
test(dynamic): keep state reducer proof product-neutral

ef953e9a455d658a3cd1502172fe18b147242b4c
fix(dynamic): make successful request flow commit atomic
```

## 6. CI evidence

Configured workflow (`.github/workflows/ci.yml`):

```text
jvm-android
  ./gradlew verifyStartupArchitecture allTests assemble

published-foundation-boundary
  verifyFoundationCoordinates
  publishFoundationToLocalRepository
  app composition Android/Desktop tests against published foundation artifacts

ios (needs both Linux jobs)
  KMP iOS compile/link
  native Debug host
  native Staging host
  published-foundation iOS compile boundary
```

Evidence:

| SHA / run | Result |
|---|---|
| `4611782c...` / `34705005052` | replacement baseline progressed after composition wiring fix. |
| `8b78a0cc87b263b7514bdc53d7192dc008330ca5` / `34706115457` | `published-foundation-boundary` green; `jvm-android` executed and exposed frozen-fixture Kotlin interpolation defect; iOS skipped because dependency failed. |
| `0e04f07da5954f90aebff9fd4019bd121b501b2f` | first fixture escaping correction. |
| `9667c828bdd6e64cc6f0ff53879871e22211d693` / `34708136537` + failed-job rerun | Linux jobs failed before any step existed; logs unavailable; rerun repeated the same pre-runner state. |
| `9113adb2650538dbc711ac5588d0ef542408a15e` / `34708480982` | same zero-step pre-runner failure. |
| `7c6dd9be1ee8eb45154a874a1363afd7c40c3698` / `34708588329` | same zero-step pre-runner failure; job metadata showed `runner_id=0`, empty runner name. |
| `c2036c2e656ff79b36083d0cc063dca835acd6e1` / `34708833317` | same zero-step pre-runner failure; `runner_id=0`. |
| `2aa4cd8c9936d39384e833ea966c6676eb959981` / `34708903412` | same zero-step pre-runner failure; `runner_id=0`. |
| `ef953e9a455d658a3cd1502172fe18b147242b4c` / `34709065432` | same zero-step pre-runner failure; both Ubuntu jobs completed in ~2–3 seconds with `steps=[]`, `runner_id=0`; iOS skipped because dependencies never executed. |

GitHub public status reported Actions operational during this sequence, so the precise repository/account-side reason cannot be proven with the available connector. No code/test conclusion is inferred from a job that never acquired a runner. Final freeze still requires a run where the configured jobs actually start and execute.

## 7. Explicitly deferred / out of generic-engine scope

These are **not missing generic SDUI architecture work**:

1. **Resend/cooldown/timer policy** — deferred until the actual OTP JSON/auth flow is decided.
2. **Production Login/OTP request payload and auth-policy certification** — validate against the real production contract later.
3. **Manual Android/iOS/Desktop visual inspection** — owner will run the app and inspect actual UI.
4. **Real backend/environment E2E** — post-engine validation using the available environment without frontend guessing or backend modification.

If any later real JSON requires a new generic capability, it must be added through the frozen generic contracts, never as a Login/OTP-specific branch.

## 8. Freeze checklist

### SDUI

- [x] canonical API envelope `data`
- [x] backend-aligned models/enums
- [x] backend-destination-driven navigation
- [x] full destination stored/restored
- [x] Back = stack pop
- [x] Refresh reloads current destination
- [x] no `templateType` routing decisions
- [x] no duplicate deep validator
- [x] capability/security support checker only
- [x] separated hierarchy registry
- [x] duplicate registration fails
- [x] reusable accessories path
- [x] one canonical field/runtime state owner
- [x] exact seven actions
- [x] exact four value references
- [x] current frozen node vocabulary represented
- [x] generic Image/Text/Input/Button renderer path
- [x] segmented input property path
- [x] full-vocabulary fixture tests
- [x] Login -> OTP -> Dashboard mock flow
- [x] legacy SDUI runtime removed
- [x] resend/cooldown excluded from current implementation pending real OTP contract
- [ ] configured JVM/Android + published-foundation + iOS CI executes green
- [~] real backend/manual visual/auth validation — explicitly deferred by owner

### Dynamic

- [x] full `DynamicDestination` in navigation/restoration
- [x] no template-type routing
- [x] normal Back uses `NavigationStore.Pop`
- [x] Refresh reuses current full destination
- [x] `DynamicScreenState` sole mutable screen-state owner
- [x] no active `FormStore`
- [x] no local canonical input state competing with store state
- [x] Store owns lifecycle/orchestration but no Compose rendering
- [x] renderer does not own network/navigation
- [x] executor supports exact wire action vocabulary
- [x] failed request/destination/session actions do not partially commit flow state
- [x] successful response/context flow commit is atomic
- [x] context provider is projection, not state owner
- [x] generic auth-shaped mock flow
- [x] generic booking navigation/back mock flow
- [x] request/state/present/dismiss/external-uri/sequence tests
- [x] cancellation/repeated-action tests
- [ ] configured multiplatform CI executes green
- [~] real backend/manual app integration — explicitly deferred by owner

## 9. Remaining sequence to `FROZEN GREEN`

1. Obtain a GitHub Actions run where jobs actually acquire runners and execute.
2. Fix only concrete compile/test failures if that executable run exposes any.
3. Require `jvm-android` green.
4. Require `published-foundation-boundary` green.
5. Require dependent `ios` job to run and finish green.
6. Perform final exact-SHA stale-code audit after that executable green run.
7. Update both authority documents from review-draft wording to implemented/frozen generic-engine truth, preserving the deferred boundaries above.
8. Run the configured workflow once more on the documentation/final exact head if the docs commit triggers CI.
9. Do **not** merge PR #11 without owner approval.
10. Only then declare: **`SDUI + DYNAMIC GENERIC ENGINE — FROZEN GREEN`**.
