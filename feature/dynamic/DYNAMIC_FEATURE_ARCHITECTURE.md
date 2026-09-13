# CarBroz Partner Frontend — Dynamic Feature Architecture

> **Status:** ACTIVE ARCHITECTURE — Dynamic ownership is established; the explicit SDUI renderer-registration boundary is implemented in source, while remaining audit corrections and executable CI still block final freeze.
>
> **Purpose:** explain the `feature:dynamic` module in simple app-flow language. This module owns the live lifecycle of every backend-driven Partner screen after Splash. It does not define the SDUI protocol or choose concrete SDUI renderers; that belongs to `runtime:sdui`.

---

# 1. What `feature:dynamic` means in CarBroz

After Splash/Bootstrap, almost every Partner screen is dynamic.

Examples:

```text
Login
OTP
Dashboard
Booking list
Booking details
On the way
Arrived
Start service OTP
Service progress
Earnings
Availability
Profile
KYC
Organization employees
Emergency transfer
```

The backend tells the app which destination to load and returns the SDUI tree for that screen.

`feature:dynamic` keeps one dynamic screen alive.

Its complete job is:

```text
receive a DynamicDestination
    ↓
call that destination endpoint
    ↓
receive SDUI JSON
    ↓
unwrap envelope.data
    ↓
ask SDUI module to decode + support-check
    ↓
store SduiScreen in DynamicScreenState
    ↓
DynamicScreen passes SduiScreen + SduiRenderContext to SduiRenderer
    ↓
SDUI registry resolves concrete Template/Component/Section/Group/Element renderers
    ↓
receive SduiInteraction back
    ↓
execute backend actions
    ↓
update DynamicScreenState or NavigationStore
```

Dynamic must not contain Login-specific, OTP-specific, Dashboard-specific, Booking-specific, or renderer-type branching.

Forbidden examples:

```kotlin
if (screenId == "partner_login") { ... }
if (templateType == "form_template") navigateToOtp()
if (templateType == "form_template") FormTemplateRenderer.Render(...)
```

Renderer lookup belongs entirely to `runtime:sdui`.

---

# 2. Package structure

```text
feature/dynamic/
└── src/commonMain/kotlin/com/carbroz/feature/dynamic/
    ├── DynamicDestination.kt
    ├── DynamicScreenState.kt
    ├── DynamicScreenIntent.kt
    ├── DynamicScreenEffect.kt
    ├── DynamicScreenStore.kt
    ├── DynamicScreen.kt
    ├── DynamicContextProvider.kt
    ├── SduiActionExecutor.kt
    └── DynamicFeatureFactory.kt
```

Keep this module small.

Do not add new manager/coordinator/use-case classes unless a real independent responsibility appears.

---

# 3. Complete live app flow

Example: app starts as a signed-out Partner.

```text
Splash
  ↓
Bootstrap
  ↓
backend nextScreen = partner_login destination
  ↓
NavigationStore.ResetTo(DynamicDestination(partner_login))
  ↓
Navigation3Host displays DynamicDestination
  ↓
DynamicFeatureFactory creates DynamicScreenStore
  ↓
DynamicScreenStore.load()
  ↓
GET destination.endpoint
  ↓
NetworkResponse
  ↓
API envelope.data
  ↓
SduiDecoder
  ↓
SduiSupportChecker
  ↓
SduiScreen
  ↓
DynamicScreenState.screen
  ↓
DynamicScreen
  ↓
SduiRenderer
  ↓
Template registry → concrete TemplateRenderer
  ↓
Component/Section/Group/Element registries → concrete renderers
```

User types:

```text
InputElementRenderer
  ↓
SduiInteraction.ValueChanged
  ↓
DynamicScreenStore
  ↓
fields[binding] updated
  ↓
state emits
  ↓
Compose recomposes
```

User triggers a backend action:

```text
Element renderer
  ↓
SduiInteraction.ActionTriggered(actual SduiAction)
  ↓
DynamicScreenStore
  ↓
SduiActionExecutor
  ↓
network/navigation/state/presentation/capability behavior
```

