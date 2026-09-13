# CarBroz Partner Frontend — SDUI Simplification & Implementation Plan

> **Status:** REVIEW DRAFT — frozen additive hierarchy contract and approved structural child-layout refactor are implemented in source; remaining class-audit findings and final executable CI/freeze are still open.
>
> **Goal:** build one simple, scalable, fully dynamic SDUI runtime for the whole CarBroz Partner app. The frontend must render and execute what the backend sends; it must not hardcode Login/OTP behavior or infer product navigation from template types.

---

# 1. First briefing — what the frontend really has to do

After Splash finishes, the backend gives the frontend the first dynamic destination. From that point the app is driven by backend SDUI.

The frontend only needs to do these jobs:

1. call the endpoint for the current dynamic destination;
2. unwrap the API `data` field;
3. decode the SDUI screen JSON;
4. check that the frontend knows the schema/node/action types it received;
5. render the screen;
6. keep current field/runtime state;
7. when the user interacts, execute the exact backend action;
8. if that action navigates, push/replace/pop the correct dynamic destination through `NavigationStore`;
9. if Android/iOS/Desktop system Back is pressed, pop the previous full destination from the navigation stack.

The target flow is intentionally simple:

```text
Splash
  ↓
Bootstrap returns nextScreen
  ↓
DynamicDestination
  ↓
NavigationStore
  ↓
DynamicScreenStore
  ↓
NetworkDataSource
  ↓
API response envelope
  ↓
data
  ↓
SduiDecoder
  ↓
SduiSupportChecker
  ↓
SduiScreen
  ↓
SduiRenderer
  ↓
User interaction
  ↓
SduiActionExecutor
  ├── request       → NetworkDataSource
  ├── navigate      → NavigationStore
  ├── present       → DynamicScreenState overlay
  ├── dismiss       → DynamicScreenState overlay
  ├── state         → DynamicScreenState node state
  ├── external_uri  → CapabilityRegistry internally
  └── sequence      → execute child actions in order
```

There should not be an unnecessary chain like:

```text
DTO → deep structural validator → compatibility engine → normalizer
→ Command → ActionRegistry → ActionDefinition → PreparedAction → executor
```

---

# 2. Navigation is fully dynamic — frontend never chooses a screen from templateType

This is critical.

`templateType` answers only:

> **How should this destination be rendered?**

It does **not** answer:

> **Where should the app navigate?**

The backend `navigate` action already sends the complete destination:

```text
screenId
templateId
templateType
endpoint
method
authentication
```

Example:

```json
{
  "type": "navigate",
  "payload": {
    "screenId": "partner_otp",
    "templateId": "tpl_P6X8N3",
    "templateType": "form_template",
    "endpoint": "/api/v1/partner/screen/auth_otp",
    "method": "GET",
    "authentication": "NONE"
  }
}
```

Frontend behavior:

```text
Navigate action received
    ↓
create DynamicDestination from payload
    ↓
NavigationStore.Push(destination)
    ↓
DynamicScreenStore loads destination.endpoint
    ↓
backend returns that screen
    ↓
SduiRenderer renders destination.templateType
```

## 2.1 What should be stored in the back stack?

Store the **full DynamicDestination**, not only `templateId`.

```kotlin
data class DynamicDestination(
    val screenId: String,
    val templateId: String,
    val templateType: String,
    val endpoint: String,
    val method: RequestMethod,
    val authentication: Authentication,
)
```

`templateId` remains part of destination identity, but it is not enough by itself to reload a screen.

A stack can therefore look like:

```text
[0] partner_login
    screenId      = partner_login
    templateId    = tpl_7K2M9Q
    templateType  = form_template
    endpoint      = /api/v1/partner/screen/auth_login

[1] partner_otp
    screenId      = partner_otp
    templateId    = tpl_P6X8N3
    templateType  = form_template
    endpoint      = /api/v1/partner/screen/auth_otp

[2] partner_dashboard
    screenId      = partner_dashboard
    templateId    = partner_dashboard_template
    templateType  = default_template
    endpoint      = /api/v1/partner/sdui/registry/partner_dashboard
```

