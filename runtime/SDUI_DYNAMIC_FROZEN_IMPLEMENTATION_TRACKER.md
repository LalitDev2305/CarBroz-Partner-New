# SDUI + Dynamic Frozen Convergence — Implementation Tracker

> **Status:** SOURCE AUDIT CONVERGENCE COMPLETE / EXACT-HEAD EXECUTABLE CI PENDING.
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
- Frontend only. Backend is not modified by this SDUI/Dynamic convergence.
- Login/OTP remain generic protocol fixtures/mock-flow proof only; no product-specific production branch was introduced.
- **Resend/cooldown/timer remains deferred** until the real OTP/auth contract.
- Real backend/manual visual/auth validation remains post-engine validation.
- Do not merge to `development` without explicit owner approval.

The owner-approved renderer correction and remaining source audit convergence are implemented:

1. frozen additive hierarchy retained;
2. separate hierarchy registries retained;
3. reusable `ChildLayout` retained;
4. one concrete renderer object exists for every current backend Template/Component/Section/Group type;
5. Element renderers remain concrete;
6. concrete renderer objects are registered explicitly;
7. generic `structural*Renderer("wire_type")` factory layer is absent;
8. registry regression proves exact concrete renderer lookup;
9. embedded Text actions have one normalized parser-owned extraction boundary;
10. accessory vocabulary is capability-checked and fails closed;
11. bound Input value ownership is canonical in `DynamicScreenState.fields`;
12. non-render hierarchy inspection reuses one canonical traversal;
13. duplicate `SduiValueReference` model is removed while the four wire-reference behaviors remain frozen;
14. unused String action decoder overload is removed and remaining decoder helpers are active;
15. element default/modifier/value-resolution inconsistencies found by the approved audit are corrected;
16. target-app/theme capability is explicit and fails closed when unsupported;
17. explicit invalid layout vocabulary is rejected rather than silently defaulted.

No backend contract, frozen hierarchy, navigation ownership, or seven-action vocabulary was reopened.

## 2. Frozen generic architecture

Canonical hierarchy:

```text
Screen
  -> Template
      -> Component
          -> Element(s)
          AND/OR
          -> Section(s)
              -> Element(s)
              AND/OR
              -> Group(s)
                  -> Element(s)
```

Frozen hierarchy semantics:

- a Component may contain direct Elements, Sections, or both;
- a Section may contain direct Elements, Groups, or both;
- a Group contains Elements;
- optional child collections are additive, not mutually exclusive;
- the previous “impossible state” finding for these child collections is withdrawn.

Frozen renderer/registration semantics:

```text
JSON node.type
    ↓
hierarchy-specific registry
    ↓
concrete renderer object for that backend type
    ↓
shared modifier/layout/accessory helpers where applicable
    ↓
Compose
```

Current mappings implemented:

```text
form_template    -> FormTemplateRenderer
stack_template   -> StackTemplateRenderer
default_template -> DefaultTemplateRenderer
stack_component  -> StackComponentRenderer
stack_section    -> StackSectionRenderer
stack_group      -> StackGroupRenderer
text             -> TextElementRenderer
image            -> ImageElementRenderer
input             -> InputElementRenderer
button           -> ButtonElementRenderer
```

Shared `ChildLayout` owns current reusable linear child mechanics:

```text
axis
spacing
mainAxisAlignment
crossAxisAlignment
```

It is not a registered backend node renderer.

Dynamic ownership remains:

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
  -> SDUI hierarchy registries / concrete renderers
  -> SduiInteraction
  -> DynamicScreenStore
  -> SduiActionExecutor
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

Exact structured reference behavior:

```text
$binding
$context
$response
$literal
```

## 3. Authority-document evidence

The renderer authorities were updated before source implementation:

```text
cf8aba53aed1d6dfa74b47774dc8cf8b9640c6e6
docs(sdui): freeze explicit renderer registration architecture

18fcb6695d61d31e2c9a83c66196e98d81ad3e69
docs(dynamic): align with explicit SDUI renderer registration

140ba56ed3b6ac538818433d501589a35acf6a7f
docs(runtime): track explicit renderer correction before implementation
```

