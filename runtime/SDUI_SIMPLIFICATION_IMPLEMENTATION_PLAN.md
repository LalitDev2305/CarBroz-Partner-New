# CarBroz Partner Frontend — SDUI Simplification & Implementation Plan

> **Status:** REVIEW DRAFT — hierarchy child-composition contract frozen; architecture re-audit otherwise open and not fully frozen yet.
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
whether a Component uses direct Elements, Sections, or both
whether a Section uses direct Elements, Groups, or both
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

# 5. Proposed module/package structure

The SDUI engine should be one cohesive module. If Gradle migration risk is high, implementation can initially keep the current module coordinate and converge packages first.

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
    │   ├── TemplateDefinitions.kt
    │   ├── ComponentDefinitions.kt
    │   ├── SectionDefinitions.kt
    │   ├── GroupDefinitions.kt
    │   └── ElementDefinitions.kt
    │
    ├── render/
    │   ├── SduiRenderer.kt
    │   ├── SduiRenderContext.kt
    │   ├── SduiInteraction.kt
    │   ├── modifier/
    │   │   └── SduiModifierResolver.kt
    │   ├── accessory/
    │   │   ├── AccessoryRenderer.kt
    │   │   ├── IconAccessoryRenderer.kt
    │   │   └── DividerAccessoryRenderer.kt
    │   ├── template/
    │   │   ├── FormTemplateRenderer.kt
    │   │   ├── StackTemplateRenderer.kt
    │   │   └── DefaultTemplateRenderer.kt
    │   ├── component/
    │   │   └── StackComponentRenderer.kt
    │   ├── section/
    │   │   └── StackSectionRenderer.kt
    │   ├── group/
    │   │   └── StackGroupRenderer.kt
    │   └── element/
    │       ├── TextElementRenderer.kt
    │       ├── ImageElementRenderer.kt
    │       ├── InputElementRenderer.kt
    │       └── ButtonElementRenderer.kt
    │
    └── testing/
        └── fixtures in commonTest, not production
```

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

The renderer uses its `type` to select the correct registered template renderer.

---

## 6.3 `SduiComponent.kt`

**What it represents:** a major block inside a template.

Frozen child contract:

```text
Component
  ├── zero or more direct Element(s)
  └── zero or more Section(s)
```

Direct Elements and Sections may coexist in the same Component. They are **not mutually exclusive**. The nullable collections reflect optional presence in the backend payload; the frontend must not reinterpret them as an either/or union.

It only holds backend data. Rendering happens elsewhere.

---

## 6.4 `SduiSection.kt`

**What it represents:** a grouping level inside a component when backend needs more structure.

Frozen child contract:

```text
Section
  ├── zero or more direct Element(s)
  └── zero or more Group(s)
```

Direct Elements and Groups may coexist in the same Section. They are **not mutually exclusive**.

Example Dashboard:

```text
Dashboard component
  ↓
active-bookings section
  ├── direct elements when required
  └── booking-card groups
```

---

## 6.5 `SduiGroup.kt`

**What it represents:** the final structural group before elements.

Frozen child contract:

```text
Group
  └── Element(s)
```

Example OTP:

```text
otp fields section
  ↓
otp fields group
  ↓
segmented input element
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

Example:

```text
mobileNumber = 123
    ↓
Continue clicked
    ↓
request.validate = true
    ↓
field rule fails
    ↓
show error
    ↓
do not call API
```

---

## 6.11 `SduiAccessory.kt`

Leading/trailing must be generic and reusable, not Text-only.

```kotlin
data class SduiAccessories(
    val leading: List<SduiAccessory> = emptyList(),
    val trailing: List<SduiAccessory> = emptyList(),
)
```

An accessory may be:

```text
icon
divider
image
future badge
future loader
```

Any element definition that supports accessories can expose the same contract.

Examples:

```text
Text + leading divider + trailing divider
Button + trailing icon
Image + trailing badge/icon in future
Input + leading country flag/icon
```

Do not hardcode `leading/trailing` into only `TextElementProperties`.

---

## 6.12 `SduiTheme.kt`

**What it represents:** theme values sent by backend for the screen.

It is data only.

Conversion to Compose/design-system values belongs in rendering helpers.

---

# 7. Parser classes

## 7.1 `SduiDecoder.kt`

**Job:** convert backend `data` JSON into typed `SduiScreen`.

Live flow:

```text
GET /partner/screen/...
    ↓
NetworkResponse.body
    ↓
API envelope.data
    ↓
SduiDecoder.decode(data)
    ↓
SduiScreen
```

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

It prevents throwing random serialization exceptions through the UI layer.

---

## 7.3 `SduiSupportChecker.kt`

**Job:** check whether this frontend build knows how to execute/render what backend sent.

It does **not** repeat backend structural validation.

Example:

