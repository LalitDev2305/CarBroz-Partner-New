# SDUI + Dynamic Frozen Convergence — Implementation Tracker

> **Status:** APPROVED RENDERER-ARCHITECTURE CORRECTION READY FOR IMPLEMENTATION — configured CI remains blocked by GitHub runner allocation.
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
- Login/OTP are not reimplemented as product-specific frontend flows; existing fixtures/mock flow remain protocol proof only.
- **Resend/cooldown/timer remains deferred** until the real OTP/auth contract.
- Real backend/manual visual/auth validation remains post-engine validation.
- Do not merge to `development` without explicit owner approval.

Approved current correction:

1. keep the frozen additive hierarchy contract;
2. keep separate hierarchy registries;
3. keep reusable `ChildLayout` mechanics;
4. restore one concrete renderer class per supported backend Template/Component/Section/Group type;
5. keep Element renderers concrete;
6. register concrete renderer objects explicitly;
7. remove the anonymous/generic `structural*Renderer("wire_type")` factory layer;
8. add regression proof for exact concrete renderer lookup;
9. do not bundle the other open audit findings into this pass.

Other audit findings remain discussion-only unless separately approved.

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
concrete renderer class for that backend type
    ↓
shared modifier/layout/accessory helpers where applicable
    ↓
Compose
```

Current required mappings:

```text
form_template    -> FormTemplateRenderer
stack_template   -> StackTemplateRenderer
default_template -> DefaultTemplateRenderer
stack_component  -> StackComponentRenderer
stack_section    -> StackSectionRenderer
stack_group      -> StackGroupRenderer
text             -> TextElementRenderer
image            -> ImageElementRenderer
input            -> InputElementRenderer
button           -> ButtonElementRenderer
```

Shared `ChildLayout` owns current reusable linear child mechanics:

```text
axis
spacing
mainAxisAlignment
crossAxisAlignment
```

It is **not** a registered backend node renderer.

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

## 3. Authority-document update evidence

Approved renderer architecture was written into the authorities before source changes:

```text
cf8aba53aed1d6dfa74b47774dc8cf8b9640c6e6
docs(sdui): freeze explicit renderer registration architecture

