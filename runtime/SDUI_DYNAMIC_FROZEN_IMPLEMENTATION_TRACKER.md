# SDUI + Dynamic Frozen Convergence — Implementation Tracker

> **Purpose:** executable task plan for implementing the complete frontend architecture defined by the two authority documents below.
>
> **Authority 1:** `runtime/SDUI_SIMPLIFICATION_IMPLEMENTATION_PLAN.md`
>
> **Authority 2:** `feature/dynamic/DYNAMIC_FEATURE_ARCHITECTURE.md`
>
> This tracker does **not** invent a third architecture. It decomposes those two documents into implementation and verification tasks, records current evidence, and is updated as each task is completed.

## 1. Scope and execution rules

- Repository: `LalitDev2305/CarBroz-Partner-New`
- Branch: `feature/sdui-frozen-convergence`
- Base: `development`
- Frontend only. Backend may be read only to confirm an SDUI/wire-contract ambiguity. Do not change backend in this convergence.
- Do not reimplement Login, OTP, or any screen-specific flow. Generic SDUI/Dynamic infrastructure must make them work.
- Do not infer navigation from `screenId`, `templateType`, or product meaning.
- Do not create duplicate state owners, action abstractions, registries, validators, coordinators, or repositories that are not required by the two authority documents.
- Existing implementation is preserved when it already matches the authorities; the task is convergence, proof, cleanup, and gap-filling.
- Every task progresses through `TODO -> IMPLEMENTED -> TESTED -> VERIFIED`. `REMOVED` is used for obsolete code. `BLOCKED` requires concrete external evidence.
- `VERIFIED` requires executable proof on the exact branch code, not documentation intent.
- Do not declare `SDUI + DYNAMIC — FROZEN GREEN` until every freeze criterion that is executable in this repository is green and any environment-only limitation is explicitly documented.

## 2. Frozen target architecture distilled from both authority documents

### 2.1 SDUI protocol/runtime

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

Required SDUI building blocks:

```text
model/
parser/
value/
runtime/
registry/
render/
```

Required protocol capabilities:

- API screen responses are decoded from canonical envelope `data`.
- Frontend performs client capability/security checking only; it does not mirror backend structural validation.
- Registry is separated by hierarchy: templates, components, sections, groups, elements.
- One obvious `SduiNodeRegistration.createRegistry()` entry point.
- Duplicate renderer registration fails fast.
- Generic reusable accessories support leading/trailing rendering where declared.
- Structured values support exactly `$binding`, `$context`, `$response`, `$literal`.
- Request-body resolution works recursively for objects and arrays.
- Runtime server screen model stays immutable; client runtime changes live separately in `FieldState`, `NodeRuntimeState`, and `SduiOverlay`.
- Exact wire actions are only: `request`, `navigate`, `present`, `dismiss`, `state`, `external_uri`, `sequence`.
- Unknown/unsupported vocabulary fails cleanly instead of being guessed.

### 2.2 Dynamic feature

Target lifecycle:

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

Ownership rules:

- Full `DynamicDestination` is the navigation-stack value: `screenId`, `templateId`, `templateType`, `endpoint`, `method`, `authentication`.
- `templateType` controls rendering only, never routing.
- `NavigationStore` is the only back-stack owner.
- Normal Back dispatches `NavigationStore.Pop`.
- Refresh reloads the current full destination.
- `DynamicScreenState` is the only mutable owner of one screen's fields/runtime UI state.
- No `FormStore` or local canonical input state competes with it.
- `DynamicContextProvider` creates a safe read-only context snapshot; it is not another store.
- `DynamicScreenStore` owns lifecycle jobs, loading, decoding/support checks, field initialization, interactions, validation, action execution result reduction, retry/refresh, and cancellation.
- `SduiActionExecutor` executes the seven backend actions using existing network/navigation/capability/session owners.
- `DynamicScreen` renders loading/failure/content/overlay and does not execute transport or product logic.
- No Login/OTP/Dashboard/Booking-specific Dynamic Store or routing code.

## 3. Current implementation baseline

Implementation already present and retained unless tests prove a defect:

- New `com.carbroz.sdui.*` model/parser/value/runtime/registry/render path.
- Backend-aligned SDUI screen/node/action/value-reference models.
- Exact seven action variants.
- Full destination model.
- `SduiDecoder`, `SduiSupportChecker`, version policy and value resolver.
- Split hierarchy definition lists and single registration entry point.
- Generic Text/Image/Input/Button renderers and template/component/section/group traversal.
- Generic accessory path.
- `DynamicDestination`, `DynamicFlowContext`, `DynamicContextProvider`.
- Generic `SduiActionExecutor`.
- One-owner `DynamicScreenStore` with separate load/action jobs.
- Generic `DynamicScreen` integration.
- Back/Refresh intent handling.
- Process-restoration migration toward full destination.
- DI/startup migration toward the new graph.
- Several obsolete Dynamic classes have already been removed.

