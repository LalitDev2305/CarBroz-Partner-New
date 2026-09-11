# CarBroz Dynamic Runtime Architecture

Status: **FROZEN ARCHITECTURE ADDENDUM** for the post-Splash runtime and its startup handoff.

This document refines the Master Architecture Constitution without creating a parallel architecture. The detailed bootstrap contract is frozen in `feature/splash/README.md`; both documents must remain consistent.

---

## 1. Feature and product-flow ownership

Splash is the intentional static application feature. Normal screens after Splash are backend-driven. Client architecture must not encode business flow names such as Login, OTP, Dashboard, Booking, KYC, Profile, Availability or Earnings merely because those screens exist.

Canonical ownership:

- `feature:splash` owns the static Splash UI and its MVI/UDF presentation state, intents and one-shot effects.
- `runtime:application` owns transport-independent startup orchestration/policy through one focused `ResolveStartupUseCase`, typed `StartupResult`/`StartupDestination`, the inward `BootstrapRepository` port and the process-scoped reusable `PartnerConfigStore`.
- `foundation:session` is the sole session owner, including persisted-session restoration, invalid/corrupt persisted-session cleanup, refresh, invalidation and logout semantics.
- `data:bootstrap` owns only the Partner bootstrap transport adapter: route, serializable DTOs, DTO mapping and the remote implementation of `BootstrapRepository`.
- `data:network` owns the canonical REST execution stack, configuration-derived metadata headers, typed response decoding, authentication integration/recovery and transport policies.
- `feature:dynamic` is the single feature for backend-driven screens. It owns dynamic destination/request contracts, screen loading/error/retry lifecycle, runtime screen cache, action-result orchestration, external/realtime screen events and Dynamic feature UI.
- `foundation:navigation` is the sole navigation mechanics owner: back-stack state, commands, restoration policy and Navigation 3 adapter/presentation.
- `runtime:sdui` is the reusable SDUI protocol/normalization/rendering engine used by `feature:dynamic`. It does not own feature lifecycle, bootstrap policy or navigation mechanics.
- `runtime:action` is the sole generic semantic-action preparation owner. It does not execute network/platform feature effects itself.
- `app:composition` is composition/adaptation only. It wires owners, maps the trusted `StartupDestination` to the existing Dynamic destination contract, consumes feature effects and owns the application-specific process-state bridge.
- `app:startup` does not exist.
- Android/Desktop/iOS hosts remain thin platform entry points.

Canonical runtime loop:

```text
Splash
  -> SplashStore
  -> ResolveStartupUseCase
       -> SessionStore.restore()
       -> BootstrapRepository
       -> data:bootstrap RemoteBootstrapRepository
       -> GET /api/v1/partner/config/bootstrap through canonical data:network
       -> typed BootstrapSnapshot
       -> PartnerConfigStore
       -> StartupResult.Ready(StartupDestination)
  -> SplashEffect.Navigate
  -> app:composition maps to DynamicDestination
  -> NavigationProcessStateBridge
  -> foundation:navigation
  -> feature:dynamic
  -> trusted screen request
  -> runtime:sdui decode
  -> schema validation
  -> compatibility
  -> normalization
  -> runtime IR
  -> render
  -> semantic command
  -> runtime:action preparation
  -> binding/form resolution
  -> feature effect adapter
  -> local effect or navigation command
  -> foundation:navigation
  -> same feature:dynamic
  -> repeat
```

The backend chooses the next screen. The client owns validation, trusted execution, rendering, lifecycle, navigation mechanics, security and restoration safety.

---

## 2. Dynamic screen identity

Canonical identity remains three distinct values:

- `screenId`: dynamic screen identity.
- `templateId`: concrete template identity/back-stack participant.
- `templateType`: registered reusable Template rendering definition.

`templateType` is rendering behavior, never business-screen identity.

Startup transports these values through the typed `StartupDestination`; normal Dynamic runtime then uses its existing trusted destination/instruction model.

---

## 3. Flexible strongly typed SDUI hierarchy

`Template` is the tree root, `Component` is mandatory composition and `Element` is terminal. `Section` and `Group` are optional structural levels.

```text
Template := Component+
Component := Element+ | Section+
Section := Element+ | Group+
Group := Element+
Element := terminal
```

Valid branches may coexist in one Template:

```text
Component -> Elements
Component -> Sections -> Elements
Component -> Sections -> Groups -> Elements
```

Every Component chooses its own branch independently. Every Section chooses its own branch independently. Backend must not send dummy Section or Group nodes.

Relationships are explicit XOR relationships, not an unsafe generic tree. A Component may not mix direct Elements and Sections as siblings. A Section may not mix direct Elements and Groups as siblings. Groups contain terminal Elements. Empty branches are invalid.

