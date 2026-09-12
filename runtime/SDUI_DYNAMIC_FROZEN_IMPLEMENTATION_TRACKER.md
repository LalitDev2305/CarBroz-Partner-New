# SDUI + Dynamic Frozen Convergence — Implementation Tracker

> **Purpose:** live execution source of truth for the CarBroz Partner frontend SDUI + Dynamic Feature frozen convergence.
>
> **Allowed states:** `TODO`, `IN_PROGRESS`, `IMPLEMENTED`, `TESTED`, `VERIFIED`, `BLOCKED`, `REMOVED`.
>
> **Verification rule:** no item becomes `VERIFIED` until the relevant executable proof has passed. Architecture documents describe target/final design; this tracker records implementation progress and proof.

## 1. Repository baselines

### Frontend

- Repository: `LalitDev2305/CarBroz-Partner-New`
- Working branch: `feature/sdui-frozen-convergence`
- Handoff/inspection HEAD verified before tracker creation: `a7f1597fbd92d7bcd3db57e912446f05398310e8`
- Handoff commit: `test(dynamic): remove legacy network action executor test`
- Base branch: `development`
- Development SHA at first compare: `6db89a06658f836a4b6bcdb74fa986c883242194`
- Feature branch relation at first compare: `ahead 27`, `behind 0`
- Target after complete verification: `development`

### Backend

- Repository: `LalitDev2305/CarBroz-Customer-Backend`
- Working branch: `feature/sdui-response-navigation-contract`
- Handoff/inspection HEAD verified: `cdf78c86cf5fd480a5054aeacf0a8f09a959c8b1`
- Handoff commit: `test(auth-sdui): freeze otp navigation and verify response flow`
- Base branch: `development`
- Development SHA at first compare: `1bdad12104abcf0cde5bee477e5e0b1f6833e2af`
- Feature branch relation at first compare: `ahead 7`, `behind 0`
- Target after complete verification: `development`

> Updating this tracker creates a new frontend commit. The immutable SHAs above are the exact verified starting baselines; later implementation/verification SHAs are recorded in the phase evidence below.

## 2. Frozen architecture authorities

### Frontend

- `runtime/SDUI_SIMPLIFICATION_IMPLEMENTATION_PLAN.md`
- `feature/dynamic/DYNAMIC_FEATURE_ARCHITECTURE.md`

### Backend

- `sdui/PARTNER-AUTH-SDUI-CONTRACT.md`
- `sdui/SDUI-COMPOSE-CONTRACT-IMPLEMENTATION-PLAN.md`

### Documentation defect

`runtime/SDUI_SIMPLIFICATION_IMPLEMENTATION_PLAN.md` and `feature/dynamic/DYNAMIC_FEATURE_ARCHITECTURE.md` still contain a stale `REVIEW DRAFT — not frozen` status header even though the current handoff explicitly freezes their architecture decisions. Do not reopen the architecture because of the stale header. Correct permanent-document status only during final documentation closeout after implementation/proof matches the frozen decisions.

## 3. Frozen rules being enforced

- Canonical hierarchy: `Screen -> Template -> Component -> [Element | Section -> (Element | Group -> Element)]`.
- Registration remains explicitly separated: `TemplateDefinitions`, `ComponentDefinitions`, `SectionDefinitions`, `GroupDefinitions`, `ElementDefinitions`, one `SduiNodeRegistration.createRegistry()` entry point.
- Wire actions are exactly: `request`, `navigate`, `present`, `dismiss`, `state`, `external_uri`, `sequence`.
- `request != navigation`.
- `request(responseMode=destination)` executes business API first, stores the full successful response, then extracts `data.nextScreen` and navigates only on success.
- Direct `navigate` uses the backend destination directly.
- Navigation modes are backend-driven: `push`, `replace`, `reset`.
- Dynamic navigation stack stores the full destination.
- Screen GET unwraps canonical API `data` before SDUI decode.
- Frontend uses support/security checks only; backend remains structural-validation owner.
- `DynamicScreenState` is the single observable owner for one live dynamic screen.
- Backend screen models stay immutable; runtime node changes live in `Map<nodeId, NodeRuntimeState>`.
- `$binding`, `$context`, `$response`, `$literal` are the structured value sources.
- Whole successful response envelope is the `$response` source.
- Backend-declared `contextUpdates` carries selected successful-request values into transient flow context.
- Auth session is established in canonical `SessionStore` before navigating to a `SESSION` destination.
- No Login/OTP endpoint-specific branching is allowed in generic SDUI/Dynamic runtime.

