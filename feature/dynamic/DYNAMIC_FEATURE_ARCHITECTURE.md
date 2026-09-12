# CarBroz Partner Frontend — Dynamic Feature Architecture

> **Status:** REVIEW DRAFT — not frozen and not an implementation mandate yet.
>
> **Purpose:** explain the `feature:dynamic` module in simple app-flow language. This module owns the live lifecycle of every backend-driven Partner screen after Splash. It does not define the SDUI protocol itself; that belongs to the SDUI module.

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

The backend tells the app which screen to load and how that screen is built.

`feature:dynamic` is the frontend feature that keeps one dynamic screen alive.

Its complete job is:

```text
receive a DynamicDestination
    ↓
call that destination endpoint
    ↓
receive SDUI JSON
    ↓
ask SDUI module to decode/check/render
    ↓
keep the user's current field/runtime state
    ↓
receive interactions from renderer
    ↓
execute backend actions
    ↓
update state or NavigationStore
```

It must not contain Login-specific, OTP-specific, Dashboard-specific, or Booking-specific branching.

Forbidden examples:

```kotlin
if (screenId == "partner_login") { ... }
if (templateType == "form_template") navigateToOtp()
```

---

# 2. Proposed package structure

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
DynamicFeatureFactory creates DynamicScreenStore for that destination
  ↓
DynamicScreenStore.load()
  ↓
GET /api/v1/partner/screen/auth_login
  ↓
NetworkResponse
  ↓
unwrap API envelope.data
  ↓
SduiDecoder
  ↓
SduiSupportChecker
  ↓
SduiScreen(partner_login)
  ↓
DynamicScreenState.screen = login screen
  ↓
DynamicScreen composable
  ↓
SduiRenderer
  ↓
Login appears
```

User types phone number:

```text
InputElementRenderer
  ↓
SduiInteraction.ValueChanged
  ↓
DynamicScreenStore
  ↓
fields["mobileNumber"] updated
  ↓
state emits
  ↓
Compose recomposes
```

User taps Continue:

```text
ButtonElementRenderer
  ↓
SduiInteraction.ActionTriggered(RequestAction)
  ↓
DynamicScreenStore
  ↓
SduiActionExecutor
  ↓
validate mobileNumber if requested
  ↓
resolve request values
  ↓
NetworkDataSource
  ↓
Send OTP response
  ↓
store response/auth-flow context as defined by final auth-flow ownership
  ↓
backend destination to partner_otp
  ↓
NavigationStore.Push(full DynamicDestination)
  ↓
OTP screen loads dynamically
```

Nothing in this flow hardcodes Login → OTP.

The same flow works for:

```text
Dashboard → Booking Details
Booking Details → Start Service
Earnings → Payout Details
Profile → KYC
```

---

# 4. `DynamicDestination.kt`

## Simple meaning

This class answers:

> **Which backend-driven screen is this navigation-stack entry?**

It stores the complete backend destination needed to load that screen.

Suggested shape:

```kotlin
@Serializable
data class DynamicDestination(
    val screenId: String,
    val templateId: String,
    val templateType: String,
    val endpoint: String,
    val method: RequestMethod,
    val authentication: Authentication,
) : NavigationDestination {
    override val navigationId: String = "$screenId:$templateId"
}
```

The exact stable `navigationId` can be finalized during implementation, but it must not throw away the destination information.

## Why full destination is needed

Example stack:

```text
partner_login
partner_otp
partner_dashboard
booking_details_421
```

Each stack entry needs enough information to load/restore itself.

Therefore store:

```text
screenId
templateId
templateType
endpoint
method
authentication
```

Do not store only `templateId`.

## What `templateType` does

Only tells SDUI which renderer should render the target template.

It never decides where to navigate.

## Back example

Current stack:

```text
Login → OTP → Dashboard → Booking Details
```

System Back:

```text
NavigationStore.Pop
    ↓