Historical `SubComponent`, `Child` and `ChildrenData` terminology is not part of this runtime.

---

## 4. Definition and registration ownership

Each hierarchy level owns its supported definitions:

- `TemplateDefinitions`
- `ComponentDefinitions`
- `SectionDefinitions`
- `GroupDefinitions`
- `ElementDefinitions`

`SduiRegistryFactory` composes those collections into one immutable `SduiRegistry`; it does not manually enumerate concrete UI definitions.

Adding a new UI type should modify only its owning hierarchy level plus focused tests. Adding a `RatingElement`, for example, must not require changes to navigation, the generic renderer dispatcher, action execution or unrelated Template definitions.

Definitions remain atomic: each `SduiDefinition<P>` owns its server `NodeType`, typed property decoding/normalization and rendering semantics when renderable.

---

## 5. Typed property architecture

Raw transport `properties` are untrusted JSON. They are never rendered directly. During normalization the registered definition decodes them into typed properties.

Common safe visual/layout concerns are represented once through shared property contracts. Concrete definitions compose common concerns with specialized typed properties.

Examples:

- Stack containers own axis, spacing and alignment.
- Form Template owns adaptive/readable content policy.
- Input owns field identity, initial value, label, placeholder, required/enabled/read-only state, max length and keyboard type.
- Button owns text/enabled/loading state.
- Text owns typography/alignment/max-lines semantics.

Do not replace this with one giant universal nullable property model. Do not duplicate common Compose modifier behavior across every renderer.

---

## 6. Rendering ownership

The normalized sealed child relationships determine traversal per node. The renderer does not assume one fixed depth for an entire screen.

`SduiRendererDispatcher` is a composite orchestrator. It resolves `(NodeKind, NodeType)` through the immutable registry and delegates rendering to the registered definition.

`SduiScreenRenderer` is the only normalized-screen rendering boundary. It receives an already normalized `Screen`, dispatches registered definitions and emits semantic interaction. Loading, errors, lifecycle, navigation and effect execution do not belong there.

`DynamicScreen` belongs to `feature:dynamic` and owns feature UI state around the SDUI renderer: loading, retry/error, action-in-flight and presentation UI. There must not be another feature host inside `runtime:sdui`.

Canonical typed traversal utilities are reused so hierarchy walking is not duplicated across interaction/form infrastructure.

---

## 7. Form Template ownership

Form behavior exists because FORM_TEMPLATE requires mutable field state, validation and command-time bindings. It belongs under the Form Template runtime inside `runtime:sdui` rather than a standalone generic application runtime.

Form Template runtime owns:

- form state/field state;
- form Store;
- validators;
- form binding adaptation;
- Form Template runtime factory.

A Form Store is created only when the current Template requires form behavior. Other Template types do not gain form state merely because they contain an Element capable of contributing a field.

Form-capable Element definitions expose a typed contribution capability. `feature:dynamic` and `app:composition` do not identify concrete Input property classes.

Generic binding expression resolution remains in `runtime:binding` because session/screen/config/event/result/runtime bindings are not exclusive to forms.

---

## 8. Generic action semantics and effect ownership

An interaction is not automatically a network call, and a network call is not automatically another screen.

Generic command families include:

- `REQUEST`
- `CAPABILITY`
- `NAVIGATION`
- `PRESENTATION`
- `LOCAL_STATE`
- `FORM`
- `BACKGROUND`
- `SEQUENCE`
- `CONDITIONAL`

`runtime:action` owns command-to-prepared-action interpretation. `feature:dynamic` contains only thin effect adapters that translate prepared actions to canonical existing owners such as `data:network`, capabilities/background owners and `foundation:navigation`.

No Partner-specific command type belongs in the generic runtime.

---

## 9. Navigation ownership and transition contract

`foundation:navigation` is the sole owner of navigation state, mutation commands, back-stack restoration policy and Navigation 3 presentation.

Splash bootstrap and subsequent dynamic actions converge on the same Dynamic destination model. `DynamicDestination` is feature data implementing the generic navigation destination contract; it does not own navigation mechanics.

A dynamic instruction carries screen identity, trusted relative request, semantic transition, stable back-stack identity and restore policy.

Framework-neutral transitions include `PUSH`, `REPLACE`, `RESET` and `STAY`. Local back-stack actions use semantic POP/POP_TO commands. Navigation framework types are not exposed to backend payloads or SDUI definitions.

`runtime:sdui` has no dependency on navigation mechanics. Renderers emit semantic command intent; execution outside the renderer may eventually ask `foundation:navigation` to navigate.

---

## 10. Screen cache and process restoration