Authority 1 requires:

- hierarchy packages `render/template`, `render/component`, `render/section`, `render/group`, `render/element`;
- one concrete renderer object/class per supported backend type;
- explicit concrete object registration;
- `ChildLayout` as shared mechanics only;
- no generic structural factory registration indirection;
- new-type workflow: create renderer -> register -> test/fixture;
- one canonical mutable owner for bound values;
- unsupported client capability fails closed rather than being guessed.

Authority 2 requires:

- Dynamic does not choose concrete SDUI renderers;
- Dynamic passes decoded screen/context into `SduiRenderer`;
- renderer selection and child layout stay inside `runtime:sdui`;
- bound field values remain owned through `DynamicScreenState.fields`.

## 4. Source implementation evidence

Renderer implementation commit:

```text
e55ded9721d06261d7880c8a9833510f95ac6c12
refactor(sdui): restore explicit hierarchy renderers
```

That commit:

- removes `render/StructuralNodeRenderers.kt`;
- restores `render/template/StackTemplateRenderer.kt`;
- restores `render/template/FormTemplateRenderer.kt`;
- restores `render/template/DefaultTemplateRenderer.kt`;
- restores `render/component/StackComponentRenderer.kt`;
- restores `render/section/StackSectionRenderer.kt`;
- restores `render/group/StackGroupRenderer.kt`;
- changes `SduiDefinitions.kt` back to explicit concrete renderer registration;
- keeps `render/layout/ChildLayout.kt` as the shared layout mechanism;
- extends `SduiNodeRegistryTest` to assert exact concrete renderer objects for all current wire types.

Approved remaining-audit convergence commit:

```text
1ac4909613e68c3524633fef982c4dda34650ef0
fix(sdui): converge frozen runtime ownership and support
```

That commit closes the approved A–I audit set:

- Text-span embedded-action discovery moved out of `SduiSupportChecker` into a normalized parser helper shared with Text rendering;
- accessory support-checking recognizes current `icon`, `divider`, `image` vocabulary and rejects unsupported types;
- bound `state(value)` routes to `FieldState` and Input renders that same canonical field value; unbound runtime values remain in `NodeRuntimeState`;
- `SduiHierarchyTraversal` is the canonical non-render traversal used by support checking and Dynamic field/target discovery;
- unused `SduiValueReference.kt` removed without changing `$binding/$context/$response/$literal` wire behavior;
- unused String `decodeAction` overload removed; JsonElement action decode and accessory decode remain active;
- Button/Input/Image/Text consistency fixes reuse the existing modifier/value-resolution ownership and remove invented Input-weight behavior;
- Partner runtime accepts `PARTNER`/`GLOBAL`, rejects `CUSTOMER`, and rejects non-null theme until theme is actually consumed;
- explicit unknown/invalid layout vocabulary is rejected while omitted layout values keep frozen defaults.

Exact-tree verification at `1ac4909613e68c3524633fef982c4dda34650ef0` confirms all of the following are absent:

```text
runtime/sdui/src/commonMain/kotlin/com/carbroz/runtime/sdui/**
StructuralNodeRenderers.kt
SduiValueReference.kt
DynamicScreenInstructionCodec.kt
```

## 5. Phase status

