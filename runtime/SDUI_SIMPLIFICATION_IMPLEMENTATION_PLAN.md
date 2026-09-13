# CarBroz Partner Frontend — SDUI Simplification & Implementation Plan

> **Status:** FREEZE CANDIDATE — frozen hierarchy, explicit renderer registration and all approved A–I source-audit corrections are implemented. Source convergence commit: `1ac4909613e68c3524633fef982c4dda34650ef0`. Exact-head executable multiplatform CI is the only remaining freeze activation gate.
>
> **Goal:** build one simple, scalable, fully dynamic SDUI runtime for the whole CarBroz Partner app. The frontend must render and execute what the backend sends; it must not hardcode Login/OTP behavior or infer product navigation from template types.

---

# 1. First briefing — what the frontend really has to do

After Splash finishes, the backend gives the frontend the first dynamic destination. From that point the app is driven by backend SDUI.

The frontend only needs to do these jobs:

1. call the endpoint for the current dynamic destination;
2. unwrap the API `data` field;
3. decode the SDUI screen JSON;
4. check that the frontend knows the schema/node/action/capability vocabulary it received;
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
registered concrete renderer for backend node type
  ↓
shared modifier/layout/accessory helpers where applicable
  ↓
Compose
  ↓
User interaction
  ↓
SduiActionExecutor
  ├── request       → NetworkDataSource
  ├── navigate      → NavigationStore
  ├── present       → DynamicScreenState overlay
  ├── dismiss       → DynamicScreenState overlay
  ├── state         → DynamicScreenState field/node state
  ├── external_uri  → CapabilityRegistry internally
  └── sequence      → execute child actions in order
```

There should not be an unnecessary chain like:

```text
DTO → deep structural validator → compatibility engine → normalizer
→ Command → ActionRegistry → ActionDefinition → PreparedAction → executor
```

There should also not be an indirect rendering path such as:

```text
backend type string
  ↓
generic renderer factory
  ↓
anonymous structural renderer
  ↓
Compose
```

For supported node vocabulary, backend type identity must remain obvious in code through a concrete registered renderer.

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
SduiRenderer resolves destination.templateType through Template registry
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
whether a component is structurally allowed to be empty
whether a section is structurally allowed to be empty
duplicate IDs
backend property schema correctness
action schema correctness already guaranteed by the backend builder/validator
```

Doing all of that again creates duplicate rules and maintenance drift.

Important frozen hierarchy clarification:

```text
Component may contain direct Element(s), Section(s), or BOTH.
Section may contain direct Element(s), Group(s), or BOTH.
Group contains Element(s).
```

The frontend must preserve every present valid branch; it must not reinterpret these collections as mutually exclusive.

## 4.1 What frontend still must protect itself from

Frontend still needs a very small runtime support check because client and server versions can differ.

The class is:

```text
SduiSupportChecker
```

not a large `SduiValidator`.

Its job is client capability/security only:

```text
Is schemaVersion supported by this app build?
Is targetApp supported by the Partner app?
Is supplied theme capability actually consumed?
Is this template type registered?
Are all component/section/group/element types registered?
Are all received direct/embedded action types supported?
Are accessory types supported?
Is explicit layout vocabulary supported?
Are endpoints/URIs safe for client execution?
```

Why this is still needed:

```text
Backend may deploy a new element/accessory/layout capability today
but an older mobile app may not know how to render it safely.
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

# 5. Module/package structure

The SDUI engine is one cohesive module. The hierarchy packages remain explicit because backend node types must be easy to locate and extend.

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
    │   ├── SduiDestination.kt
    │   ├── SduiValidationRule.kt
    │   ├── SduiAccessory.kt
    │   ├── SduiTheme.kt
    │   └── SduiHierarchyTraversal.kt
    │
    ├── parser/
    │   ├── SduiDecoder.kt
    │   ├── SduiDecodeResult.kt
    │   ├── SduiEmbeddedActionReader.kt
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
    │   ├── modifier/
    │   │   └── SduiModifierResolver.kt
    │   ├── layout/
    │   │   └── ChildLayout.kt
    │   ├── accessory/
    │   │   └── AccessoryRenderer.kt
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

## 5.1 Package rule for new backend node types

A backend type has one obvious frontend home:

```text
Template type  → render/template/<TypeName>TemplateRenderer.kt
Component type → render/component/<TypeName>ComponentRenderer.kt
Section type   → render/section/<TypeName>SectionRenderer.kt
Group type     → render/group/<TypeName>GroupRenderer.kt
Element type   → render/element/<TypeName>ElementRenderer.kt
```

Shared implementation helpers live outside those semantic packages:

```text
render/layout/      → reusable child-arrangement mechanics
render/modifier/    → reusable self-presentation modifiers
render/accessory/   → reusable accessory rendering
model traversal     → reusable non-render hierarchy inspection
action reader       → reusable embedded-action extraction/decoding
```

A helper is never registered as a backend node type and must not hide which concrete class owns that backend type.

---

# 6. Every SDUI model — simple responsibility with CarBroz live-flow example

## 6.1 `SduiScreen.kt`

Represents the complete screen returned by backend.

```text
partner_login screen arrives from API
    ↓