Dashboard DynamicDestination becomes current
```

No server screen rule is needed for normal stack Back.

## Refresh example

Current destination already contains:

```text
endpoint
method
authentication
```

Refresh can reload that same destination.

---

# 5. `DynamicScreenState.kt`

## Simple meaning

This class answers:

> **What is currently visible/known for this dynamic screen?**

One `DynamicScreenStore` owns one `StateFlow<DynamicScreenState>`.

Suggested conceptual shape:

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

The exact fields can be refined, but there must be one canonical mutable state owner.

## Live Login example

Before API returns:

```text
loading = true
screen = null
```

After Login SDUI arrives:

```text
loading = false
screen = partner_login
fields["mobileNumber"] = ""
```

After typing:

```text
fields["mobileNumber"] = "9876543210"
```

If button request is running:

```text
actionInFlight = true
```

## Live booking example

Backend state action marks Accept button loading:

```text
nodeStates["accept_booking"].loading = true
```

The immutable server `SduiScreen` is not modified.

---

# 6. `DynamicScreenIntent.kt`

## Simple meaning

An Intent means:

> **Something happened that the Store must handle.**

Use intents only for events entering the feature boundary.

Suggested examples:

```kotlin
sealed interface DynamicScreenIntent {
    data object Load : DynamicScreenIntent
    data object Retry : DynamicScreenIntent
    data object Refresh : DynamicScreenIntent
    data class Interaction(val value: SduiInteraction) : DynamicScreenIntent
    data object BackRequested : DynamicScreenIntent
}
```

Do not create dozens of intents for internal helper methods.

## Live example

```text
DynamicScreen first appears
    ↓
Load
    ↓
Store loads backend screen
```

```text
User changes mobile input
    ↓
Interaction(ValueChanged)
    ↓
Store updates field state
```

```text
User taps Retry after network failure
    ↓
Retry
    ↓
Store reloads current destination
```

---

# 7. `DynamicScreenEffect.kt`

## Simple meaning

An Effect is for a one-time UI/platform event that should not be stored as permanent screen state.

Use it only when necessary.

Possible examples:

```kotlin
sealed interface DynamicScreenEffect {
    data class ShowTransientMessage(val message: String) : DynamicScreenEffect
}
```

Navigation should normally go directly through the injected `NavigationStore`/navigation owner from action execution instead of creating a second navigation state machine.

External URI can similarly delegate to the capability owner.

If after implementation no true one-time feature effect remains, this class should be deleted instead of kept just because MVI examples often contain an Effect type.

This is an intentional anti-overengineering rule.

---

# 8. `DynamicScreenStore.kt`

## Simple meaning

This is the **brain of one currently displayed dynamic screen**.

It owns state and coordinates existing owners.

It does not render Compose itself.

It does not implement network transport.

It does not know Login/OTP business names.

## Main responsibilities

```text
load current destination
unwrap screen API envelope
ask SDUI decoder/support checker for screen
initialize bound field state
receive SduiInteraction
update field state
invoke SduiActionExecutor
apply returned state updates
retry/refresh
cancel jobs when feature closes/background policy requires
```

## Load flow

```text
Store receives Load
    ↓
state.loading = true
    ↓
create NetworkRequest from DynamicDestination
    ↓
NetworkDataSource.execute
    ↓
Success?
    ├── No  → state.failure
    └── Yes
         ↓
       unwrap envelope.data
         ↓
       SduiDecoder
         ↓
       SduiSupportChecker
         ↓
       initialize fields from bound elements
         ↓
       state.screen = decoded screen
       state.loading = false
```

## Interaction flow

```text
Store receives SduiInteraction
    ↓
ValueChanged?
    ├── Yes → update fields
    └── No, ActionTriggered
            ↓
          SduiActionExecutor.execute
```

## What Store must not do

Do not place inside Store:

```text
Ktor transport details
JSON union serializers
Compose rendering
per-element rendering logic
Navigation3 classes
device platform APIs
backend screen-name rules
```

## Why not split Store immediately

First simplify current architecture.

If the final Store remains understandable, keep it as one class.

Only extract another collaborator when there is a real second responsibility.

---

# 9. `DynamicScreen.kt`

## Simple meaning

This is the Compose entry point for one dynamic screen.

It takes state and draws the correct high-level state:

```text
loading
failure
content
overlay
```

Suggested flow:

```kotlin
@Composable
fun DynamicScreen(
    state: DynamicScreenState,
    onIntent: (DynamicScreenIntent) -> Unit,
    renderer: SduiRenderer,
)
```

Conceptually:

```text
if first load → loading UI
if failure    → error/retry UI
if screen     → SduiRenderer
if overlay    → render presentation over content
```

## Login example

```text
state.screen = partner_login
    ↓
DynamicScreen
    ↓
SduiRenderer.Render(partner_login)
```

## Booking example

```text
state.overlay = booking_cancel_sheet
    ↓
DynamicScreen keeps booking screen underneath
    ↓
