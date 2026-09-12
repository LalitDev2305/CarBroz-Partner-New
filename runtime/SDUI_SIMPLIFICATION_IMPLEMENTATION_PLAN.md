# CarBroz Partner Frontend — SDUI Simplification & Implementation Plan

> **Status:** REVIEW DRAFT — not frozen and not an implementation mandate yet.
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
component contains sections or elements
section contains groups or elements
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

Example Login:

```text
form_template
  ↓
brand component
phone-entry component
action component
```

It only holds backend data. Rendering happens elsewhere.

---

## 6.4 `SduiSection.kt`

**What it represents:** a grouping level inside a component when backend needs more structure.

Example Dashboard:

```text
Dashboard component
  ↓
active-bookings section
  ↓
booking-card groups/elements
```

---

## 6.5 `SduiGroup.kt`

**What it represents:** the final structural group before elements.

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

Use hierarchy-specific definition collections:

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

Then one clear registration class:

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

This is intentionally easy to read:

```text
all templates     registered together
all components    registered together
all sections      registered together
all groups        registered together
all elements      registered together
```

`SduiNodeRegistry` should reject duplicate types inside the same hierarchy.

Example:

```text
registerElements([ButtonRenderer, AnotherButtonRenderer])
    ↓
both type = button
    ↓
startup/test failure: duplicate element renderer
```

That prevents silent override.

---

# 11. Rendering classes — why they exist, using live app flow

## 11.1 `SduiRenderer.kt`

This is the **traffic controller for drawing one SDUI screen**.

Example Login flow:

```text
DynamicScreen receives SduiScreen(partner_login)
    ↓
DynamicScreen calls SduiRenderer.Render(screen)
    ↓
SduiRenderer sees template.type = form_template
    ↓
find FormTemplateRenderer in registry
    ↓
FormTemplateRenderer renders its components
    ↓
component renderer renders sections/elements
    ↓
ButtonElementRenderer finally draws Continue button
```

`SduiRenderer` does not know Login business logic.

Its job is only:

```text
read node type
find correct renderer
call it
walk children
```

---

## 11.2 `SduiRenderContext.kt`

This exists because renderers need a small amount of runtime information, but we do not want every renderer to depend directly on `DynamicScreenStore`.

Think of it as a **read-only bag the renderer receives while drawing**.

Example Input renderer needs:

```text
current value for binding "mobileNumber"
current field error
overridden enabled/visible state
function to emit interaction
```

Instead of passing 10 parameters to every renderer, pass one context:

```kotlin
data class SduiRenderContext(
    val fields: Map<String, FieldState>,
    val nodeStates: Map<String, NodeRuntimeState>,
    val onInteraction: (SduiInteraction) -> Unit,
)
```

Live example:

```text
InputElementRenderer renders mobile input
    ↓
asks context.fields["mobileNumber"] for value
    ↓
user types 9876...
    ↓
renderer calls context.onInteraction(...)
```

The context does not execute actions or mutate state itself.

---

## 11.3 `SduiInteraction.kt`

This represents **what the user just did in the rendered SDUI**.

Examples:

```text
user changed input
user clicked button
user clicked legal text span
user long-pressed text
user focused input
```

Suggested shape:

```kotlin
sealed interface SduiInteraction {
    data class ValueChanged(
        val elementId: String,
        val bindingKey: String,
        val value: JsonElement,
    ) : SduiInteraction

    data class ActionTriggered(
        val sourceId: String,
        val event: String,
        val action: SduiAction,
    ) : SduiInteraction
}
```

Why carry the action directly?

Because the renderer already has the element/span and knows which backend action belongs to `onClick`/`onLongClick`.

We do not need:

```text
NodePath → CommandIndex → look up action again
```

Live Continue-button example:

```text
ButtonElementRenderer has element.actions["onClick"]
    ↓
user clicks Continue
    ↓
ActionTriggered(
  sourceId = "continue_button",
  event = "onClick",
  action = Request(...)
)
    ↓
DynamicScreenStore receives interaction
    ↓
SduiActionExecutor executes Request
```

---

## 11.4 `SduiModifierResolver.kt`

**Job:** convert common backend layout/appearance values into Compose modifiers.

Example:

```text
fillMaxWidth = true
height = 56
shape.cornerRadius = 16
background = gradient
```