SduiDecoder creates SduiScreen
```

It contains screen identity, schema version, template, theme and metadata. It does not call APIs, hold Compose state, or navigate.

## 6.2 `SduiTemplate.kt`

Represents the top structural node of one screen.

Examples:

```text
Login → form_template
Dashboard → default_template
Future simple page → stack_template
```

`SduiRenderer` uses `template.type` to find the registered concrete `TemplateRenderer`.

## 6.3 `SduiComponent.kt`

Represents a major block inside a template.

A Component may contain:

```text
direct Element(s)
Section(s)
OR BOTH
```

It only holds backend data. Rendering happens elsewhere.

## 6.4 `SduiSection.kt`

Represents a grouping level inside a component.

A Section may contain:

```text
direct Element(s)
Group(s)
OR BOTH
```

## 6.5 `SduiGroup.kt`

Represents the final structural group before elements.

```text
Group
  ↓
Element(s)
```

## 6.6 `SduiElement.kt`

Represents something the user sees or interacts with.

Examples:

```text
text
image
input
button
```

It can contain properties/actions/binding/validation/accessibility/metadata.

Example:

```text
SduiElement(type = button)
    ↓
Element registry
    ↓
ButtonElementRenderer
    ↓
Compose button
```

## 6.7 `SduiAction.kt`

Represents exactly what backend wants to happen when an interaction occurs.

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

Do not create `Command`, `PreparedAction`, `ParentAction`, or `ChildAction` copies.

## 6.8 Runtime value-reference wire forms

The frozen backend runtime-reference behavior is:

```json
{ "$binding": "mobileNumber" }
{ "$context": "deviceId" }
{ "$response": "data.challengeId" }
{ "$literal": "PARTNER" }
```

There is intentionally no parallel typed `SduiValueReference` production model after the approved cleanup. `SduiValueResolver` directly recognizes/resolves the exact four structured wire forms recursively. Removing the duplicate model does not change the backend JSON contract.

## 6.9 `SduiDestination.kt`

Represents the backend-defined destination payload:

```text
screenId
templateId
templateType
endpoint
method
authentication
```

It does not decide stack behavior by template type.

## 6.10 `SduiValidationRule.kt`

Represents backend field validation rules executed before a request when `validate=true`.

Current supported scope:

```text
required
pattern
message
```

## 6.11 `SduiAccessory.kt`

Leading/trailing accessory data is generic and reusable across elements that support it.

Current supported accessory vocabulary is explicit:

```text
icon
divider
image
```

`SduiSupportChecker` decodes/checks leading/trailing accessory payloads and rejects unsupported types before rendering instead of allowing an unknown accessory to disappear silently.

## 6.12 `SduiTheme.kt` and target-app capability

`SduiScreen` can decode theme/target metadata, but the client must never claim a capability it does not implement.

Current Partner-runtime support policy:

```text
targetApp = PARTNER  → supported
targetApp = GLOBAL   → supported
targetApp = CUSTOMER → unsupported in Partner runtime
non-null screen theme → unsupported until theme is actually consumed
```

This is intentionally fail-closed. Future theme support must first add real rendering/host consumption and focused tests, then relax the support check.

## 6.13 `SduiHierarchyTraversal.kt`

Provides one canonical non-render traversal of the frozen additive hierarchy for tasks such as:

```text
support checking
bound-field discovery
target element lookup
```

It preserves both Component branches and both Section branches. Compose rendering keeps its nested traversal because parent renderer/layout composition is a different responsibility.

---

# 7. Parser classes

## 7.1 `SduiDecoder.kt`

Converts backend `data` JSON into typed `SduiScreen`.

Rules:

- accept `JsonElement`;
- do not add JSON → String → JSON round trips;
- fail cleanly if JSON cannot be decoded;
- keep backend field names;
- decode action unions explicitly.

The unused String action-decoder overload is removed. JsonElement action decoding remains active for embedded actions; accessory decoding remains active for capability checking.

## 7.2 `SduiDecodeResult.kt`

Makes decode success/failure explicit.

## 7.3 `SduiSupportChecker.kt`

Checks whether this frontend build can execute/render what backend sent. It does **not** repeat backend structural validation.

Current responsibility includes:

```text
supported schema
PARTNER/GLOBAL target capability
fail-closed unconsumed theme
registered hierarchy renderer vocabulary
direct actions
embedded Text-span actions via normalized parser helper
supported leading/trailing accessories
explicit layout vocabulary
unsupported Input weight capability
safe request/navigate endpoints
GET-only dynamic navigation destination loading
```

It does not directly parse Text visual span structure; embedded-action discovery/decoding is owned by `SduiEmbeddedActionReader` and reused by Text rendering/support checking.

## 7.4 `SduiVersionPolicy.kt`

Answers one question: whether this app build supports the received schema version.

---

# 8. Value-resolution classes

## 8.1 `SduiExecutionContext.kt`

Provides runtime data actions may reference. It is an input snapshot, not another mutable state owner.

## 8.2 `SduiValueResolver.kt`

Resolves the exact four reference behaviors recursively:

```text
$binding
$context
$response
$literal
```

No second reference model is required.

## 8.3 `JsonPathResolver.kt`

Resolves small dotted paths such as `authFlow.phoneNumber` or `data.challengeId`.

---

# 9. Runtime state classes

## 9.1 `FieldState.kt`

Owns current value/error/touched state for one bound input through `DynamicScreenState.fields`.

For a bound element this is the sole mutable value source used by rendering, validation and `$binding` action resolution.

## 9.2 `NodeRuntimeState.kt`

Holds client-side state-action overrides without mutating the immutable backend screen.

Current ownership rule:

```text
state(target = bound element, property = value)
    → resolve binding
    → update FieldState.value