presents cancel sheet target
```

`DynamicScreen` should not call the network or inspect backend action types.

---

# 10. `DynamicContextProvider.kt`

## Simple meaning

Backend actions may ask for values that do not belong to the current input fields.

Examples:

```text
$context(deviceId)
$context(authFlow.phoneNumber)
$context(legal.termsUri)
$context(partner.id)
```

`DynamicContextProvider` supplies those frontend/app values in one predictable JSON object.

Suggested API:

```kotlin
fun interface DynamicContextProvider {
    fun current(): JsonObject
}
```

## OTP example

OTP screen backend asks:

```text
$context(authFlow.phoneNumber)
```

Provider returns:

```json
{
  "authFlow": {
    "phoneNumber": "9876543210"
  }
}
```

## Future booking example

A backend request may ask for:

```text
$context(partner.id)
```

Provider can expose current authenticated partner ID.

## Important ownership rule

The provider does not become a second Store.

Long-lived data such as auth-flow data/session data should remain in their proper owner. The provider only creates the SDUI-visible context snapshot from those owners.

---

# 11. `SduiActionExecutor.kt`

## Simple meaning

This class answers:

> **Backend gave us this action. What should happen now?**

It executes the seven backend SDUI action types.

Suggested dependencies:

```text
NetworkDataSource
NavigationStore
CapabilityRegistry
SduiValueResolver
DynamicContextProvider
```

For state/overlay updates, prefer returning an execution result for the Store to reduce rather than secretly owning a second state object.

Example result model:

```kotlin
sealed interface SduiActionResult {
    data object Completed : SduiActionResult
    data class Response(val body: JsonElement?) : SduiActionResult
    data class Navigate(val destination: DynamicDestination) : SduiActionResult
    data class NodeStateChanged(...) : SduiActionResult
    data class OverlayChanged(...) : SduiActionResult
    data class Failure(val reason: String) : SduiActionResult
}
```

Exact result shape should stay as small as implementation allows.

## 11.1 Request action

Live Login example:

```text
Continue clicked
    ↓
RequestAction
    ↓
if validate=true, validate bound fields
    ↓
resolve:
  phoneNumber = $binding(mobileNumber)
  deviceId    = $context(deviceId)
    ↓
NetworkRequest
    ↓
NetworkDataSource
```

`responseMode = none`:

```text
store response as lastResponse/context owner as required
stay on current screen
```

`responseMode = destination`:

```text
read destination from response
convert to DynamicDestination
navigate
```

## 11.2 Navigate action

```text
backend sends full destination
    ↓
executor converts payload to DynamicDestination
    ↓
NavigationStore.Push(destination)
```

No template-based routing.

## 11.3 Present action

```text
present(targetId = cancel_sheet, bottom_sheet)
    ↓
return overlay state change
    ↓
DynamicScreenStore updates state.overlay
```

## 11.4 Dismiss action

```text
dismiss
    ↓
clear requested/current overlay
```

## 11.5 State action

```text
state(targetId = resend, set enabled = true)
    ↓
return NodeRuntimeState update
    ↓
Store updates nodeStates
```

## 11.6 External URI

```text
Terms text clicked
    ↓
external_uri($context(legal.termsUri))
    ↓
SduiValueResolver
    ↓
CapabilityRegistry(EXTERNAL_URI)
```

The wire action remains `external_uri`; we do not expose a generic server `CAPABILITY` action just because capability infrastructure exists internally.

## 11.7 Sequence

```text
sequence
  ↓
action 1
  ↓
action 2
  ↓
action 3
```

Children are normal `SduiAction`s.

No ParentAction/ChildAction classes.

---

# 12. `DynamicFeatureFactory.kt`

## Simple meaning

The navigation host should not manually know every dependency required to create a DynamicScreenStore.

The factory does that construction in one place.

Example:

```text
Navigation3Host sees DynamicDestination
    ↓
DynamicFeature
    ↓
DynamicFeatureFactory.create(destination, scope)
    ↓
DynamicScreenStore
```

Possible constructor dependencies:

```text
SduiDecoder
SduiSupportChecker
SduiRenderer/SduiRegistry reference where needed
NetworkDataSource
SduiActionExecutor
DynamicContextProvider
```

The factory has no state machine and no business logic.

Its only job is object creation/wiring.

---

# 13. How this connects to the SDUI module

`feature:dynamic` depends on SDUI contracts.

SDUI must not depend on `feature:dynamic`.

```text
feature:dynamic
    ↓
sdui
```

Examples:

```text
DynamicScreenState contains SduiScreen
DynamicScreen uses SduiRenderer
DynamicScreenStore receives SduiInteraction
SduiActionExecutor executes SduiAction
SduiActionExecutor uses SduiValueResolver
```

But SDUI classes know nothing about:

```text
NavigationStore
NetworkDataSource
Partner session
Koin
DynamicScreenStore
```

---

# 14. How this connects to NavigationStore

NavigationStore remains the one application back-stack owner.

```text
NavigationStore
  ↓ current destination