Recent compilation fixes already applied before this tracker revision:

- `DynamicScreen` Material3 bottom-sheet opt-in.
- common-safe `DynamicScreenStore` validation pattern handling.

Known current CI blocker before the next implementation step:

- `runtime/sdui/.../SduiNodeRegistry.kt` uses `putIfAbsent`, which is not available in Kotlin common/native. Replace it with common-safe fail-fast duplicate registration semantics.

## 4. Master implementation phases

These phases directly follow `SDUI_SIMPLIFICATION_IMPLEMENTATION_PLAN` phase order and absorb the matching `DYNAMIC_FEATURE_ARCHITECTURE` responsibilities.

| Phase | Authority requirement | State | Completion proof |
|---|---|---|---|
| 1 | Capture canonical realistic SDUI fixtures | TODO | Test fixtures for Login, OTP, Dashboard/minimal, booking examples, all-node/action/value vocabulary, invalid unsupported cases. |
| 2 | Confirm backend destination consistency read-only | IN_PROGRESS | Frontend rejects inconsistent destination identity; any backend inconsistency is documented, not patched here. |
| 3 | Exact SDUI models/actions/value references | IMPLEMENTED | Source exists; decoder/model tests must verify backend names/values and seven-action union. |
| 4 | Canonical envelope unwrapping + decoder | IMPLEMENTED | Store/decoder tests prove `envelope.data`, valid decode, malformed/unknown action failure. |
| 5 | Replace deep validation with capability-only support checker | IMPLEMENTED | Support matrix tests prove schema/node/action/safety checks only. |
| 6 | Separated hierarchy registry + single registration entry | IN_PROGRESS | Fix common compile defect; registry registration/lookup/duplicate tests; all platform compile proof. |
| 7 | Common properties/accessories/render context/interaction | IMPLEMENTED | Tests prove generic accessories, runtime overrides and interaction emission. |
| 8 | Template/component/section/group/element renderers | IMPLEMENTED | Renderer/fixture tests prove full hierarchy, Text/Image/Input/segmented OTP/Button/spans. |
| 9 | One DynamicScreenState owner for fields/runtime state | IMPLEMENTED | Store/state/architecture tests prove no competing FormStore/local canonical field owner. |
| 10 | Structured value resolver | IMPLEMENTED | Unit tests for four reference forms, nested object/array, missing-path behavior. |
| 11 | Exact seven-action SduiActionExecutor | IMPLEMENTED | Unit/integration tests for request/navigate/present/dismiss/state/external_uri/sequence and failure behavior. |
| 12 | Full DynamicDestination/navigation/back/refresh/restoration | IN_PROGRESS | Destination tests + NavigationStore/back/refresh/process-restoration proof. |
| 13 | Remove obsolete Command/PreparedAction/ActionRegistry/FormStore/old SDUI path | IN_PROGRESS | Repository-wide consumer audit first, then safe removal and dependency cleanup; no active legacy imports. |
| 14 | Complete mock vocabulary regression suite | TODO | All-node-types, all-action-types, all-value-reference-types tests green. |
| 15 | Complete Dynamic class + mock app-flow tests | TODO | Dynamic unit matrix + Login->OTP->Dashboard + Dashboard->Booking Details->Back + sequence/presentation/refresh flows green. |
| 16 | Real Desktop backend integration | TODO | Existing backend/environment flow executed without backend modification; otherwise concrete environment limitation recorded. |
| 17 | Android/iOS/Desktop + architecture CI | IN_PROGRESS | Exact-SHA PR CI green for JVM/Android/published-foundation/iOS/other configured jobs. |
| 18 | Documentation freeze | TODO | Tracker complete, stale-code audit clean, authority docs updated to final implemented truth and only then marked frozen. |

## 5. Detailed task backlog

### Phase 1 — canonical fixtures

- [ ] Add realistic API-envelope fixture for Login.
- [ ] Add realistic API-envelope fixture for OTP.
- [ ] Add minimal Dashboard fixture.
- [ ] Add booking-list/details fixture(s) sufficient to prove engine is not auth-specific.
- [ ] Add `all_node_types` fixture covering every currently registered hierarchy/node/accessory type.
- [ ] Add `all_action_types` fixture covering the exact seven actions.
- [ ] Add `all_value_reference_types` fixture covering binding/context/response/literal including nested values.
- [ ] Add malformed/unsupported node/action fixtures.