| Phase | Requirement | State | Evidence |
|---|---|---|---|
| 1 | Canonical realistic fixtures | VERIFIED | Login, OTP, Dashboard, Booking Details, all-node, all-action, all-value-reference and unsupported fixtures exist. |
| 2 | Destination consistency boundary | VERIFIED | Full destination identity and safe GET load contract exist. |
| 3 | Exact SDUI models/actions/value refs | VERIFIED | Hierarchy/actions remain exact; duplicate typed value-reference wrapper removed while four wire behaviors remain frozen. |
| 4 | API envelope + decoder | VERIFIED | Direct `JsonElement` decode retained; unused String action overload removed; active embedded/accessory decoding is covered. |
| 5 | Capability/security support checker | VERIFIED IN SOURCE / CI PENDING | Hierarchy/action/endpoint, target-app/theme, embedded action, accessory and layout capability checks fail closed. |
| 6 | Separated hierarchy registry | VERIFIED IN SOURCE | Hierarchy registry separation, duplicate rejection and explicit concrete renderer registration are present. |
| 7 | Common rendering/helpers | VERIFIED IN SOURCE / CI PENDING | `ChildLayout`/modifier ownership retained; approved element consistency corrections implemented. |
| 8 | Template/component/section/group/element renderers | VERIFIED IN SOURCE / CI PENDING | Explicit hierarchy renderer files exist and Elements remain concrete. |
| 9 | One Dynamic screen-state owner | VERIFIED IN SOURCE / CI PENDING | Bound values are canonical in `fields`; bound state-value actions update that owner; unbound node value remains runtime state. |
| 10 | Structured value resolver | VERIFIED | Four frozen reference behaviors resolve recursively; duplicate typed model removed. |
| 11 | Seven-action executor | VERIFIED IN SOURCE/TESTS | Atomic request flow, SESSION, no-body none, targeted dismiss, nested validation, sequence covered. |
| 12 | Full destination/navigation/lifecycle | VERIFIED | Back/Refresh/restoration/full destination covered. |
| 13 | Legacy convergence/removal | VERIFIED EXACT TREE | old `com.carbroz.runtime.sdui.*` tree and audited stale files absent at `1ac490…`. |
| 14 | Vocabulary regression | SOURCE TESTS UPDATED / CI PENDING | hierarchy traversal, capability/layout and concrete renderer regressions updated. |
| 15 | Dynamic + mock flow tests | VERIFIED BASELINE | generic auth-shaped and booking navigation/back flows exist. |
| 16 | Real backend/manual app integration | DEFERRED BY OWNER | post-engine validation. |
| 17 | Configured multiplatform/architecture CI | WAITING ON EXECUTION | exact source run `34748576665` for `1ac490…` remains workflow-level queued with zero jobs created; no source conclusion inferred. |
| 18 | Documentation freeze | FREEZE CANDIDATE | authorities/tracker align with implemented source; exact-head executable CI remains the only freeze gate. |

## 6. Regression inventory for the final source pass

Must remain true:

```text
Component direct Elements + Sections coexist
Section direct Elements + Groups coexist
SduiRenderer traverses both branches
canonical non-render traversal preserves both additive branches
vertical/horizontal ChildLayout
spacing + positional/distributed main-axis alignment
invalid explicit layout vocabulary fails closed
separate hierarchy registry
no duplicate registration
exact concrete renderer object returned for each current wire type
no generic structural renderer factory registration remains
bound state(value) and user editing use one FieldState value owner
unbound runtime value remains NodeRuntimeState
unsupported target/theme/accessory vocabulary fails closed
```

Current source/test evidence includes:

`SduiFrozenVocabularyTest`
- Component has direct Elements + Sections;
- Section has direct Elements + Groups;
- decoded supported screen passes support checker;
- unsupported embedded Text action fails at capability boundary.

`SduiHierarchyTraversalTest`
- canonical traversal preserves additive hierarchy order;
- element discovery and target lookup use the shared traversal model.

`ChildLayoutTest`
- default vertical layout contract;
- horizontal resolution;
- spacing and positional main-axis alignment resolved independently;
- distributed main-axis alignment parsed;
- invalid explicit vocabulary rejected by support checks.

`SduiNodeRegistryTest`
- current Template/Component/Section/Group/Element vocabulary registered;
- exact concrete renderer object asserted for every current wire type;
- definitions expose the same concrete objects;
- duplicate Template/Element registration fails fast.

`SduiDecoderSupportCheckerTest`
- Partner/Global target capability accepted;
- Customer target rejected in Partner runtime;
- unconsumed theme rejected;
- unsupported accessory/layout/Input-weight capability rejected.