```text
backend sends element type = map_live_tracking
old frontend has no renderer registered
    ↓
SduiSupportChecker detects unsupported element
    ↓
controlled unsupported-screen result
```

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

Example Partner app context:

```json
{
  "deviceId": "device-123",
  "authFlow": {
    "phoneNumber": "9876543210"
  },
  "legal": {
    "termsUri": "...",
    "privacyUri": "..."
  }
}
```

Also contains current bindings and latest response.

It is an input to the resolver; it is not another state owner.

---

## 8.2 `SduiValueResolver.kt`

**Job:** convert references into concrete JSON values.

Example:

```text
$binding(otp)
    ↓
DynamicScreenState.fields["otp"].value
```

```text
$response(data.challengeId)
    ↓
lastResponse.data.challengeId
```

It should recursively resolve references inside nested request bodies.

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

Example:

```text
binding key = mobileNumber
value       = 9876543210
error       = null
```

There must not be a second FormStore owning the same value.

---

## 9.2 `NodeRuntimeState.kt`

**Job:** hold client-side changes applied by backend `state` actions without mutating the immutable server screen.

Example:

```text
backend action:
state(targetId = resend, property = enabled, value = true)
    ↓
nodeStates["resend"].enabled = true
    ↓
renderer recomposes
```

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

---

## 9.3 `SduiOverlay.kt`

**Job:** describe what is currently being presented over the screen.

Example:

```text
backend PresentAction(targetId = booking_cancel_sheet, bottom_sheet)
    ↓
overlay = bottom_sheet(targetId)
```

`DismissAction` clears the correct overlay.

---

# 10. Registry design — separated by hierarchy, registered through one obvious entry point

The goal is readability and preventing accidental duplicate/wrong-level registration.

Do not maintain one long mixed list containing templates, components, sections, groups and elements together.

Use hierarchy-specific definition collections.

`SduiNodeRegistry` should reject duplicate types inside the same hierarchy.

The hierarchy-safe registry remains approved. Layout implementation must not require a separate renderer class for every hierarchy × layout combination when the behavior is identical.

---

# 11. Rendering classes — why they exist, using live app flow

## 11.1 `SduiRenderer.kt`

This is the **traffic controller for drawing one SDUI screen**.

`SduiRenderer` does not know Login business logic.

Its job is only:

```text
read node type
find correct renderer
call it
walk all valid children
```

For Component and Section, "all valid children" means both direct Elements and the nested structural collections may be present and must both be traversed.

---

## 11.2 `SduiRenderContext.kt`

This exists because renderers need a small amount of runtime information, but we do not want every renderer to depend directly on `DynamicScreenStore`.

The context does not execute actions or mutate state itself.

---

## 11.3 `SduiInteraction.kt`

This represents **what the user just did in the rendered SDUI**.

The renderer carries the backend action directly; do not introduce a second command lookup system.

---

## 11.4 `SduiModifierResolver.kt`

**Job:** convert common backend layout/appearance values into Compose modifiers.

Renderer asks the modifier resolver instead of reimplementing spacing/size/border rules in every element.

---

# 12. Accessory rendering — leading/trailing are reusable across supported elements

Accessory implementations are shared across owning elements. The owner element decides whether it supports accessories; the accessory implementation is shared.

---

# 13. Template/component/section/group/element renderer responsibilities

Every renderer follows the same rule:

> read only its own node properties, render only its own level, and delegate children to the shared SDUI renderer.

## Template renderer

A Template renders its own semantic/template behavior and delegates Components.

## Component renderer

A Component may render **direct Elements and Sections in the same node**. Those collections are AND/OR, not XOR.

```text
Component
  ├── Element(s)
  └── Section(s)
```

## Section renderer

A Section may render **direct Elements and Groups in the same node**.

```text
Section
  ├── Element(s)
  └── Group(s)
```

## Group renderer

A Group delegates its Elements.

## Element renderer

An Element renders its visual/interactable leaf behavior and emits `SduiInteraction` for supported events. No Element renderer directly executes network/navigation.

---

# 14. How a new developer adds a new SDUI type

New semantic node types should be added only when the backend contract introduces genuinely different behavior. A new child-layout algorithm must be implemented once and reused across structural hierarchy levels where allowed; do not generate hierarchy × layout class matrices.

New elements still follow the normal renderer + registration + test/fixture path.

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

---

# 17. Support for the whole Partner app

The engine must remain product-neutral and support future Partner screens through backend composition rather than frontend screen branching.

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

In addition to existing fixtures/tests, the frozen hierarchy contract requires a regression fixture/test containing:

```text
one Component with BOTH direct Element(s) and Section(s)
one Section with BOTH direct Element(s) and Group(s)
```

The test must prove decoder/support/traversal preserve and visit both branches. This is a protocol capability test, not a client-side structural-validation rule.

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
16. `SduiComponent.elements` + `sections` and `SduiSection.elements` + `groups` are intentionally coexistent child branches; never remodel them as mutually exclusive alternatives.