## 2.2 Normal system Back

If the user presses system Back from OTP:

```text
NavigationStore.Pop
    ↓
previous full destination = partner_login
    ↓
Dynamic feature displays/restores that destination
```

The frontend does not ask `templateType` where to go.

## 2.3 Backend-driven Back button

If a server-driven toolbar/button must go to a specific screen, backend sends a `navigate` action with the exact destination.

Do not create frontend rules such as:

```text
if form_template then go login
if dashboard_template then go dashboard
```

Those rules are forbidden.

## 2.4 Refresh

Refreshing the current screen can reuse the current `DynamicDestination` because it already contains endpoint/method/authentication.

```text
current destination
    ↓
call current endpoint again
    ↓
replace current rendered SduiScreen/state as required
```

If the backend later needs a special refresh destination or a special back-stack operation, it must be represented explicitly in the backend action contract. The frontend must not infer it.

---

# 3. Backend consistency rule

Because navigation is fully backend-driven, a backend destination must describe the actual target correctly.

Example currently found during review:

```text
partner_login actual template = form_template
```

If any backend action sends:

```text
templateType = stack_template
```

for that same target, backend should correct it.

Frontend should not weaken its destination matching or guess the real template type.

This is not Login-specific. The same rule applies to every future screen:

```text
screenId + templateId + templateType + endpoint
```

must describe one consistent target.

---

# 4. Frontend should not duplicate backend structural validation

The backend SDUI engine already validates the SDUI structure before returning it.

Therefore the frontend should **not** perform another large structural validation pass for things such as:

```text
whether a Component contains direct Elements, Sections, or both
whether a Section contains direct Elements, Groups, or both
duplicate IDs
backend property schema correctness
action schema correctness already guaranteed by the backend builder/validator
```

Doing all of that again creates duplicate rules and maintenance drift.

## 4.1 What frontend still must protect itself from

Frontend still needs a very small runtime support check because client and server versions can differ.

The class should therefore be called something like:

```text
SduiSupportChecker
```

not a large `SduiValidator`.

Its job is only:

```text
Is schemaVersion supported by this app build?
Is this template type registered?
Are all component/section/group/element types registered?
Are all received action types supported?
Are endpoints/URIs safe for client execution?
```

Why this is still needed:

```text
Backend may deploy a new element type today
but an older mobile app may not know how to render it.
```

That is a client capability problem, not duplicate structural validation.

Target flow:

```text
backend validated JSON
    ↓
frontend decode
    ↓
SduiSupportChecker
    ↓
supported → render
unsupported → controlled unsupported-screen/protocol failure
```

---

# 5. Current module/package structure after the approved layout convergence

The SDUI engine remains one cohesive module. The current structural-rendering shape is intentionally small:

```text
sdui/
└── src/commonMain/kotlin/com/carbroz/sdui/
    ├── model/
    │   ├── SduiScreen.kt
    │   ├── SduiTemplate.kt
    │   ├── SduiComponent.kt
    │   ├── SduiSection.kt
    │   ├── SduiGroup.kt
    │   ├── SduiElement.kt
    │   ├── SduiAction.kt
    │   ├── SduiValueReference.kt
    │   ├── SduiDestination.kt
    │   ├── SduiValidationRule.kt
    │   ├── SduiAccessory.kt
    │   └── SduiTheme.kt
    │
    ├── parser/
    │   ├── SduiDecoder.kt
    │   ├── SduiDecodeResult.kt
    │   ├── SduiSupportChecker.kt
    │   └── SduiVersionPolicy.kt
    │
    ├── value/
    │   ├── SduiExecutionContext.kt
    │   ├── SduiValueResolver.kt
    │   └── JsonPathResolver.kt
    │
    ├── runtime/
    │   ├── FieldState.kt
    │   ├── NodeRuntimeState.kt
    │   └── SduiOverlay.kt
    │
    ├── registry/
    │   ├── SduiNodeRegistry.kt
    │   ├── SduiNodeRegistration.kt
    │   └── SduiDefinitions.kt
    │
    ├── render/
    │   ├── SduiRenderer.kt
    │   ├── SduiRenderContext.kt
    │   ├── SduiInteraction.kt
    │   ├── StructuralNodeRenderers.kt
    │   ├── layout/
    │   │   └── ChildLayout.kt
    │   ├── modifier/
    │   │   └── SduiModifierResolver.kt
    │   ├── accessory/
    │   │   └── AccessoryRenderer.kt
    │   └── element/
    │       ├── TextElementRenderer.kt
    │       ├── ImageElementRenderer.kt
    │       ├── InputElementRenderer.kt
    │       └── ButtonElementRenderer.kt
    │
    └── testing/
        └── fixtures in commonTest, not production
```