Nothing in this flow hardcodes Login → OTP. The same mechanism handles Dashboard → Booking Details, Booking Details → Start Service, Earnings → Payout Details, Profile → KYC, and future dynamic screens.

---

# 4. `DynamicDestination.kt`

`DynamicDestination` stores the complete backend destination needed to load/restore a dynamic screen:

```text
screenId
templateId
templateType
endpoint
method
authentication
```

`templateType` is render identity metadata for SDUI consistency. It is not a navigation rule and Dynamic does not use it to instantiate renderer classes.

Normal Back remains:

```text
NavigationStore.Pop
```

Refresh reloads the current complete destination.

---

# 5. `DynamicScreenState.kt`

One `DynamicScreenStore` owns one canonical `StateFlow<DynamicScreenState>`.

Conceptual state:

```kotlin
data class DynamicScreenState(
    val destination: DynamicDestination,
    val loading: Boolean = false,
    val actionInFlight: Boolean = false,
    val screen: SduiScreen? = null,
    val fields: Map<String, FieldState> = emptyMap(),
    val nodeStates: Map<String, NodeRuntimeState> = emptyMap(),
    val overlay: SduiOverlay? = null,
    val lastResponse: JsonElement? = null,
    val failure: DynamicScreenFailure? = null,
)
```

There must be one canonical mutable owner. The current audit finding about `FieldState.value` vs `NodeRuntimeState.value` remains a separate correction and is not changed by renderer registration work.

---

# 6. `DynamicScreenIntent.kt`

Intents represent events entering the feature boundary, for example:

```text
Load
Retry
Refresh
Interaction(SduiInteraction)
BackRequested
```

Do not create dozens of intents for internal helper methods.

---

# 7. `DynamicScreenEffect.kt`

Effects are only for genuine one-time feature/platform events that should not live as persistent screen state.

Navigation goes through `NavigationStore`; external URI goes through the capability owner. Delete `DynamicScreenEffect` if no true effect remains after final convergence.

---

# 8. `DynamicScreenStore.kt`

This is the brain of one displayed dynamic screen.

Responsibilities:

```text
load current destination
unwrap API envelope
ask SDUI decoder/support checker for screen
initialize bound field state
receive SduiInteraction
update field state
invoke SduiActionExecutor
apply returned state changes
retry/refresh
cancel feature-owned jobs
```

What Store must not do:

```text
Compose rendering
per-node renderer selection
Template/Component/Section/Group layout logic
Ktor transport internals
Navigation3 implementation internals
backend screen-name rules
```

Load flow:

```text
DynamicDestination
  ↓
NetworkDataSource
  ↓
envelope.data
  ↓
SduiDecoder
  ↓
SduiSupportChecker
  ↓
SduiScreen
  ↓
initialize fields
  ↓
DynamicScreenState.screen
```

---

# 9. `DynamicScreen.kt`

Compose entry point for one dynamic screen.

It handles high-level states:

```text
loading
failure
content
overlay
```

For content:

```text
state.screen
  ↓
SduiRenderer.Render(screen, renderContext)
```

`DynamicScreen` does not inspect backend node types and does not call a specific Template/Component/Section/Group/Element renderer itself.

---

# 10. `DynamicContextProvider.kt`

Produces a safe read-only SDUI context projection from existing application/session/flow owners.

Examples:

```text
$context(deviceId)
$context(authFlow.phoneNumber)
$context(legal.termsUri)
$context(partner.id)
```

It is not another Store.

---

# 11. `SduiActionExecutor.kt`

Executes exactly the seven backend actions:

```text
request
navigate
present
dismiss
state
external_uri
sequence
```

Dependencies can include:

```text
NetworkDataSource
NavigationStore
CapabilityRegistry
SduiValueResolver
DynamicContextProvider
```

It does not render UI and does not choose node renderers.

Request navigation is explicit through `responseMode=destination`; `request` and `navigate` remain distinct.