---

# 21. Implementation order after this document is approved

The previous convergence phases remain historical execution order. For the current audit-correction pass the immediate order is:

```text
A. freeze hierarchy child-composition semantics
B. refactor child-layout ownership away from Stack-specific helper naming/wrappers
C. add hierarchy + layout regressions
D. discuss remaining audit findings one by one before changing them
E. resume executable CI/freeze sequence only after approved audit corrections
```

---

# 22. Freeze checklist

Do not mark SDUI frozen until all answers are yes:

```text
[ ] frontend consumes actual backend API envelope/data
[ ] frontend models match backend names and values
[ ] Component supports direct Elements + Sections together
[ ] Section supports direct Elements + Groups together
[ ] Group supports Elements
[ ] navigation is fully backend-destination driven
[ ] full DynamicDestination is stored/restored in navigation stack
[ ] system Back works by stack pop
[ ] refresh can reload current full destination
[ ] no templateType-based navigation decisions exist
[ ] no duplicate structural validator mirrors backend
[ ] support checker covers client compatibility only
[ ] templates/components/sections/groups/elements are registered separately and clearly
[ ] duplicate registrations fail
[ ] leading/trailing accessory model is reusable across supported element types
[ ] one canonical field/runtime state owner exists
[ ] all seven backend actions work
[ ] all four value-reference forms work
[ ] all current backend node types render
[ ] mock full-vocabulary tests pass
[ ] hierarchy coexistence regression passes
[ ] real Desktop backend flow passes
[ ] Android/iOS/Desktop CI is green
```

---

# 23. Final mental model for a new developer

A new backend-driven screen should normally need no new Dynamic feature class. New semantic SDUI vocabulary is added only at the correct SDUI layer, registered once and tested.

---

# 24. Architecture amendment — semantic node identity vs child layout

A structural SDUI node answers two independent questions:

1. **What semantic node is this?** — its backend `type`, hierarchy level, capabilities and any truly type-specific behavior.
2. **How are this node's children arranged?** — a reusable child-layout concern driven by the node's properties.

These concerns must not be multiplied together.

```text
Template / Component / Section / Group
        │
        ├── self presentation
        │     size / padding / background / border / shape / etc.
        │             ↓
        │     shared node-property/modifier handling
        │
        └── child arrangement
              axis / spacing / main-axis alignment / cross-axis alignment
                              ↓
                    reusable child-layout implementation
```

The properties:

```text
axis
spacing
mainAxisAlignment
crossAxisAlignment
```

represent a **linear/axis child-layout algorithm**. They are not inherently Template, Component, Section or Group properties and do not justify a separate Stack renderer implementation at every hierarchy level.

## 24.1 Required direction

- Keep semantic node identity separate from child-layout behavior.
- Keep hierarchy-safe registration.
- Reuse one child-layout implementation across hierarchy levels whenever they share the same arrangement contract.
- A future genuinely different layout algorithm such as grid, overlay or flow should be implemented once and reused where the backend contract allows it.
- Do not create a hierarchy × layout matrix merely to choose a layout algorithm.
- A semantic node type may still have its own renderer when it has real type-specific behavior beyond generic child arrangement.
- Generic self-presentation properties remain separate from child arrangement.
- Normal leaf Elements remain leaves unless the protocol explicitly introduces a compositional/container Element.
- `templateType` remains backend semantic/render-capability identity. It must never become a navigation rule and must not be assumed to equal one child-layout algorithm.

## 24.2 Correctness requirements

- spacing and main-axis alignment must be able to work together rather than one silently disabling the other;
- unsupported layout vocabulary must fail through the client support/capability boundary instead of silently guessing;
- node self modifiers and child arrangement must have consistent ownership;
- adding another layout algorithm must not require duplicate implementations at Template, Component, Section and Group levels.

---

# 25. Frozen hierarchy child-composition contract

> **Status:** FROZEN — this specific hierarchy rule is no longer under review.
>
> This section supersedes any earlier wording that could be read as `Element OR Section` or `Element OR Group` exclusivity.

The canonical hierarchy is:

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

The branches are additive:

- a `Component` may contain direct Elements, Sections, or both;
- a `Section` may contain direct Elements, Groups, or both;
- a `Group` contains Elements;
- optional child collections do not imply mutual exclusion;
- the frontend must traverse every present valid branch;
- the frontend must not introduce an XOR/sealed-child model that prevents coexistence;
- whether an otherwise valid structural node may be completely empty remains a backend structural-validation concern unless the wire contract later explicitly assigns that check to the client.

Therefore the current `SduiComponent` and `SduiSection` child model shape is accepted for this contract. The previous audit claim that these nullable collections inherently represented impossible in-memory combinations is withdrawn.