## 4. Current evidence snapshot

### Already implemented in source, not yet globally verified

- Backend-aligned SDUI models and schema `3.0.0`.
- Exact seven action model variants.
- Full SDUI destination model.
- Split node definitions and single registry entry point.
- Decoder/support checker/value resolver/runtime field/node/overlay models.
- Template/component/section/group traversal renderer plus Text/Image/Input/Button renderers.
- Generic leading/trailing accessories.
- New `DynamicDestination`, `DynamicFlowContext`, `DynamicContextProvider`.
- New generic `SduiActionExecutor`.
- New one-owner `DynamicScreenStore` path.
- New Dynamic screen/state/intent integration.
- Removal of several legacy Dynamic files, including `DynamicFeatureStore`, `DynamicBindingContext`, `DynamicActionExecutors`, `DynamicRealtimeCoordinator`, `DynamicScreenInstructionCodec`.
- Backend request response-mode, navigation-mode and context-update contract changes.
- Backend Login Continue emits POST Send OTP with `responseMode=destination`, `navigationMode=push`, and request-declared auth-flow context carry.
- Backend OTP Verify emits POST Verify OTP with `responseMode=destination`, `navigationMode=reset`.
- Backend OTP edit-number destination currently targets Login with `form_template`.

### Not yet allowed to call VERIFIED from current evidence

- Full backend contract/gate matrix.
- Full frontend unit/integration test matrix.
- Resend cooldown runtime behavior.
- Process-restoration proof for full new destination.
- Complete DI/startup convergence proof.
- Complete stale-reference removal/classification.
- Full frontend local Gradle gate.
- Full backend repository gates.
- Desktop real-backend E2E.
- Exact-SHA GitHub JVM/Android/iOS CI.
- Final permanent-document closeout.
- `SDUI + DYNAMIC — FROZEN GREEN`.

## 5. Phase tracker

| Phase | Responsibility | State | Evidence / next proof |
|---|---|---|---|
| 1 | Create live implementation tracker | IMPLEMENTED | This file created from verified handoff SHAs. Verify its presence on branch after commit. |
| 2 | Inspect current frontend/backend diffs | VERIFIED | Frontend compare: 27 ahead/0 behind from `development`; backend compare: 7 ahead/0 behind. Changed-file inventories reviewed before implementation. |
| 3 | Finish backend wire contract | IN_PROGRESS | Core action/schema + Login/OTP changes exist. Complete contract docs/tests and full backend gates before `VERIFIED`. |
| 4 | Finish frontend `SduiActionExecutor` | IN_PROGRESS | Seven-action executor exists. Must prove request/navigation/context/session/sequence/external URI semantics and fix defects found by tests/review. |
| 5 | Finish `DynamicScreenStore` | IN_PROGRESS | New one-owner store exists. Must prove validation/load/retry/identity/runtime-state/navigation/lifecycle behavior. |
| 6 | Finish DynamicScreen/renderer integration | IN_PROGRESS | New renderer path is wired in feature code. Must compile/test and remove remaining legacy consumers. |
| 7 | Finish DI convergence | IN_PROGRESS | Composition DI already changed in feature diff. Audit exact registrations/removals and compile proof. |
| 8 | Startup integration | IN_PROGRESS | `CarBrozApp.kt`/composition changed. Verify typed startup destination maps directly to new `DynamicDestination`. |
| 9 | Process restoration | IN_PROGRESS | `NavigationProcessState.kt` changed. Verify full new destination serialization/restoration with tests. |
| 10 | Remove superseded code | IN_PROGRESS | Multiple legacy files removed. Run repository-wide stale-reference audit and classify every remaining occurrence. |
| 11 | Complete frontend test matrix | TODO | Add/retain decoder, support, registry, resolver, validation, renderer, interaction, executor, state/present/external/sequence, Dynamic Store, Login→OTP, OTP→Dashboard, resend, navigation/restoration, architecture-gate tests. |
| 12 | Backend full verification | TODO | Run repository-standard build/lint/test/architecture gates on exact backend SHA. |
| 13 | Frontend local verification | TODO | Run canonical `verifyStartupArchitecture allTests assemble` plus new architecture gate(s) on exact SHA. |
| 14 | Desktop real-backend E2E | TODO | Prove Splash→Login GET→Send OTP→OTP GET→Verify→session→Dashboard SESSION GET plus Back/Refresh. |
| 15 | Frontend exact-SHA CI | TODO | Push final feature SHA and prove JVM/Android/host/published-foundation/iOS matrix green. |
| 16 | Final stale-code audit | TODO | Search prohibited legacy names/protocols and classify/remove all active stale paths. |
| 17 | Permanent documentation closeout | TODO | Update SDUI/Dynamic permanent docs with actual final truth; remove stale draft status if final proof supports it. |
| 18 | Merge/freeze | TODO | Merge only after all proof; rerun verification on resulting exact `development` SHAs; only then declare Frozen Green. |

