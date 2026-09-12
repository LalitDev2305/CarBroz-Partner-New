# CarBroz Partner Frontend — SDUI Simplification & Implementation Plan

> **Status:** REVIEW DRAFT — not frozen, not yet an implementation mandate.
>
> **Purpose:** define the proposed production SDUI architecture before implementation. This document intentionally starts from the actual backend JSON contract and the current frontend code path, then proposes the smallest scalable architecture that keeps strong boundaries, single responsibility, testability, and long-term extensibility without speculative framework layers.
>
> **Scope:** frontend SDUI runtime after Splash/Bootstrap. It includes network response handling, screen decoding, validation, typed SDUI models, rendering, input/form state, dynamic value resolution, actions, presentation/state overlays, navigation integration, process restoration, registration of new SDUI node types, coding standards, migration stages, and the full test strategy.
>
> **Out of scope for this document:** redesigning Login/OTP UI in hardcoded Compose, changing bootstrap ownership, changing the generic network/session layer, or implementing product features not represented in the backend SDUI contract.

---

## 1. Executive briefing

The current frontend SDUI implementation contains strong individual ideas, but the complete chain is larger than required and several parts implement a protocol that the backend does not emit. The target architecture proposed here follows four rules:

1. **The backend JSON contract is the protocol authority.** Frontend classes should mirror its semantics rather than invent a parallel command/property language.
2. **One owner per responsibility.** Decode, validate, state, render, resolve values, execute actions, and navigate must each have a clear owner.
3. **No speculative runtime frameworks.** A feature does not enter generic SDUI simply because the app may need it later. Capability/background/realtime infrastructure stays outside SDUI until the wire contract genuinely requires it.
4. **Prefer compile-time exhaustiveness over runtime registries when the vocabulary is small and closed.** Registration is retained only where it meaningfully helps node extensibility.

The proposed runtime is therefore:

```text
Splash / Bootstrap
    ↓ typed startup destination
NavigationStore
    ↓ DynamicDestination
DynamicScreenStore
    ↓ NetworkDataSource
GET backend screen endpoint
    ↓ API envelope
ApiEnvelope.data
    ↓
SduiDecoder
    ↓
SduiValidator
    ↓
SduiScreen
    ↓
DynamicScreenStore initializes field/runtime state
    ↓
SduiRenderer
    ↓
User Interaction
    ↓
SduiAction
    ↓
SduiActionExecutor
    ├── Request → NetworkDataSource
    ├── Navigate → NavigationStore
    ├── Present/Dismiss → DynamicScreenState overlay
    ├── State → DynamicScreenState node runtime overlay
    ├── ExternalUri → CapabilityRegistry internally
    └── Sequence → recursively execute children
```

This is deliberately much smaller than the current `DTO → validator → compatibility → normalizer → command → registry → action definition → prepared action → executor` pipeline.

---

## 2. Source-of-truth backend contract

Frontend implementation must support the backend production SDUI contract currently emitted by `@carbroz/sdui-engine`.

### 2.1 API transport envelope

A screen endpoint returns the standard API envelope:

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "Partner login screen fetched successfully.",
  "data": {
    "screenId": "partner_login",
    "schemaVersion": "3.0.0",
    "targetApp": "PARTNER",
    "template": {},
    "theme": {}
  },
  "traceId": "..."
}
```

Frontend must **unwrap `data` exactly once** before SDUI decoding.

### 2.2 Canonical screen structure

```text
Screen
└── Template
    └── Component[]
        ├── Element[]
        └── Section[]
            ├── Element[]
            └── Group[]
                └── Element[]
```

The hierarchy is flexible at two branch points:

```text
Component → Elements OR Sections
Section   → Elements OR Groups
```

A node must never carry both branches simultaneously.

### 2.3 Current production node vocabulary

```text
Templates
- stack_template
- form_template
- default_template

Components
- stack_component

Sections
- stack_section

Groups
- stack_group

Elements
- text
- image
- input
- button
```

### 2.4 Current action vocabulary

```text
request
navigate
present
dismiss
state
external_uri
sequence
```

Do not introduce generic SDUI wire actions such as `CAPABILITY`, `BACKGROUND`, `FORM`, `CONDITIONAL`, or unrelated local state commands until backend explicitly adds them to the schema.

### 2.5 Dynamic value references

The current backend uses structured references:

```json
{ "$binding": "mobileNumber" }
{ "$context": "deviceId" }
{ "$response": "data.challengeId" }
{ "$literal": "literal-value" }
```

The frontend must not translate these into a separate `$form.foo`, `$session.foo`, `$screen.foo` string language.

### 2.6 Known backend consistency defect to fix before protocol freeze

The OTP screen currently navigates back to `partner_login` with `templateType = stack_template`, while Login is actually built with `form_template`.

Target correction:

```text
partner_login
  templateId   = tpl_7K2M9Q
  templateType = form_template
```

Frontend should continue validating destination identity; backend should correct the inconsistent destination contract rather than weakening frontend validation.

---

## 3. Architectural principles

### 3.1 Single Responsibility Principle

Every production class must answer one clear question.

Good examples:

```text
SduiDecoder       → Can this JSON be decoded into the wire model?
SduiValidator     → Is this decoded screen structurally and semantically valid?
SduiValueResolver → What concrete JSON value does this dynamic reference produce?
SduiRenderer      → How is a validated screen drawn?
SduiActionExecutor→ What does this validated action do?
DynamicScreenStore→ What is the current dynamic screen state and how does it transition?
NavigationStore   → What is the application navigation stack?
```

Bad responsibility combinations:

```text
A renderer that performs network calls
A decoder that mutates screen state
A form store owning navigation
An action definition that also registers itself and serializes DTOs
A navigation destination carrying cache policy, network policy and UI state
```

### 3.2 Dependency direction

Target direction:

```text
feature:dynamic
    ↓
sdui
    ↓
foundation/design-system only where needed by rendering

feature:dynamic
    ↓
data:network
foundation:navigation
foundation:capabilities
```

SDUI must not depend on `feature:dynamic`.

Navigation foundation must not depend on SDUI.

### 3.3 UDF/MVI

Dynamic screen mutable state has one canonical owner: `DynamicScreenStore`.

Renderers read state and emit events. They do not own business state.

```text
State
 ↓
Compose
 ↓
Event
 ↓
Store
 ↓
Reducer / operation
 ↓
New State
```

### 3.4 Immutability

The server `SduiScreen` tree should be immutable after successful decode/validation.

Client-only runtime mutations belong in overlays:

```text
fieldStates: Map<String, FieldState>
nodeStates:  Map<String, NodeRuntimeState>
overlay:     SduiOverlay?
lastResponse: JsonElement?
```

Never mutate the decoded server model to express user typing, loading, selected, expanded, or presentation state.

### 3.5 YAGNI with explicit extension points

The architecture should make future extensions easy without implementing them now.

Example:

- Async validation can be added later behind `FieldValidator` when backend schema supports it.
- Background actions can be added later as a new `SduiAction.Background` only after backend schema defines it.
- Realtime updates can remain in `data:realtime` and integrate with a feature only when a product screen needs them.

---

## 4. Proposed Gradle/module structure

### 4.1 Preferred simplification

The current separation between `runtime:sdui`, `runtime:binding`, and `runtime:action` is not justified by the actual protocol because actions and value references are intrinsic parts of SDUI and have no independent application-level consumer.

Preferred target:

```text
:sdui
```

If changing Gradle coordinates immediately creates excessive migration risk, Phase 1 may keep the existing `:runtime:sdui` module but reorganize its packages exactly as described below. `runtime:binding` and `runtime:action` should then converge into it before freeze.

### 4.2 Target package structure

```text
sdui/
└── src/commonMain/kotlin/com/carbroz/sdui/
    ├── model/
    │   ├── SduiScreen.kt
    │   ├── SduiNode.kt
    │   ├── SduiAction.kt
    │   ├── SduiValueReference.kt
    │   ├── SduiTheme.kt
    │   ├── SduiValidation.kt
    │   └── SduiProperties.kt
    │
    ├── parser/
    │   ├── SduiDecoder.kt
    │   ├── SduiValidator.kt
    │   ├── SduiParseResult.kt
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
    │   ├── SduiOverlay.kt
    │   └── SduiRuntimeState.kt
    │
    ├── registry/
    │   ├── SduiNodeRegistry.kt
    │   └── CoreNodeDefinitions.kt
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
        └── SduiFixtures.kt   # test source set preferred; production only if shared fixture API is intentionally required