### Phase 2 — destination consistency boundary

- [x] Dynamic load validates screen identity against current destination.
- [ ] Test screenId mismatch.
- [ ] Test templateId mismatch.
- [ ] Test templateType mismatch.
- [ ] Test unsafe endpoint rejection at the support/security boundary.
- [ ] Record any backend-only inconsistency as read-only evidence; do not add frontend guessing.

### Phase 3 — SDUI models

- [x] `SduiScreen`, template/component/section/group/element contracts exist.
- [x] Exact seven actions exist.
- [x] Destination model exists.
- [x] Validation/accessory/theme/runtime models exist.
- [ ] Model/decode tests prove exact backend field names and enum values.
- [ ] Verify no speculative frontend-only wire action variants remain.

### Phase 4 — decoder/envelope

- [x] Screen GET extracts API `data` before decode.
- [x] Decoder accepts `JsonElement` directly.
- [x] Decode failure is represented predictably.
- [ ] Valid fixture decode tests.
- [ ] Malformed required data failure test.
- [ ] Unknown action deterministic failure test.
- [ ] Prove there is no JSON->String->JSON round trip in the active path.

### Phase 5 — support/security checker

- [x] Schema-version check exists.
- [x] Template/component/section/group/element registration checks exist.
- [x] Action-support checking exists.
- [x] Endpoint/URI safety logic exists in active path.
- [ ] Supported schema test.
- [ ] Unsupported schema test.
- [ ] Unknown template/component/section/group/element tests.
- [ ] Unknown action test.
- [ ] Unsafe endpoint/URI test.
- [ ] Architecture check that old deep structural validator is not active.

### Phase 6 — registry

- [x] Hierarchy definitions are split.
- [x] One `SduiNodeRegistration` entry point exists.
- [ ] Replace common-incompatible `putIfAbsent` implementation.
- [ ] All definition collections register successfully.
- [ ] Lookup returns correct renderer.
- [ ] Duplicate template type fails.
- [ ] Duplicate element type fails.
- [ ] Wrong hierarchy cannot be registered through the wrong API/type system.

### Phase 7 — common rendering contracts

- [x] `SduiRenderContext` path exists.
- [x] `SduiInteraction` carries value changes/action directly.
- [x] Generic leading/trailing accessory model/renderer exists.
- [ ] Test accessory use on more than one supported owning element.
- [ ] Test runtime visible/enabled/loading override application.
- [ ] Test input emits `ValueChanged`.
- [ ] Test button/text/span actions emit `ActionTriggered` without executing transport.

### Phase 8 — renderers

- [x] Template traversal exists.
- [x] Component traversal exists.
- [x] Section traversal exists.
- [x] Group traversal exists.
- [x] Text renderer exists.
- [x] Image renderer exists.
- [x] Input renderer exists.
- [x] Segmented OTP path exists in the current input design.
- [x] Button renderer exists.
- [ ] Test horizontal/vertical stack properties.
- [ ] Test text spans/clickable legal action.
- [ ] Test image sizing/content scale.
- [ ] Test normal input + segmented OTP input.
- [ ] Test button trailing icon/accessory.
- [ ] Test gradient/background/border/shape where currently supported.

### Phase 9 — Dynamic state ownership

- [x] One `MutableStateFlow<DynamicScreenState>` owner exists per DynamicScreenStore.
- [x] Bound fields are initialized in DynamicScreenState.
- [x] `ValueChanged` updates store state.
- [x] Immutable server screen is separate from runtime node state.
- [x] Loading/action/failure/overlay/last-response state is represented in the Dynamic path.
- [ ] Tests prove one field update preserves others.
- [ ] Tests prove node state update does not mutate the server screen.
- [ ] Tests prove required/pattern/valid/error-clear behavior.
- [ ] Test `validate=false` does not block request.
- [ ] Test `validate=true` blocks invalid request.
- [ ] Remove/verify absence of active old `FormStore` ownership.
- [ ] Verify input renderers do not keep a competing canonical local value.

### Phase 10 — value resolver

- [x] `$binding` support exists.
- [x] `$context` support exists.
- [x] `$response` support exists.
- [x] `$literal` support exists.
- [x] Recursive object/array resolution exists in active design.
- [ ] Unit test every reference type.
- [ ] Unit test nested object/array.
- [ ] Unit test missing binding/context/response paths and deterministic failure behavior.

### Phase 11 — seven actions