Navigation3Host
  ↓
DynamicFeature
```

Backend navigation action:

```text
SduiAction.Navigate
    ↓
SduiActionExecutor
    ↓
NavigationStore.Push(full DynamicDestination)
```

System Back:

```text
Navigation3/back callback
    ↓
NavigationStore.Pop
```

The dynamic module must not create another private back stack.

---

# 15. Refresh behavior

Refresh is not navigation inference.

The currently displayed `DynamicDestination` already knows how to load itself.

```text
Refresh intent
    ↓
DynamicScreenStore
    ↓
load current destination.endpoint again
```

Example Booking Details:

```text
booking details screen currently showing booking 421
    ↓
pull-to-refresh / backend-driven refresh policy when introduced
    ↓
GET same booking-details destination endpoint
    ↓
new SDUI screen replaces current rendered server model
```

Field/runtime-state preservation policy must be explicit per refresh behavior; do not silently merge old and new server trees.

---

# 16. Back-stack restoration

For process restoration, persist enough data to reconstruct every dynamic stack entry.

Minimum dynamic destination data:

```text
screenId
templateId
templateType
endpoint
method
authentication
```

Restored flow:

```text
process state contains dynamic destinations
    ↓
app restarts
    ↓
Splash/bootstrap establishes fresh authoritative root
    ↓
restore previous stack only if compatible with fresh root policy
    ↓
NavigationStore owns restored full destinations
```

Do not persist only a template ID and then guess an endpoint later.

---

# 17. Failure handling

Suggested failure categories:

```text
Network
Decode
UnsupportedContract
Action
```

Avoid creating many failure classes unless UI behavior differs.

Examples:

```text
Network → Retry
Decode → safe protocol error
UnsupportedContract → update/unsupported screen path as product decides
Action → action-specific error without corrupting current screen state
```

Do not expose raw exception text to production UI.

---

# 18. Lifecycle/coroutine rules

`DynamicScreenStore` may own:

```text
loadJob
actionJob
```

Rules:

1. entering a different destination cancels obsolete work for the previous feature instance;
2. repeated action taps respect `actionInFlight`/element loading policy;
3. cancellation is not converted into an error;
4. `close()` cancels feature-owned jobs;
5. never use global scope;
6. network/session implementations remain outside this feature.

---

# 19. Tests for every dynamic class

## `DynamicDestinationTest`

```text
navigationId stable
full destination data retained
serialization/restoration retains all load fields
endpoint safety inherited/checked at correct boundary
```

## `DynamicScreenStateTest`

```text
default loading/content state
field update preserves other fields
node state update preserves server screen
lastResponse update
failure transitions
```

## `DynamicScreenIntentTest`

Usually no heavy tests unless intent mapping contains logic. Prefer testing Store behavior from intents.

## `DynamicScreenEffectTest`

Only if effects remain after simplification. Delete the type if there is no genuine effect.

## `DynamicScreenStoreTest`

Use fake/mocked network + decoder + support checker + action executor.

Cover:

```text
Load success
Load network failure
Decode failure
Unsupported node/schema
Retry
Refresh same destination
ValueChanged updates binding
ActionTriggered calls executor
request result updates lastResponse
navigate result updates NavigationStore
state action updates node state
present/dismiss updates overlay
close cancels jobs
```

## `DynamicScreenTest`

Compose tests:

```text
loading displayed
failure/retry displayed
screen delegates to SduiRenderer
overlay displayed
interaction forwarded to Store/intent callback
```

## `DynamicContextProviderTest`

```text
deviceId available
authFlow.phoneNumber available when owner has it
legal URI paths available
missing optional context behaves predictably
no secret values exposed unless protocol requires them
```

## `SduiActionExecutorTest`

Cover every action and every dynamic reference combination.

## `DynamicFeatureFactoryTest`

Verify each created Store receives correct destination and required dependencies. Do not over-test DI framework internals.

---

# 20. Complete mock dynamic flow tests

## Test A — Login → OTP → Dashboard

```text
Bootstrap returns Login destination
    ↓
Login screen loads
    ↓
user types mobile
    ↓
Continue request resolves binding/context
    ↓
Send OTP succeeds
    ↓
OTP destination pushed
    ↓
OTP screen loads
    ↓
Verify action resolves otp + context + challenge response
    ↓
Dashboard destination pushed/reset according to backend response contract
    ↓
