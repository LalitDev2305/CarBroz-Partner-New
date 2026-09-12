# SDUI + Dynamic Frozen Convergence — Implementation Tracker

> **Status:** SOURCE-COMPLETE FOR THE GENERIC ENGINE — final configured CI execution and authority-document freeze remain blocked by GitHub runner allocation.
>
> **Authority 1:** `runtime/SDUI_SIMPLIFICATION_IMPLEMENTATION_PLAN.md`
>
> **Authority 2:** `feature/dynamic/DYNAMIC_FEATURE_ARCHITECTURE.md`
>
> This file is execution/evidence tracking only. It does not define a third architecture.

## 1. Scope and owner decisions

- Repository: `LalitDev2305/CarBroz-Partner-New`
- Branch: `feature/sdui-frozen-convergence`
- Base: `development`
- PR: `#11` (draft, open, not merged)
- Frontend only. Backend was not modified by this convergence.
- Login/OTP are not reimplemented as product-specific frontend flows. They exist only as generic SDUI fixtures/mock-flow proof.
- **Resend/cooldown/timer remains explicitly deferred** until the real OTP JSON/auth contract is implemented.
- Real backend/manual visual/auth validation remains post-engine validation for the owner’s actual app run.
- Do not merge to `development` without explicit owner approval.

## 2. Frozen generic architecture being implemented

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

Current generic rules proven in source/tests:

- Full `DynamicDestination` is stored/restored: `screenId`, `templateId`, `templateType`, `endpoint`, `method`, `authentication`.
- `templateType` controls rendering only; it never chooses a route.
- `NavigationStore` is the only back-stack owner; Back dispatches `Pop`.
- Refresh reloads the current full destination.
- Dynamic screen GET unwraps API envelope `data` before decode.
- Frontend performs capability/security support checking, not a second backend structural validator.
- `DynamicScreenState` is the single mutable owner of fields/runtime node state/overlay for a live screen.
- `DynamicContextProvider` is a read-only projection over transient flow/application context.
- `SduiActionExecutor` executes exactly the seven backend actions.
- `request` and `navigate` remain distinct; request navigation is explicit through `responseMode=destination`.
- Request `$response` + `contextUpdates` commit atomically only after the requested response contract succeeds.
- Destination-mode request validates/constructs the destination and satisfies SESSION requirements before committing transient flow state.
- `responseMode=none` accepts successful no-body responses and clears stale `$response` rather than retaining a previous response accidentally.
- SESSION destination establishes canonical session before navigation.
- Request validation runs at the exact request execution point, including requests nested inside `sequence`.
- Validation blocking stops a sequence without turning normal field validation into an action failure.
- Targeted `dismiss(targetId)` clears only that requested/current overlay; untargeted dismiss clears the active overlay.
- Runtime `visible` state is honored centrally across template/component/section/group/element traversal.
- Action failure preserves an already-loaded screen instead of replacing valid content with the load-failure UI.
- Production Dynamic/SDUI code contains no Login/OTP/Dashboard/Booking-specific routing branch.

## 3. Phase status