There are no longer separate `StackTemplateRenderer`, `FormTemplateRenderer`, `DefaultTemplateRenderer`, `StackComponentRenderer`, `StackSectionRenderer`, `StackGroupRenderer`, or `StackContainerRenderer` source files merely to delegate the same child-layout algorithm.

The backend wire names remain unchanged. Removing duplicate frontend wrapper classes does **not** rename or weaken backend protocol vocabulary.

---

# 6. Every SDUI class — simple responsibility with CarBroz live-flow example

This section is intentionally written so a new developer can understand why each class exists.

## 6.1 `SduiScreen.kt`

**What it represents:** the complete screen returned by backend.

Example:

```text
partner_login screen arrives from API
    ↓
SduiDecoder creates SduiScreen
```

It contains screen identity, schema version, template, theme and metadata.

It does not call APIs, hold Compose state, or navigate.

---

## 6.2 `SduiTemplate.kt`

**What it represents:** the top UI layout of one screen.

Examples:

```text
Login → form_template
Dashboard → default_template
Future simple page → stack_template
```

The renderer uses its `type` to select the correct registered template renderer contract.

---

## 6.3 `SduiComponent.kt`

**What it represents:** a major block inside a template.

Frozen child contract:

```text
Component
  ├── direct Element(s)
  └── Section(s)
```

Direct Elements and Sections may coexist in the same Component. They are not mutually exclusive.

It only holds backend data. Rendering happens elsewhere.

---

## 6.4 `SduiSection.kt`

**What it represents:** a grouping level inside a component when backend needs more structure.

Frozen child contract:

```text
Section
  ├── direct Element(s)
  └── Group(s)
```

Direct Elements and Groups may coexist in the same Section. They are not mutually exclusive.

---

## 6.5 `SduiGroup.kt`

**What it represents:** the final structural group before elements.

```text
Group
  └── Element(s)
```

---

## 6.6 `SduiElement.kt`

**What it represents:** something the user actually sees or interacts with.

Examples:

```text
text
image
input
button
```

It can contain:

```text
properties
actions
binding
validation
accessibility
metadata
```

Example Login button:

```text
SduiElement(type = button)
    ↓
ButtonElementRenderer
    ↓
user sees Continue button
```

---

## 6.7 `SduiAction.kt`

**What it represents:** exactly what backend wants to happen when an interaction occurs.

Supported actions:

```text
request
navigate
present
dismiss
state
external_uri
sequence
```

Example:

```text
Continue button clicked
    ↓
element.actions["onClick"]
    ↓
SduiAction.Request
    ↓
SduiActionExecutor
```

Do not create `Command`, `PreparedAction`, `ParentAction`, or `ChildAction` copies.

`sequence` is naturally the parent/composite action:

```text
sequence
  ├── request
  ├── state
  └── navigate
```

---

## 6.8 `SduiValueReference.kt`

**What it represents:** a value that backend wants frontend to resolve at runtime.

Examples:

```json
{ "$binding": "mobileNumber" }
{ "$context": "deviceId" }
{ "$response": "data.challengeId" }
{ "$literal": "PARTNER" }
```

Example OTP verification:

```text
backend request body asks for:
challengeId = $response(data.challengeId)
otp         = $binding(otp)
deviceId    = $context(deviceId)
    ↓
SduiValueResolver returns concrete values
```

Its continued need as a separate typed model remains an open audit item; the active resolver behavior itself remains frozen to the four reference forms above.