18fcb6695d61d31e2c9a83c66196e98d81ad3e69
docs(dynamic): align with explicit SDUI renderer registration
```

Authority 1 now explicitly requires:

- hierarchy packages `render/template`, `render/component`, `render/section`, `render/group`, `render/element`;
- one concrete renderer class per supported backend type;
- explicit concrete object registration;
- `ChildLayout` as shared mechanics only;
- no `structuralTemplateRenderer("...")` / `structuralComponentRenderer("...")` registration indirection;
- exact new-type workflow: create class -> register -> test/fixture.

Authority 2 now explicitly requires:

- Dynamic does not choose concrete SDUI renderers;
- Dynamic passes decoded screen/context into `SduiRenderer`;
- renderer selection and child layout stay entirely inside `runtime:sdui`.

## 4. Phase status

| Phase | Requirement | State | Evidence |
|---|---|---|---|
| 1 | Canonical realistic fixtures | VERIFIED | Login, OTP, Dashboard, Booking Details, all-node, all-action, all-value-reference and unsupported fixtures exist. |
| 2 | Destination consistency boundary | VERIFIED | Full destination identity and safe GET load contract exist. |
| 3 | Exact SDUI models/actions/value refs | RE-AUDIT OPEN | Hierarchy model is accepted/frozen; typed `SduiValueReference` duplication remains separate discussion. |
| 4 | API envelope + decoder | RE-AUDIT OPEN | Direct `JsonElement` decode is baseline-correct; unused helpers remain separate discussion. |
| 5 | Capability/security support checker | RE-AUDIT OPEN | Existing hierarchy/action/endpoint checks remain; embedded action/accessory capability ownership remains separate discussion. |
| 6 | Separated hierarchy registry | VERIFIED DESIGN / CORRECTION PENDING | Registry separation and duplicate rejection stay; concrete renderer registration must be restored. |
| 7 | Common rendering/helpers | PARTIALLY VERIFIED | `ChildLayout` exists and spacing/alignment regression exists; unsupported layout vocabulary remains separate capability audit. |
| 8 | Template/component/section/group/element renderers | APPROVED CORRECTION PENDING | Replace generic structural factory layer with concrete hierarchy renderer files delegating to shared helpers. |
| 9 | One Dynamic screen-state owner | RE-AUDIT OPEN | Bound value vs runtime node value remains separate discussion. |
| 10 | Structured value resolver | VERIFIED BEHAVIOR / CLEANUP OPEN | Four behaviors work; duplicate typed model under review. |
| 11 | Seven-action executor | VERIFIED IN SOURCE/TESTS | Atomic request flow, SESSION, no-body none, targeted dismiss, nested validation, sequence covered. |
| 12 | Full destination/navigation/lifecycle | VERIFIED | Back/Refresh/restoration/full destination covered. |
| 13 | Legacy convergence/removal | VERIFIED STRUCTURALLY | old `com.carbroz.runtime.sdui.*` tree removed. |
| 14 | Vocabulary regression | PARTIALLY VERIFIED | hierarchy coexistence + child-layout regressions exist; exact concrete renderer mapping regression must be added. |
| 15 | Dynamic + mock flow tests | VERIFIED BASELINE | generic auth-shaped and booking navigation/back flows exist. |
| 16 | Real backend/manual app integration | DEFERRED BY OWNER | post-engine validation. |
| 17 | Configured multiplatform/architecture CI | WAITING ON RUNNER | Linux jobs repeatedly fail before runner allocation; iOS skips. |
| 18 | Documentation freeze | WAITING | Authorities now match approved renderer direction, but implementation + remaining approved audit work + executable CI still required. |

## 5. Current source state before this correction

Current source head before implementation still contains the previous generic factory approach introduced by:

```text
b9a35a1426b971a9578d37058e3568f91566b179
refactor(sdui): separate structural nodes from child layout
```

Useful part to keep from that commit:

```text
runtime/sdui/.../render/layout/ChildLayout.kt
```

Useful tests to keep:

```text
ChildLayoutTest
hierarchy coexistence assertions in SduiFrozenVocabularyTest
```

Part to replace:

```text
runtime/sdui/.../render/StructuralNodeRenderers.kt