| Phase | Requirement | State | Evidence |
|---|---|---|---|
| 1 | Canonical realistic fixtures | VERIFIED | Login, OTP, Dashboard, Booking Details, all-node, all-action, all-value-reference and unsupported fixtures exist. OTP fixture deliberately contains no resend/cooldown policy. |
| 2 | Destination consistency boundary | VERIFIED | Store checks `screenId + templateId + templateType`; full destination constructor enforces safe relative endpoint + GET load method. |
| 3 | Exact SDUI models/actions/value refs | VERIFIED | Immutable models use exact seven-action union and exact four reference forms. |
| 4 | API envelope + decoder | VERIFIED | Store unwraps `data`; decoder consumes `JsonElement`; malformed/unknown action behavior is deterministic. |
| 5 | Capability/security support checker | VERIFIED | Schema + hierarchy vocabulary + request/navigate endpoint safety + navigate GET requirement + embedded text-span actions are checked. |
| 6 | Separated hierarchy registry | VERIFIED | One `SduiNodeRegistration.createRegistry()` entry point; hierarchy-specific registration/lookup and duplicate rejection tests exist. |
| 7 | Common rendering/accessories/contracts | VERIFIED FOR ENGINE | Shared render context/runtime state/accessory renderer exists; Text/Image/Input/Button use reusable accessory paths. Canonical stack wire keys are `axis`, `spacing`, `mainAxisAlignment`, `crossAxisAlignment`. |
| 8 | Template/component/section/group/element renderers | VERIFIED FOR ENGINE | Stack/Form/Default templates, stack structural renderers and Text/Image/Input/Button leaves exist. Segmented input is property-driven; hierarchy visibility is centrally enforced. |
| 9 | One Dynamic screen-state owner | VERIFIED | Field updates, validation, node state, overlay, immutable server model and cancellation are owned by `DynamicScreenState`/Store; legacy `FormStore` is gone. |
| 10 | Structured value resolver | VERIFIED | `$binding/$context/$response/$literal`, nested object/array and deterministic missing-path behavior covered. |
| 11 | Seven-action executor | VERIFIED IN SOURCE/TESTS | HTTP/destination/session commit policy, atomic flow commit, no-body none-response, targeted dismiss, nested request validation, external URI, state and sequence failure policy covered. |
| 12 | Full destination/navigation/lifecycle | VERIFIED | Serialization/navigation identity/conversion, process restoration, Back/Refresh, cancellation and repeated-action suppression covered. |
| 13 | Legacy convergence/removal | VERIFIED STRUCTURALLY | Entire old `com.carbroz.runtime.sdui.*` production/test tree removed; temporary `runtime:binding` dependency removed; unrelated runtime modules preserved. |
| 14 | Full vocabulary regression | VERIFIED | Full hierarchy/actions/references/accessories/clickable text span capability and unsupported vocabulary proof exist. |
| 15 | Dynamic + mock flow tests | VERIFIED FOR ENGINE | Generic Login -> OTP -> Dashboard and Dashboard -> Booking Details -> Back flows covered; Store/Context/Executor regressions cover generic behavior. |
| 16 | Real Desktop/backend/manual app integration | DEFERRED BY OWNER | Post-engine real JSON/auth/UI/backend validation. No fake E2E claim. |
| 17 | Configured multiplatform/architecture CI | WAITING ON RUNNER | `jvm-android` and `published-foundation-boundary` repeatedly fail before allocation (`runner_id=0`, `steps=[]`); dependent iOS therefore skips. |
| 18 | Documentation freeze | WAITING ON PHASE 17 | Tracker reflects source truth. Authority docs remain review draft until configured CI actually executes green. |

## 4. Executable proof inventory

### SDUI runtime

`SduiNodeRegistryTest`
- all frozen hierarchy definitions register;
- lookups resolve renderers;
- duplicate template/element registration fails.

`SduiValueResolverTest`
- all four reference forms;
- nested object/array resolution;
- deterministic missing-path failures.

`SduiDecoderSupportCheckerTest`
- direct `JsonElement` decode;
- malformed data;
- unknown action;
- supported/unsupported schema/template/element;
- unsafe request endpoint.

`SduiSupportCheckerHierarchyTest`
- unsupported component/section/group;
- unsafe navigate endpoint;
- non-GET navigate destination rejected before execution.

`SduiFrozenVocabularyTest`
- realistic fixtures decode and support-check;
- all hierarchy/node types represented;
- shared accessories represented on multiple element types including Input;
- clickable Text span action represented;
- unknown embedded span action fails at capability boundary;
- exactly seven wire actions;
- all four references recursively resolve;
- unsupported vocabulary fails explicitly.

### Dynamic feature

`DynamicDestinationFlowContextTest`
- complete destination serialization round-trip;
- stable navigation identity;
- lossless SDUI destination conversion;
- unsafe endpoint/non-GET load rejection;
- flow-context merge/latest-response/clear behavior.

`DynamicContextProviderTest`
- read-only safe runtime context projection.

`SduiActionExecutorTest`
- request reference resolution and exact network request;
- HTTP failure commit policy;
- 2xx destination-contract failure leaves flow state untouched;
- 2xx SESSION validation failure leaves flow state untouched;
- successful response/context commit only after full action contract success;
- atomic response/context commit;
- `responseMode=none` supports 204/no body and clears stale response;
- response destination navigation;
- SESSION establishment before navigation;
- direct navigate mode/destination;
- present/dismiss/state;
- external URI resolution/safety;
- sequence order/stop-on-failure;
- latest successful request replaces `$response`.

`DynamicScreenStoreTest`
- envelope load/identity/field initialization;
- malformed envelope/unsupported schema/network failure;
- binding update;
- validate true/false policy;
- state/present/dismiss reduction;
- retry/refresh/back;
- background cancellation.

`DynamicScreenStoreFrozenRegressionTest`
- templateId/templateType mismatch;
- runtime node state does not mutate immutable server screen;
- targeted dismiss cannot clear an unrelated overlay;
- validated request nested inside sequence cannot bypass field validation;
- repeated interaction suppressed while action is in flight.

`DynamicFrozenFlowIntegrationTest`
- Login -> OTP -> Dashboard through generic request/response/destination/session contracts;
- Dashboard -> Booking Details -> Back through generic Navigate + `NavigationStore.Pop`.

### Composition/startup