```

### 4.3 Dynamic feature structure

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

Do not create additional manager/coordinator/use-case classes unless the resulting class demonstrates a real second responsibility that cannot reasonably remain in its owner.

---

## 5. Complete class responsibilities

## 5.1 Model package

### `SduiScreen`

Responsibility: immutable representation of one decoded SDUI screen.

Suggested shape:

```kotlin
@Serializable
data class SduiScreen(
    val screenId: String,
    val schemaVersion: String,
    val targetApp: SduiTargetApp,
    val template: SduiTemplate,
    val theme: SduiTheme? = null,
    val metadata: JsonObject? = null,
)
```

Rules:

- no network logic;
- no Compose imports;
- no mutable state;
- no navigation logic;
- no normalization into different server names.

### `SduiTemplate`, `SduiComponent`, `SduiSection`, `SduiGroup`, `SduiElement`

Responsibility: mirror the structural protocol.

Recommended model:

```kotlin
data class SduiTemplate(
    val id: String,
    val type: String,
    val properties: JsonObject,
    val components: List<SduiComponent>,
)

data class SduiComponent(
    val id: String,
    val type: String,
    val properties: JsonObject,
    val content: ComponentContent,
)

sealed interface ComponentContent {
    data class Elements(val values: List<SduiElement>) : ComponentContent
    data class Sections(val values: List<SduiSection>) : ComponentContent
}
```

Use the same pattern for Section.

`SduiElement` should preserve real backend semantics:

```kotlin
data class SduiElement(
    val id: String,
    val type: String,
    val properties: JsonObject,
    val actions: Map<String, SduiAction> = emptyMap(),
    val binding: SduiElementBinding? = null,
    val validation: SduiValidationRule? = null,
    val analytics: JsonObject? = null,
    val accessibility: JsonObject? = null,
    val visibility: JsonObject? = null,
    val metadata: JsonObject? = null,
)
```

Do not add `NodePath`. Server guarantees structural ID uniqueness and actions are already attached to the element/span that emits them.

### `SduiAction`

Responsibility: exact typed representation of the backend action union.

```kotlin
sealed interface SduiAction {
    data class Request(...) : SduiAction
    data class Navigate(...) : SduiAction
    data class Present(...) : SduiAction
    data class Dismiss(...) : SduiAction
    data class State(...) : SduiAction
    data class ExternalUri(...) : SduiAction
    data class Sequence(val actions: List<SduiAction>) : SduiAction
}
```

Use one model only. Do not create a second `Command` or `PreparedAction` hierarchy.

### `SduiValueReference`

Responsibility: typed structured dynamic references.

```kotlin
sealed interface SduiValueReference {
    data class Binding(val key: String) : SduiValueReference
    data class Context(val path: String) : SduiValueReference
    data class Response(val path: String) : SduiValueReference
    data class Literal(val value: JsonElement) : SduiValueReference
}
```

### `SduiTheme`

Responsibility: screen visual theme contract only.

No design-system lookup or Compose conversion in the model.

### `SduiValidationRule`

Initial scope:

```kotlin
data class SduiValidationRule(
    val required: Boolean = false,
    val pattern: String? = null,
    val message: String? = null,
)
```

Only extend when backend schema adds a supported rule.

---

## 5.2 Parser package

### `SduiDecoder`

Responsibility: `JsonElement → decoded wire model`.

Recommended API:

```kotlin
interface SduiDecoder {
    fun decode(json: JsonElement): SduiDecodeResult
}
```

Implementation should use Kotlin Serialization with explicit serializers for union types where required.

Rules:

- Accept `JsonElement`, not `String`, when network already provides JSON.
- Never call `body.toString()` just to parse again.
- Unknown action type = deterministic decode failure.
- Unknown node `type` may decode structurally; supportability is checked by validator/registry.
- `ignoreUnknownKeys` decision must be explicit and tested. Prefer strictness for core structural/action objects; tolerate metadata extension bags where backend schema intentionally permits open records.

### `SduiVersionPolicy`

Responsibility: decide whether `schemaVersion` is readable.

Keep it tiny:

```kotlin
class SduiVersionPolicy(
    private val supported: Set<String>,
) {
    fun supports(version: String): Boolean = version in supported
}
```

No independent protocolVersion/minimumClientVersion framework unless backend adds those fields.

### `SduiValidator`

Responsibility: validate semantic invariants not guaranteed by serialization alone.

Checks should include:

- supported schema version;
- correct target app (`PARTNER` or acceptable global value);
- nonblank screen/template/node IDs;
- globally unique structural IDs;
- component contains exactly one legal branch;
- section contains exactly one legal branch;
- all node types are registered for the correct structural level;
- action-specific constraints;
- request endpoints are relative and not protocol-relative/absolute;
- request method/authentication are supported;
- `sequence` is non-empty;
- state set/toggle invariants;
- binding key nonblank;
- destination identity fields nonblank;
- dynamic destination method requirements if backend constrains it;
- template/component/section/group children are nonempty when schema requires it.

Do not validate visual design choices such as whether a font size looks good.

### `SduiParseResult`

One stable public result type:

```kotlin
sealed interface SduiParseResult {
    data class Success(val screen: SduiScreen) : SduiParseResult
    data class Failure(val error: SduiParseError) : SduiParseResult
}
```

Avoid exposing thrown exceptions to feature code for malformed server documents.

---

## 5.3 Value package

### `SduiExecutionContext`

Responsibility: immutable values available when an action executes.

```kotlin
data class SduiExecutionContext(
    val bindings: Map<String, JsonElement>,
    val context: JsonObject,
    val lastResponse: JsonElement?,
)
```

Do not add namespaces without a real backend reference type.

### `JsonPathResolver`

Responsibility: safely traverse object/array JSON paths such as:

```text
data.challengeId
legal.termsUri
```

Recommended behavior:

- object key traversal;
- optional numeric array index only if backend contract needs it;
- missing path returns null/result failure, never crashes;
- no reflection;
- no arbitrary expression evaluation.

### `SduiValueResolver`

Responsibility: recursively resolve value references inside request bodies/presentation values/etc.

Examples:

```text
{ "$binding":"otp" }
→ fieldStates["otp"].value

{ "$context":"authFlow.phoneNumber" }
→ context.authFlow.phoneNumber

{ "$response":"data.challengeId" }
→ lastResponse.data.challengeId