state(target = unbound element, property = value)
    → NodeRuntimeState.value

visible/enabled/selected/expanded/checked/loading
    → NodeRuntimeState
```

Input rendering does not prefer `NodeRuntimeState.value` over its bound `FieldState`. This prevents a displayed value from diverging from the value submitted through `$binding`.

## 9.3 `SduiOverlay.kt`

Describes the current backend-driven presentation overlay.

---

# 10. Registry design — separated by hierarchy, explicit concrete renderers

The goal is readability and preventing accidental duplicate/wrong-level registration.

Do not maintain one long mixed list containing templates, components, sections, groups and elements together.

Use hierarchy-specific definition collections containing **concrete renderer objects**:

```kotlin
object TemplateDefinitions {
    val all = listOf(
        StackTemplateRenderer,
        FormTemplateRenderer,
        DefaultTemplateRenderer,
    )
}

object ComponentDefinitions {
    val all = listOf(
        StackComponentRenderer,
    )
}

object SectionDefinitions {
    val all = listOf(
        StackSectionRenderer,
    )
}

object GroupDefinitions {
    val all = listOf(
        StackGroupRenderer,
    )
}

object ElementDefinitions {
    val all = listOf(
        TextElementRenderer,
        ImageElementRenderer,
        InputElementRenderer,
        ButtonElementRenderer,
    )
}
```

One clear registration entry point registers those lists:

```kotlin
object SduiNodeRegistration {
    fun createRegistry(): SduiNodeRegistry = SduiNodeRegistry().apply {
        registerTemplates(TemplateDefinitions.all)
        registerComponents(ComponentDefinitions.all)
        registerSections(SectionDefinitions.all)
        registerGroups(GroupDefinitions.all)
        registerElements(ElementDefinitions.all)
    }
}
```

The registry flow is therefore:

```text
JSON node.type
    ↓
hierarchy-specific registry lookup
    ↓
concrete renderer object
    ↓
renderer reads that node's properties
    ↓
shared helper(s) if applicable
    ↓
Compose
```

Forbidden registration style for supported semantic node types:

```kotlin
structuralTemplateRenderer("form_template")
structuralComponentRenderer("stack_component")
```

because that hides the explicit backend-type implementation behind a generic factory and makes future type-specific behavior less discoverable.

`SduiNodeRegistry` must reject duplicate types inside the same hierarchy.

---

# 11. Rendering classes — complete runtime flow

## 11.1 `SduiRenderer.kt`

`SduiRenderer` is the traffic controller for drawing one SDUI screen.

Example:

```text
DynamicScreen receives SduiScreen
    ↓
SduiRenderer reads template.type
    ↓
Template registry returns FormTemplateRenderer
    ↓
FormTemplateRenderer renders its own node/container
    ↓
SduiRenderer traverses all Component children
    ↓
Component registry resolves each concrete Component renderer
    ↓
Component renderer draws own container
    ↓
SduiRenderer traverses BOTH direct Elements and Sections when present
    ↓
Section renderer draws own container
    ↓
SduiRenderer traverses BOTH direct Elements and Groups when present
    ↓
Group renderer draws own container
    ↓
SduiRenderer traverses Elements
    ↓
Element registry resolves Text/Image/Input/Button/etc.
```

`SduiRenderer` does not know Login business logic.

Its job is only:

```text
read node type
find correct registered renderer
call it
walk every allowed child branch for compositional rendering
```

A missing registered type should already have been rejected by `SduiSupportChecker`; renderer fallback remains defensive rather than a place to guess unsupported behavior.

`RenderTarget` may traverse the hierarchy to render an overlay target because it must preserve the correct concrete renderer entry point; non-render inspection elsewhere uses `SduiHierarchyTraversal`.

## 11.2 `SduiRenderContext.kt`

Provides renderers a small read-only projection of fields/node runtime state/interactions without coupling renderers directly to `DynamicScreenStore`.

## 11.3 `SduiInteraction.kt`

Represents what the user did:

```text
ValueChanged
ActionTriggered(actual backend SduiAction)
```

The renderer emits interaction; it does not execute network/navigation itself.

## 11.4 `SduiModifierResolver.kt`

Owns reusable **self-presentation** properties such as:

```text
fill/size/min/max
padding
background/gradient
border
shape
```

It is separate from child arrangement.

Renderers must not add conflicting hardcoded self-padding/size semantics on top of this ownership unless the component contract explicitly defines them.

## 11.5 `ChildLayout.kt`

Owns reusable **child-arrangement mechanics** for the currently supported linear contract:

```text
axis
spacing
mainAxisAlignment
crossAxisAlignment
```

It may choose Row/Column and resolve alignment/spacing behavior. It is an implementation helper, not a backend node renderer and not a registry entry.

The helper preserves both spacing and main-axis alignment semantics where Compose permits that contract.

Omitted layout properties use the frozen defaults. Explicit unknown axis/alignment values and invalid spacing fail client capability checking rather than silently defaulting.

---

# 12. Accessory rendering

Accessory rendering is reusable across supported elements. The owner element decides whether it supports leading/trailing accessories; shared rendering lives under `render/accessory/`.

Do not create Text-specific copies of generic accessory rendering.

Current supported accessory types are:

```text
icon
divider
image
```

`SduiSupportChecker` validates leading/trailing accessory payloads against that capability before rendering. Unsupported accessory vocabulary fails closed.

Accessory image URL values use the existing runtime value-resolution path where applicable; accessory rendering remains a rendering concern, not a network/action owner.

---

# 13. Template / Component / Section / Group / Element renderer responsibilities

Every backend node type has a concrete renderer class in the matching hierarchy package.

Every renderer follows the same boundary:

> own the semantic backend type and its type-specific rendering behavior; read only its own node; use shared helpers for common mechanics; receive child content from `SduiRenderer` rather than performing product navigation/network logic.

## Template renderer

Example `FormTemplateRenderer`:

```text
JSON template.type = form_template
    ↓