- Navigation process-state tests use full `DynamicDestination` persistence/restoration.
- Composition DI tests use the new decoder/support/registry/renderer/value/executor graph.
- Startup mapping creates the full dynamic destination without `templateType` routing inference.

## 5. Legacy/stale-code proof

Legacy retirement commit:

```text
9667c828bdd6e64cc6f0ff53879871e22211d693
refactor(sdui): retire legacy runtime and prove frozen contracts
```

Removed namespace:

```text
runtime/sdui/src/commonMain/kotlin/com/carbroz/runtime/sdui/**
runtime/sdui/src/commonTest/kotlin/com/carbroz/runtime/sdui/**
```

The current `runtime/sdui/build.gradle.kts` has no temporary `runtime:binding` dependency.

Preserved intentionally because they are not duplicate SDUI ownership:

```text
runtime/action
runtime/binding
runtime/application
realtime/platform/foundation infrastructure
```

Exact-tree audits on source-complete heads found no old SDUI namespace, `FormStore.kt`, `DynamicScreenInstructionCodec.kt`, old Dynamic action wrappers or binding-context tree.

PR/source audit also found:
- no `templateType` routing rule;
- no product `screenId == ...` routing rule;
- no resend/cooldown implementation or test naming; resend/cooldown appears only as the explicit deferred owner decision in documentation.

## 6. Late generic-engine hardening evidence

These were found by auditing the frozen generic contract after the initial legacy convergence, not by reintroducing product-specific behavior:

```text
c2036c2e656ff79b36083d0cc063dca835acd6e1
fix(dynamic): commit request flow state only after action success

ef953e9a455d658a3cd1502172fe18b147242b4c
fix(dynamic): make successful request flow commit atomic

09c76ca66c9f8dc5593d2eb4be0859f26ff1ad11
fix(sdui): support shared accessories on input elements

39ecab03d077d1cef2b28c52f33a657cf43ae40d
fix(sdui): render canonical stack layout properties

f921e512e696b80451d8107f80739e3733938d9d
fix(sdui): check embedded text span action support

9f5d7b44e000031b3cf91f929b8406e874af29c6
fix(dynamic): preserve targeted dismiss semantics

145e14355dbf5ffdb535c365e9ce5eba2c9b4c97
fix(dynamic): dismiss only requested overlay target

f7e8eca70939b1789b545413f59963b5719cce9e
fix(sdui): apply runtime visibility across hierarchy

63b312bfc70ab20341554e60910c5eb4e4d46bdc
fix(sdui): reject unsupported navigate load methods

c34260be1bb31b0b159957fb9cd6f8c4216f67a1
fix(dynamic): validate nested request actions at execution point

e9022e8528b4ed96f02bcac4df2cea1004af1004
fix(dynamic): route request validation through executor sequence

72aad87329df0d12e0a5cfa6039744bd8dc9c0dd
fix(dynamic): keep loaded content on action failure

ba7fe66d5fa5eb26d9b1ae1ba6a4fc88877f1ecf
fix(dynamic): allow successful no-body request commits

560f0d40cbb57ad1f1543f484eafd1f1aedcd584
fix(dynamic): permit no-body responseMode none success
```

Associated regression/fixture commits include:

```text
1d076b5ba4a713825681741b8af61a97d477b124
4a622ea3d32886b1b8bcbf1e085410be7131d306
690e49788d49493e5f6f645b4cdaa8e03d4d9390
41dcccea1076b328e15e66d90c18e51dc966fb57
71208bf834cc6a7a2b49b72d5f57b00c45cc025a
6d3966659aa5c49f538a51ce00aa04510b278e74
90471632fb4db9163566d43ee9745f4e652ab189
909ce570435214de736aac358cbfcdbd1320c5ee
```

## 7. CI evidence

Configured workflow (`.github/workflows/ci.yml`):

```text
jvm-android
  ./gradlew verifyStartupArchitecture allTests assemble

published-foundation-boundary
  verifyFoundationCoordinates
  publishFoundationToLocalRepository
  Android/Desktop composition tests against published foundation artifacts

ios (needs both Linux jobs)
  KMP iOS compile/link
  native Debug host
  native Staging host
  published-foundation iOS compile boundary
```

Meaningful executable evidence before the runner-allocation incident:

| SHA / run | Result |
|---|---|
| `8b78a0cc87b263b7514bdc53d7192dc008330ca5` / `34706115457` | `published-foundation-boundary` green; `jvm-android` actually executed and exposed the frozen-fixture Kotlin interpolation defect; iOS skipped due dependency failure. The concrete source defect was fixed afterward. |

Persistent pre-runner evidence afterward includes:

| SHA / run | Result |
|---|---|
| `9667c828...` / `34708136537` | Linux jobs failed with zero steps; rerun repeated same state. |
| `7c6dd9be...` / `34708588329` | `runner_id=0`, empty runner name, zero steps. |
| `c2036c2e...` / `34708833317` | same pre-runner failure. |
| `ef953e9a...` / `34709065432` | same pre-runner failure. |
| `79c34674...` / `34709111488` | same pre-runner failure on initial attempt and explicit failed-job rerun. |
| `4a622ea3...` / `34714015894` | same pre-runner failure after accessory regression work. |
| `6d396665...` / `34714419540` | same pre-runner failure after support/destination hardening. |
| `909ce570435214de736aac358cbfcdbd1320c5ee` / `34714752791` | exact source-complete head: both Ubuntu jobs completed in ~3 seconds with `steps=[]`, `runner_id=0`, empty runner name; iOS skipped because dependencies never executed. |

No compile/test conclusion is inferred from a job that never acquired a runner. The workflow file itself has not been weakened or bypassed. Final generic-engine freeze still requires an exact-head run where the configured jobs actually execute.

## 8. Explicitly deferred / outside generic-engine freeze

These are not missing generic SDUI architecture work:

1. **Resend/cooldown/timer policy** — decide only with actual OTP JSON/auth flow.
2. **Production Login/OTP request payload/auth-policy certification** — validate against real production contract later.
3. **Manual Android/iOS/Desktop visual inspection** — owner will run the app and inspect actual UI.
4. **Real backend/environment E2E** — post-engine validation without frontend guessing or backend modification.

Any later real JSON that needs a new generic capability must extend the frozen generic contracts, never introduce a Login/OTP-specific branch.

## 9. Freeze checklist

### SDUI

- [x] canonical API envelope `data`
- [x] backend-aligned typed models/enums
- [x] backend-destination-driven navigation
- [x] full destination stored/restored
- [x] Back = stack pop
- [x] Refresh reloads current destination
- [x] no `templateType` routing decisions
- [x] no duplicate deep structural validator
- [x] capability/security support checker only
- [x] safe request/navigate endpoint checks
- [x] navigate dynamic destination GET capability check
- [x] embedded clickable text-span actions support-checked
- [x] separated hierarchy registry
- [x] duplicate registration fails
- [x] reusable accessories on multiple element types including Input
- [x] canonical stack layout wire keys restored
- [x] hierarchy-wide runtime visibility
- [x] one canonical field/runtime state owner
- [x] exact seven wire actions
- [x] exact four value references
- [x] current frozen node vocabulary represented
- [x] Image/Text/Input/Button renderer path
- [x] segmented input property path
- [x] full-vocabulary fixture tests
- [x] Login -> OTP -> Dashboard mock flow
- [x] legacy SDUI runtime removed
- [x] resend/cooldown excluded from current implementation
- [ ] configured JVM/Android + published-foundation + iOS CI actually executes green
- [~] real backend/manual visual/auth validation explicitly deferred

### Dynamic

- [x] full `DynamicDestination` in navigation/restoration
- [x] no template-type routing
- [x] normal Back uses `NavigationStore.Pop`
- [x] Refresh reuses current full destination
- [x] `DynamicScreenState` sole mutable screen-state owner
- [x] no active `FormStore`
- [x] no local canonical input state competing with Store state
- [x] Store owns orchestration but not Compose rendering
- [x] renderer owns no network/navigation
- [x] exact seven-action executor
- [x] failed request/destination/session actions do not partially commit flow state
- [x] successful response/context flow commit is atomic
- [x] no-body `responseMode=none` success does not retain stale response
- [x] request validation applies inside nested sequences
- [x] targeted dismiss preserves unrelated active overlay
- [x] action failure preserves loaded content
- [x] context provider is projection, not second mutable owner
- [x] generic auth-shaped mock flow
- [x] generic booking navigation/back mock flow
- [x] cancellation/repeated-action tests
- [ ] configured multiplatform CI actually executes green
- [~] real backend/manual app integration explicitly deferred

## 10. Remaining sequence to `FROZEN GREEN`

1. Obtain a GitHub Actions run where jobs actually acquire runners and execute.
2. Fix only concrete compile/test failures exposed by that executable run.
3. Require `jvm-android` green.
4. Require `published-foundation-boundary` green.
5. Require dependent `ios` to execute and finish green.
6. Perform one final exact-SHA stale-code audit after executable green CI.
7. Update both authority documents from review-draft wording to implemented/frozen generic-engine truth while preserving all deferred boundaries above.
8. Let the configured workflow run on that final documentation head as applicable.
9. Do not merge PR #11 without owner approval.
10. Only then declare: **`SDUI + DYNAMIC GENERIC ENGINE — FROZEN GREEN`**.