{ "$literal": {"a":1} }
→ exact JSON object
```

The resolver must preserve JSON types.

It should recursively support nested objects and arrays because request bodies can contain nested structures.

Failure should be explicit:

```kotlin
sealed interface SduiValueResolution {
    data class Success(val value: JsonElement) : SduiValueResolution
    data class MissingBinding(val key: String) : SduiValueResolution
    data class MissingContext(val path: String) : SduiValueResolution
    data class MissingResponse(val path: String) : SduiValueResolution
}
```

---

## 5.4 Runtime state package

### `FieldState`

Responsibility: current client state of one backend-bound input.

Minimal initial model:

```kotlin
data class FieldState(
    val value: JsonElement,
    val touched: Boolean = false,
    val error: String? = null,
)
```

Do not create a separate `FormStore` StateFlow.

### `NodeRuntimeState`

Responsibility: local runtime overlay for backend `state` actions.

```kotlin
data class NodeRuntimeState(
    val visible: Boolean? = null,
    val enabled: Boolean? = null,
    val selected: Boolean? = null,
    val expanded: Boolean? = null,
    val checked: Boolean? = null,
    val loading: Boolean? = null,
    val value: JsonElement? = null,
)
```

Null means “use server/base property”.

### `SduiOverlay`

Responsibility: currently presented server-owned target.

```kotlin
data class SduiOverlay(
    val targetId: String,
    val mode: PresentationMode,
)
```

`DismissAction` clears matching/current overlay.

### `SduiRuntimeState`

Prefer keeping these fields directly on `DynamicScreenState` unless grouping improves clarity. Do not create a wrapper merely for symmetry.

---

## 5.5 Registry package

The registry is only for node type support, not actions.

### `SduiNodeRegistry`

Responsibility: determine which renderer/property decoder handles a node type at a structural level.

A simple immutable registry is sufficient:

```kotlin
class SduiNodeRegistry(
    definitions: List<SduiNodeDefinition>,
) {
    private val definitionsByKey = definitions.associateBy { it.level to it.type }

    init {
        require(definitionsByKey.size == definitions.size) { "Duplicate SDUI definition" }
    }

    fun find(level: SduiNodeLevel, type: String): SduiNodeDefinition? =
        definitionsByKey[level to type]
}
```

No mutable builder lifecycle is required in production.

### `CoreNodeDefinitions`

One canonical list:

```kotlin
object CoreNodeDefinitions {
    val all = listOf(
        StackTemplateDefinition,
        FormTemplateDefinition,
        DefaultTemplateDefinition,
        StackComponentDefinition,
        StackSectionDefinition,
        StackGroupDefinition,
        TextElementDefinition,
        ImageElementDefinition,
        InputElementDefinition,
        ButtonElementDefinition,
    )
}
```

Do not create separate `TemplateDefinitions`, `ComponentDefinitions`, `SectionDefinitions`, `GroupDefinitions`, `ElementDefinitions` collection objects unless separate ownership later becomes useful.

---

## 5.6 Rendering package

### `SduiRenderContext`

Responsibility: read-only runtime values and event sink required during rendering.

```kotlin
data class SduiRenderContext(
    val fields: Map<String, FieldState>,
    val nodeStates: Map<String, NodeRuntimeState>,
    val onInteraction: (SduiInteraction) -> Unit,
)
```

Do not pass network/navigation services to renderers.

### `SduiInteraction`

Responsibility: renderer → store event.

```kotlin
sealed interface SduiInteraction {
    data class Action(
        val sourceId: String,
        val eventName: String,
        val action: SduiAction,
    ) : SduiInteraction

    data class InputChanged(
        val elementId: String,
        val bindingKey: String,
        val value: JsonElement,
    ) : SduiInteraction
}
```

This directly carries the action. Do not emit a path and perform a second lookup through `SduiCommandIndex`.

### `SduiRenderer`

Responsibility: recursively render one already validated `SduiScreen`.

Expected flow:

```text
RenderScreen
 ↓
RenderTemplate
 ↓
RenderComponent(s)
 ↓
Render Section/Group branches where present
 ↓