Template registry
    ↓
FormTemplateRenderer
    ↓
read Form Template properties
    ↓
apply shared self-modifier helper
    ↓
apply shared ChildLayout when that template uses the common linear contract
    ↓
render children supplied by SduiRenderer
```

`FormTemplateRenderer` remains a real class/object even if its current implementation delegates entirely to shared helpers. That object is the explicit implementation point for `form_template` and can later gain form-template-specific behavior without changing the registration architecture.

## Component renderer

Example `StackComponentRenderer`:

```text
JSON component.type = stack_component
    ↓
Component registry
    ↓
StackComponentRenderer
    ↓
read component.properties
    ↓
shared modifier + ChildLayout
    ↓
child content supplied by SduiRenderer contains:
   direct Element(s)
   AND/OR
   Section(s)
```

The renderer does not choose between Elements and Sections. `SduiRenderer` traverses every present branch.

## Section renderer

Example `StackSectionRenderer`:

```text
JSON section.type = stack_section
    ↓
Section registry
    ↓
StackSectionRenderer
    ↓
shared modifier + ChildLayout
    ↓
child content supplied by SduiRenderer contains:
   direct Element(s)
   AND/OR
   Group(s)
```

## Group renderer

Example `StackGroupRenderer`:

```text
JSON group.type = stack_group
    ↓
Group registry
    ↓
StackGroupRenderer
    ↓
shared modifier + ChildLayout
    ↓
Element children
```

## Element renderer

Example button:

```text
JSON element.type = button
    ↓
Element registry
    ↓
ButtonElementRenderer
    ↓
read own properties/accessories/runtime state
    ↓
render Compose button
    ↓
onClick emits SduiInteraction.ActionTriggered
```

Common approved consistency rules:

- runtime enabled state overrides property enabled; property enabled falls back to true;
- bound Input value comes only from the canonical FieldState;
- resolvable Text/Button/Image/Input/accessory content uses the existing value-resolution ownership where supported;
- disabled Text falls back to normal color when `disabledColor` is absent;
- Image accessibility label resolves through the same content path rather than raw JSON stringification;
- Input `weight` is not guessed as `fillMaxWidth`; the currently unsupported capability is rejected by support checking;
- Button does not add hidden hardcoded padding after the common modifier resolver.

No renderer directly executes network/navigation.

---

# 14. How a new developer adds a new SDUI type

This extension workflow is **frozen** because it is the maintainability model for future CarBroz screens.

## 14.1 Add a new Template type

Suppose backend introduces:

```text
type = dashboard_template
```

Developer does:

```text
1. confirm backend JSON contract for dashboard_template;
2. create render/template/DashboardTemplateRenderer.kt;
3. set override val type = "dashboard_template";
4. implement dashboard-template-specific rendering;
5. reuse SduiModifierResolver / ChildLayout / other shared helpers where applicable;
6. add DashboardTemplateRenderer to TemplateDefinitions.all;
7. add registry/support/regression tests and representative fixture coverage.
```

Nothing in Dynamic navigation changes just because a new Template renderer exists.

## 14.2 Add a new Component type

Example backend type:

```text
card_component
```

Developer does:

```text
1. create render/component/CardComponentRenderer.kt;
2. set type = "card_component";
3. implement only Card Component behavior;
4. reuse shared helpers for common mechanics;
5. register in ComponentDefinitions.all;
6. add tests + fixture coverage.
```

## 14.3 Add a new Section type

```text
1. create render/section/<TypeName>SectionRenderer.kt;
2. set its exact backend wire type;
3. implement only that Section behavior;
4. register in SectionDefinitions.all;
5. add tests + fixture coverage.
```

## 14.4 Add a new Group type

```text
1. create render/group/<TypeName>GroupRenderer.kt;
2. set its exact backend wire type;
3. implement only that Group behavior;
4. register in GroupDefinitions.all;
5. add tests + fixture coverage.
```

## 14.5 Add a new Element type

Example future backend type:

```text
rating
```

Developer does:

```text
1. confirm backend rating schema and supported events;
2. create render/element/RatingElementRenderer.kt;
3. set type = "rating";
4. read current field/runtime state through SduiRenderContext if required;
5. render the Element;
6. emit SduiInteraction for backend-declared events;
7. register in ElementDefinitions.all;
8. add renderer/support/fixture/interaction tests.
```

Do **not** create a new feature Store, navigation branch, action engine, registry framework, or network adapter simply because a new renderer is added.

## 14.6 New layout algorithm vs new backend node type

These are different changes.

If backend introduces a reusable child-layout contract such as Grid:

```text
shared Grid child-layout helper
```

may be added once under `render/layout/`.

If backend also defines concrete semantic types such as:

```text
grid_component
grid_section
```

then each supported backend type still gets its concrete hierarchy renderer class/object:

```text
GridComponentRenderer.kt
GridSectionRenderer.kt
```

Those concrete renderers can delegate the common Grid mechanics to the one shared Grid helper. The architecture removes duplicated mechanics, not explicit backend-type ownership.

New layout vocabulary must be capability-checked. Do not silently map an unknown backend value to an unrelated default.

---

# 15. Complete action execution flow

The action system is for the entire app, not Login/OTP only.

## Request

```text
user action
    ↓