Renderer asks the modifier resolver instead of reimplementing spacing/size/border rules in every element.

---

# 12. Accessory rendering — leading/trailing are reusable across supported elements

## `AccessoryRenderer.kt`

**Job:** render an accessory definition independent of the owning element.

Example:

```text
Button trailing = icon arrow_forward
    ↓
ButtonElementRenderer
    ↓
AccessoryRenderer
    ↓
IconAccessoryRenderer
```

Example text:

```text
PARTNER text
leading divider
trailing divider
```

Example future image:

```text
partner profile image
trailing verification badge/icon
```

The owner element decides whether it supports accessories; the accessory implementation is shared.

Do not create Text-specific leading/trailing rendering logic.

---

# 13. Template/component/section/group/element renderer responsibilities

Every renderer follows the same rule:

> read only its own node properties, render only its own level, and delegate children to the shared SDUI renderer.

## Template renderer

Example `FormTemplateRenderer`:

```text
Receives form_template
    ↓
renders top-level form layout
    ↓
asks SduiRenderer to render each component
```

It does not validate phone numbers or call APIs.

## Component renderer

Example `StackComponentRenderer`:

```text
Receives stack_component
    ↓
creates Column/Row based on backend properties
    ↓
renders its sections OR elements
```

## Section renderer

```text
Receives stack_section
    ↓
creates section layout
    ↓
renders groups OR elements
```

## Group renderer

```text
Receives stack_group
    ↓
creates final layout group
    ↓
renders elements
```

## Element renderer

Example button:

```text
Receives button element
    ↓
reads text/background/accessories/runtime enabled state
    ↓
renders Compose button
    ↓
onClick emits SduiInteraction.ActionTriggered
```

No element renderer directly executes network/navigation.

---

# 14. How a new developer adds a new SDUI type

These steps are mandatory and intentionally predictable.

## 14.1 Add a new template

Example backend adds `dashboard_template`.

Step 1 — confirm backend JSON contract:

```text
type = dashboard_template
properties = ...
children = components
```

Step 2 — create:

```text
render/template/DashboardTemplateRenderer.kt
```

Step 3 — implement only dashboard-template rendering.

Step 4 — register in:

```kotlin
TemplateDefinitions.all
```

Step 5 — add tests:

```text
DashboardTemplateRendererTest
SduiSupportChecker recognizes dashboard_template
registry duplicate test remains green
mock full-screen render test
```

Nothing else should need modification.

## 14.2 Add a new component

Example `card_component`.

1. create `CardComponentRenderer.kt`;
2. support only that component's properties/layout;
3. register in `ComponentDefinitions.all`;
4. add component renderer tests;
5. add one mock screen using it.

## 14.3 Add a new section

1. create renderer under `render/section/`;
2. register in `SectionDefinitions.all`;
3. add tests.

## 14.4 Add a new group

1. create renderer under `render/group/`;
2. register in `GroupDefinitions.all`;
3. add tests.

## 14.5 Add a new element

Example future `rating` element.

1. confirm backend `rating` schema and supported events;
2. create `RatingElementRenderer.kt`;
3. renderer reads value from `SduiRenderContext` if bound;
4. renderer emits `SduiInteraction` when user changes/clicks;
5. register in `ElementDefinitions.all`;
6. add renderer tests;
7. add mock screen fixture;
8. add end-to-end interaction test.

Do **not** create a new feature Store, action engine, registry framework, or network adapter just because one new element is added.

---

# 15. Complete action execution flow

The action system is for the entire app, not Login/OTP only.

## Request

Example future booking screen:

```text
Partner taps Accept Job
    ↓
button.onClick = request action
    ↓
DynamicScreenStore receives ActionTriggered
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

Example Dashboard → Booking Details:

```text
booking card clicked
    ↓
navigate action contains full booking-details destination
    ↓
NavigationStore.Push(full destination)
```

## Present

Example booking cancellation bottom sheet:

```text
Cancel clicked
    ↓
present(targetId = cancel_sheet, bottom_sheet)
    ↓
DynamicScreenState.overlay set
    ↓
Compose displays target as bottom sheet
```

## Dismiss

```text
Close clicked
    ↓
dismiss
    ↓