`feature:dynamic` owns bounded process-memory dynamic screen cache separately from HTTP caching because it may contain normalized screen/form/runtime/action-result/presentation state.

Generic whole-stack restoration mechanics and fail-closed fallback policy belong to `foundation:navigation`.

`NavigationProcessStateBridge` remains in `app:composition` because application saved-state adaptation must coordinate the static Splash root and Dynamic destinations with generic restoration semantics.

Its platform-facing restore operation only captures pending state. It must never mutate `NavigationStore` before fresh startup resolves.

After fresh bootstrap produces the server-authoritative root:

```text
fresh root
  ↓
NavigationProcessStateBridge.applyAfterBootstrap(...)
  ├── restored root navigationId matches fresh root
  │     -> restore validated saved stack when safe
  └── mismatch/malformed/unsafe
        -> discard saved state and reset to fresh root
```

The bridge serializes semantic restorable destinations only and must never write arbitrary untrusted dynamic payloads to durable preferences.

---

## 11. Frozen startup and Dynamic handoff

### One presentation state owner

`SplashStore` is the single startup presentation state owner. It observes common lifecycle/user intents, calls `ResolveStartupUseCase`, reduces `StartupResult` into `SplashState` and emits typed one-shot effects.

Do **not** maintain a parallel observable `ApplicationRuntimeState` describing Loading/Ready/Blocked/Failed again.

### One startup application orchestrator

`ResolveStartupUseCase` is a focused application use case, not a task framework.

It performs:

```text
SessionStore.restore()
   ↓
BootstrapRepository.load()
   ↓
PartnerConfigStore.update(valid reusable config)
   ↓
required update policy
   ↓
maintenance policy
   ↓
StartupResult.Ready(StartupDestination)
```

It does not parse transport DTOs, own Ktor, manipulate navigation directly, render Splash, duplicate session repair or execute arbitrary pluggable startup tasks.

### Bootstrap transport

The application-owned `BootstrapRepository` is implemented by `data:bootstrap/RemoteBootstrapRepository`.

Canonical request:

```text
GET /api/v1/partner/config/bootstrap
NetworkAuthentication.OPTIONAL_SESSION
```

Canonical backend envelope:

```text
status
code
message
data
traceId
```

The stale `/api/v1/partner/bootstrap` route and `success: Boolean` envelope assumption are superseded.

`data:network/ConfigurationNetworkHeaderProvider` supplies platform/app/build metadata through the canonical network extension point. Session/network infrastructure owns Authorization and authentication recovery. Bootstrap adapter adds neither manually.

### Typed destination

Bootstrap `nextScreen` is mapped once into the application-owned typed `StartupDestination`.

Do not use:

```text
JsonElement -> String -> StartupPayloadDecoder -> decode again
```

`runtime:application` must not depend on `feature:dynamic`. `app:composition` performs a thin mapping from trusted `StartupDestination` to the existing Dynamic destination/instruction contract.

### Partner configuration

Reusable bootstrap config is retained in a small process-scoped `PartnerConfigStore`, including configuration version, Partner feature switches and optional-update metadata needed by approved consumers.

Do not persist bootstrap to Room/DataStore by default. Maintenance, required update, authentication truth and initial destination remain fresh server-authoritative startup decisions.

### Optional-session recovery

When an OPTIONAL_SESSION request receives 401:

```text
refresh succeeds
  -> retry with refreshed token

refresh/session recovery definitively invalidates session
  -> canonical session becomes SignedOut
  -> OPTIONAL_SESSION only: retry once without Authorization

recovery unavailable
  -> return failure
```

A required `SESSION` request never downgrades to anonymous after invalidation.

This is generic network/session behavior, not bootstrap-specific repository logic.

### Startup result semantics

Required Update and Maintenance are valid blocking application results, not transport failures.

Optional Update remains non-blocking config metadata; do not create a generic `StartupNotice` framework without a real consumer.

When startup returns Ready, Splash emits one navigation effect for that successful attempt. `app:composition` adapts the destination and delegates to `NavigationProcessStateBridge.applyAfterBootstrap(...)` so restored process state cannot bypass fresh startup authority.

From that point every normal backend-driven screen is handled by the same `feature:dynamic` feature.

---

## 12. Existing-code-first and migration rule

Before introducing, moving, renaming or deleting an implementation, audit the complete repository for the responsibility and all consumers/tests.

- Reuse a canonical owner when one exists.
- If new ownership supersedes an old implementation, migrate consumers/tests and delete the obsolete source/dependencies in the same focused slice.
- Do not preserve `Old`, `New`, `Reference`, compatibility wrappers or alternate startup hosts merely to avoid deletion.
- Similar names are not the only duplicate signal; different names implementing the same responsibility are duplicates too.
- A module created for one responsibility must not silently become owner of unrelated concerns.