Element renderer emits ActionTriggered(request)
    ↓
DynamicScreenStore
    ↓
SduiActionExecutor
    ↓
resolve $binding/$context/$response/$literal
    ↓
NetworkDataSource
    ↓
responseMode = none or destination
```

## Navigate

```text
backend navigate action contains full destination
    ↓
SduiActionExecutor
    ↓
NavigationStore.Push(full destination)
```

## Present / Dismiss / State

These reduce through the canonical Dynamic screen state owner. Targeted dismiss semantics and runtime visibility remain as already implemented.

For `state(property=value)`, the Dynamic Store routes a bound target to `FieldState.value`; an unbound target can use `NodeRuntimeState.value`.

## External URI

Resolved through `SduiValueResolver` and delegated to the platform capability owner.

## Sequence

Executes child `SduiAction`s in order and stops according to the frozen failure policy.

---

# 16. Field/binding state — one owner only

Bound input values have one canonical mutable owner through `DynamicScreenState.fields`.

```text
Input binding key = mobileNumber
    ↓
fields["mobileNumber"].value
```

All bound-value paths converge there:

```text
Input renderer reads fields[binding]
user ValueChanged writes fields[binding]
backend state(value) for a bound target writes fields[binding]
validation reads fields[binding]
$binding action resolution reads fields[binding]
```

`NodeRuntimeState.value` must not shadow a bound Input value. It remains only for unbound node-value state.

The immutable `SduiScreen` received from backend remains unchanged.

---

# 17. Support for the whole Partner app

The same engine must support future screens such as:

```text
Dashboard
Booking list
Booking details
On-the-way state
Arrived state
Start service OTP
Service checklist
Photo capture entry points
Earnings
Payouts
Availability
Leaves
Profile
KYC
Organization employees
Emergency transfer
```

The engine remains generic because:

```text
screen structure comes from backend
node types map to registered concrete renderers
actions come from backend
bindings come from backend
navigation destinations come from backend
```

Product-specific meaning stays in backend composition/domain APIs, not hardcoded screen branching in frontend.

---

# 18. Dynamic feature ownership

The SDUI module does not own networking/navigation feature state.

`feature:dynamic` owns the live dynamic-screen lifecycle.

Detailed authority:

```text
feature/dynamic/DYNAMIC_FEATURE_ARCHITECTURE.md
```

High-level flow:

```text
NavigationStore gives DynamicDestination
    ↓
DynamicScreenStore loads and owns current screen state
    ↓
SduiRenderer draws it through registered concrete hierarchy renderers
    ↓
SduiInteraction returns to DynamicScreenStore
    ↓