structuralTemplateRenderer("stack_template")
structuralTemplateRenderer("form_template")
structuralTemplateRenderer("default_template")
structuralComponentRenderer("stack_component")
structuralSectionRenderer("stack_section")
structuralGroupRenderer("stack_group")
```

Concrete files to restore:

```text
render/template/StackTemplateRenderer.kt
render/template/FormTemplateRenderer.kt
render/template/DefaultTemplateRenderer.kt
render/component/StackComponentRenderer.kt
render/section/StackSectionRenderer.kt
render/group/StackGroupRenderer.kt
```

Each restored class should delegate common mechanics to `ChildLayout` rather than reintroducing duplicated Row/Column logic.

## 6. Regression requirements for this implementation pass

Must retain/prove:

```text
Component direct Elements + Sections coexist
Section direct Elements + Groups coexist
SduiRenderer traverses both branches
vertical ChildLayout
horizontal ChildLayout
spacing + positional main-axis alignment
separate hierarchy registry
no duplicate registration
exact concrete renderer object/type returned for every current structural wire type
no generic structural renderer factory registration remains
```

No new product-specific Login/OTP logic.

No changes in this pass to:

```text
SduiSupportChecker text-span capability design
accessory capability design
FieldState.value vs NodeRuntimeState.value ownership
hierarchy traversal deduplication
SduiValueReference cleanup
unused decoder helpers
element common-modifier/default inconsistencies
theme/targetApp decisions
```

unless the owner separately approves one of them.

## 7. Existing executable proof inventory

### SDUI runtime

`SduiNodeRegistryTest`
- hierarchy definitions register;
- lookup works;
- duplicate registration fails.

`SduiFrozenVocabularyTest`
- current fixtures decode/support-check;
- Component contains both direct Elements + Sections;
- Section contains both direct Elements + Groups;
- exact seven actions;
- exact four reference behaviors;
- unsupported vocabulary fails.

`ChildLayoutTest`
- current defaults;
- horizontal resolution;
- spacing and positional main-axis alignment are independently preserved;
- distributed main-axis alignment parses correctly.

### Dynamic feature

`DynamicDestinationFlowContextTest`, `DynamicContextProviderTest`, `SduiActionExecutorTest`, `DynamicScreenStoreTest`, `DynamicScreenStoreFrozenRegressionTest`, `DynamicFrozenFlowIntegrationTest` provide the existing generic orchestration/navigation/action proof.

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

These are not being redone by the current renderer correction.

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

Last meaningful executable evidence before runner-allocation failure:

```text
8b78a0cc87b263b7514bdc53d7192dc008330ca5 / run 34706115457
```

Later Linux jobs repeatedly show the pre-runner pattern (`steps=[]`, previously confirmed `runner_id=0`, empty runner name), so those failures are not treated as source compile/test conclusions.

For the previous child-layout source commit:

```text
b9a35a1426b971a9578d37058e3568f91566b179 / run 34742504145
```

both Linux jobs again completed as failure with no steps and iOS skipped.

Final freeze still requires a run where jobs actually acquire runners and execute.

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
- [ ] remove generic `StructuralNodeRenderers` factory layer
- [ ] restore concrete Template renderer classes
- [ ] restore concrete Component renderer class
- [ ] restore concrete Section renderer class
- [ ] restore concrete Group renderer class
- [x] Element renderers are concrete
- [ ] SduiDefinitions registers concrete renderer objects
- [ ] registry test proves exact concrete renderer mapping
- [x] exact seven wire actions baseline
- [x] exact four reference behaviors baseline
- [x] generic mock flows baseline
- [x] legacy SDUI runtime removed
- [x] resend/cooldown excluded
- [ ] remaining separately approved audit corrections
- [ ] executable JVM/Android + published-foundation + iOS CI green
- [~] real backend/manual validation deferred

### Dynamic

- [x] full DynamicDestination
- [x] no template-type routing
- [x] Back = NavigationStore.Pop
- [x] Refresh current destination
- [x] DynamicScreenState owns live state
- [x] no active FormStore
- [x] Dynamic does not choose concrete SDUI renderer types
- [x] Store does not render Compose
- [x] exact seven-action executor
- [x] request/session/sequence flow hardening baseline
- [~] bound value vs runtime node value remains separate audit item
- [ ] remaining approved audit corrections
- [ ] executable multiplatform CI green
- [~] real backend/manual validation deferred

## 12. Remaining sequence to `FROZEN GREEN`

1. Implement only the approved concrete-renderer correction described above.
2. Add exact concrete-renderer registry regression.
3. Audit the resulting diff for no generic factory residue and no unrelated changes.
4. Update this tracker with the source/test commit evidence.
5. Review the next open audit finding with the owner before changing production source.
6. Continue remaining approved audit corrections one by one.
7. Obtain GitHub Actions runners that actually execute the configured jobs.
8. Fix only concrete failures from executable CI.
9. Require `jvm-android`, `published-foundation-boundary`, and dependent `ios` green.
10. Perform final exact-SHA stale/dead-code/architecture audit.
11. Keep real backend/manual visual/auth validation explicitly deferred as agreed.
12. Do not merge PR #11 without owner approval.
13. Only then declare: **`SDUI + DYNAMIC GENERIC ENGINE — FROZEN GREEN`**.

## 13. Open class-audit items after this renderer correction

1. `SduiSupportChecker` Text-span embedded-action capability ownership — discussion-only.
2. Accessory vocabulary/capability checking — discussion-only.
3. Bound `Input` display vs `$binding` submission ownership divergence — discussion-only.
4. Duplicated hierarchy traversal between renderer/support checker/Dynamic — discussion-only.
5. `SduiValueReference` active-use duplication — discussion-only.
6. Unused `SduiDecoder` accessory/string-action helpers — discussion-only.
7. Element common modifier/enabled/value/default consistency — discussion-only.
8. Theme/targetApp consumption/capability decisions — discussion-only.

Withdrawn item:

- `SduiComponent.elements + sections` and `SduiSection.elements + groups` are intentionally coexistent. Keep the current model shape; do not convert it to XOR/either-or.