---

## 6.9 `SduiDestination.kt`

**What it represents:** the backend-defined destination payload used by actions.

It mirrors:

```text
screenId
templateId
templateType
endpoint
method
authentication
```

It does not decide stack behavior by template type.

---

## 6.10 `SduiValidationRule.kt`

**What it represents:** backend field validation rules that frontend executes before a request when `validate = true`.

Initial supported scope:

```text
required
pattern
message
```

---

## 6.11 `SduiAccessory.kt`

Leading/trailing must be generic and reusable, not Text-only.

An accessory may be:

```text
icon
divider
image
future badge
future loader
```

Any element definition that supports accessories can expose the same contract.

Do not hardcode reusable accessory infrastructure into only one element renderer.

---

## 6.12 `SduiTheme.kt`

**What it represents:** theme values sent by backend for the screen.

It is data only.

Conversion to Compose/design-system values belongs in rendering helpers.

---

# 7. Parser classes

## 7.1 `SduiDecoder.kt`

**Job:** convert backend `data` JSON into typed `SduiScreen`.

Rules:

- accept `JsonElement`; do not convert JSON → String → JSON again;
- fail cleanly if JSON cannot be decoded;
- keep backend field names;
- decode action unions/value-reference unions explicitly.

---

## 7.2 `SduiDecodeResult.kt`

**Job:** make decode success/failure explicit.

```kotlin
sealed interface SduiDecodeResult {
    data class Success(val screen: SduiScreen) : SduiDecodeResult
    data class Failure(val reason: String) : SduiDecodeResult
}
```

---

## 7.3 `SduiSupportChecker.kt`

**Job:** check whether this frontend build knows how to execute/render what backend sent.

It does **not** repeat backend structural validation.

Checks:

```text
schema supported?
template registered?
components registered?
sections registered?
groups registered?
elements registered?
action types supported?
unsafe absolute API endpoint rejected?
```

Concrete embedded-action/accessory capability ownership remains a separate class-audit discussion and is not changed by the layout refactor.

---

## 7.4 `SduiVersionPolicy.kt`

**Job:** answer one question:

```text
Does this app version support schemaVersion X?
```

Keep it tiny.

---

# 8. Value-resolution classes

## 8.1 `SduiExecutionContext.kt`

**Job:** provide runtime data actions may reference.

Also contains current bindings and latest response.

It is an input to the resolver; it is not another state owner.

---

## 8.2 `SduiValueResolver.kt`

**Job:** convert references into concrete JSON values.

It recursively resolves `$binding`, `$context`, `$response`, and `$literal` inside nested request bodies.

---

## 8.3 `JsonPathResolver.kt`

**Job:** one small reusable helper for paths such as:

```text
authFlow.phoneNumber
data.challengeId
legal.termsUri
```

No SDUI action logic belongs here.

---

# 9. Runtime state classes

## 9.1 `FieldState.kt`

**Job:** current value/error/touched state of one bound input.

There must not be a second FormStore owning the same value.

---

## 9.2 `NodeRuntimeState.kt`

**Job:** hold client-side changes applied by backend `state` actions without mutating the immutable server screen.

Possible fields:

```text
visible
enabled
selected
expanded
checked
loading
value
```

The audit finding around `value` versus canonical bound `FieldState.value` remains open and is not part of the approved layout pass.

---

## 9.3 `SduiOverlay.kt`

**Job:** describe what is currently being presented over the screen.

`DismissAction` clears the correct overlay.

---

# 10. Registry design — hierarchy-safe registration with shared structural rendering

The goal is readability, preventing accidental duplicate/wrong-level registration, and avoiding hierarchy × layout renderer multiplication.

Keep the hierarchy-specific registry interfaces and maps. Register current structural wire types through the shared structural renderer factories:

```kotlin
object TemplateDefinitions {
    val all = listOf(
        structuralTemplateRenderer("stack_template"),
        structuralTemplateRenderer("form_template"),
        structuralTemplateRenderer("default_template"),
    )
}

object ComponentDefinitions {
    val all = listOf(
        structuralComponentRenderer("stack_component"),
    )
}

object SectionDefinitions {
    val all = listOf(
        structuralSectionRenderer("stack_section"),
    )
}

object GroupDefinitions {
    val all = listOf(
        structuralGroupRenderer("stack_group"),
    )
}
```

Element definitions remain concrete because Text/Image/Input/Button have genuinely different leaf rendering behavior.

`SduiNodeRegistry` continues to reject duplicate types inside the same hierarchy.

A future semantic node type gets a dedicated renderer only if it has real type-specific behavior beyond the reusable structural child-layout path.

---

# 11. Rendering classes — current responsibilities

## 11.1 `SduiRenderer.kt`

This is the **traffic controller for drawing one SDUI screen**.

Its job is only:

```text
read node type
find correct hierarchy renderer
call it
walk every present valid child branch
```

For Component and Section, both additive child branches may be present and must both be traversed.

`SduiRenderer` does not know Login business logic and does not choose child-layout algorithms itself.

---

## 11.2 `SduiRenderContext.kt`

This is the small read-only runtime bag renderers receive while drawing.

The context does not execute actions or mutate state itself.

---

## 11.3 `SduiInteraction.kt`

This represents what the user just did in rendered SDUI.

The renderer carries the backend action directly; do not introduce a second NodePath/Command lookup system.

---

## 11.4 `SduiModifierResolver.kt`

**Job:** convert common backend self-presentation values into Compose modifiers.

Examples:

```text
fillMaxWidth
width / height
padding
shape
background
border
```

This concern is separate from child arrangement.

---

## 11.5 `StructuralNodeRenderers.kt`

**Job:** preserve hierarchy-specific renderer contracts while avoiding duplicate wrapper classes when the current semantic types share the same structural rendering behavior.

It supplies shared factories for Template, Component, Section, and Group renderer interfaces and delegates node properties/children to the common structural rendering path.

It does not change backend wire type names.

---

## 11.6 `render/layout/ChildLayout.kt`

**Job:** own reusable child arrangement.

Current linear layout properties are:

```text
axis
spacing
mainAxisAlignment
crossAxisAlignment
```

The implementation resolves them into one `LinearChildLayoutSpec` and chooses Row/Column internally. Positional main-axis alignment (`start`, `center`, `end`) can now coexist with nonzero fixed spacing instead of spacing silently replacing alignment.

A future genuinely different algorithm such as grid, overlay, or flow should be added here/as a sibling layout strategy once and reused by allowed structural nodes. Do not create GridTemplate + GridComponent + GridSection + GridGroup copies.

---

# 12. Accessory rendering — leading/trailing are reusable across supported elements

Accessory implementations are shared across owning elements.

The capability/failure behavior for unknown accessory vocabulary remains a separate audit item and is intentionally not bundled into the layout refactor.

---

# 13. Template/component/section/group/element renderer responsibilities — FROZEN STRUCTURAL RULE

Every renderer follows the same rule:

> read only its own node properties/semantic behavior, render only its own level, and delegate children to the shared SDUI renderer/layout machinery.

## Template

A Template is the top structural node for a screen and delegates Components. Current `stack_template`, `form_template`, and `default_template` wire types share the same structural child-layout implementation because no distinct rendering behavior is currently required by those source contracts.

If a future Template type has genuine semantic behavior beyond child arrangement, it may receive a dedicated Template renderer without duplicating the underlying layout algorithm.

## Component

A Component may contain both branches simultaneously:

```text
Component
  ├── direct Element(s)
  └── Section(s)
```

Both are rendered when present.

## Section

A Section may contain both branches simultaneously:

```text
Section
  ├── direct Element(s)
  └── Group(s)
```

Both are rendered when present.

## Group

```text
Group
  └── Element(s)
```

## Element

An Element is a leaf in the current protocol. A leaf renderer owns its actual visual/interactable implementation and emits `SduiInteraction` for supported events.

No renderer directly executes network/navigation.

---

# 14. How a new developer adds SDUI vocabulary

## 14.1 Add a new semantic Template type