SduiActionExecutor performs action
```

SDUI may provide pure hierarchy/value/capability helpers used by Dynamic without reversing the dependency direction.

---

# 19. Testing strategy

## 19.1 Contract fixtures

Maintain realistic backend-shaped fixtures for Login, OTP, Dashboard, Booking Details, all-node, all-action, all-value-reference and unsupported vocabulary.

Canonical product fixtures remain compatible with the final capability pass: they use `PARTNER`, no unconsumed screen theme, supported accessory/layout vocabulary, and no unsupported Input weight.

## 19.2 Decoder tests

Cover valid/malformed data and exact supported action/value-reference decoding.

## 19.3 Support-checker tests

Cover client capability concerns only:

```text
supported schema
PARTNER/GLOBAL target supported
CUSTOMER target rejected by Partner runtime
non-null unconsumed theme rejected
unknown Template/Component/Section/Group/Element rejected
unknown direct/embedded action rejected
unsafe endpoint rejected
unsupported accessory rejected
unsupported explicit layout vocabulary rejected
unsupported Input weight rejected
```

Do not duplicate backend structural validation.

## 19.4 Registry tests

Must prove:

```text
all concrete TemplateDefinitions register
all concrete ComponentDefinitions register
all concrete SectionDefinitions register
all concrete GroupDefinitions register
all concrete ElementDefinitions register
lookup returns the expected concrete renderer instance/type
duplicate registration fails
```

The registry regression prevents a future return to anonymous `structural*Renderer("type")` factories for supported semantic node types.

## 19.5 Renderer/layout tests

Cover:

```text
backend type selects correct concrete renderer
renderer delegates children
Component preserves direct Elements + Sections
Section preserves direct Elements + Groups
vertical/horizontal linear layout
spacing + main-axis alignment together
all supported alignment vocabulary
self modifier and child-layout responsibilities remain separate
```

## 19.6 Value-resolver tests

Cover all four reference forms and nested recursion.

## 19.7 Field-state tests

Cover:

```text
ValueChanged updates one canonical FieldState
required/pattern validation
valid field clears error
bound state(value) updates the same field owner
bound state(value) leaves immutable server screen unchanged
bound state(value) does not create NodeRuntimeState.value shadow state
later user edit still updates the same field
unbound state(value) remains NodeRuntimeState
```

## 19.8 Action-executor tests

Cover all seven actions including atomic request-flow semantics, nested request validation and no-body responseMode none behavior.

## 19.9 Navigation tests

Cover full destination persistence/restoration, Push/Pop/Refresh and no templateType routing.

## 19.10 Complete mock flows

Keep generic Login → OTP → Dashboard and Dashboard → Booking Details → Back coverage as protocol-flow proof, not product-specific runtime branches.

## 19.11 Hierarchy traversal tests

Cover canonical non-render traversal order and both additive branches, plus element target lookup used by Dynamic state ownership.

---

# 20. Code-writing rules

1. One class/object = one clear reason to change.
2. Every supported backend Template/Component/Section/Group/Element type has one concrete renderer in its matching hierarchy package.
3. Registration lists concrete renderer objects; do not hide semantic backend types behind generic renderer factories.
4. Shared layout/modifier/accessory helpers remove duplicated mechanics but never replace concrete backend-type renderers.
5. No UI renderer calls network/navigation directly.
6. No backend screen-name checks such as `if (screenId == "partner_login")` inside generic SDUI.
7. No navigation decisions based on `templateType`.
8. No second form/bound-value state owner.
9. No `JsonElement → String → JsonElement` round trips.
10. No duplicate Command/PreparedAction models.
11. No duplicate typed runtime-reference model when the resolver already owns the wire forms.
12. No speculative action types not defined by backend.
13. No generic manager/coordinator class without a concrete responsibility.
14. Prefer immutable models and exhaustive `when` for action types.
15. Reusable properties/accessories/layout mechanics belong in shared helpers, not copied into every renderer.
16. Non-render hierarchy inspection uses the canonical traversal; renderer nesting remains compositional.
17. Every new renderer must have registration + unit test + representative fixture coverage.
18. Every new action must originate in backend protocol first, then frontend model/executor/tests.
19. Unknown/unsupported server vocabulary must fail cleanly; never silently guess.
20. Keep Dynamic and SDUI responsibilities separate.
21. Preserve all valid additive hierarchy branches: Component Elements + Sections; Section Elements + Groups.
22. Do not claim target/theme/accessory/layout capability until the client actually implements it.

---

# 21. Implementation order

The historical phases remain. The explicit renderer correction and approved A–I convergence are now implemented in source.

```text
Phase 1  Capture canonical backend JSON fixtures                         COMPLETE
Phase 2  Fix backend destination inconsistencies                        COMPLETE
Phase 3  Define exact frontend SDUI models/actions/value references     COMPLETE
Phase 4  Unwrap API envelope correctly and implement SduiDecoder        COMPLETE
Phase 5  Replace deep frontend validation with SduiSupportChecker       COMPLETE
Phase 6  Build separated hierarchy registry + clear registration        COMPLETE
Phase 7  Implement common properties/accessories/render context         COMPLETE
Phase 8  Implement explicit hierarchy/element renderer classes          COMPLETE
Phase 9  Move bound field/runtime state to one DynamicScreenState owner COMPLETE
Phase 10 Implement SduiValueResolver                                     COMPLETE
Phase 11 Implement exact seven-action SduiActionExecutor                 COMPLETE
Phase 12 Simplify DynamicDestination/navigation/back/refresh             COMPLETE
Phase 13 Remove obsolete legacy/duplicate runtime layers                 COMPLETE
Phase 14 Run complete mock vocabulary/source regressions                 SOURCE COMPLETE / CI PENDING
Phase 15 Run generic dynamic-flow integration tests                      SOURCE COMPLETE / CI PENDING
Phase 16 Real backend/manual app validation                              DEFERRED BY OWNER
Phase 17 Android/iOS/Desktop full verification + architecture gates      CI PENDING
Phase 18 Freeze                                                          ACTIVATES AFTER EXACT-HEAD CI GREEN
```

Renderer correction:

```text
[x] keep ChildLayout as shared mechanics
[x] remove StructuralNodeRenderers generic factory layer
[x] restore FormTemplateRenderer / StackTemplateRenderer / DefaultTemplateRenderer
[x] restore StackComponentRenderer / StackSectionRenderer / StackGroupRenderer
[x] register those concrete objects explicitly in SduiDefinitions
[x] keep existing Element renderers concrete
[x] add registry tests proving exact concrete lookup
[x] retain hierarchy coexistence and layout regressions
```

Approved final A–I convergence:

```text
[x] A embedded Text action ownership
[x] B accessory capability handling
[x] C bound Input displayed/submitted value ownership
[x] D non-render hierarchy traversal deduplication
[x] E SduiValueReference duplication cleanup
[x] F unused decoder helper cleanup
[x] G approved common renderer consistency fixes
[x] H theme/targetApp capability policy
[x] I explicit layout vocabulary validation
```

Source evidence:

```text
e55ded9721d06261d7880c8a9833510f95ac6c12
refactor(sdui): restore explicit hierarchy renderers