## 6. Backend wire-contract tasks

| Task | State | Implementation files | Tests / evidence |
|---|---|---|---|
| Request `responseMode: none|destination` | IMPLEMENTED | `sdui/engine/src/core/Action.ts`, `SduiModel.ts` | `Action.spec.ts` changed; execute focused tests/full gates. |
| Request `navigationMode: push|replace|reset` | IMPLEMENTED | `Action.ts`, `SduiModel.ts` | `Action.spec.ts` changed; execute focused tests/full gates. |
| Generic successful-request transient context carry (`contextUpdates`) | IMPLEMENTED | `Action.ts`, `SduiModel.ts` | `Action.spec.ts` changed; verify nested serialization and failure semantics. |
| Login Continue is Send OTP POST | IMPLEMENTED | `PartnerLoginScreen.ts` | `PartnerLoginScreen.spec.ts` changed; focused test required. |
| Login request does not pre-navigate before API success | IMPLEMENTED | `PartnerLoginScreen.ts` | Prove action type is request and destination comes only from response. |
| Send OTP use case returns `nextScreen` | IMPLEMENTED | Identity/API existing path | Re-run relevant auth tests; no frontend assumption. |
| OTP Verify is Verify OTP POST | IMPLEMENTED | `PartnerOtpScreen.ts` | `PartnerOtpScreen.spec.ts` changed; focused test required. |
| Verify response returns Dashboard destination | IMPLEMENTED | Identity/API existing path | Re-run auth/controller/use-case tests. |
| OTP edit destination uses Login `form_template` | IMPLEMENTED | `PartnerOtpScreen.ts` | `PartnerOtpScreen.spec.ts` changed. |
| Login/OTP pass engine schema validation | TODO | SDUI engine | Run engine screen tests. |
| Other action types unaffected | TODO | SDUI engine | Run complete engine tests. |
| Partner Auth contract docs include final request/carry/navigation semantics | IN_PROGRESS | `sdui/PARTNER-AUTH-SDUI-CONTRACT.md` | Current doc covers transient ownership but must be checked for explicit wire fields. |
| Full backend gates | TODO | repository | Record exact SHA and commands/results. |

## 7. Frontend SDUI/action tasks