The focused bootstrap migration targets removal, after proven replacement, of:

```text
ApplicationRuntime / ApplicationRuntimeState
StartupCoordinator
StartupTask / StartupTaskResult / StartupResolution
SessionRestoreStartupTask
BootstrapStartupTask
StartupPayload / StartupPayloadDecoder
DynamicStartupPayloadDecoder
StartupNotice
```

`ResolveBootstrapUseCase` converges into `ResolveStartupUseCase`.

The architecture is accepted only when one authoritative implementation path remains.

---

## 13. Extension invariants

The runtime is considered extensible only when all of these remain true:

1. New Element definitions do not require central renderer/navigation/action modifications.
2. New Template/Component/Section/Group definitions register only through their owning level.
3. Common properties are decoded once and composed with type-specific properties.
4. No renderer performs networking directly.
5. No screen-specific Login/OTP/Dashboard renderer or destination is introduced merely for server screens.
6. Transport JSON is decoded, validated, compatibility-checked and normalized before rendering/execution.
7. Different Component hierarchy depths can coexist in one Template response.
8. Template-specific runtime behavior remains underneath that Template unless evidence proves it cross-template.
9. `app:composition` remains composition/adaptation only.
10. `foundation:navigation` remains the only back-stack/navigation mechanics owner.
11. `runtime:sdui` remains the only generic SDUI runtime/rendering owner.
12. `runtime:action` remains the only generic semantic-action preparation owner.
13. `feature:splash` remains the only startup presentation state owner; no second startup observable state machine may reappear.
14. `runtime:application` remains the startup application-policy/orchestration owner through focused use cases/contracts, not a generic task engine.
15. Partner bootstrap transport remains in `data:bootstrap`; application bootstrap semantics remain in `runtime:application`; `app:startup` must not be introduced.
16. `data:network` remains product-neutral and contains no Partner bootstrap DTOs/routes.
17. All normal post-Splash screens/actions remain on the one `feature:dynamic` + SDUI/action/network path unless a genuine client-owned invariant justifies domain-specific code.
18. Bootstrap `nextScreen` remains typed across the application boundary; do not reintroduce JSON round-tripping.
19. Bootstrap persistence is not introduced without an explicit freshness/security product contract.
20. `NavigationProcessStateBridge` continues to validate saved state against a fresh server-authoritative root before restoration.

---

## 14. Prohibited regressions

Do not reintroduce:

- `app:startup` or another mixed-responsibility startup dumping ground;
- a generic `StartupTask` pipeline without demonstrated need;
- a second observable startup state machine beside `SplashStore`;
- `BootstrapStore` or `BootstrapDestinationStore`;
- bootstrap-specific Ktor/HttpClient infrastructure;
- Partner bootstrap DTOs/routes inside `data:network`;
- the stale `/api/v1/partner/bootstrap` route;
- the stale `success: Boolean` bootstrap envelope;
- JSON String/`JsonElement` round-trip for `nextScreen`;
- Splash-owned remote-config/bootstrap cache;
- Room/DataStore bootstrap persistence without explicit approved freshness semantics;
- networking, session persistence or bootstrap DTO decoding inside `feature:splash`;
- `LoginDestination`, `OtpDestination`, `DashboardDestination` or equivalent business-screen navigation architecture;
- a fixed `Template -> Component -> Section -> Group -> Element` depth requirement;
- dummy Section/Group nodes;
- unsafe generic recursive node hierarchies;
- one giant concrete SDUI definition list;
- one universal nullable property object;
- application-composition knowledge of concrete Input properties;
- duplicate generic SDUI runtime inside `feature:dynamic`;
- feature-owned navigation mechanics;
- manual back-stack/current-destination state parallel to `NavigationStore`/Navigation 3;
- direct networking from renderers;
- absolute backend-provided request URLs;
- raw platform navigation/background/capability types in SDUI wire payloads;
- mutation of canonical normalized server Screen objects for transient state;
- automatic replay of non-idempotent requests during restoration/realtime refresh;
- applying saved dynamic navigation before fresh session/bootstrap validation;
- durable storage of arbitrary dynamic-instruction payloads;
- anonymous downgrade for a required `SESSION` request.

---

## 15. Freeze condition

This addendum is frozen only while it agrees with:

```text
MASTER-ARCHITECTURE-CONSTITUTION.md
feature/splash/README.md
module dependency graph
DI graph
current backend bootstrap contract
implementation
architecture tests
```

The focused refactor must preserve all unrelated Dynamic/SDUI/navigation behavior while simplifying only startup ownership and transport convergence.

A green build with old and new startup frameworks left in parallel is not a successful migration.