1ac4909613e68c3524633fef982c4dda34650ef0
fix(sdui): converge frozen runtime ownership and support
```

---

# 22. Freeze checklist

Do not activate the final freeze until all answers are yes:

```text
[x] frontend consumes canonical API envelope/data in current baseline
[x] navigation is backend-destination driven
[x] full DynamicDestination is stored/restored
[x] normal Back = NavigationStore.Pop
[x] Refresh reloads current destination
[x] no templateType-based navigation decisions
[x] no duplicate deep backend structural validator
[x] Template/Component/Section/Group/Element registries remain separated
[x] duplicate registration fails
[x] Component may contain direct Elements + Sections together
[x] Section may contain direct Elements + Groups together
[x] Group contains Elements
[x] every current backend structural type maps to an explicit concrete renderer object
[x] registry regression proves concrete renderer lookup
[x] reusable ChildLayout owns current linear child-arrangement mechanics
[x] spacing + alignment behavior has focused regression coverage
[x] invalid explicit layout vocabulary fails closed
[x] exact seven backend actions work in current baseline
[x] exact four value-reference behaviors work in current baseline
[x] duplicate SduiValueReference production model removed
[x] canonical non-render hierarchy traversal implemented
[x] embedded action ownership converged
[x] accessory capability ownership converged
[x] bound Input value has one canonical mutable owner
[x] approved element consistency corrections implemented
[x] targetApp/theme capability explicit and fail-closed
[x] unused decoder String-action helper removed
[x] baseline full-vocabulary fixture tests exist
[x] legacy SDUI runtime removed
[x] audited stale production files absent from exact source tree
[x] resend/cooldown remains outside current implementation
[x] all separately approved A–I audit corrections complete
[ ] final documentation-closeout HEAD has executed-green jvm-android + published-foundation-boundary + ios
[~] real backend/manual visual/auth validation is explicitly deferred
```

Once the final exact HEAD satisfies the one unchecked CI line, the conditional freeze declaration in Section 26 activates without another evidence-only commit.

---

# 23. Final mental model for a new developer

When backend sends a supported screen, think:

```text
API JSON
  ↓
SduiDecoder
  ↓
SduiSupportChecker
  ↓
SduiRenderer
  ↓
Template.type → Template registry → concrete TemplateRenderer
  ↓
Component.type → Component registry → concrete ComponentRenderer
  ↓
Element(s) AND/OR Section(s)
  ↓
Section.type → Section registry → concrete SectionRenderer
  ↓
Element(s) AND/OR Group(s)
  ↓
Group.type → Group registry → concrete GroupRenderer
  ↓
Element.type → Element registry → concrete ElementRenderer
```

Inside any concrete renderer:

```text
read own node properties
  ↓
use shared SduiModifierResolver for self presentation
  ↓
use shared ChildLayout when the type uses that common arrangement contract
  ↓
render child content supplied by SduiRenderer
```

For non-render hierarchy inspection:

```text
SduiHierarchyTraversal
  ↓
visit Component direct Elements + Sections
  ↓
visit Section direct Elements + Groups
  ↓
visit Group Elements
```

For a bound value:

```text
FieldState.value
  ├── Input display
  ├── validation
  ├── user editing
  ├── backend state(value)
  └── $binding submission
```

When backend adds a new Template:

```text
create its TemplateRenderer
register it
add tests/fixture
```

When backend adds a new Component/Section/Group/Element, follow the identical hierarchy-specific pattern.

This is the standard: **one obvious implementation object per backend type, one obvious registration location, one bound-value owner, shared mechanics without hidden semantic indirection.**

---

# 24. Frozen renderer ownership and child-layout architecture

> **Status:** SOURCE COMPLETE FOR THE CURRENT VOCABULARY — final engine freeze activation waits only on exact-head executable CI.
>
> **Supersedes:** the previous review wording that allowed generic `structural*Renderer("wire_type")` factories to replace concrete hierarchy renderer classes.

A structural SDUI node answers two separate questions:

1. **Which backend semantic type is this?**
2. **How are this node's children arranged?**

The approved architecture keeps both questions visible:

```text
backend node type
      ↓
concrete hierarchy renderer
      ↓
shared self-presentation helper
      ↓
shared child-layout helper
      ↓
