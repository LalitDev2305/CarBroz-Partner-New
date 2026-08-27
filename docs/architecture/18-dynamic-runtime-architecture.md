# CarBroz Dynamic Runtime Architecture

Status: frozen architecture addendum for the post-Splash runtime.

## 1. Product flow ownership

Splash is the only currently known static application screen. Every screen after Splash is backend-driven. Client code must not encode business flow names such as Login, OTP, Dashboard, Booking, KYC, Profile, Availability or Earnings into navigation or runtime architecture.

Canonical runtime loop:

`Splash -> startup/session restore -> /api/v1/app -> validated DynamicScreenInstruction -> DynamicDestination -> trusted screen request -> SDUI decode -> schema validation -> compatibility -> normalization -> runtime IR -> render -> semantic command -> binding/form/action execution -> local effect or next DynamicDestination -> repeat`.

The backend chooses the next screen. The client owns validation, trusted execution, rendering, lifecycle, navigation mechanics, security and restoration safety.

## 2. Dynamic screen identity

The canonical identity remains three distinct values:

- `screenId`: dynamic screen identity.
- `templateId`: concrete template identity/back-stack participant.
- `templateType`: registered reusable Template rendering definition.

`templateType` is rendering behavior, never business-screen identity.

## 3. Flexible strongly typed SDUI hierarchy

`Template` is the tree root, `Component` is the mandatory composition level and `Element` is terminal. `Section` and `Group` are optional structural levels.

Allowed grammar:

```text
Template := Component+
Component := Element+ | Section+
Section := Element+ | Group+
Group := Element+
Element := terminal
```

Therefore all of the following are valid, including simultaneously inside one Template:

```text
Component -> Elements
Component -> Sections -> Elements
Component -> Sections -> Groups -> Elements
```

Every Component chooses its own branch independently. Every Section chooses its own branch independently. The backend must not send dummy Section or Group nodes.

The relationships are explicit XOR relationships, not an unsafe generic tree. A Component may not mix direct Elements and Sections as siblings. A Section may not mix direct Elements and Groups as siblings. Groups must contain terminal Elements. Empty branches are invalid.

Historical `SubComponent`, `Child` and `ChildrenData` terminology is not part of this runtime.

## 4. Definition and registration ownership

Each hierarchy level owns its supported definitions:

- `TemplateDefinitions`
- `ComponentDefinitions`
- `SectionDefinitions`
- `GroupDefinitions`
- `ElementDefinitions`

`SduiRegistryFactory` only composes those five collections into one immutable `SduiRegistry`; it does not manually enumerate concrete UI definitions.

Adding a new UI type should modify only its owning hierarchy level plus focused tests. For example, adding a `RatingElement` must not require changes to navigation, `SduiRendererDispatcher`, action execution, unrelated Templates or other hierarchy levels.

Definitions remain atomic: each `SduiDefinition<P>` owns its server `NodeType`, typed property decoding and, when renderable, its Compose rendering semantics.

## 5. Typed property architecture

Raw transport `properties` are untrusted `JsonObject` values. They are never rendered directly. During normalization the registered definition decodes them into typed `NodeProperties`.

Common safe visual/layout concerns are represented by `CommonNodeProperties`, including visibility, fill behavior, dimensions/min-max constraints, padding, margin, background, border and shape radius. Concrete definitions compose that common contract with their own specialized typed properties.

Examples:

- Stack containers own axis, spacing and main/cross-axis alignment.
- Form Template owns adaptive/readable content-width and content-padding policy.
- Input owns field identity, initial value, label, placeholder, required/enabled/read-only state, max length and keyboard type.
- Button owns text, enabled and loading state.
- Text owns typography token, alignment and max lines.

Do not replace this with one giant universal nullable property object. Do not duplicate common Compose modifier behavior across every renderer. `applyCommonNodeProperties` is the shared modifier boundary; container-specific arrangement remains owned by the container definition.

## 6. Rendering

The normalized sealed child relationships determine traversal per node. The renderer does not assume one fixed depth for the entire screen.

`SduiRendererDispatcher` is a composite orchestrator only. It resolves `(NodeKind, NodeType)` through the immutable registry and delegates rendering to the registered definition. Common visibility is enforced before delegation.

Canonical typed traversal utilities such as `Screen.elements()` are reused by interaction/form infrastructure so hierarchy walking is not duplicated in multiple subsystems.

## 7. Binding and form ownership

Bindings are resolved at command execution time from execution snapshots. Generic namespaces include form, screen, session, configuration, event, result and runtime values. Session bindings deliberately exclude access/refresh tokens and other secrets.

