# SDUI + Dynamic Frozen Convergence — Implementation Tracker

> **Status:** EXPLICIT RENDERER ARCHITECTURE IMPLEMENTED IN SOURCE / EXECUTABLE CI STILL RUNNER-BLOCKED.
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

The owner-approved renderer correction is now implemented in source:

1. frozen additive hierarchy retained;
2. separate hierarchy registries retained;
3. reusable `ChildLayout` retained;
4. one concrete renderer object restored for every current backend Template/Component/Section/Group type;
5. Element renderers remain concrete;
6. concrete renderer objects are registered explicitly;
7. generic `structural*Renderer("wire_type")` factory layer removed;
8. registry regression now proves exact concrete renderer lookup;
9. unrelated audit findings were not bundled.

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
concrete renderer object for that backend type
    ↓
shared modifier/layout/accessory helpers where applicable
    ↓
Compose
```

Current mappings now implemented:

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

The authorities were updated before source implementation:

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
- new-type workflow: create renderer -> register -> test/fixture.

Authority 2 requires:

- Dynamic does not choose concrete SDUI renderers;
- Dynamic passes decoded screen/context into `SduiRenderer`;
- renderer selection and child layout stay inside `runtime:sdui`.

## 4. Source implementation evidence

Implementation commit:

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
- keeps `render/layout/ChildLayout.kt` unchanged as the shared layout mechanism;
- extends `SduiNodeRegistryTest` to assert exact concrete renderer objects for all current wire types.

Exact-tree verification after the commit confirmed `StructuralNodeRenderers.kt` is absent and `SduiDefinitions.kt` directly imports/registers the concrete hierarchy renderers.

## 5. Phase status

| Phase | Requirement | State | Evidence |
|---|---|---|---|
| 1 | Canonical realistic fixtures | VERIFIED | Login, OTP, Dashboard, Booking Details, all-node, all-action, all-value-reference and unsupported fixtures exist. |
| 2 | Destination consistency boundary | VERIFIED | Full destination identity and safe GET load contract exist. |
| 3 | Exact SDUI models/actions/value refs | RE-AUDIT OPEN | Hierarchy model is accepted/frozen; typed `SduiValueReference` duplication remains separate discussion. |
| 4 | API envelope + decoder | RE-AUDIT OPEN | Direct `JsonElement` decode is baseline-correct; unused helpers remain separate discussion. |
| 5 | Capability/security support checker | RE-AUDIT OPEN | Existing hierarchy/action/endpoint checks remain; embedded action/accessory capability ownership remains separate discussion. |
| 6 | Separated hierarchy registry | VERIFIED IN SOURCE | Hierarchy registry separation, duplicate rejection and explicit concrete renderer registration are present. |
| 7 | Common rendering/helpers | VERIFIED FOR APPROVED LAYOUT PASS | `ChildLayout` exists; spacing/alignment regression exists; other capability concerns remain separate audit items. |
| 8 | Template/component/section/group/element renderers | VERIFIED IN SOURCE / CI PENDING | Explicit structural renderer files restored and Elements remain concrete. |
| 9 | One Dynamic screen-state owner | RE-AUDIT OPEN | Bound value vs runtime node value remains separate discussion. |
| 10 | Structured value resolver | VERIFIED BEHAVIOR / CLEANUP OPEN | Four behaviors work; duplicate typed model under review. |
| 11 | Seven-action executor | VERIFIED IN SOURCE/TESTS | Atomic request flow, SESSION, no-body none, targeted dismiss, nested validation, sequence covered. |
| 12 | Full destination/navigation/lifecycle | VERIFIED | Back/Refresh/restoration/full destination covered. |
| 13 | Legacy convergence/removal | VERIFIED STRUCTURALLY | old `com.carbroz.runtime.sdui.*` tree removed. |
| 14 | Vocabulary regression | SOURCE TESTS UPDATED / CI PENDING | hierarchy coexistence + child-layout regressions retained; concrete renderer mapping regression added. |
| 15 | Dynamic + mock flow tests | VERIFIED BASELINE | generic auth-shaped and booking navigation/back flows exist. |
| 16 | Real backend/manual app integration | DEFERRED BY OWNER | post-engine validation. |
| 17 | Configured multiplatform/architecture CI | WAITING ON RUNNER | exact implementation commit again failed before any Linux job steps; iOS skipped. |
| 18 | Documentation freeze | WAITING | renderer authorities now match source, but remaining approved audit items + executable green CI still required. |

## 6. Regression inventory for this pass

Must remain true:

```text
Component direct Elements + Sections coexist
Section direct Elements + Groups coexist
SduiRenderer traverses both branches
vertical ChildLayout
horizontal ChildLayout
spacing + positional main-axis alignment
separate hierarchy registry
no duplicate registration
exact concrete renderer object returned for each current wire type
no generic structural renderer factory registration remains
```

Current source/test evidence:

`SduiFrozenVocabularyTest`
- Component has direct Elements + Sections;
- Section has direct Elements + Groups;
- decoded screen passes support checker.

`ChildLayoutTest`
- default vertical layout contract;
- horizontal resolution;
- spacing and positional main-axis alignment resolved independently;
- distributed main-axis alignment parsed.

`SduiNodeRegistryTest`
- current Template/Component/Section/Group/Element vocabulary registered;
- exact concrete renderer object asserted for every current wire type;
- definitions expose the same concrete objects;
- duplicate Template/Element registration fails fast.

## 7. Explicitly untouched audit items

This renderer correction did not change:

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

These remain owner-discussion items.

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

These were not redone by the renderer correction.

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

Last meaningful executable evidence before the runner-allocation incident:

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

Current explicit-renderer implementation commit:

```text
e55ded9721d06261d7880c8a9833510f95ac6c12 / run 34743762761
published-foundation-boundary: failure, steps=null
jvm-android: failure, steps=null
ios: skipped
```

No compile/test conclusion is inferred from jobs that never execute any steps. Final freeze still requires a run where configured jobs acquire runners and actually execute.

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
- [x] generic `StructuralNodeRenderers` factory layer removed
- [x] concrete Template renderer objects restored
- [x] concrete Component renderer restored
- [x] concrete Section renderer restored
- [x] concrete Group renderer restored
- [x] Element renderers remain concrete
- [x] `SduiDefinitions` registers concrete renderer objects
- [x] registry test proves exact concrete renderer mapping
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

1. Review the next open audit finding with the owner before changing production source.
2. Continue remaining approved audit corrections one by one; do not bundle them.
3. Re-run exact-tree stale/dead-code/architecture audit after those corrections.
4. Obtain GitHub Actions runners that actually execute configured jobs.
5. Fix only concrete failures from executable CI.
6. Require `jvm-android`, `published-foundation-boundary`, and dependent `ios` green.
7. Perform final exact-SHA stale/dead-code/architecture audit.
8. Keep real backend/manual visual/auth validation explicitly deferred as agreed.
9. Do not merge PR #11 without owner approval.
10. Only then declare: **`SDUI + DYNAMIC GENERIC ENGINE — FROZEN GREEN`**.

## 13. Open class-audit items after renderer correction

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