- [x] `request` execution exists.
- [x] `navigate` execution exists.
- [x] `present` execution exists.
- [x] `dismiss` execution exists.
- [x] `state` execution exists.
- [x] `external_uri` execution exists.
- [x] `sequence` execution exists.
- [ ] Request builds exact NetworkRequest and resolves references.
- [ ] Request failure never navigates.
- [ ] `responseMode=none` stores latest successful response and stays.
- [ ] `responseMode=destination` reads response destination and navigates only after success.
- [ ] Successful context updates are applied only after success.
- [ ] Direct navigate uses exact backend destination + navigation mode.
- [ ] SESSION destination establishes canonical session before navigation.
- [ ] Present dialog/bottom-sheet/popup behavior tested where modeled.
- [ ] Dismiss behavior tested.
- [ ] State set/toggle behavior tested.
- [ ] External URI resolution/delegation/safety tested.
- [ ] Sequence order and stop-on-failure tested.
- [ ] Generic resend/cooldown behavior audited against actual metadata model; implement only if required by the authority/backend contract, with no OTP-specific branch.
- [ ] Successful resend/latest request replaces `$response` source so newest challenge data is used.

### Phase 12 — Dynamic destination/navigation/lifecycle

- [x] Full DynamicDestination exists.
- [x] Back delegates to NavigationStore Pop.
- [x] Refresh reloads current destination.
- [x] Separate load/action jobs exist.
- [x] Cancellation is handled as lifecycle work, not product failure.
- [ ] Destination serialization/restoration retains every load field.
- [ ] Stable navigationId test.
- [ ] Startup/bootstrap maps typed destination without template inference.
- [ ] System Back test.
- [ ] Refresh same-destination endpoint test.
- [ ] Process restoration test.
- [ ] Close/background cancels feature-owned jobs.
- [ ] Repeated action policy/actionInFlight test.

### Phase 13 — legacy convergence/removal

Do not delete before proving no active consumer.

- [ ] Inventory every `com.carbroz.runtime.sdui.*` production/test consumer.
- [ ] Inventory old Command/PreparedAction/ActionRegistry/normalizer/compatibility/FormStore classes.
- [ ] Inventory temporary `runtime:binding` dependency from the new SDUI module/feature path.
- [ ] Classify unrelated runtime action/binding/realtime infrastructure that must remain for non-SDUI consumers.
- [ ] Remove old SDUI production implementations only after replacement proof.
- [ ] Remove old SDUI tests that test deleted architecture; retain/migrate behavioral tests that still express valid contract requirements.
- [ ] Remove unused temporary dependencies.
- [ ] Repository search proves no active generic SDUI/Dynamic import references the obsolete implementation.

### Phase 14 — full vocabulary regression

- [ ] All-node-types fixture decodes/support-checks/renders.
- [ ] All-action-types fixture decodes and every action is executable/tested.
- [ ] All-value-reference-types fixture resolves.
- [ ] Unsupported vocabulary fixture fails in controlled fashion.

### Phase 15 — Dynamic tests and app-flow integration

#### Dynamic class tests

- [ ] `DynamicDestinationTest`.
- [ ] `DynamicScreenState` behavior tests through store/reducer path.
- [ ] `DynamicScreenStoreTest`: load success, network failure, decode failure, unsupported contract, retry, refresh, binding update, validation, action result, overlay/state, cancellation.
- [ ] `DynamicScreen` Compose tests for loading/failure/content/overlay/interaction forwarding where host CI supports them.
- [ ] `DynamicContextProviderTest`: required safe context only; optional missing behavior predictable.
- [ ] `SduiActionExecutorTest`: full seven-action matrix.
- [ ] `DynamicFeatureFactoryTest`: correct destination/dependency wiring without DI-internal over-testing.

#### Mock app flows

- [ ] Login -> OTP -> Dashboard: generic request/context/response/destination/session flow, no screen-specific branching.
- [ ] Dashboard -> Booking Details -> Back: full destination push and NavigationStore Pop.
- [ ] Booking sequence flow: ordered state/request/navigation behavior and failure stop policy.
- [ ] Presentation flow: present/dismiss overlay state.
- [ ] Refresh flow: current destination reload and server screen replacement policy.

### Phase 16 — real Desktop integration

- [ ] Use the existing backend/environment without modifying backend.
- [ ] Prove Splash/bootstrap -> first DynamicDestination -> screen GET -> render.
- [ ] Prove auth request/destination/session flow if the available environment supports it.
- [ ] Prove Back and Refresh.
- [ ] If unavailable due external environment/credentials/service state, record the exact limitation and the strongest executable substitute; never fake green.

### Phase 17 — multiplatform and architecture verification