Interactive form state is owned by `runtime:form`, not business-screen code. Form-capable Element definitions optionally implement `FormFieldContributor`. Application composition discovers those capabilities through the SDUI registry; it does not import or identify concrete Input property classes.

An INPUT renderer emits its stable `fieldId`. Runtime updates the generic FormStore and `$form.<field>` bindings resolve the latest value when a command executes.

## 8. Generic action semantics

An interaction is not assumed to be a network call and a network call is not assumed to return another screen.

Generic commands include:

- `REQUEST`: relative trusted request with `SCREEN` or `NONE` response mode.
- `CAPABILITY`: platform capability through the capability registry.
- `NAVIGATION`: local back-stack operation.
- `PRESENTATION`: message/dialog/sheet state.
- `LOCAL_STATE`: runtime value mutation.
- `FORM`: generic form operation.
- `BACKGROUND`: deferred/continuous execution semantic.
- `SEQUENCE`: ordered composition of generic commands.
- `CONDITIONAL`: binding-resolved boolean branch selection.

Sequence/conditional commands are meta-actions prepared recursively by `ActionPreparer`; renderers remain unaware of action composition semantics. Composite command recursion and sequence sizes are protocol-bounded.

No Partner-specific command type belongs in the generic runtime.

## 9. One transition contract

Bootstrap and subsequent dynamic actions converge on the same trusted `DynamicScreenInstruction` / `DynamicDestination` model. There is no separate client-known bootstrap route enum.

A dynamic instruction carries screen identity, trusted relative request, semantic transition, stable back-stack key and restore policy.

Screen transitions are framework-neutral: `PUSH`, `REPLACE`, `RESET`, `STAY`. Local back-stack actions use `POP` and `POP_TO`. Navigation framework types are not exposed to backend payloads.

## 10. Screen cache and process restoration

Dynamic screen restoration uses a bounded process-memory `DynamicScreenCache`, separate from HTTP caching because it contains normalized screen state, form state, runtime values, action result and presentation state.

Process restoration is whole-stack and fail-closed. Missing/malformed/unknown/unsafe entries restore no prefix and navigation falls back atomically to Splash. `CACHE_ONLY` destinations are not persisted because replay could repeat a non-idempotent request.

`DynamicNavigationProcessStateBridge` serializes only semantic restored destinations into transient platform-owned saved-state. It must never be persisted in durable preferences because a dynamic instruction may contain arbitrary request payload data.

## 11. Startup, external events, background and sync

Session restoration precedes `/api/v1/app`. Bootstrap uses optional-session authentication, so the backend selects the first dynamic screen for both signed-out and returning sessions.

Realtime, notifications/deep-links and future external entry mechanisms must use the same dynamic-instruction contract. A realtime refresh must not blindly replay non-idempotent acquisition requests.

Deferred background work and genuinely continuous execution remain separate semantics. Automatic sync remains opt-in until an operation has explicit safe offline/idempotency/conflict behavior.

## 12. Extension invariants

The runtime is considered extensible only when these remain true:

1. New Element definitions do not require central renderer/navigation/action modifications.
2. New Template/Component/Section/Group definitions register only through their owning level.
3. Common properties are decoded once and composed with type-specific properties.
4. No renderer performs networking directly.
5. No screen-specific Login/OTP/Dashboard/etc. renderer or destination is introduced.
6. Transport JSON is decoded, validated, compatibility-checked and normalized before rendering/execution.
7. Different Component hierarchy depths can coexist in one Template response.

## 13. Prohibited regressions

Do not reintroduce:

- `ReferenceDestination`, `ReferenceSduiStore`, `REFERENCE_SCREEN_ENDPOINT` or bootstrap `Reference` routes;
- `LoginDestination`, `OtpDestination`, `DashboardDestination` or equivalent business-flow destinations;
- a fixed `Template -> Component -> Section -> Group -> Element` requirement;
- dummy Section/Group nodes;
- unsafe `Node(children: List<Node>)` hierarchies;
- one giant concrete SDUI definition list;
- one universal nullable property object;
- application-composition knowledge of concrete Input properties;
- direct networking from renderers;
- absolute backend-provided request URLs;
- raw platform navigation/background/capability types in SDUI wire payloads;
- mutation of canonical normalized server `Screen` objects for transient state;
- automatic replay of non-idempotent requests during restoration or realtime refresh;
- durable storage of arbitrary dynamic-instruction payloads;
- duplicate bootstrap/action dynamic-screen contracts.