| Task | State | Implementation files | Tests / evidence |
|---|---|---|---|
| Decode screen API `envelope.data`, not whole envelope | IMPLEMENTED | `DynamicScreenStore.kt` | Store tests required. |
| Destination identity verification | IMPLEMENTED | `DynamicScreenStore.kt` | Store tests required. |
| Support/security checker only | IMPLEMENTED | `SduiSupportChecker.kt` | Support matrix tests required. |
| Request body structured-reference resolution | IMPLEMENTED | `SduiActionExecutor.kt`, `SduiValueResolver.kt` | Executor/resolver tests required. |
| Store whole successful request envelope as `$response` | IMPLEMENTED | `SduiActionExecutor.kt`, `DynamicFlowContext.kt` | Request tests required. |
| Apply backend `contextUpdates` only after successful request | IMPLEMENTED | `SduiActionExecutor.kt`, `DynamicFlowContext.kt` | Success/failure tests required. |
| `responseMode=none` stays on current screen | IMPLEMENTED | `SduiActionExecutor.kt` | Test required. |
| `responseMode=destination` reads `data.nextScreen` | IMPLEMENTED | `SduiActionExecutor.kt` | Test required. |
| Failed request never navigates | IMPLEMENTED | `SduiActionExecutor.kt` | Test required. |
| Session established before SESSION navigation | IMPLEMENTED | `SduiActionExecutor.kt`, canonical `SessionStore` | Verify against actual Verify OTP response field names and tests. |
| Direct navigate uses backend destination | IMPLEMENTED | `SduiActionExecutor.kt` | push/replace/reset tests required. |
| `present`/`dismiss` | IMPLEMENTED | `SduiActionExecutor.kt`, store result reduction | Overlay/target behavior tests required. |
| `state` runtime overlay | IMPLEMENTED | executor/store | State-operation tests required; missing-target behavior must be decided/proven from contract. |
| `external_uri` via capability infrastructure | IMPLEMENTED | `SduiActionExecutor.kt` | safe URI/context tests required. |
| `sequence` ordered execution / stop on failure | IMPLEMENTED | `SduiActionExecutor.kt` | sequence behavior tests required. |
| Resend hidden cooldown | TODO | Dynamic runtime | Must implement generically from successful response metadata without Login/OTP branching. |
| Successful resend replaces latest response/challenge | IMPLEMENTED at response-source level | `DynamicFlowContext.updateResponse` | Add explicit resend integration proof. |

## 8. Dynamic state/UI tasks

| Task | State | Implementation files | Tests / evidence |
|---|---|---|---|
| Single observable `DynamicScreenState` owner | IMPLEMENTED | `DynamicScreenStore.kt`, `DynamicScreenState.kt` | Architecture/store tests required. |
| Field updates via UDF `ValueChanged` | IMPLEMENTED | renderer + store | Interaction/store tests required. |
| Validation blocks request | IMPLEMENTED | `DynamicScreenStore.kt` | required/pattern/correction tests required. |
| Runtime node state immutable overlay | IMPLEMENTED | `NodeRuntimeState.kt`, store | state/render tests required. |
| Loading/action/failure/retry | IMPLEMENTED | store/state/screen | tests required. |
| Lifecycle cancellation | IMPLEMENTED | store jobs | cancellation tests required. |
| New renderer path only | IN_PROGRESS | `DynamicScreen.kt`, `runtime/sdui` | compile/stale-reference audit required. |
| Back delegates to `NavigationStore.Pop` | IMPLEMENTED | `DynamicScreenStore.kt` | navigation test required. |
| Refresh reuses full current destination | IMPLEMENTED | `DynamicScreenStore.kt` | refresh test required. |

## 9. Test matrix

All items remain below `VERIFIED` until executable proof is recorded.

### SDUI core

- [ ] Login fixture decodes.
- [ ] OTP fixture decodes.
- [ ] Dashboard fixture decodes.
- [ ] Unknown harmless fields ignored where contract allows.
- [ ] Malformed required model fails.
- [ ] Schema version `3.0.0` supported.
- [ ] Unsupported template/component/section/group/element rejected.
- [ ] Unsafe endpoint rejected.
- [ ] Nested sequence support checked.
- [ ] All registered template/component/section/group/element definitions present.
- [ ] Duplicate template/element registration fails.
- [ ] `$binding`, `$context`, nested context, `$response`, nested response, `$literal`, arrays, nested objects, missing-reference behavior.
- [ ] Required/pattern/valid/invalid/error-clear/OTP/mobile field validation.

### Rendering / interaction

- [ ] Template/component/section/group traversal.
- [ ] Text/spans/span actions.
- [ ] Image.
- [ ] Input/segmented input.
- [ ] Button.
- [ ] Leading/trailing accessories.
- [ ] Runtime visibility/enabled/loading.
- [ ] Input emits `ValueChanged`.
- [ ] Button/Text/span actions emit `ActionTriggered`.
- [ ] Renderer does not execute network/navigation.

### Action executor