First decide whether the new wire type requires genuinely different Template behavior.

If it only uses an already-supported structural child-layout algorithm:

```text
1. register the new wire type through the shared structural Template renderer path;
2. add support/registry/fixture tests;
3. do not create a one-line wrapper class.
```

If it has real Template-specific behavior:

```text
1. create one dedicated Template renderer for that semantic behavior;
2. reuse shared self-presentation/child-layout helpers inside it;
3. register it;
4. add tests/fixture.
```

## 14.2 Add a Component/Section/Group type

Use the same rule: shared structural renderer when behavior is only common self-presentation + child layout; a dedicated renderer only for real type-specific behavior.

## 14.3 Add a new Element

Example future `rating` element:

1. confirm backend `rating` schema and supported events;
2. create `RatingElementRenderer.kt`;
3. renderer reads runtime/bound state through `SduiRenderContext` as appropriate;
4. renderer emits `SduiInteraction`;
5. register in `ElementDefinitions.all`;
6. add renderer tests;
7. add mock fixture;
8. add end-to-end interaction test when required.

Do **not** create a new feature Store, action engine, registry framework, or network adapter just because one new element is added.

---

# 15. Complete action execution flow

The action system is for the entire app, not Login/OTP only.

Supported actions remain exactly:

```text
request
navigate
present
dismiss
state
external_uri
sequence
```

Request and Navigate remain separate. Navigation always uses the full backend destination. State/presentation results reduce into the single live Dynamic screen state owner.

---

# 16. Field/binding state — one owner only

Bound input values live only in `DynamicScreenState`.

No local canonical `remember` value plus FormStore plus DynamicStore duplication.

The remaining audit concern about `NodeRuntimeState.value` versus `FieldState.value` must be resolved separately before final freeze.

---

# 17. Support for the whole Partner app

Do not design around only Login and OTP.

The same engine must handle future Partner screens through backend composition, registered semantic vocabulary, generic actions, bindings, and backend destinations.

Product-specific meaning stays in backend composition/domain APIs, not hardcoded screen branching in frontend.

---

# 18. Dynamic feature ownership

The SDUI module should not own networking/navigation feature state.

`feature:dynamic` owns the live dynamic-screen lifecycle.

Detailed class-by-class documentation is kept separately in:

```text
feature/dynamic/DYNAMIC_FEATURE_ARCHITECTURE.md
```

---

# 19. Testing strategy — easy mock-driven full SDUI verification

Tests should prove both small units and complete flows.

## 19.1 Contract fixtures

Maintain realistic backend JSON fixtures in test sources. Existing `allNodeTypes` is intentionally reused for additive hierarchy proof instead of creating a duplicate fixture.

---

## 19.2 Decoder tests

Test valid/malformed JSON, hierarchy, action unions, value references, and deterministic unknown-action behavior.

---

## 19.3 Support-checker tests

Test only client capability concerns; do not duplicate backend structural-schema validation.

---

## 19.4 Registry tests

Test all hierarchy definitions, lookup, duplicate rejection, and hierarchy type safety.

---

## 19.5 Renderer/layout tests

Current approved layout regressions cover:

```text
linear default properties
horizontal axis resolution
vertical axis default
spacing retained independently from main-axis alignment
cross-axis alignment resolution
additive Component + Section child branches in the canonical fixture
additive Section + Group child branches in the canonical fixture
```

Final freeze still requires executable CI plus any additional rendering proof discovered during the remaining audit.

---

## 19.6 Value-resolver tests

Test all four references, nested object/array resolution and deterministic missing paths.

---

## 19.7 Field-state tests

Test binding updates and validation behavior.

---

## 19.8 Action-executor tests

Test all seven actions, sequence ordering/failure, request contracts, navigation/session handling and value resolution.

---

## 19.9 Navigation tests

Test full destination stack, Back, Refresh, restoration and absence of template-type routing.

---

## 19.10 Complete mock Login → OTP flow

This remains a generic integration fixture only; no Login/OTP-specific production branch is allowed.

---

## 19.11 Full vocabulary fixture

