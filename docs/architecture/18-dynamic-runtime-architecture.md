# CarBroz Dynamic Runtime Architecture

Status: frozen architecture addendum for the post-Splash runtime.

## 1. Feature and product-flow ownership

Splash is the only currently known static application screen. Every screen after Splash is backend-driven. Client code must not encode business flow names such as Login, OTP, Dashboard, Booking, KYC, Profile, Availability or Earnings into navigation or runtime architecture.

Feature ownership is explicit:

- `feature:splash` owns static Splash presentation only: UI state, user/lifecycle intents and one-shot effects. It does not own networking, bootstrap DTOs, caching, session persistence, headers or startup policy.
- `app:startup` owns Partner-specific startup orchestration: session restoration task, Partner bootstrap transport contract, client-metadata header provider and pure bootstrap policy evaluation.
- `runtime:application` is the single canonical owner of startup lifecycle state and ordered startup task execution.
- `feature:dynamic` is the single feature for every backend-driven screen. It owns dynamic destination/request contracts, screen loading/error/retry lifecycle, runtime screen cache, action-result orchestration, external/realtime screen events and the Compose Dynamic feature UI.
- `foundation:navigation` is the sole navigation mechanics owner: back-stack state, commands, restoration policy and the canonical `Navigation3Host` presentation adapter.
- `runtime:sdui` is the reusable SDUI engine used by `feature:dynamic`; it owns protocol validation, normalization, definitions, registries, `SduiRuntime` assembly and normalized-screen rendering through `SduiScreenRenderer`. It does not own feature lifecycle or navigation.
- `runtime:action` is the sole generic semantic-action preparation owner, including `ActionPreparerFactory`; it does not execute network/platform feature effects itself.
- `app:composition` is a composition root only. It assembles process dependencies, maps navigation destinations to feature content, consumes feature effects and owns the application-specific transient process-state adapter. It must not implement another startup store, navigation, dynamic-screen, SDUI, action or networking engine.
- Android/Desktop/iOS hosts remain thin platform entry points.

Canonical runtime loop:

`Splash -> common lifecycle Foreground -> ApplicationRuntime -> session restore -> app:startup Partner bootstrap -> GET /api/v1/partner/bootstrap through canonical networking -> validated DynamicScreenInstruction -> SplashEffect.Navigate -> deferred restoration check -> DynamicDestination -> foundation:navigation -> feature:dynamic -> trusted screen request -> runtime:sdui decode -> schema validation -> compatibility -> normalization -> runtime IR -> render -> semantic command -> runtime:action preparation -> binding/form resolution -> feature effect adapter -> local effect or navigation command -> foundation:navigation -> same feature:dynamic -> repeat`.

The backend chooses the next screen. The client owns validation, trusted execution, rendering, lifecycle, navigation mechanics, security and restoration safety.

The detailed Splash/startup contract is frozen in `feature/splash/README.md`; this addendum must remain consistent with it.

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

## 6. Rendering ownership

The normalized sealed child relationships determine traversal per node. The renderer does not assume one fixed depth for the entire screen.

`SduiRendererDispatcher` is a composite orchestrator only. It resolves `(NodeKind, NodeType)` through the immutable registry and delegates rendering to the registered definition. Common visibility is enforced before delegation.

`SduiScreenRenderer` is the only normalized-screen rendering boundary. Despite rendering a server-driven screen, it is not a feature host: it receives an already normalized `Screen`, dispatches registered SDUI definitions and emits semantic command intents. Feature loading, errors, lifecycle, navigation and effect execution do not belong there.

`DynamicScreen` belongs to `feature:dynamic` and owns feature UI state around the SDUI renderer: loading, retry/error, action-in-flight and presentation UI. There must not be another `DynamicScreenHost` inside `runtime:sdui`.

Canonical typed traversal utilities such as `Screen.elements()` are reused by interaction/form infrastructure so hierarchy walking is not duplicated in multiple subsystems.

## 7. Form Template ownership

Form behavior is not a standalone application runtime. It exists because `FORM_TEMPLATE` requires mutable field state, validation and command-time bindings, therefore it lives under:

`runtime:sdui/template/form/runtime`.

That package owns `FormState`, `FormFieldState`, `FormStore`, validators, form binding adaptation and `FormTemplateRuntimeFactory`. The old standalone `runtime:form` module must not be reintroduced.

`FormTemplateRuntimeFactory` creates a FormStore only when `screen.template.type == FORM_TEMPLATE`. Other Template types never gain form state simply because they contain an Element that can contribute a field.

Form-capable Element definitions optionally implement `FormFieldContributor`. The Form Template runtime discovers those capabilities through the SDUI registry; `feature:dynamic` and `app:composition` do not identify concrete `InputElementProperties` or other concrete input types.

An INPUT renderer emits its stable `fieldId`. The Form Template runtime owns the current value/validation state and `$form.<field>` bindings resolve that latest value when a command executes.

Generic binding expression resolution remains in `runtime:binding` because screen/session/config/event/result/runtime binding semantics are not exclusive to FORM_TEMPLATE.