Compose
```

For the current vocabulary:

```text
form_template    → FormTemplateRenderer
stack_template   → StackTemplateRenderer
default_template → DefaultTemplateRenderer
stack_component  → StackComponentRenderer
stack_section    → StackSectionRenderer
stack_group      → StackGroupRenderer
text             → TextElementRenderer
image            → ImageElementRenderer
input            → InputElementRenderer
button           → ButtonElementRenderer
```

## 24.1 Concrete renderer ownership is mandatory

A backend type must be discoverable by file/object name and explicit registry entry.

The generic form below is forbidden and absent from production source:

```kotlin
structuralTemplateRenderer("form_template")
structuralComponentRenderer("stack_component")
```

Even if multiple types are mechanically identical today, each type keeps its concrete renderer because that renderer is the semantic extension point for the backend contract.

## 24.2 Shared helpers are still required

Concrete renderers must not copy common Row/Column algorithms.

Current shared child-layout properties:

```text
axis
spacing
mainAxisAlignment
crossAxisAlignment
```

belong to reusable `ChildLayout` mechanics.

Common node self presentation remains in `SduiModifierResolver`.

Therefore:

```text
StackComponentRenderer
   └── delegates common mechanics to ChildLayout

StackSectionRenderer
   └── delegates common mechanics to ChildLayout

StackGroupRenderer
   └── delegates common mechanics to ChildLayout
```

The concrete objects are semantic ownership; the helper is mechanical reuse.

## 24.3 Future layout algorithms

If backend adds another layout algorithm such as Grid/Overlay/Flow, implement the reusable mechanics once under `render/layout/`.

Do not copy the Grid algorithm into every hierarchy renderer.

However, if backend protocol defines distinct semantic wire types such as `grid_component` and `grid_section`, those supported wire types still receive concrete `GridComponentRenderer` and `GridSectionRenderer` objects that delegate to the shared Grid helper.

The goal is:

```text
no duplicated layout algorithm
+
explicit renderer ownership for every backend type
```

## 24.4 Correctness requirements

- spacing and positional main-axis alignment must not silently disable one another;
- Component and Section must preserve every additive child branch;
- explicit unsupported layout vocabulary must fail through capability checking rather than guessing;
- helpers must not become hidden registries or product-specific routing layers;
- `templateType` remains render identity only, never navigation logic.

## 24.5 Current implementation gates

```text
[x] StructuralNodeRenderers generic factory layer removed
[x] concrete Template renderers restored and registered
[x] concrete Component renderer restored and registered
[x] concrete Section renderer restored and registered
[x] concrete Group renderer restored and registered
[x] Element renderers remain concrete
[x] ChildLayout remains the single current linear child-layout helper
[x] registry tests prove exact concrete renderer lookup
[x] hierarchy-coexistence regression retained
[x] spacing + alignment regression retained
[x] explicit invalid layout vocabulary fails closed
[x] approved remaining A–I audit corrections implemented
[ ] exact-head runner-executed multiplatform CI green
```

Implementation evidence:

```text
e55ded9721d06261d7880c8a9833510f95ac6c12
refactor(sdui): restore explicit hierarchy renderers

1ac4909613e68c3524633fef982c4dda34650ef0
fix(sdui): converge frozen runtime ownership and support
```

---

# 25. Frozen hierarchy child-composition contract

> **Status:** FROZEN — this hierarchy rule is approved and is no longer under architecture review.

Canonical hierarchy:

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
- nullable/optional child collections do **not** define an XOR/either-or relationship;
- every present valid branch must be traversed;
- frontend must not remodel these into mutually exclusive child unions;
- whether a structurally valid node may contain no children remains a backend structural-validation concern unless protocol explicitly delegates that rule to the client.

Therefore the current `SduiComponent` and `SduiSection` model shape is accepted. The earlier "impossible state" finding is withdrawn.

Regression requirements:

```text
[x] fixture contains Component with direct Elements + Sections
[x] fixture contains Section with direct Elements + Groups
[x] decoder preserves both branches
[x] support checking visits both branches through canonical non-render traversal
[x] SduiRenderer source traverses both branches compositionally
[x] focused hierarchy traversal regression covers the additive order
[ ] final exact-head executable CI confirms the complete regression suite
```

---

# 26. Final approved audit convergence and freeze activation

Approved source-audit closure is implemented in:

```text
1ac4909613e68c3524633fef982c4dda34650ef0
fix(sdui): converge frozen runtime ownership and support
```

Closed audit set:

```text
A  Embedded Text action ownership                    CLOSED
B  Accessory capability handling                     CLOSED
C  Bound Input display/submission value ownership    CLOSED
D  Duplicate non-render hierarchy traversal          CLOSED
E  SduiValueReference duplication                    CLOSED
F  Unused decoder APIs                                CLOSED
G  Approved renderer consistency findings            CLOSED
H  Theme/targetApp capability                        CLOSED
I  Explicit layout vocabulary validation             CLOSED
```

Exact-tree audit at that source commit confirms no legacy `com/carbroz/runtime/sdui/**`, `StructuralNodeRenderers.kt`, `SduiValueReference.kt`, or old `DynamicScreenInstructionCodec.kt` is present.

The documentation-closeout commit that contains this section is intentionally the final content change before CI. Once that exact final HEAD has executed-green results for:

```text
jvm-android
published-foundation-boundary
ios
```

then, with no additional evidence-only source/document commit, declare:

**`SDUI + DYNAMIC GENERIC ENGINE — FROZEN GREEN`**

Real backend/manual visual/auth validation remains explicitly deferred as agreed. PR #11 must remain unmerged until explicit owner approval.