`DynamicScreenStoreFrozenRegressionTest`
- immutable server screen remains unchanged;
- bound `state(value)` updates the canonical FieldState and never creates a runtime shadow value;
- later user editing continues through the same field owner;
- unbound state value remains NodeRuntimeState.

## 7. Approved A–I audit closure

The previously open audit list is now closed in source:

```text
A  SduiSupportChecker Text-span ownership             -> CLOSED
B  Accessory capability handling                      -> CLOSED
C  Bound Input displayed/submitted value ownership    -> CLOSED
D  Duplicate hierarchy traversal                      -> CLOSED for non-render traversal; renderer nesting intentionally remains compositional
E  SduiValueReference duplication                     -> CLOSED
F  Unused decoder APIs                                 -> CLOSED
G  Common renderer consistency                        -> CLOSED for approved findings
H  theme/targetApp capability                         -> CLOSED with explicit fail-closed policy
I  layout vocabulary validation                       -> CLOSED
```

No frozen product semantics were reopened to close these items.

## 8. Legacy/stale-code proof

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

Temporary `runtime:binding` dependency was removed. Unrelated runtime/action/binding/application/platform infrastructure remains intentionally preserved.

Exact-tree audit at `1ac4909613e68c3524633fef982c4dda34650ef0` additionally confirms the audited stale files listed in Section 4 are absent.

## 9. Important generic-engine hardening already completed

```text
c2036c2e... request flow commit only after success
ef953e9a... atomic successful request flow commit
09c76ca6... shared Input accessories
39ecab03... canonical stack layout keys
f921e512... embedded text span action support checking
9f5d7b44... targeted dismiss semantics
145e1435... dismiss only requested overlay
f7e8eca7... hierarchy-wide runtime visibility
63b312bf... reject non-GET navigate destination
c34260be... nested request validation execution point
e9022e85... executor-sequence request validation
72aad873... preserve loaded content on action failure
ba7fe66d... successful no-body request commits
560f0d40... responseMode none supports no body
```

These were not redone; the final audit convergence only corrected the approved ownership/capability/dead-code gaps around them.

## 10. CI evidence

Configured workflow remains:

```text
jvm-android
  ./gradlew verifyStartupArchitecture allTests assemble

published-foundation-boundary
  verifyFoundationCoordinates
  publishFoundationToLocalRepository
  Android/Desktop composition tests against published foundation artifacts

ios
  needs both Linux jobs
  KMP iOS compile/link + native host/published-foundation checks
```

The workflow uses GitHub-hosted `ubuntu-latest` for the two Linux jobs and `macos-latest` for iOS; it is not pinned to a custom/self-hosted runner.

Last meaningful executable evidence before the runner/scheduler incident:

```text
8b78a0cc87b263b7514bdc53d7192dc008330ca5 / run 34706115457
```

Previous child-layout source commit:

```text
b9a35a1426b971a9578d37058e3568f91566b179 / run 34742504145
published-foundation-boundary: failure, steps=null
jvm-android: failure, steps=null
ios: skipped
```

Explicit-renderer implementation commit:

```text
e55ded9721d06261d7880c8a9833510f95ac6c12 / run 34743762761
published-foundation-boundary: failure, steps=null
jvm-android: failure, steps=null
ios: skipped
```

Final approved source-audit commit:

```text
1ac4909613e68c3524633fef982c4dda34650ef0 / run 34748576665
workflow status: queued
jobs: []
```

No compile/test conclusion is inferred from jobs that never execute. The documentation-closeout commit intentionally supersedes this queued source-only run and must itself receive an executable exact-head run before freeze activation.

## 11. Freeze checklist

### SDUI