## 8. Generic action semantics and effect ownership

An interaction is not assumed to be a network call and a network call is not assumed to return another screen.

Generic commands include:

- `REQUEST`: relative trusted request with `SCREEN` or `NONE` response mode.
- `CAPABILITY`: platform capability through the capability registry.
- `NAVIGATION`: local back-stack operation.
- `PRESENTATION`: message/dialog/sheet state.
- `LOCAL_STATE`: runtime value mutation.
- `FORM`: FORM_TEMPLATE state operation.
- `BACKGROUND`: deferred/continuous execution semantic.
- `SEQUENCE`: ordered composition of generic commands.
- `CONDITIONAL`: binding-resolved boolean branch selection.

Sequence/conditional commands are meta-actions prepared recursively by `ActionPreparer`; renderers remain unaware of action composition semantics. Composite command recursion and sequence sizes are protocol-bounded.

`runtime:action` owns command-to-`PreparedAction` interpretation. `feature:dynamic` contains only thin effect adapters that translate prepared actions to the already existing owners: `data:network`, `foundation:capabilities`, `platform:background`, and `foundation:navigation`. Those adapters must not recreate HTTP, capability, background or navigation engines.

No Partner-specific command type belongs in the generic runtime.

## 9. Navigation ownership and transition contract

`foundation:navigation` is the sole owner of navigation state, mutation commands, back-stack restoration policy and Navigation3 presentation. `CarBrozApp` must render the canonical navigation state through `Navigation3Host`; it must not implement another current-destination/back-stack host.

Splash bootstrap and subsequent dynamic actions converge on the same feature-owned `DynamicScreenInstruction` / `DynamicDestination` model. `DynamicDestination` is feature data implementing the generic `NavigationDestination` contract; it does not own navigation mechanics. The old misleading `DynamicNavigation.kt` ownership/name must not be reintroduced.

A dynamic instruction carries screen identity, trusted relative request, semantic transition, stable back-stack key and restore policy.

Screen transitions are framework-neutral: `PUSH`, `REPLACE`, `RESET`, `STAY`. `feature:dynamic` translates those semantic transitions into `NavigationCommand` values and submits them to `NavigationStore`; only `foundation:navigation` mutates the stack. Local back-stack actions use `POP` and `POP_TO`. Navigation framework types are not exposed to backend payloads or SDUI rendering definitions.

`runtime:sdui` has no dependency on navigation mechanics. A renderer emits a semantic command; execution/orchestration outside the renderer eventually asks `foundation:navigation` to navigate.

## 10. Screen cache and process restoration

`feature:dynamic` owns a bounded process-memory `DynamicScreenCache`, separate from HTTP caching because it contains normalized screen state, FORM_TEMPLATE state, runtime values, action result and presentation state.

Generic whole-stack restoration mechanics and fail-closed fallback policy belong to `foundation:navigation`. Missing/malformed/unknown/unsafe entries restore no prefix and navigation falls back atomically to Splash. `CACHE_ONLY` destinations are not persisted because replay could repeat a non-idempotent request.

`NavigationProcessStateBridge` remains in `app:composition` because persistence has to map application-specific Splash/Dynamic destinations to the generic restoration contract and integrate with platform-owned transient saved-state. The platform-facing `restore(encodedState)` operation only captures pending state; it must never mutate `NavigationStore` before startup resolves.

After fresh bootstrap produces the server-authoritative root, `applyAfterBootstrap(freshRoot)` may restore a saved stack only when its restored root `navigationId` exactly matches the fresh root. Otherwise the saved state is discarded and navigation is atomically reset to the fresh bootstrap destination.

The bridge serializes only semantic restored destinations and must never write arbitrary dynamic-instruction payloads to durable preferences.

## 11. Startup and dynamic feature handoff

`SplashStore` is presentation-only. It observes the common lifecycle and the single `ApplicationRuntime` state, maps those values to Splash presentation state and emits typed effects. It does not call the bootstrap endpoint, persist config or hold a second bootstrap state.

`ApplicationRuntime` owns the canonical startup lifecycle. `StartupCoordinator` executes `SessionRestoreStartupTask` first and `PartnerBootstrapStartupTask` second. Session restoration returns `Continue`; Partner bootstrap is the final resolver.

`PartnerBootstrapStartupTask` lives in `app:startup` and is deliberately thin:

`PartnerBootstrapClient -> PartnerBootstrapPolicyEvaluator -> StartupTaskResult`.

`PartnerBootstrapClient` calls `GET /api/v1/partner/bootstrap` through the canonical `NetworkDataSource` with `OPTIONAL_SESSION`. It does not manually attach client metadata or Authorization headers. `ClientMetadataHeaderProvider` supplies platform/app/build metadata through the canonical network header extension point; session infrastructure owns Authorization and authentication recovery.

`PartnerBootstrapPolicyEvaluator` purely converts the typed response to one of:

- Ready with the validated existing `DynamicScreenInstruction` payload;
- Required Update blocker;
- Maintenance blocker;
- invalid-contract failure.