SESSION destinations establish session before navigation. Flow response/context updates commit only after the full requested action contract succeeds.

---

# 12. `DynamicFeatureFactory.kt`

Constructs/wires one `DynamicScreenStore` for the supplied destination. It has no state machine and no business logic.

---

# 13. Dependency direction and SDUI renderer boundary

Dependency direction is one-way:

```text
feature:dynamic
    ↓
runtime:sdui
```

Dynamic may depend on:

```text
SduiScreen
SduiDecoder
SduiSupportChecker
SduiRenderer
SduiRenderContext
SduiInteraction
SduiAction
SduiValueResolver
FieldState / NodeRuntimeState / SduiOverlay
```

SDUI must not depend on:

```text
DynamicScreenStore
NavigationStore
NetworkDataSource
Partner session implementation
Dynamic feature intents/effects
```

Most importantly:

```text
Dynamic does NOT do:
  form_template -> FormTemplateRenderer
  stack_component -> StackComponentRenderer

SDUI does:
  template.type -> Template registry -> concrete TemplateRenderer
  component.type -> Component registry -> concrete ComponentRenderer
  section.type -> Section registry -> concrete SectionRenderer
  group.type -> Group registry -> concrete GroupRenderer
  element.type -> Element registry -> concrete ElementRenderer
```

This makes renderer extension a pure SDUI concern.

---

# 14. NavigationStore ownership

`NavigationStore` remains the one application back-stack owner.

```text
SduiAction.Navigate
    ↓
SduiActionExecutor
    ↓
NavigationStore.Push(full DynamicDestination)
```

System Back:

```text
NavigationStore.Pop
```

No private Dynamic back stack.

---

# 15. Refresh behavior

Refresh reloads the current `DynamicDestination`. It does not infer a new screen from `templateType`.

Field/runtime-state preservation policy must remain explicit; do not silently merge old and new server trees.

---

# 16. Back-stack restoration

Persist enough information to reconstruct full dynamic destinations:

```text
screenId
templateId
templateType
endpoint
method
authentication
```

Do not persist only a template ID and guess an endpoint later.

---

# 17. Failure handling

Useful categories remain:

```text
Network
Decode
UnsupportedContract
Action
```

Action failure must not corrupt or replace already valid loaded content.

---

# 18. Lifecycle/coroutine rules

`DynamicScreenStore` may own load/action jobs.

Rules:

1. obsolete destination work is cancelled;
2. repeated actions respect action-in-flight/loading policy;
3. cancellation is not converted into an error;
4. `close()` cancels feature-owned jobs;
5. no global scope;
6. network/session implementations remain outside this feature.

---

# 19. Tests for Dynamic

Core coverage includes:

```text
full DynamicDestination serialization/restoration
load success/network/decode/unsupported failures
Retry/Refresh
ValueChanged updates canonical field state
ActionTriggered invokes executor
request flow commit semantics
Navigate uses exact backend destination
state/present/dismiss reduction
Back = NavigationStore.Pop
lifecycle cancellation
repeated-action suppression
```

Renderer registration tests belong to `runtime:sdui`, not `feature:dynamic`.

---

# 20. Complete mock dynamic flows

## Login → OTP → Dashboard

Used as generic action/destination/context proof only. There must be no screen-specific production branch.

## Dashboard → Booking Details → Back

Proves exact backend Navigate destination and `NavigationStore.Pop`.

## Presentation / action sequence / refresh

Remain generic protocol-flow tests.

---

# 21. New-developer rule for `feature:dynamic`

When backend adds a new screen whose existing SDUI vocabulary is already supported:

```text
Usually: change NOTHING in feature:dynamic.
```

When backend adds a new Template/Component/Section/Group/Element type:

```text
change runtime:sdui renderer package + registration + tests
not feature:dynamic
```

When backend adds a genuinely new action type, update the SDUI action contract and Dynamic executor only after the backend protocol defines it.

---

# 22. Anti-overengineering rules