overlay cleared
```

## State

Example resend button becomes enabled:

```text
state(targetId = resend, set enabled = true)
    ↓
nodeStates[resend]
    ↓
recompose
```

## External URI

Example Terms & Conditions:

```text
text span clicked
    ↓
external_uri($context(legal.termsUri))
    ↓
ValueResolver
    ↓
CapabilityRegistry opens URL
```

## Sequence

Example future action:

```text
sequence
  1. state(button.loading = true)
  2. request(...)
  3. navigate(...)
```

Execute children in order and stop on failure according to the final action-error policy.

---

# 16. Field/binding state — one owner only

Bound input values live only in `DynamicScreenState`.

Example:

```text
Input binding key = mobileNumber
```

State:

```kotlin
fields["mobileNumber"] = FieldState(
    value = JsonPrimitive("9876543210"),
    error = null,
)
```

Flow:

```text
Input renderer reads fields[mobileNumber]
    ↓
user types
    ↓
ValueChanged interaction
    ↓
DynamicScreenStore updates field
    ↓
state changes
    ↓
Compose recomposes
```

No local canonical `remember` value plus FormStore plus DynamicStore duplication.

---

# 17. Support for the whole Partner app

Do not design around only Login and OTP.

The same engine must handle future screens such as:

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
node types come from registered renderers
actions come from backend
bindings come from backend
navigation destinations come from backend
```

Product-specific meaning should stay in backend composition and domain APIs, not hardcoded screen branching in frontend.

---

# 18. Dynamic feature ownership

The SDUI module should not own networking/navigation feature state.

`feature:dynamic` owns the live dynamic-screen lifecycle.

Detailed class-by-class documentation is kept separately in:

```text
feature/dynamic/DYNAMIC_FEATURE_ARCHITECTURE.md
```

At high level:

```text
NavigationStore gives DynamicDestination
    ↓
DynamicScreenStore loads and owns current screen state
    ↓
SduiRenderer draws it
    ↓
SduiInteraction returns to DynamicScreenStore
    ↓
SduiActionExecutor performs action
```

---

# 19. Testing strategy — easy mock-driven full SDUI verification

Tests should prove both small units and complete flows.

## 19.1 Contract fixtures

Maintain realistic backend JSON fixtures in test sources:

```text
partner_login.json
partner_otp.json
partner_dashboard_minimal.json
booking_list.json
booking_details.json
all_node_types.json
all_action_types.json
all_value_reference_types.json
invalid_unsupported_node.json
invalid_unsupported_action.json
```

Fixtures should use the same envelope shape the API actually returns.

---

## 19.2 Decoder tests

Test:

```text
valid API data decodes
all templates decode
all structural levels decode
all element types decode
all actions decode
all value references decode
accessories decode on multiple supported element types
malformed JSON fails cleanly
unknown action type fails deterministically
```

---

## 19.3 Support-checker tests

Test only client capability concerns:

```text
supported schema passes
unsupported schema fails
unknown template fails
unknown component fails
unknown section fails
unknown group fails
unknown element fails
unknown action fails
unsafe endpoint fails
```

Do not duplicate backend structural-schema tests here.

---

## 19.4 Registry tests

```text
all TemplateDefinitions register
all ComponentDefinitions register
all SectionDefinitions register
all GroupDefinitions register
all ElementDefinitions register
lookup returns correct renderer
duplicate template type fails
duplicate element type fails
wrong hierarchy cannot be registered through the wrong method
```

---

## 19.5 Renderer tests

For every renderer:

```text
correct renderer chosen for type
properties applied
children delegated
visible/enabled runtime override applied
interaction emitted correctly
leading/trailing accessory rendering supported where declared
```

Important fixtures:

```text
Text spans + clickable legal link
Image with sizing/contentScale
Input normal
Input segmented OTP
Button with trailing icon
Horizontal stack
Vertical stack
Gradient/background/border/shape
```

---

## 19.6 Value-resolver tests

```text
$binding resolves
$context resolves
$response resolves
$literal resolves
nested object resolves
nested array resolves
missing binding fails predictably
missing context path fails predictably
missing response path fails predictably
```

---

## 19.7 Field-state tests

```text
ValueChanged updates one field
other fields remain unchanged
required validation
pattern validation
valid field clears error
validate=false does not block request
validate=true blocks invalid request
```