- [ ] JVM/unit/lint job green.
- [ ] Android build/test job green.
- [ ] Published-foundation boundary job green.
- [ ] iOS/Native job green if present in repository workflow.
- [ ] Startup architecture gate green.
- [ ] Active SDUI architecture gate/search green.
- [ ] Exact tested commit SHA recorded here.
- [ ] Fix every newly exposed compile/test defect; do not suppress failures with platform-specific hacks that violate common architecture.

### Phase 18 — final freeze/documentation

- [ ] Final stale-code search is clean/classified.
- [ ] Every SDUI plan freeze-checklist item is mapped to proof below.
- [ ] Every Dynamic architecture freeze-checklist item is mapped to proof below.
- [ ] `SDUI_SIMPLIFICATION_IMPLEMENTATION_PLAN.md` updated from review draft to implemented/frozen truth only after proof.
- [ ] `DYNAMIC_FEATURE_ARCHITECTURE.md` updated from review draft to implemented/frozen truth only after proof.
- [ ] This tracker records final exact SHA and CI run IDs.
- [ ] No merge to `development` unless explicitly requested/approved.
- [ ] Only after all executable criteria pass may the status be declared: `SDUI + DYNAMIC — FROZEN GREEN`.

## 6. Freeze checklist mapping

### SDUI plan checklist

- [ ] consumes actual API envelope `data`
- [ ] models match backend names/values
- [ ] backend-destination-driven navigation
- [ ] full DynamicDestination stored/restored
- [ ] Back = stack pop
- [ ] Refresh reloads current destination
- [ ] no templateType navigation decisions
- [ ] no duplicate deep structural validator
- [ ] support checker is capability/security only
- [ ] separate hierarchy registration
- [ ] duplicate registration fails
- [ ] reusable leading/trailing accessories
- [ ] one canonical field/runtime owner
- [ ] seven actions work
- [ ] four structured value references work
- [ ] all current node types render
- [ ] Image renderer verified
- [ ] text spans/actions verified
- [ ] segmented OTP input verified
- [ ] mock full-vocabulary tests green
- [ ] Login -> OTP -> Dashboard mock flow green
- [ ] real Desktop backend flow green or concrete external limitation recorded
- [ ] Android/iOS/Desktop configured CI green

### Dynamic architecture checklist

- [ ] full DynamicDestination in stack/restoration
- [ ] no templateType-based routing
- [ ] normal Back uses NavigationStore.Pop
- [ ] Refresh reloads current destination
- [ ] DynamicScreenState sole mutable screen-state owner
- [ ] no FormStore duplicate owner
- [ ] no local canonical input competing with store state
- [ ] Store does not render Compose
- [ ] renderer does not call network/navigation
- [ ] ActionExecutor supports exact backend action vocabulary
- [ ] DynamicContextProvider exposes only required safe context
- [ ] Login -> OTP -> Dashboard mock flow green
- [ ] Dashboard -> Booking Details -> Back mock flow green
- [ ] request/state/present/dismiss/external-uri/sequence tests green
- [ ] lifecycle cancellation tests green
- [ ] real Desktop integration green or external limitation explicitly recorded
- [ ] configured Android/iOS/Desktop CI green

## 7. Evidence log

Record immutable proof here as convergence progresses.

| Evidence | SHA / run | Result |
|---|---|---|
| Tracker originally introduced | `5d23e2f1e8e5470f09bfefde08ac28966d730528` | created |
| Navigation process-state test migrated to full destination | `d62be0f...` | implemented |
| Composition DI proof migrated to new graph | `0ba565179bf8323ab54e60d4e46de648bdb23c08` | implemented |
| Generic DynamicScreen Material3 overlay compile fix | `cd21c3341af36d3b4f474ebe20d8fa8eab1f2fe4` | implemented |
| Cross-module validation smart-cast compile fix | `7f2d974777875297ff8b5ed8cc32984bac320b33` | implemented |
| CI run on `0ba565...` | `34701225310` | failed; exposed Dynamic compile blockers and common `SduiNodeRegistry.putIfAbsent` blocker |
| CI run on `7f2d974...` | `34702415112` | pending/needs final inspection after subsequent commits |

## 8. Current next action

1. Fix `SduiNodeRegistry` common/native compile blocker without changing registry semantics.
2. Run/fetch exact-SHA CI and fix the next real failure.
3. Add the missing SDUI/Dynamic common tests and fixtures defined above.
4. Prove replacement ownership, then remove obsolete legacy SDUI path/dependencies.
5. Continue exact-SHA CI/test/failure-driven convergence until every Phase 1-18 criterion is completed or an external environment-only limitation is explicitly evidenced.