Required Update and Maintenance are valid blocked startup outcomes, not technical failures. Optional Update is a non-blocking Ready notice.

When the runtime becomes Ready, `SplashStore` emits one `SplashEffect.Navigate` per startup attempt. `app:composition` consumes that effect, converts the already-validated instruction to `DynamicDestination`, and delegates to `NavigationProcessStateBridge.applyAfterBootstrap(...)` so saved process state cannot bypass fresh startup.

`foundation:navigation` performs the actual stack mutation and `Navigation3Host` maps the resulting semantic destination to feature content. From that point every backend-driven screen is handled by the same `feature:dynamic` feature.

`CarBrozApp` must not construct or operate the Dynamic store from individual SDUI/network/action/form dependencies. It receives one `DynamicFeatureFactory` and delegates a `DynamicDestination` to `DynamicFeature`.

Realtime, notifications/deep-links and future external entry mechanisms must converge on the same Dynamic instruction contract. A realtime refresh must not blindly replay non-idempotent acquisition requests.

Deferred background work and genuinely continuous execution remain separate semantics. Automatic sync remains opt-in until an operation has explicit safe offline/idempotency/conflict behavior.

## 12. Existing-code-first rule

Before introducing, moving or renaming an implementation, audit the complete repository for the same responsibility, not merely the same class name.

- If a canonical owner already exists, extend or reuse it instead of creating a parallel engine.
- If new ownership supersedes an old implementation, delete the obsolete source, tests, dependencies and stale documentation in the same change.
- Do not preserve `Old`, `New`, `Reference`, `Core`, `Dynamic`, compatibility wrappers or alternate hosts solely to avoid deleting obsolete code.
- Similar names are not the only duplicate signal; different names implementing substantially the same responsibility are also duplicates.
- A module created for one responsibility must not silently become the owner of unrelated concerns.

The architecture is accepted only when there is one authoritative implementation path per responsibility.

## 13. Extension invariants

The runtime is considered extensible only when these remain true:

1. New Element definitions do not require central renderer/navigation/action modifications.
2. New Template/Component/Section/Group definitions register only through their owning level.
3. Common properties are decoded once and composed with type-specific properties.
4. No renderer performs networking directly.
5. No screen-specific Login/OTP/Dashboard/etc. renderer or destination is introduced.
6. Transport JSON is decoded, validated, compatibility-checked and normalized before rendering/execution.
7. Different Component hierarchy depths can coexist in one Template response.
8. Template-specific runtime behavior remains underneath that Template package unless evidence proves it is cross-template.
9. `app:composition` remains composition only; dynamic lifecycle changes belong to `feature:dynamic`.
10. `foundation:navigation` remains the only back-stack/navigation mechanics owner.
11. `runtime:sdui` remains the only generic SDUI runtime/rendering owner.
12. `runtime:action` remains the only generic semantic-action preparation owner.
13. `runtime:application` remains the only startup-state owner; no `BootstrapStore`/`BootstrapDestinationStore` may reappear.
14. `feature:splash` remains presentation-only and may not depend on `data:network` or `data:preferences`.
15. Partner-specific startup transport/policy remains in `app:startup`, not in neutral foundation/runtime modules.

## 14. Prohibited regressions

Do not reintroduce:

- `ReferenceDestination`, `ReferenceSduiStore`, `REFERENCE_SCREEN_ENDPOINT` or bootstrap `Reference` routes;
- `LoginDestination`, `OtpDestination`, `DashboardDestination` or equivalent business-flow destinations;
- `BootstrapStore`, `BootstrapDestinationStore`, `BootstrapConfigurationStartupTask`, the old `/api/v1/app` startup path or a Splash-owned remote-config cache;
- networking, session persistence, bootstrap DTO decoding or client-metadata header construction inside `feature:splash`;
- a fixed `Template -> Component -> Section -> Group -> Element` requirement;
- dummy Section/Group nodes;
- unsafe `Node(children: List<Node>)` hierarchies;
- one giant concrete SDUI definition list;
- one universal nullable property object;
- application-composition knowledge of concrete Input properties;
- a standalone `runtime:form` module for FORM_TEMPLATE-owned state;
- `DynamicScreenHost` inside `runtime:sdui` or another feature-like SDUI host;
- `DynamicSduiRuntime` inside `feature:dynamic` or another feature-owned generic SDUI assembly;
- `DynamicNavigation.kt` or another feature-owned navigation mechanics implementation;
- manual application back-stack/current-destination presentation parallel to `Navigation3Host`;
- dynamic-screen state/store/network/action implementation inside `app:composition`;
- direct networking from renderers;
- absolute backend-provided request URLs;
- raw platform navigation/background/capability types in SDUI wire payloads;
- mutation of canonical normalized server `Screen` objects for transient state;
- automatic replay of non-idempotent requests during restoration or realtime refresh;
- applying saved dynamic navigation before fresh session/bootstrap validation;
- durable storage of arbitrary dynamic-instruction payloads;
- duplicate bootstrap/action dynamic-screen contracts.
