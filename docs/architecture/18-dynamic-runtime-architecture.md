# CarBroz Dynamic Runtime Architecture

Status: frozen architecture addendum for the post-Splash runtime.

## 1. Product flow ownership

Splash is the only currently known static application screen. Every screen after Splash is backend-driven. Client code must not encode business flow names such as Login, OTP, Dashboard, Booking, KYC, Profile, Availability or Earnings into navigation or runtime architecture.

The canonical runtime loop is:

`Splash -> startup/session restore -> /api/v1/app -> validated DynamicScreenInstruction -> DynamicDestination -> trusted screen request -> SDUI decode -> schema validation -> compatibility -> normalization -> runtime IR -> render -> semantic command -> binding/form/action execution -> local effect or next DynamicDestination -> repeat`.

The backend chooses the next screen. The client owns validation, trusted execution, rendering, lifecycle, navigation mechanics, security and restoration safety.

## 2. Dynamic screen identity

The canonical identity remains three distinct values:

- `screenId`: dynamic screen identity.
- `templateId`: concrete template identity.
- `templateType`: registered rendering definition type.

`templateType` is rendering behavior, never business-screen identity.

## 3. SDUI hierarchy

The normalized hierarchy is:

`Screen -> Template -> Component -> Section -> Group -> Element`.

`Element` is terminal. Historical `SubComponent`, `Child` and `ChildrenData` terminology is not part of the current runtime.

## 4. One transition contract

Bootstrap and subsequent dynamic actions converge on the same trusted `DynamicScreenInstruction` / `DynamicDestination` model. There is no separate client-known bootstrap route enum.

A dynamic instruction carries the screen destination identity, trusted relative request, semantic screen transition, stable back-stack key and restore policy.

Allowed screen transitions are semantic and framework-neutral: `PUSH`, `REPLACE`, `RESET`, `STAY`. Local back-stack actions use the navigation runtime (`POP`, `POP_TO`). Navigation 3 types are not exposed to backend payloads.

## 5. Request and action semantics

A button or other interaction is not assumed to be an API call, and an API call is not assumed to return another screen.

The generic command vocabulary is:

- `REQUEST`: trusted relative network request; response mode may be `SCREEN` or `NONE`.
- `CAPABILITY`: platform capability through the capability registry.
- `NAVIGATION`: local back-stack operation.
- `PRESENTATION`: generic message/dialog/sheet presentation state.
- `LOCAL_STATE`: screen-runtime local value mutation.
- `FORM`: generic form operation.
- `BACKGROUND`: semantic deferred/continuous execution operation.

No Partner-specific command type is part of the generic runtime.

## 6. Binding and form ownership

Bindings are resolved at command execution time from immutable execution snapshots. Supported generic namespaces include form, screen, session, configuration, event, result and runtime values. Session bindings deliberately exclude access/refresh tokens and other secrets.

Interactive input state is owned by `runtime:form`, not by business-screen code. SDUI input definitions emit canonical field identifiers so renderer events update the form store before action binding resolution.

Form runtime supports synchronous validation, async validation, state restoration, visibility/enabled state, server errors and submission lifecycle. Cross-field rules receive full form state.

## 7. Screen runtime cache vs HTTP cache

Dynamic screen restoration uses a bounded process-memory `DynamicScreenCache`. This is intentionally separate from the network response cache because screen runtime restoration includes normalized screen state, form state, runtime values, action result and presentation state.

## 8. Process restoration safety

Dynamic destinations participate in the foundation navigation restoration contract only when safe to persist and recreate.

`CACHE_ONLY` destinations are deliberately not persisted across process death because they may have been acquired through a non-idempotent request. Recreating such a destination could replay a POST/PUT/PATCH/DELETE. In that case restoration falls back to the static root/bootstrap path.

Persisted dynamic instructions are decoded through the same `DynamicScreenInstructionCodec` used for external dynamic instructions and are accepted only when the reconstructed navigation identity matches the persisted identity.

## 9. Startup and session behavior

Session restoration runs before bootstrap. `/api/v1/app` uses optional-session authentication: a session is attached when available, while a fresh signed-out launch remains valid. The backend therefore chooses the first dynamic screen for both new and returning users.

Bootstrap never maps server values to client-known screen names.

## 10. External/realtime entry

Realtime, notification/deep-link adapters and future external entry mechanisms must resolve dynamic destinations through the same canonical dynamic-instruction codec. They must not invent parallel routing contracts.

A realtime refresh must never blindly replay a non-idempotent acquisition request. Unsafe transitions must arrive as a new validated dynamic instruction.

## 11. Background and sync policy

Deferred work and genuinely continuous execution remain separate semantic concepts. WorkManager/Apple/Desktop platform details stay outside shared runtime commands.

Automatic sync is not enabled merely because outbox/sync infrastructure exists. Foreground/connectivity auto-sync remains opt-in until a product operation establishes safe offline semantics, idempotency and conflict policy.

## 12. Extension rule

Missing Partner-specific screens/components are not architecture defects during foundation work. New SDUI definitions and product/domain modules are added only when real rendering or client-owned business invariants require them.

The runtime must remain extensible through registries and trusted semantic contracts rather than hardcoded business flow classes.

## 13. Prohibited architecture regressions

Do not reintroduce:

- `ReferenceDestination`, `ReferenceSduiStore`, `REFERENCE_SCREEN_ENDPOINT` or bootstrap `Reference` routes;
- `LoginDestination`, `OtpDestination`, `DashboardDestination` or equivalent business-flow destinations;
- direct networking from renderers;
- absolute backend-provided request URLs;
- raw platform navigation/background/capability types in SDUI wire payloads;
- mutation of canonical normalized server `Screen` objects for transient runtime state;
- automatic replay of non-idempotent requests during restoration or realtime refresh;
- duplicate bootstrap/action dynamic-screen contracts.