- [x] canonical API envelope `data`
- [x] backend-destination-driven navigation
- [x] full destination stored/restored
- [x] Back = stack pop
- [x] Refresh reloads current destination
- [x] no `templateType` routing decisions
- [x] no duplicate deep structural validator
- [x] Component direct Elements + Sections allowed together
- [x] Section direct Elements + Groups allowed together
- [x] Group contains Elements
- [x] separated hierarchy registry
- [x] duplicate registration fails
- [x] reusable `ChildLayout` exists
- [x] spacing + alignment regression exists
- [x] explicit invalid layout vocabulary fails closed
- [x] generic `StructuralNodeRenderers` factory layer removed
- [x] concrete Template renderer objects restored
- [x] concrete Component renderer restored
- [x] concrete Section renderer restored
- [x] concrete Group renderer restored
- [x] Element renderers remain concrete
- [x] `SduiDefinitions` registers concrete renderer objects
- [x] registry test proves exact concrete renderer mapping
- [x] embedded Text action ownership converged
- [x] accessory capability ownership converged
- [x] bound value ownership converged
- [x] canonical non-render hierarchy traversal exists
- [x] duplicate typed value-reference model removed
- [x] unused decoder API removed
- [x] approved element consistency corrections implemented
- [x] targetApp/theme capability explicit
- [x] exact seven wire actions baseline
- [x] exact four reference behaviors baseline
- [x] generic mock flows baseline
- [x] legacy SDUI runtime removed
- [x] exact-tree audited stale files absent
- [x] resend/cooldown excluded
- [x] remaining separately approved A–I audit corrections complete
- [ ] exact-head executable JVM/Android + published-foundation + iOS CI green
- [~] real backend/manual validation deferred

### Dynamic

- [x] full DynamicDestination
- [x] no template-type routing
- [x] Back = NavigationStore.Pop
- [x] Refresh current destination
- [x] DynamicScreenState owns live state
- [x] bound mutable values are canonical in `fields`
- [x] no active FormStore
- [x] Dynamic does not choose concrete SDUI renderer types
- [x] Store does not render Compose
- [x] exact seven-action executor
- [x] request/session/sequence flow hardening baseline
- [x] bound value vs runtime node value divergence resolved
- [x] remaining approved audit corrections complete
- [ ] exact-head executable multiplatform CI green
- [~] real backend/manual validation deferred

## 12. Remaining sequence to `FROZEN GREEN`

Source and documentation convergence are complete after the documentation-closeout commit. The only activation sequence is:

1. obtain an exact-head workflow run that actually creates and executes the configured jobs;
2. fix only concrete failures from executed CI, if any;
3. require `jvm-android`, `published-foundation-boundary`, and dependent `ios` to be green on the same final HEAD;
4. re-check that the final HEAD tree still contains no audited stale/legacy files;
5. keep real backend/manual visual/auth validation explicitly deferred as agreed;
6. do not merge PR #11 without owner approval;
7. once exact-head CI is green, the conditional freeze declaration below activates without another evidence-only commit.

**Conditional freeze declaration:** when the final documentation-closeout HEAD has executed-green `jvm-android`, `published-foundation-boundary`, and dependent `ios`, declare **`SDUI + DYNAMIC GENERIC ENGINE — FROZEN GREEN`** with no additional source/document change required.

## 13. Closed class-audit items

1. `SduiSupportChecker` Text-span embedded-action capability ownership — CLOSED.
2. Accessory vocabulary/capability checking — CLOSED.
3. Bound `Input` display vs `$binding` submission ownership divergence — CLOSED.
4. Duplicated hierarchy traversal between support checker/Dynamic — CLOSED through canonical non-render traversal; renderer nesting intentionally remains compositional.
5. `SduiValueReference` active-use duplication — CLOSED; unused model removed.
6. Unused `SduiDecoder` String-action helper — CLOSED; active JsonElement/accessory decoders retained.
7. Element common modifier/enabled/value/default consistency — CLOSED for approved findings.
8. Theme/targetApp consumption/capability decisions — CLOSED through explicit support/fail-closed policy.
9. Layout vocabulary validation — CLOSED.

Withdrawn item remains withdrawn:

- `SduiComponent.elements + sections` and `SduiSection.elements + groups` are intentionally coexistent. Keep the current model shape; do not convert it to XOR/either-or.