1. No screen-specific Dynamic Stores.
2. No Login/OTP/Dashboard routing logic.
3. No second back stack.
4. No ActionRegistry/PreparedAction layer.
5. No FormStore beside DynamicScreenState.
6. No cache policy framework until required.
7. No child-layout algorithm in Dynamic.
8. No renderer registry or renderer factory in Dynamic.
9. No class kept solely because an MVI pattern suggests it.
10. Prefer one understandable Store over many tiny coordinators.

---

# 23. Final connection diagram

```text
NavigationStore
      ↓ current DynamicDestination
DynamicFeatureFactory
      ↓
DynamicScreenStore
      ├── load → NetworkDataSource → envelope.data → SduiDecoder → SupportChecker
      └── action → SduiActionExecutor
      ↓
DynamicScreenState(screen, fields, nodeStates, overlay)
      ↓
DynamicScreen
      ↓
SduiRenderer
      ↓
SDUI hierarchy registries
      ↓
concrete Template/Component/Section/Group/Element renderers
      ↓
SduiInteraction
      └────────────────────────────→ DynamicScreenStore
```

---

# 24. Dynamic freeze checklist

```text
[x] full DynamicDestination model exists and is used in current baseline
[x] no templateType-based routing
[x] normal Back uses NavigationStore.Pop
[x] Refresh uses current destination
[x] DynamicScreenState is sole mutable screen-state container
[x] no active FormStore duplicate owner
[x] Store does not render Compose
[x] Dynamic does not choose concrete hierarchy renderers
[x] renderer does not call network/navigation
[x] exact seven-action executor exists
[x] generic Login→OTP→Dashboard mock flow exists
[x] generic Dashboard→Booking Details→Back mock flow exists
[~] bound field value vs NodeRuntimeState.value remains a separate audit item
[ ] remaining approved audit corrections complete
[ ] configured JVM/Android + published-foundation + iOS CI actually executes green
[~] real backend/manual validation deferred by owner
```

---

# 25. Frozen SDUI renderer/layout ownership boundary

> **Status:** IMPLEMENTED IN `runtime:sdui` FOR THE CURRENT VOCABULARY — source commit `e55ded9721d06261d7880c8a9833510f95ac6c12`; final engine freeze still waits on remaining audit work and executable CI.

`feature:dynamic` owns destination loading, live mutable state, interaction orchestration and action execution.

`runtime:sdui` owns:

```text
backend node type recognition
hierarchy-specific registry lookup
concrete renderer selection
node property interpretation
self-presentation modifiers
child-layout mechanics
Element rendering
```

The exact runtime path is:

```text
DynamicScreen
  ↓
SduiRenderer
  ↓
Template.type
  ↓
Template registry
  ↓
concrete TemplateRenderer
  ↓
Component.type
  ↓
Component registry
  ↓
concrete ComponentRenderer
  ↓
Element(s) AND/OR Section(s)
  ↓
Section registry / Element registry
  ↓
Section child Element(s) AND/OR Group(s)
  ↓
Group registry / Element registry
```

Shared helpers such as `ChildLayout` are internal SDUI mechanics. They are not backend node registrations and are never selected by Dynamic.

Therefore Dynamic must never contain:

```text
if stack_template -> Column
if form_template -> FormTemplateRenderer
if stack_component -> StackComponentRenderer
if grid_component -> LazyGrid
```

Future new render node types are added to `runtime:sdui` through concrete renderer object/file + hierarchy registration + tests. Dynamic changes only if the backend introduces a genuinely new application-side action/effect contract.

Implementation evidence:

```text
e55ded9721d06261d7880c8a9833510f95ac6c12
refactor(sdui): restore explicit hierarchy renderers
```

Additional freeze gates:

```text
[x] feature:dynamic contains no child-layout algorithm
[x] feature:dynamic contains no concrete node-renderer selection
[x] corrected runtime:sdui explicit-renderer architecture is implemented
[x] registry regression proves concrete renderer mapping
[~] canonical bound-value ownership remains separate audit work
[ ] exact-head executable CI is green before final freeze
```