---

## 19.8 Action-executor tests

Test each action independently:

```text
request builds correct NetworkRequest
request resolves bindings/context/response
request none stores lastResponse
request destination converts backend response to DynamicDestination
navigate dispatches exact backend destination
present sets overlay
dismiss clears overlay
state set works
state toggle works
external_uri resolves URI and delegates to capability owner
sequence executes in order
sequence stops according to failure policy
```

---

## 19.9 Navigation tests

```text
Splash bootstrap destination becomes root
navigate pushes full DynamicDestination
system Back pops previous full destination
refresh reuses current destination endpoint
restored stack preserves screenId/templateId/templateType/endpoint/auth
frontend never navigates based on templateType alone
```

---

## 19.10 Complete mock Login → OTP flow

Mock backend:

```text
bootstrap → Login destination
Login GET → Login SDUI
Send OTP POST → challenge response
Navigate/response destination → OTP
OTP GET → OTP SDUI
Verify OTP POST → Dashboard destination
Dashboard GET → Dashboard SDUI
```

Assertions:

```text
correct endpoint called at every step
field values resolved
challengeId survives in the correct shared auth-flow/response context design
NavigationStore contains full destinations
Back from OTP returns Login destination
no hardcoded Login/OTP screen branching exists
```

---

## 19.11 Full vocabulary fixture

Create one synthetic `all_node_types.json` screen that intentionally contains every registered:

```text
template
component
section
group
element
accessory
```

Create one `all_action_types.json` fixture containing:

```text
request
navigate
present
dismiss
state
external_uri
sequence
```

This gives one fast regression test proving the engine understands the complete current protocol vocabulary.

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

---

# 21. Implementation order after this document is approved

```text
Phase 1  Capture canonical backend JSON fixtures
Phase 2  Fix backend destination inconsistencies
Phase 3  Define exact frontend SDUI models/actions/value references
Phase 4  Unwrap API envelope correctly and implement SduiDecoder
Phase 5  Replace deep frontend validation with SduiSupportChecker
Phase 6  Build separated hierarchy registry + clear registration entry point
Phase 7  Implement common properties/accessories/render context/interaction
Phase 8  Implement template/component/section/group/element renderers
Phase 9  Move bound field/runtime state to one DynamicScreenState owner
Phase 10 Implement SduiValueResolver
Phase 11 Implement exact seven-action SduiActionExecutor
Phase 12 Simplify DynamicDestination/navigation/back/refresh handling
Phase 13 Remove obsolete Command/PreparedAction/ActionRegistry/FormStore/etc.
Phase 14 Run complete mock vocabulary tests
Phase 15 Run Login → OTP → Dashboard mock integration
Phase 16 Run real backend Desktop integration
Phase 17 Android/iOS/Desktop full verification + architecture gates
Phase 18 Freeze only after all green
```

---

# 22. Freeze checklist

Do not mark SDUI frozen until all answers are yes:

```text
[ ] frontend consumes actual backend API envelope/data
[ ] frontend models match backend names and values
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
[ ] Image renderer exists
[ ] text spans/actions work
[ ] segmented OTP input works
[ ] mock full-vocabulary tests pass
[ ] Login→OTP→Dashboard mock flow passes
[ ] real Desktop backend flow passes
[ ] Android/iOS/Desktop CI is green
```

---

# 23. Final mental model for a new developer

When you receive a requirement such as:

> Add a new Partner Earnings chart element.

Do this:

```text
1. Backend defines/finalizes `earnings_chart` element JSON.
2. Frontend adds `EarningsChartElementRenderer`.
3. Register it in `ElementDefinitions.all`.
4. Add renderer unit tests.
5. Add/update mock SDUI fixture.
6. Add interaction test only if the element emits events.
7. Nothing else changes.
```

When backend adds a new template:

```text
1. Add renderer.
2. Register in TemplateDefinitions.all.
3. Add tests/fixture.
```

When backend adds a new action:

```text
1. Add exactly one SduiAction variant.
2. Add exactly one execution branch.
3. Add resolver/security handling if needed.
4. Add tests.
```

That is the standard we want: **simple enough that a new developer knows exactly where to work, but strict enough that the engine remains safe and scalable for the entire Partner app.**