RenderElement
```

Rendering failure should be explicit and feature-observable, but malformed unsupported screens should ideally have been rejected before rendering.

### `SduiModifierResolver`

Responsibility: convert shared server visual properties to Compose `Modifier` operations.

It should handle only common concerns:

- width/height/min/max;
- fillMaxWidth/fillMaxHeight/fillMaxSize;
- padding;
- background;
- border;
- shape;
- weight only where parent scope can legally apply it.

Keep parent-scope-specific modifiers out of a generic resolver when Compose scope matters.

### Accessories

Backend text/button can use `leading` / `trailing` accessories.

Keep them generic but small:

```text
AccessoryRenderer
├── icon
└── divider
```

Unknown accessory type should follow explicit policy: reject screen if required for correctness, or render nothing only if schema treats accessories as optional/degradable. Decide and test once; do not silently vary by renderer.

---

## 6. Node renderer responsibilities

## 6.1 Templates

### `FormTemplateRenderer`

Responsibility: visual/layout semantics of `form_template` only.

It must **not own form state**.

`form_template` can be treated as a stack-like container with semantic role `form`. Form validation is controlled by action `validate=true` and bound input metadata.

### `StackTemplateRenderer`

Responsibility: stack layout semantics for `stack_template`.

### `DefaultTemplateRenderer`

Responsibility: default screen container semantics.

Do not duplicate stack code. Share a small `StackContainerRenderer` or `StackLayoutProperties` mapper if the three templates share identical container layout behavior.

## 6.2 Component / Section / Group

`StackComponentRenderer`, `StackSectionRenderer`, and `StackGroupRenderer` may delegate to the same pure stack-layout function because their layout semantics are the same while structural level remains distinct for validation/registration.

This is acceptable reuse:

```text
StackComponentRenderer ─┐
StackSectionRenderer   ─┼→ StackContainer()
StackGroupRenderer     ─┘
```

Do not merge Component/Section/Group models merely because rendering is shared; their structural semantics differ.

## 6.3 Text

`TextElementRenderer` must support the actual contract:

- exactly one of `text` or `spans`;
- dynamic value reference as text where allowed;
- font size;
- weight;
- line height;
- letter spacing;
- color/disabledColor;
- alignment;
- width/height/maxWidth/weight/fillMaxWidth;
- enabled;
- leading/trailing accessories;
- element action map;
- span-level action.

Span clicks should emit the span's own action directly.

## 6.4 Image

`ImageElementRenderer` must support:

- URL;
- width/height/min/max;
- fillMaxWidth/fillMaxHeight;
- contentScale;
- onClick/onLongClick if provided.

Image loading must use one shared image-loading abstraction/library. Do not let each element create its own HTTP client.

Relative backend asset URLs must be resolved against the configured backend asset/base URL in one owner.

## 6.5 Input

`InputElementRenderer` is stateless regarding canonical value.

Reads:

```text
context.fields[binding.key].value
```

Emits:

```text
InputChanged(bindingKey, newValue)
```

Supports:

- placeholder;
- keyboardType;
- maxLength;
- textAlign;
- dimensions;
- background/border/shape;
- normal input;
- segmented presentation for OTP.

Validation error presentation reads `FieldState.error`.

No internal `remember { mutableStateOf(canonicalValue) }`.

A small ephemeral UI-only state such as focus object or animation state is allowed because it is not business/runtime data.

## 6.6 Button

`ButtonElementRenderer` supports:

- text;
- dimensions/fill;
- font size/weight/color;
- background;
- shape;
- leading/trailing accessory;
- enabled/runtime-enabled overlay;
- onClick/onLongClick.

Renderer never executes action directly. It emits `SduiInteraction.Action`.

---

## 7. Dynamic feature classes

## 7.1 `DynamicDestination`

Responsibility: one semantic navigation destination representing one backend-driven screen acquisition instruction.

Recommended model:

```kotlin
@Serializable
data class DynamicDestination(
    val screenId: String,
    val templateId: String,
    val templateType: String,
    val endpoint: String,
    val method: RequestMethod,
    val authentication: RequestAuthentication,
) : NavigationDestination {
    override val navigationId: String = "dynamic:$screenId:$templateId"
}
```

Do not put screen cache policy, action state, form state, or network result in the destination.

For bootstrap, mapping from `StartupDestination` occurs once in app composition.

## 7.2 `DynamicScreenState`

Single canonical feature state:

```kotlin
data class DynamicScreenState(
    val destination: DynamicDestination? = null,
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

One StateFlow only.

## 7.3 `DynamicScreenIntent`

Use explicit intents if they make event flow clearer:

```kotlin
sealed interface DynamicScreenIntent {
    data class Show(val destination: DynamicDestination) : DynamicScreenIntent
    data class Interaction(val interaction: SduiInteraction) : DynamicScreenIntent
    data object Retry : DynamicScreenIntent
    data object DismissOverlay : DynamicScreenIntent
    data object Backgrounded : DynamicScreenIntent
}
```

Do not create intent types for simple private helper calls that never cross the store boundary.

## 7.4 `DynamicContextProvider`

Responsibility: provide trusted client context referenced by `$context`.

Example result:

```json
{
  "deviceId": "...",
  "authFlow": {
    "phoneNumber": "+919..."
  },
  "legal": {
    "termsUri": "...",
    "privacyUri": "..."
  }
}
```

This provider may depend on configuration/session/device state, but the SDUI value resolver should only see the resulting `JsonObject`.

Avoid exposing entire Session objects/config objects to SDUI.

## 7.5 `SduiActionExecutor`

Responsibility: execute one typed SDUI action using already-owned platform/application services.

Dependencies likely:

```text
NetworkDataSource
NavigationStore
CapabilityRegistry
SduiValueResolver
DynamicContextProvider
```

It should not own a StateFlow. It returns an execution result/operation result to the Store, or invokes small callbacks supplied by the Store for state/overlay updates.

Preferred clean design:

```kotlin
class SduiActionExecutor(
    private val network: NetworkDataSource,
    private val navigation: NavigationStore,
    private val capabilities: CapabilityRegistry,
    private val values: SduiValueResolver,
) {
    suspend fun execute(
        action: SduiAction,
        environment: ActionEnvironment,
    ): ActionExecutionResult
}
```

`ActionEnvironment` contains current bindings/context/response and functions/policies needed to update screen-local state without giving executor direct access to the whole Store.

Do not create one class per action unless a particular action becomes independently complex.

## 7.6 `DynamicScreenStore`

Responsibility: sole MVI owner of one dynamic screen feature instance.

Owns:

- loading current destination;
- API envelope unwrapping;
- parse/validate result handling;
- destination identity verification;
- initial field state creation;
- input updates;
- field validation;
- node state overlay updates;
- overlay state;
- last response;
- action-in-flight status;
- retry;
- cancellation when lifecycle/background requires it.

It does not implement:

- JSON serializer internals;
- layout rendering;
- Navigation reducer logic;
- network auth/retry policy;
- capability platform implementations.

### Store load flow

```text
show(destination)
  ↓
state.loading = true
  ↓
NetworkRequest(destination)
  ↓
NetworkDataSource.execute
  ↓
HTTP status check
  ↓
unwrap API envelope.data
  ↓
SduiDecoder.decode
  ↓
SduiValidator.validate
  ↓
verify:
 screenId == destination.screenId
 template.id == destination.templateId
 template.type == destination.templateType
  ↓
initialize fields from bound input elements
  ↓
state = Ready(screen, fields)
```

---

## 8. Action execution design

## 8.1 Request

Wire fields:

```text
method
endpoint
authentication
validate
body?
responseMode = none | destination
```

Flow:

```text
RequestAction
  ↓
if validate == true
  ↓
validate current bound fields
  ├── invalid → update field errors and stop
  └── valid
       ↓
resolve body recursively
       ↓
NetworkRequest
       ↓
NetworkDataSource.execute
       ↓
2xx?
  ├── no → action failure
  └── yes
       ↓
state.lastResponse = body
       ↓
responseMode
  ├── none → finish
  └── destination
       ↓
extract/parse canonical destination from response contract
       ↓
NavigationStore dispatch
```

The exact response shape for `responseMode=destination` must be captured in contract fixtures before implementation. Do not invent the destination path.

## 8.2 Navigate

Backend navigate action already contains a dynamic destination.

Flow:

```text
NavigateAction
 ↓
validate destination
 ↓
DynamicDestination
 ↓
NavigationCommand.Push (default current semantics unless wire adds transition)
```

If product needs replace/reset later, add an explicit backend field first rather than hidden frontend guessing.

## 8.3 Present / Dismiss

```text
PresentAction(targetId, mode)
 ↓
validate target exists
 ↓
state.overlay = SduiOverlay(targetId, mode)
 ↓
Compose overlay host renders target
```

Dismiss clears overlay according to target/current semantics.

## 8.4 State

```text
StateAction(targetId, operation, property, value?)
 ↓
existing NodeRuntimeState or empty
 ↓
set/toggle supported property
 ↓
state.nodeStates[targetId] = updated
```

The server screen remains immutable.

## 8.5 External URI

```text
ExternalUriAction(uri reference)
 ↓
SduiValueResolver
 ↓
string URI
 ↓
CapabilityRegistry(EXTERNAL_URI/open)
```

Generic capabilities remain internal infrastructure; they are not exposed as generic SDUI wire actions.

## 8.6 Sequence

Composite Pattern only:

```text
Sequence [A, B, C]
  ↓
execute A
  ↓ if success
execute B
  ↓ if success
execute C
```

Rules:

- preserve order;
- stop at first failure;
- cancellation rethrows;
- no parallel execution unless protocol later introduces it;
- navigation/terminal action ordering must follow explicit server rules if such constraints exist.

No `ParentAction` / `ChildAction` model is needed.

---

## 9. Validation design

### 9.1 Screen contract validation

Test and enforce:

```text
schema version supported
target app supported
all structural ids unique
all ids/types nonblank
node type registered at correct level
one valid branch per component/section
required child arrays nonempty
```

### 9.2 Request validation

```text
endpoint starts with '/'
endpoint does not start '//'
endpoint does not contain '://'
method supported
authentication supported
responseMode supported
```

### 9.3 State action validation

```text
set    → value required
toggle → value forbidden
toggle → boolean-capable runtime property only
```

### 9.4 Form/input validation

Initial contract:

```text
required
pattern
message
```

One helper is enough:

```kotlin
object SduiFieldValidator {
    fun validate(value: JsonElement, rule: SduiValidationRule?): String?
}
```

Do not add asynchronous or cross-field validation until backend schema defines it.

---

## 10. Rendering and runtime property resolution

Render-time effective property = server property overridden by runtime state where allowed.

Example:

```text
server enabled = true
runtime enabled = false
→ effective enabled = false
```

Recommended helper:

```kotlin
fun effectiveEnabled(
    serverEnabled: Boolean?,
    runtime: NodeRuntimeState?,
): Boolean = runtime?.enabled ?: serverEnabled ?: true
```

Keep these helpers pure and unit tested.

Avoid a giant universal `PropertyResolver` that handles every property of every node. Shared layout behavior belongs in common helpers; element-specific behavior remains in the element renderer.

---

## 11. Complete startup-to-SDUI application flow

```text
App launch
  ↓
SplashScreen
  ↓
SplashStore
  ↓
ResolveStartupUseCase
  ├── SessionStore.restore
  ├── BootstrapRepository.load
  └── PartnerConfigStore.update
  ↓
StartupResult.Ready(StartupDestination)
  ↓
SplashEffect.Navigate
  ↓
app:composition maps StartupDestination → DynamicDestination
  ↓
NavigationProcessStateBridge.applyAfterBootstrap
  ↓
NavigationStore.ResetTo/restore compatible stack
  ↓
Navigation3Host sees DynamicDestination
  ↓
DynamicFeature(destination)
  ↓
DynamicScreenStore.show(destination)
  ↓
NetworkDataSource
  ↓
GET /api/v1/partner/screen/auth_login
  ↓
API envelope
  ↓
extract data
  ↓
SduiDecoder
  ↓
SduiValidator
  ↓
SduiScreen
  ↓
identity verification against DynamicDestination
  ↓
field initialization
  ↓
DynamicScreenState.Ready
  ↓
SduiRenderer
  ↓
Login rendered
```

Login Continue:

```text
Input typing
 ↓
InputChanged(binding=mobileNumber)
 ↓
DynamicScreenStore.fields updated

Continue click
 ↓
RequestAction(send_otp)
 ↓
validate mobileNumber
 ↓
resolve:
  phoneNumber = $binding.mobileNumber
  deviceId    = $context.deviceId
 ↓
POST send_otp
 ↓
store lastResponse
 ↓
responseMode=destination
 ↓
DynamicDestination(partner_otp)
 ↓
NavigationStore.Push
 ↓
DynamicFeature(partner_otp)
 ↓
load OTP SDUI
```

OTP Verify:

```text
OTP typing
 ↓
fields[otp]

Verify click
 ↓
validate otp
 ↓
resolve body:
 challengeId = $response.data.challengeId
 phoneNumber = $context.authFlow.phoneNumber
 otp         = $binding.otp
 deviceId    = $context.deviceId
 ↓
POST verify_otp
 ↓
response destination
 ↓
NavigationStore
```

Important implementation question to freeze before coding: when moving Login → OTP, the OTP action needs the Send OTP response `challengeId`. Therefore either:

1. required auth-flow response/context is intentionally carried into the next destination context, or
2. the backend destination contains/contextualizes the challenge data, or
3. a scoped auth-flow state owner stores it outside an individual screen instance.

Do **not** rely on accidental `DynamicScreenStore.lastResponse` survival if a new destination creates a new store. This must be explicitly designed and contract-tested before freeze.

Recommended approach: create a small feature/application-owned `DynamicFlowContextStore` only if the backend truly requires cross-screen transient values. Keep it scoped and generic enough for SDUI flow context, but do not create it until the exact send-OTP response/destination contract confirms the requirement.

---

## 12. Navigation architecture

Keep current navigation fundamentals:

```text
NavigationDestination
NavigationState
NavigationCommand
NavigationReducer
NavigationStore
Navigation3Host adapter
NavigationProcessStateBridge
```

They have clear responsibilities and are independent of SDUI.

### Dynamic navigation rule

SDUI action executor produces application semantic navigation commands. It does not directly manipulate Navigation3.

```text
SduiAction.Navigate
 ↓
DynamicDestination
 ↓
NavigationCommand.Push
 ↓
NavigationStore
 ↓
Navigation3Host
```

### Restoration

Prefer serializing `DynamicDestination` directly if it is `@Serializable`.

Avoid maintaining a second `DynamicScreenInstructionDto` if it mirrors the destination one-to-one.

Process restoration must still be applied only after fresh bootstrap authorizes/defines the current root.

### Navigation future systems

Deep-link/guard/adaptive navigation classes should stay only when there is a current production consumer or near-term frozen requirement. A repo-wide usage gate should be performed during migration before deleting them.

---

## 13. How to add a new SDUI node type

This is the extension process that all future developers should follow.

### Example: adding `card` element

#### Step 1 — backend contract first

Backend adds and validates:

```text
Element type = card
properties schema
supported events
defaults
```

Do not start frontend implementation based on a speculative local model.

#### Step 2 — add frontend property model only if typed properties materially help

Example:

```kotlin
data class CardProperties(...)
```

For very small/simple nodes, decoding from `JsonObject` inside the renderer may be enough. Avoid a property class solely for ceremony.

#### Step 3 — implement renderer

```kotlin
object CardElementRenderer : ElementRenderer {
    override val type = "card"

    @Composable
    override fun Render(...)
}
```

Renderer responsibilities only:

- interpret card properties;
- render Compose;
- read runtime state;
- emit interactions.

#### Step 4 — register once

Add to `CoreNodeDefinitions.all` or the immutable node registry list.

#### Step 5 — add contract fixture

Add representative backend JSON for card.

#### Step 6 — add focused tests

At minimum:

- decoder/validator accepts valid card;
- invalid card properties fail predictably;
- registry resolves card at `ELEMENT` level;
- renderer smoke test;
- action event emitted correctly where supported.

No other module should require modification unless card introduces a genuinely new cross-cutting capability.

---

## 14. How to add a new template/component/section/group

Use the same six-step process:

```text
backend schema
 ↓
frontend level/type contract
 ↓
renderer
 ↓
registration
 ↓
fixture
 ↓
tests
```

Level correctness is important:

```text
stack_template  registered as TEMPLATE
stack_component registered as COMPONENT
stack_section   registered as SECTION
stack_group     registered as GROUP
```

Do not normalize all four to a generic `STACK` type. Exact server type names make compatibility/debugging much clearer.

If multiple structural types share identical visual behavior, share a private composable/helper:

```text
StackTemplateRenderer ──┐
StackComponentRenderer ─┼→ StackContainer
StackSectionRenderer ───┤
StackGroupRenderer ─────┘
```

This is code reuse without erasing protocol identity.

---

## 15. How to add a new action

Only after backend action schema is defined.

Example future `copy_to_clipboard`:

1. Add backend action schema and fixtures.
2. Add one `SduiAction.CopyToClipboard` subtype.
3. Decoder supports it.
4. Validator enforces its invariants.
5. Add one branch in exhaustive `SduiActionExecutor.execute`.
6. Delegate to existing capability/service implementation.
7. Add unit + integration tests.

Do not create an `ActionDefinition`, registry entry, `PreparedAction`, and separate executor unless that action becomes independently complex enough to justify it.

Compiler exhaustiveness is the registration mechanism for action types.

---

## 16. Coding standards for this implementation

### 16.1 Class creation rule

Before creating a class, answer:

```text
1. What single responsibility does this class own?
2. Which production caller needs it?
3. Why cannot an existing owner reasonably handle this responsibility?
4. What would make this class change?
```

If answers 1–3 are weak, do not create the class.

### 16.2 Naming

Use domain names, not generic architecture words.

Good:

```text
SduiValueResolver
DynamicScreenStore
TextElementRenderer
NodeRuntimeState
```

Avoid:

```text
Manager
Helper
Processor
Engine
Coordinator
Factory
```

unless the class genuinely performs that well-defined role.

### 16.3 Functions

Prefer small functions with one result.

Guideline, not mechanical rule:

- public method typically expresses one use case/action;
- extract private functions when they improve meaning, reuse, or testability;
- do not split a five-line coherent operation across five one-line wrappers.

### 16.4 Sealed types

Use sealed interfaces for closed semantic unions such as actions and failures.

Use plain data classes/strings for open server type identifiers.

### 16.5 Exceptions

- Programmer/configuration invariant → `require/check` acceptable.
- Malformed/untrusted backend JSON → typed failure, not app crash.
- Coroutine cancellation → always rethrow.
- Platform/network failures → map to stable feature failure.

### 16.6 JSON

- Stay in `JsonElement` while data is JSON.
- Avoid `JsonElement → String → JsonElement` round trips.
- Resolve dynamic references without converting everything to strings.
- Preserve number/boolean/null/object/array types.

### 16.7 Compose

- Composables render state and emit callbacks.
- No network calls from renderer.
- No navigation service lookup from renderer.
- No duplicate canonical input state in `remember`.
- `remember` is fine for ephemeral UI concerns such as focus/animation/local object allocation.

### 16.8 Dependency injection

Register application-level owners/services only.

Do not put every renderer or pure utility in Koin.

Recommended DI examples:

```text
SduiNodeRegistry        singleton
SduiDecoder             singleton
SduiValidator           singleton
SduiValueResolver       singleton
DynamicContextProvider  singleton/scoped according to data
DynamicFeatureFactory   singleton
```

Pure singleton objects and stateless renderers can be directly referenced by `CoreNodeDefinitions`.

### 16.9 Logging / observability

Log at meaningful boundaries:

```text
screen request started/completed/failed
SDUI decode rejected
SDUI validation rejected
unsupported schema/type
action execution failed
navigation destination identity mismatch
```

Do not log every node render/recomposition.

Never log sensitive field values such as OTP/session tokens.

---

## 17. Failure model

Suggested feature failures:

```kotlin
sealed interface DynamicScreenFailure {
    data class Network(val code: String) : DynamicScreenFailure
    data class Api(val code: String, val message: String?) : DynamicScreenFailure
    data class Decode(val code: String) : DynamicScreenFailure
    data class Validation(val issues: List<SduiValidationIssue>) : DynamicScreenFailure
    data class IdentityMismatch(val expected: String, val actual: String) : DynamicScreenFailure
    data class Action(val code: String) : DynamicScreenFailure
}
```

Avoid `failure.toString()` as a production error contract.

UI can map failures to user-friendly retry/error screens without exposing internal class names.

---

## 18. API response handling

Create one generic response-envelope decoder in the correct data/network/API boundary if such an abstraction already exists. Do not create an SDUI-specific copy of the global API envelope unless necessary.

Dynamic feature should conceptually receive:

```text
ApiResult<SduiScreenJson>
```

rather than parsing HTTP envelope details in multiple places.

Before implementation, inspect the existing network/data conventions and reuse the canonical envelope model if one already exists.

---

## 19. Migration plan

No big-bang blind rewrite. Each phase must leave the repository compiling and tested where practical.

### Phase 0 — contract capture and freeze candidate

Create JSON fixtures from the actual backend production builders/endpoints:

```text
partner_login.json
partner_otp.json
partner_dashboard.json (when contract is ready)
request_response_none.json
request_response_destination.json
```

Also capture malformed fixtures for negative testing.

Resolve the backend OTP → Login templateType mismatch.

Determine exact `send_otp` and `verify_otp` response envelopes, especially how `challengeId` and destination are represented.

Deliverable: contract test corpus.

### Phase 1 — new wire model + parser

Implement exact backend screen/action/value-reference models.

Implement:

```text
SduiDecoder
SduiVersionPolicy
SduiValidator
```

Do not connect renderer yet.

Tests must prove Login and OTP fixtures parse successfully.

### Phase 2 — simplify runtime state

Introduce new:

```text
FieldState
NodeRuntimeState
SduiOverlay
DynamicScreenState fields
```

Remove dependency on `FormStore` for the new path.

### Phase 3 — rendering parity

Implement exact nodes needed by Login/OTP:

```text
form_template
stack_component
stack_section
stack_group
text
image
input
button
accessories
```

Run renderer tests against parsed fixtures.

### Phase 4 — value resolution

Implement structured:

```text
$binding
$context
$response
$literal
```

Remove old binding namespace/parser from the new flow.

### Phase 5 — actions

Implement seven exact action types and one `SduiActionExecutor`.

Connect request/navigate/present/dismiss/state/external_uri/sequence.

### Phase 6 — DynamicScreenStore convergence

Connect:

```text
network → API envelope → parse → validate → identity → state → renderer → action → navigation
```

Remove old `Command`, `ActionRegistry`, `ActionDefinition`, `ActionPreparer`, `PreparedAction` from production path.

### Phase 7 — remove obsolete code/modules

Only after no production references remain:

```text
runtime:binding old implementation
runtime:action old implementation
old compatibility protocol
old normalizer command hierarchy
FormStore/runtime factory/contributor layers
NodePath / command index
speculative SDUI background/capability actions
unused dynamic cache/restoration abstractions if confirmed unnecessary
```

Use repository-wide reference checks before every deletion.

### Phase 8 — navigation restoration simplification

Make `DynamicDestination` directly serializable if practical.

Remove mirrored instruction DTO/codec if no longer needed.

Keep bootstrap-authoritative root restoration behavior.

### Phase 9 — package/module convergence

Move packages only after behavior is stable.

Do not mix large package moves with semantic rewrites where avoidable because that makes review/debugging harder.

### Phase 10 — full regression and freeze

Run all SDUI tests, feature tests, architecture tests, full Gradle tests/assemble, Android/Desktop runtime checks, and exact-SHA CI including iOS.

Only then update architecture documentation from REVIEW DRAFT to FROZEN.

---

## 20. Testing strategy

Testing should be fixture-first and layered so a failure immediately identifies the broken responsibility.

### Test categories

```text
1. Contract fixture tests
2. Decoder tests
3. Validator tests
4. Value resolver tests
5. Field validation tests
6. Runtime state reducer tests
7. Renderer tests
8. Action executor tests
9. DynamicScreenStore tests
10. Navigation integration tests
11. End-to-end mock SDUI flow tests
12. Architecture/dependency tests
13. Platform smoke tests
```

---

## 21. Contract fixture test suite

Use exact/mock JSON under commonTest resources or Kotlin fixture builders.

Recommended fixtures:

### Valid

```text
valid/minimal_screen.json
valid/login_screen.json
valid/otp_screen.json
valid/direct_element_component.json
valid/component_with_sections.json
valid/section_with_elements.json
valid/section_with_groups.json
valid/all_action_types.json
valid/all_value_reference_types.json
valid/all_runtime_state_properties.json
valid/text_spans.json
valid/segmented_input.json
valid/image.json
valid/accessories.json
```

### Invalid

```text
invalid/unsupported_schema.json
invalid/duplicate_node_id.json
invalid/component_with_both_elements_and_sections.json
invalid/component_with_no_content.json
invalid/section_with_both_elements_and_groups.json
invalid/section_with_no_content.json
invalid/unknown_required_node_type.json
invalid/absolute_request_url.json
invalid/protocol_relative_url.json
invalid/empty_sequence.json
invalid/state_set_without_value.json
invalid/state_toggle_with_value.json
invalid/state_toggle_value_property.json
invalid/text_with_both_text_and_spans.json
invalid/text_with_neither_text_nor_spans.json
invalid/input_binding_blank.json
invalid/destination_missing_identity.json
```

Each valid production fixture should come from or be mechanically equivalent to backend schema/builder output.

---

## 22. Decoder tests

Must cover:

- minimal valid screen;
- Login exact JSON;
- OTP exact JSON;
- all seven action variants;
- nested sequence;
- all four dynamic value references;
- nested request body objects/arrays;
- text spans and span actions;
- element actions map with multiple events;
- optional metadata/accessibility/analytics/visibility;
- invalid enum/action shape;
- malformed JSON object;
- wrong primitive type;
- unknown fields according to chosen strictness policy.

Goal: decoder tests prove syntax/model conversion only, not semantic screen support.

---

## 23. Validator tests

Must cover:

- supported/unsupported schema version;
- targetApp PARTNER/GLOBAL policy;
- unique IDs across all structural levels;
- unknown template/component/section/group/element types;
- type registered at wrong level;
- component branch invariants;
- section branch invariants;
- endpoint safety;
- action invariants;
- destination validation;
- binding/validation metadata consistency;
- valid Login and OTP pass fully.

Use table-driven tests wherever many cases differ only by one field.

---

## 24. Value resolver tests

### `$binding`

```text
existing key → exact JsonElement
missing key → MissingBinding
boolean/number/string/object values preserved
```

### `$context`

```text
flat path
nested path
missing path
wrong intermediate type
```

### `$response`

```text
response exists/path exists
response absent
path missing
nested object
```

### `$literal`

```text
string
number
boolean
null
object
array
```

### Recursive body

Example:

```json
{
  "a": { "$binding": "one" },
  "nested": {
    "b": { "$context": "x.y" },
    "items": [
      { "$response": "data.id" },
      { "$literal": false }
    ]
  }
}
```

Assert exact resolved JSON equality.

---

## 25. Field validation tests

Required cases:

```text
required + blank → error
required + value → success
pattern + match → success
pattern + mismatch → configured message
no rule → success
```

Login-specific:

```text
9876543210 → valid
5876543210 → invalid
12345      → invalid
```

OTP-specific:

```text
123456 → valid
12345  → invalid
abcdef → invalid
```

Do not use Android instrumentation for pure validation logic.

---

## 26. Runtime state tests

For each `NodeRuntimeState` field:

```text
SET visible
SET enabled
SET selected
SET expanded
SET checked
SET loading
SET value
TOGGLE each legal boolean property
```

Also:

```text
toggle uses server/base value when runtime value absent
runtime override wins over server value
state for node A does not affect node B
unknown target rejected predictably
```

Overlay tests:

```text
present dialog
present bottom_sheet
present popup
dismiss current
dismiss matching target
invalid target
```

---

## 27. Renderer tests

Prefer Compose Multiplatform/common-compatible testing where supported; otherwise use the appropriate platform test target while keeping renderer input common.

### Structural renderer tests

```text
form_template renders component children
stack_component elements branch
stack_component sections branch
stack_section elements branch
stack_section groups branch
stack_group elements
```

### Element renderer tests

Text:

```text
plain text
spans
style values
leading/trailing accessories
span action callback
```

Image:

```text
url mapping
contentScale
size/fill constraints
click action
```

Input:

```text
reads FieldState value
emits InputChanged
maxLength
phone keyboard mapping
number keyboard mapping
segmented OTP presentation
shows validation error
```

Button:

```text
text/styles
leading/trailing
runtime enabled override
emits exact action
```

### Important renderer contract test

Given an element with:

```text
actions["onClick"] = X
```

clicking must emit **the exact action X directly**. No NodePath/index lookup should exist.

---

## 28. Action executor tests

### Request

Mock `NetworkDataSource`.

Test:

```text
correct method
correct endpoint
correct authentication
resolved body
validate=false skips field validation
validate=true blocks invalid fields
2xx none stores last response
non-2xx failure
transport failure
missing binding blocks call
missing context blocks call
missing response blocks call
```

### Navigate

Mock/real in-memory `NavigationStore`:

```text
valid destination → Push
invalid destination → failure
```

### Present/Dismiss

Assert returned state operation/result, not Compose UI.

### State

All set/toggle combinations.

### External URI

Mock capability registry/provider and assert resolved URI is forwarded exactly once.

### Sequence

```text
A then B then C order
failure at B stops C
cancellation propagates
nested sequence preserves order
```

---

## 29. DynamicScreenStore tests

Use fake dependencies only; no real network required.

### Loading

```text
Show → loading true
successful response → screen ready
network failure → Network failure
non-2xx → Api/Network failure according to contract
missing data → Decode/API failure
malformed SDUI → Decode failure
validation failure → Validation failure
identity mismatch screenId
identity mismatch templateId
identity mismatch templateType
```

### Field initialization

From Login fixture:

```text
mobileNumber field exists
initial value correct
validation metadata attached/accessible
```

From OTP fixture:

```text
otp field exists
```

### Input flow

```text
InputChanged → field value updated
second field unaffected
renderer observes new state
```

### Action flow

```text
click sets actionInFlight
success clears actionInFlight
failure clears actionInFlight and sets failure
second action ignored/serialized according to chosen policy
```

### Retry/cancellation

```text
retry repeats current destination request
show new destination cancels previous load
background cancellation leaves consistent non-loading state
close cancels jobs
CancellationException not converted to failure
```

---

## 30. Navigation tests

Keep existing pure navigation tests and add dynamic-specific integration:

```text
Splash bootstrap Ready → ResetTo DynamicDestination
Push OTP from Login
Pop OTP → Login
OTP Edit Number Navigate → correct Login destination
process restoration only after fresh bootstrap
restored root mismatch → fresh bootstrap root wins
restored root match → compatible stack restored
serializing/restoring DynamicDestination preserves navigationId
```

Explicit regression test for the known backend mismatch should fail until backend is corrected, then remain as a guard.

---

## 31. End-to-end mock SDUI tests

These are the most valuable high-level tests and should use only fake network responses and in-memory navigation/capability implementations.

### E2E 1 — Login render/load

```text
bootstrap destination partner_login
 ↓
fake GET login returns API envelope + Login SDUI
 ↓
store parses/validates
 ↓
state.screen == partner_login
 ↓
field mobileNumber initialized
 ↓
renderer smoke succeeds
```

### E2E 2 — invalid Login validation

```text
mobileNumber = 123
click Continue
 ↓
validation error
 ↓
POST send_otp NOT called
```

### E2E 3 — Login → OTP

```text
mobileNumber = valid
click Continue
 ↓
request body contains exact phoneNumber/deviceId
 ↓
fake send_otp response succeeds with destination
 ↓
navigation current == partner_otp
 ↓
fake GET OTP returns OTP screen
 ↓
OTP ready
```

### E2E 4 — OTP verify dynamic references

```text
OTP input = 123456
 ↓
Verify
 ↓
challengeId resolved from correct flow response/context source
phone resolved from authFlow context
otp resolved from binding
deviceId resolved from context
 ↓
verify_otp receives exact JSON body
```

### E2E 5 — OTP edit number

```text
click displayed phone/edit target
 ↓
Navigate action
 ↓
Login destination pushed/replaced according to frozen semantics
 ↓
identity matches actual Login form_template
```

### E2E 6 — legal URL

```text
click Terms span
 ↓
$context legal.termsUri resolved
 ↓
EXTERNAL_URI capability called exactly once
```

### E2E 7 — sequence

Mock screen containing sequence:

```text
state loading=true
request something
state loading=false
```

Assert order and final state.

### E2E 8 — present/dismiss

Mock target node and present action, assert overlay lifecycle.

### E2E 9 — malformed server screen safe failure

Return duplicate ID/unsupported action/unsafe URL and assert no renderer/network action crash.

---

## 32. Mock infrastructure for tests

Create small reusable fakes in test source sets:

```text
FakeNetworkDataSource
FakeDynamicContextProvider
FakeCapabilityProvider/Registry
InMemoryNavigationStore (existing real NavigationStore is already cheap enough)
SduiFixtureLoader / SduiFixtures
RecordingActionObserver if useful
```

Do not introduce a full mocking framework unless existing project standards already require one. Lightweight fakes make state transitions easy to inspect in KMP common tests.

Example fake network behavior:

```kotlin
class FakeNetworkDataSource : NetworkDataSource {
    val requests = mutableListOf<NetworkRequest>()
    var handler: suspend (NetworkRequest) -> NetworkResult = { error("No response configured") }

    override suspend fun execute(request: NetworkRequest): NetworkResult {
        requests += request
        return handler(request)
    }
}
```

This supports deterministic complete-flow testing.

---

## 33. Architecture tests

Add source/dependency gates for the target design.

Examples:

```text
sdui must not depend on feature:dynamic
sdui model/parser/value must not depend on Compose
render package may depend on Compose/design system
foundation:navigation must not depend on sdui
feature:dynamic may depend on sdui/network/navigation/capabilities
runtime:binding no longer used after convergence
runtime:action no longer used after convergence
no production reference to Command/PreparedAction/ActionRegistry after deletion
no body.toString() passed back into SDUI parser
no renderer depends on NetworkDataSource/NavigationStore
```

A lightweight Gradle/source architecture verification task is appropriate because these boundaries are easy to regress.

---

## 34. Test commands / verification gates

Exact task names should be confirmed after module convergence, but the freeze gate should conceptually include:

```powershell
# focused SDUI common tests
.\gradlew.bat :runtime:sdui:allTests

# dynamic feature tests
.\gradlew.bat :feature:dynamic:allTests

# navigation tests
.\gradlew.bat :foundation:navigation:allTests

# architecture gate
.\gradlew.bat verifyStartupArchitecture
# plus a new/updated verifySduiArchitecture gate

# JVM/Desktop full
.\gradlew.bat allTests assemble

# desktop runtime smoke with dev backend
.\gradlew.bat :desktopApp:run
```

If the module is renamed to `:sdui`, use that final coordinate instead.

CI freeze criteria:

```text
JVM/Android tests GREEN
Desktop tests/build GREEN
iOS compile/framework/host builds GREEN
published-foundation boundary checks GREEN
exact latest commit SHA verified
```

No “frozen green” declaration on an older SHA.

---

## 35. Coverage expectations

Do not chase arbitrary 100% line coverage. Require behavioral coverage of every protocol branch.

Mandatory branch coverage:

```text
all structural branch variants
all production node types
all action types
all value reference types
all runtime state operations/properties
all request response modes
all validation rules currently supported
all navigation transitions currently supported by wire/application policy
all failure categories
```

For pure parser/value/action/state packages, high line/branch coverage should naturally be achievable.

Renderer snapshot/golden tests may be added later for visual regression, but functional rendering tests are mandatory first.

---

## 36. Performance considerations

Do not pre-optimize with complex indexes unless profiling shows need.

Current screen sizes are small enough for straightforward recursive rendering/traversal.

Good baseline:

- decode once per network screen response;
- validate once;
- keep immutable screen tree;
- do not rebuild registries per recomposition;
- do not recursively scan the tree on every keystroke if fields can be initialized once;
- use stable IDs/keys in repeated Compose lists;
- no `JsonElement → String → JsonElement` conversion;
- no per-node DI lookups;
- no per-recomposition regex compilation where validation pattern can be cached if needed.

Add performance complexity only after measurement.

---

## 37. Security considerations

Because backend controls behavior, SDUI is effectively a remote instruction protocol. Validate accordingly.

Mandatory protections:

```text
relative API endpoints only
no protocol-relative URLs
no arbitrary absolute network action URLs
allowed authentication enum only
external URI delegated through controlled capability policy
no reflection/eval/expression language in value resolver
bounded JSON/reference/path sizes where sensible
unsupported action/node fails closed
sensitive values not logged
session/network auth still owned by data:network
```

SDUI must never be able to inject Authorization headers or bypass canonical network authentication policy.

---

## 38. Observability

Recommended stable events/metrics:

```text
sdui.screen.load
sdui.screen.decode_failure
sdui.screen.validation_failure
sdui.screen.identity_mismatch
sdui.action.execute
sdui.action.failure
sdui.render.failure
```

Useful dimensions only:

```text
screenId
templateType
actionType
failureCode
```

Never attach:

```text
OTP
phone number
token
raw request body
full context payload
```

---

## 39. Design-pattern decisions

Use patterns only where they solve a current problem.

### Composite

Used naturally by:

```text
SduiAction.Sequence
```

### Strategy / registry

Use for node renderers because node vocabulary is extensible by type and level.

Keep the registry immutable and simple.

### State reducer / UDF

`DynamicScreenStore` owns state transitions.

### Adapter

Use at real boundaries:

```text
Sdui request action → NetworkRequest
Sdui external_uri   → CapabilityRegistry
DynamicDestination  → NavigationDestination
```

### Patterns intentionally not used

```text
Abstract Factory per node
Visitor over every SDUI object
Command + PreparedCommand duplication
Service Locator inside renderers
Chain of Responsibility for seven fixed actions
Plugin architecture for action types
```

These add ceremony without current benefit.

---

## 40. Definition of done before architecture freeze

This document should only become FROZEN after all of the following are agreed:

```text
[ ] exact backend screen envelope confirmed
[ ] Login JSON captured
[ ] OTP JSON captured
[ ] send_otp response contract captured
[ ] verify_otp response contract captured
[ ] responseMode=destination shape confirmed
[ ] OTP→Login templateType backend mismatch fixed
[ ] cross-screen challenge/authFlow context ownership decided
[ ] target package/module structure approved
[ ] seven action semantics approved
[ ] validation policy approved
[ ] presentation target semantics approved
[ ] state action semantics approved
[ ] process restoration scope approved
[ ] cache/realtime exclusion or inclusion explicitly approved
[ ] migration phases approved
[ ] test matrix approved
```

Only after approval should implementation begin.

---

## 41. Proposed final class relationship diagram

```text
                        ┌──────────────────────┐
                        │   DynamicScreen      │
                        │      Compose         │
                        └──────────┬───────────┘
                                   │ state / interactions
                                   ▼
                        ┌──────────────────────┐
                        │ DynamicScreenStore   │
                        │ single state owner   │
                        └───┬──────┬──────┬────┘
                            │      │      │
             load screen ───┘      │      └── execute action
                                   │
                ┌──────────────────┘
                ▼
       ┌───────────────────┐
       │   SduiRenderer    │
       └────────┬──────────┘
                │
     ┌──────────┼───────────────────────────────┐
     ▼          ▼            ▼          ▼       ▼
 Template   Component     Section     Group   Element
 Renderers   Renderer     Renderer   Renderer Renderers

DynamicScreenStore load side:

NetworkDataSource
      ↓
 API envelope
      ↓ data
 SduiDecoder
      ↓
 SduiValidator ←── SduiNodeRegistry
      ↓
 SduiScreen

Action side:

SduiInteraction.Action
      ↓
SduiActionExecutor
 ├── SduiValueResolver ← SduiExecutionContext
 ├── NetworkDataSource
 ├── NavigationStore
 └── CapabilityRegistry
```

---

## 42. Proposed ownership matrix

| Concern | Single owner |
|---|---|
| HTTP/session/retry | `data:network` |
| API envelope | canonical transport/data model |
| SDUI JSON syntax decoding | `SduiDecoder` |
| SDUI semantic validation | `SduiValidator` |
| Supported node types | `SduiNodeRegistry` |
| Screen immutable contract | `SduiScreen` model |
| Dynamic reference resolution | `SduiValueResolver` |
| Current input values/errors | `DynamicScreenStore.fields` |
| Runtime visible/enabled/etc. | `DynamicScreenStore.nodeStates` |
| Current overlay | `DynamicScreenStore.overlay` |
| Last action response | `DynamicScreenStore` or explicitly approved flow context owner |
| Screen rendering | `SduiRenderer` + node renderers |
| Action execution | `SduiActionExecutor` |
| App back stack | `NavigationStore` |
| Navigation mechanics | `foundation:navigation` |
| Platform URI opening | `CapabilityRegistry` implementation |
| Bootstrap | existing Splash/ResolveStartupUseCase path |

If two classes appear to own the same row, implementation should be reconsidered.

---

## 43. Open decisions requiring review before freeze

These are the only areas I would intentionally leave unresolved until exact backend response fixtures are inspected:

1. **Cross-screen response/context ownership for OTP challengeId.** A new OTP screen instance cannot accidentally depend on a destroyed Login Store's `lastResponse`.
2. **Exact destination response format for request `responseMode=destination`.** Frontend should mirror it, not invent it.
3. **Presentation target rendering semantics.** Confirm whether `targetId` always points to an existing node/subtree and how hidden presentation content is represented in the current hierarchy.
4. **Unknown optional node/accessory degradation policy.** Decide fail-closed versus safe omission per schema semantics.
5. **Whether `runtime:sdui` is renamed to `sdui` immediately or after semantic convergence.** Architecture is the same either way; avoid risky rename churn during behavior migration.
6. **Whether dynamic screen in-memory cache is genuinely required.** Default recommendation is remove/defer unless a current UX requirement proves need.
7. **Whether Dynamic Realtime belongs in the first SDUI runtime.** Default recommendation is keep realtime infrastructure separate until a concrete screen contract needs it.

Everything else in this document can be reviewed as the proposed target architecture.

---

## 44. Review checklist for the architecture owner

When reviewing this document, evaluate each proposal against:

```text
Does this map directly to actual JSON?
Does one class have one clear reason to change?
Can a developer understand the request→render→interaction flow without tracing 10 abstractions?
Can malformed backend JSON fail safely?
Can every production action be tested without Compose/platform UI?
Can every renderer be tested with fixture data?
Does adding one node require only its renderer/model/registration/tests?
Does adding one action require only model/decoder/validator/executor/tests?
Is canonical mutable state owned exactly once?
Does navigation remain independent of SDUI?
Are platform capabilities kept behind existing platform abstractions?
Is any class present only for hypothetical future needs?
```

If a class cannot survive these questions, simplify it before freeze.

---

## 45. Final proposed implementation rule

The implementation should optimize for **correctness, clarity, extension safety and debuggability**, not abstraction count.

The target mental model for any developer should fit in one sentence:

> Fetch a server screen, decode and validate it once, keep the server tree immutable, render it from one feature state, resolve only the four supported dynamic value references, execute only the seven supported actions, and send navigation through the existing NavigationStore.

That is the architecture this document proposes for review.