One synthetic fixture should continue covering every current registered hierarchy/node/accessory vocabulary and exact action/reference vocabulary.

---

# 20. Code-writing rules

1. One class = one reason to change.
2. No UI renderer calls network/navigation directly.
3. No backend screen name checks such as `if (screenId == "partner_login")` inside generic SDUI.
4. No navigation decisions based on `templateType`.
5. No second form state owner.
6. No `JsonElement → String → JsonElement` round trips.
7. No duplicate Command/PreparedAction models.
8. No speculative action types not defined by backend.
9. No generic manager/coordinator class without a concrete single responsibility.
10. Prefer immutable models and exhaustive `when` for action types.
11. Reusable properties/accessories belong in shared SDUI model/render helpers, not one element implementation.
12. Every new renderer must have registration + unit test + mock-fixture coverage.
13. Every new action must originate in backend protocol first, then frontend model/executor/tests.
14. Unknown/unsupported server vocabulary must fail cleanly; never silently guess.
15. Keep dynamic feature and SDUI responsibilities separate.
16. `SduiComponent.elements + sections` and `SduiSection.elements + groups` are additive child branches, not XOR alternatives.
17. Do not create hierarchy × layout wrapper classes when behavior is only a shared child-layout algorithm.

---

# 21. Implementation order

Historical convergence phases remain useful context, but the current architecture-audit correction sequence is:

```text
A. freeze additive hierarchy semantics                         DONE
B. separate structural semantic identity from child layout    SOURCE IMPLEMENTED
C. remove duplicate Stack wrapper classes                     SOURCE IMPLEMENTED
D. add hierarchy/layout regressions                           SOURCE ADDED
E. review remaining audit findings with owner one by one       NEXT
F. run configured executable CI                                BLOCKED BY RUNNER ALLOCATION
G. final exact-SHA audit + authority freeze                    PENDING
```

No remaining audit item should be bundled into the implementation merely because it was discovered during review.

---

# 22. Freeze checklist

Do not mark the whole SDUI engine frozen until all answers are yes:

```text
[x] additive Component Elements + Sections contract frozen
[x] additive Section Elements + Groups contract frozen
[x] semantic structural identity separated from reusable child-layout implementation in source
[x] hierarchy × layout Stack wrapper class multiplication removed for current shared behavior
[x] hierarchy-coexistence regression added
[x] spacing + positional main-axis alignment regression added
[ ] remaining approved class-audit corrections completed
[ ] unsupported child-layout/accessory/embedded-action vocabulary policy fully resolved
[ ] canonical bound-value ownership fully resolved
[ ] configured JVM/Android + published-foundation + iOS CI actually executes green
[ ] real backend/manual visual/auth validation completed when owner reaches that phase
```

All previously frozen generic navigation/action/state rules remain in force.

---

# 23. Final mental model for a new developer

When backend adds a new screen using existing vocabulary, usually no frontend class is added.

When backend adds a new semantic node type, first ask whether it has real behavior beyond a supported structural layout. If not, register it through the shared structural renderer path instead of creating a trivial wrapper class.

When backend adds a genuinely new child-layout algorithm, implement that algorithm once under the layout boundary and reuse it across allowed hierarchy levels.

When backend adds a new action, extend the exact protocol/executor/tests once; never create a screen-specific action engine.

---

# 24. Implemented architecture amendment — semantic node identity vs child layout

> **Status:** APPROVED AND SOURCE IMPLEMENTED for the current linear structural vocabulary. Final green status still requires executable CI and the remaining audit/freeze sequence.
>
> **Supersedes:** any Stack-specific package/renderer examples earlier than this amendment.

A structural SDUI node answers two independent questions:

1. **What semantic node is this?** — backend `type`, hierarchy level, capabilities and any truly type-specific behavior.
2. **How are this node's children arranged?** — reusable layout behavior driven by node properties.

Current implementation:

```text
Template / Component / Section / Group hierarchy contract
        ↓
StructuralNodeRenderers.kt
        ↓
shared RenderStructuralNode
        ├── self presentation → SduiModifierResolver
        └── child arrangement → render/layout/ChildLayout.kt
                                   ↓
                            LinearChildLayoutSpec
                                   ↓
                           Row / Column rendering
```