Dashboard loads
```

Assertions:

```text
no hardcoded screen switch
full destinations in NavigationStore
correct endpoint each time
correct auth mode each time
field state is one owner
correct response/context data survives where required
```

## Test B — Dashboard → Booking Details → Back

```text
Dashboard booking card emits Navigate
    ↓
full booking-details destination pushed
    ↓
Booking Details API loads
    ↓
system Back
    ↓
NavigationStore.Pop
    ↓
Dashboard destination becomes current
```

Assert no `templateType` routing occurs.

## Test C — Booking Details action sequence

```text
Accept Job
    ↓
state(button.loading=true)
    ↓
request accept API
    ↓
state/button or navigation according to returned actions/result
```

Assert sequence order and failure stopping policy.

## Test D — Presentation

```text
Cancel booking
    ↓
present cancellation bottom sheet
    ↓
confirm/dismiss
    ↓
overlay state correct
```

## Test E — Refresh

```text
Current screen = Earnings
    ↓
Refresh
    ↓
same DynamicDestination endpoint called
    ↓
new SduiScreen replaces current content
```

---

# 21. New-developer checklist for `feature:dynamic`

When adding a new backend-driven screen:

```text
Usually: do nothing in feature:dynamic.
```

If its template/elements already exist, backend simply returns the new screen and the existing dynamic flow handles it.

Example:

```text
new screen = partner_payout_history
existing template = default_template
existing nodes = stack/text/image/button
existing actions = request/navigate
```

Frontend dynamic module should need **zero new screen-specific classes**.

When adding a new SDUI element/action, follow the SDUI implementation plan. Only change dynamic feature if the new action requires a genuinely new application-side effect.

---

# 22. Anti-overengineering rules

1. No screen-specific Dynamic Stores.
2. No separate Login/OTP/Dashboard navigation logic.
3. No second back stack.
4. No ActionRegistry/PreparedAction layer inside this feature.
5. No FormStore beside DynamicScreenState.
6. No cache policy framework until a real product requirement needs one.
7. No realtime coordinator in the core dynamic path until backend/product contract requires it.
8. No background/capability SDUI actions unless backend defines them.
9. No class kept only because an architecture pattern says every MVI feature must have it.
10. Delete `DynamicScreenEffect` if there is no real effect after implementation.
11. Prefer one understandable Store over many tiny managers.
12. Use existing Network/Navigation/Capability owners instead of wrapping them repeatedly.

---

# 23. Final class connection diagram

```text
                         ┌─────────────────────┐
                         │   NavigationStore   │
                         └──────────┬──────────┘
                                    │ current DynamicDestination
                                    ▼
                         ┌─────────────────────┐
                         │ DynamicFeatureFactory│
                         └──────────┬──────────┘
                                    │ creates
                                    ▼
                         ┌─────────────────────┐
                         │ DynamicScreenStore  │
                         └──────┬───────┬──────┘
                                │       │
                     load screen│       │execute action
                                ▼       ▼
                    ┌──────────────┐  ┌──────────────────┐
                    │NetworkDataSrc│  │SduiActionExecutor│
                    └──────┬───────┘  └──────┬───────────┘
                           │                 │
                           ▼                 ├── NetworkDataSource
                    API envelope             ├── NavigationStore
                           │                 ├── CapabilityRegistry
                           ▼                 └── SduiValueResolver
                    SduiDecoder
                           │
                           ▼
                    SupportChecker
                           │
                           ▼
                      SduiScreen
                           │
                           ▼
                DynamicScreenState
                           │
                           ▼
                    DynamicScreen
                           │
                           ▼
                    SduiRenderer
                           │
                           ▼
                  SduiInteraction
                           │
                           └──────────────► DynamicScreenStore
```

---

# 24. Freeze checklist for dynamic feature

```text
[ ] full DynamicDestination stored in navigation stack
[ ] no templateType-based routing
[ ] normal Back uses NavigationStore.Pop
[ ] refresh reloads current destination
[ ] DynamicScreenState is sole mutable screen-state owner
[ ] no FormStore duplicate owner
[ ] no local input value competing with Store state
[ ] Store does not render Compose
[ ] renderer does not call network/navigation
[ ] ActionExecutor supports exact backend action vocabulary
[ ] DynamicContextProvider exposes only required safe context
[ ] Login→OTP→Dashboard mock flow green
[ ] Dashboard→Booking Details→Back mock flow green
[ ] request/state/present/dismiss/external-uri/sequence tests green
[ ] lifecycle cancellation tests green
[ ] real Desktop integration green
[ ] Android/iOS/Desktop CI green
```

Only after these are green should `feature:dynamic` be marked frozen.