- [ ] Request body references resolve.
- [ ] `responseMode=none` stays.
- [ ] `responseMode=destination` extracts `data.nextScreen`.
- [ ] Failed request does not navigate.
- [ ] Missing destination fails predictably.
- [ ] Successful response retained for `$response`.
- [ ] Declared transient context carried only after success.
- [ ] Direct navigate push/replace/reset.
- [ ] No navigation inference from template type.
- [ ] State set/toggle visible/enabled/loading/value.
- [ ] Missing state target behavior.
- [ ] Present dialog/bottom-sheet/popup and dismiss.
- [ ] External URI resolves/delegates/fails safely.
- [ ] Sequence order/failure/request+state/request+navigate contract.

### Dynamic Store / integration

- [ ] Screen load/unwrapping/identity/field initialization.
- [ ] Field update/validation/request/failure/retry/lifecycle cancellation.
- [ ] Login -> Send OTP -> carried phone -> push OTP -> OTP GET/render.
- [ ] OTP Verify -> latest challenge response + context phone + binding OTP -> session -> reset Dashboard -> SESSION GET.
- [ ] Resend cooldown disabled/ready/newest challenge.
- [ ] Back/Refresh.
- [ ] Full destination process restoration.
- [ ] SESSION auth preserved.

### Architecture gates

Prevent unapproved active reintroduction of:

- [ ] `CommandModel` / old Command protocol graph.
- [ ] `PreparedAction`.
- [ ] `ActionRegistry`.
- [ ] `BindingNamespace`.
- [ ] `FormStore`.
- [ ] `NodePath`.
- [ ] `SduiCommandIndex`.
- [ ] old compatibility envelope/runtime.
- [ ] old `DynamicScreenInstruction` graph.

## 10. Verification evidence log

| Exact SHA | Repository | Command / proof | Result | Notes |
|---|---|---|---|---|
| `a7f1597fbd92d7bcd3db57e912446f05398310e8` | frontend | Branch HEAD + compare inspection | PASS | Exact handoff SHA matched; compare showed 27 ahead / 0 behind. |
| `cdf78c86cf5fd480a5054aeacf0a8f09a959c8b1` | backend | Branch HEAD + compare inspection | PASS | Exact handoff SHA matched; compare showed 7 ahead / 0 behind. |

## 11. Defects / risks found

| ID | State | Finding | Required action |
|---|---|---|---|
| DOC-001 | IN_PROGRESS | Frozen frontend architecture docs still say `REVIEW DRAFT — not frozen`. | Correct during final permanent-doc closeout, after implementation/proof. |
| TEST-001 | IN_PROGRESS | Large portions of old Dynamic tests were removed while replacement matrix is not yet present in the feature diff. | Add new tests before claiming convergence. |
| RESEND-001 | TODO | Hidden resend cooldown is required by frozen Partner Auth contract but is not yet proven in the new Dynamic runtime. | Implement generic runtime mechanism and tests without screen/endpoint special cases. |
| PROOF-001 | TODO | No full local/backend/desktop/exact-SHA CI proof is recorded yet. | Complete phases 12–15. |

## 12. Remaining work summary

Current implementation is **NOT FROZEN GREEN**.

The next executable item is Phase 3/4 convergence proof: finish backend contract documentation/tests and frontend action-executor/store tests, fixing any contract mismatch found by source review. Continue responsibility-by-responsibility; after each coherent implementation, add/update tests, run relevant verification, update this tracker, then proceed.

## 13. Frozen Green checklist

Do not change this section to complete until every item is proven on the final exact SHAs.

- [ ] Architecture implemented.
- [ ] Backend/frontend wire contracts aligned.
- [ ] No known incompatible legacy path active.
- [ ] Required old code removed.
- [ ] Unit tests green.
- [ ] Integration/mock flows green.
- [ ] Full frontend local Gradle gate green.
- [ ] Backend full verification green.
- [ ] Desktop real-backend E2E green.
- [ ] Latest exact frontend SHA CI green.
- [ ] JVM/Android green.
- [ ] iOS green.
- [ ] Published-foundation checks green.
- [ ] Final architecture audit green.
- [ ] Implementation tracker fully `VERIFIED`.
- [ ] Permanent module docs updated.
- [ ] No newer commit supersedes verification.

Final declaration is forbidden until all boxes are checked:

```text
SDUI + DYNAMIC — FROZEN GREEN
```