Current linear wire properties remain unchanged:

```text
axis
spacing
mainAxisAlignment
crossAxisAlignment
```

## 24.1 Frozen implementation direction

- Hierarchy-specific registry interfaces remain.
- Backend wire type names remain unchanged.
- Current structural wire types that share behavior use structural renderer factories instead of one-line Stack/Form/Default wrapper files.
- Self-presentation and child-layout remain separate concerns.
- New layout algorithms are implemented once under the layout boundary and reused.
- Dedicated semantic renderers remain allowed when a type has genuine behavior beyond common structural rendering.
- Normal Elements remain leaves unless the protocol explicitly introduces a compositional/container Element.
- `templateType` remains rendering capability identity only; never navigation.

## 24.2 Spacing and alignment correction

The previous `RenderStack` implementation selected `Arrangement.spacedBy(...)` whenever spacing was nonzero, which meant positional main-axis alignment could be ignored.

The current linear layout resolves spacing and alignment independently. For positional `start`, `center`, and `end`, fixed spacing is combined with the requested alignment through the alignment-aware spaced arrangement.

Distributed modes (`spaceBetween`, `spaceAround`, `spaceEvenly`) remain distribution strategies rather than being converted into positional alignment.

Any stronger backend contract requiring a simultaneous explicit minimum gap plus distributed free-space behavior must be defined explicitly before frontend invents such semantics.

## 24.3 Finalized current names

The current implementation names are now:

```text
render/StructuralNodeRenderers.kt
render/layout/ChildLayout.kt
LinearChildLayoutSpec
RenderChildLayout
```

These names describe responsibility rather than a specific hierarchy level. They can be revisited only if later real protocol requirements prove a clearer abstraction; there is no reason to recreate Stack-specific wrapper classes.

## 24.4 Remaining freeze gates

```text
[x] semantic structural-node identity separated from reusable child-layout behavior in source
[x] current hierarchy × linear-layout wrapper explosion removed
[x] hierarchy coexistence regression added
[x] spacing + alignment resolution regression added
[ ] executable configured CI runs successfully on the implementation
[ ] unsupported child-layout/accessory/embedded-action capability behavior fully resolved
[ ] remaining owner-approved audit corrections implemented
[ ] final exact-SHA architecture/stale-code audit completed
```

---

# 25. Frozen hierarchy child-composition contract

> **Status:** FROZEN — this specific hierarchy rule is approved and is no longer under architecture review.
>
> **Supersedes:** any earlier `OR` wording that could be read as mutual exclusion between the child collections below.

The canonical structural hierarchy is:

```text
Screen
  └── Template
      └── Component
          ├── Element(s)
          └── Section(s)
              ├── Element(s)
              └── Group(s)
                  └── Element(s)
```

The child branches are additive, not exclusive:

- `SduiComponent.elements` and `SduiComponent.sections` may both be present in the same Component;
- `SduiSection.elements` and `SduiSection.groups` may both be present in the same Section;
- `SduiGroup` contains Elements;
- nullable/optional child collections indicate optional presence in the backend payload and do **not** define an XOR/either-or relationship;
- every present valid child branch must be traversed by rendering, support/capability inspection and any shared hierarchy utility;
- the frontend must not remodel Component or Section into a mutually exclusive sealed child union that prevents these combinations;
- whether a structurally valid node is allowed to contain no children at all remains a backend structural-validation concern unless the protocol explicitly assigns that rule to the client.

Therefore the current `SduiComponent` and `SduiSection` model shape is accepted for this hierarchy contract. The earlier audit concern that the nullable child collections inherently represented impossible in-memory combinations is withdrawn.

Current regression status:

```text
[x] canonical fixture contains Component direct Element(s) + Section(s)
[x] canonical fixture contains Section direct Element(s) + Group(s)
[x] decoder preserves both branches in the regression
[x] support checker accepts/visits both present branches in the regression
[ ] dedicated renderer traversal execution proof remains part of final executable verification
```
