# Phase 17 — Foundation Governance, Versioning & Product Adoption

Status: **IN PROGRESS**

The Master Architecture Constitution remains the frozen source of truth. This document records the implementation rules for Phase 17 only and must not redefine architectural ownership.

## Slice 17.1 — Canonical foundation identity

Neutral reusable modules are the modules whose Gradle paths begin with:

- `:foundation:`
- `:runtime:`
- `:data:`
- `:platform:`

Product hosts, application composition, and product features are not foundation artifacts and do not inherit the neutral foundation coordinates.

The canonical coordinate owner is the root Gradle configuration:

- group property: `carbroz.foundation.group`
- version property: `carbroz.foundation.version`
- initial internal version: `1.0.0-alpha01`

No individual neutral module may declare an independent group or version unless the Master Architecture Constitution is amended with evidence for independent lifecycle/versioning.

## Compatibility policy

Foundation versions follow Semantic Versioning semantics.

- `MAJOR`: incompatible public architectural/API contract change.
- `MINOR`: backward-compatible capability or API addition.
- `PATCH`: backward-compatible defect, security, performance, documentation, or implementation correction.
- Pre-release identifiers (`alpha`, `beta`, `rc`) indicate that product-adoption validation is still in progress and compatibility may change before stable `1.0.0`.

A version bump is required when a public contract consumed across product boundaries changes. Internal implementation-only refactors that preserve public contracts do not require an independent module version because the neutral foundation is versioned as one governed release train.

## Deprecation policy

Public foundation APIs must not be silently removed from a stable release line.

1. Add the replacement first.
2. Mark the superseded API deprecated with migration guidance.
3. Keep architecture ownership singular; deprecation must not create a second active runtime implementation.
4. Remove the deprecated API only in a permitted breaking release.
5. Security-critical removal may bypass the normal window when retaining the API would keep a known vulnerability or privacy defect.

Pre-stable releases may remove or reshape APIs when required to complete Partner/Customer adoption, but the change must be documented and validated across all current consumers before the next freeze.

## Product adoption rule

Partner is the first consumer used to prove the neutral foundation contract. Customer adoption must consume the same neutral contracts where semantics are truly shared; Partner-specific assumptions must never be moved into foundation merely to reduce duplication.

## Slice 17.2 — Internal publication and first Partner consumer proof

Neutral modules publish through Gradle Maven publications to the build-local repository at `build/foundation-repository`. This is an internal verification boundary only; no external artifact repository or credentials are introduced by this slice.

The first Partner artifact-consumption pilot is `:foundation:time` because it is a leaf KMP module with Android, Desktop, iOS Arm64 and iOS Simulator Arm64 targets and no production module dependencies. `:app:composition` is its direct product-side consumer.

Artifact consumption is enabled only when `-Pcarbroz.foundation.consumePublished=true` is supplied. In that mode, `:app:composition` requests `com.carbroz.foundation:time:1.0.0-alpha01` from the build-local Maven repository instead of declaring the direct project dependency. In the default mode the existing project dependency remains active so clean clones and ordinary development builds do not depend on a pre-populated local artifact repository while the pilot is being proven.

The build-local Maven repository is configured only when published-consumption mode is enabled, and it is content-filtered to the canonical foundation group. Ordinary project-dependency builds therefore retain their pre-pilot repository-resolution behavior.

Gradle dependency insight proved that selecting the published `foundation:time` component also resolves transitive requests for the same foundation coordinate to the published module across the consumer graph. This is compatible with the single-version foundation release train, but means the pilot toggle affects the resolved component globally rather than only one syntactic dependency edge. For that reason published consumption remains an explicit validation mode and is not yet the default Partner boundary.

The two declaration paths are mutually exclusive at `:app:composition`; the same responsibility must never be directly declared simultaneously as both a project dependency and a published artifact. CI must publish the neutral release train first and then execute Partner Android/Desktop tests and both Partner iOS compilation targets through published mode so the artifact boundary cannot silently regress on any supported KMP target.

### Upstream Compose 1.12 dependency-family observation

During iOS metadata compilation, Kotlin may report duplicate KLIB `unique_name` warnings for paired upstream `androidx.*` and compatibility `org.jetbrains.androidx.*` artifacts in Lifecycle, SavedState, and Navigation Event. Dependency insight traced these pairs through the published Compose Multiplatform 1.12 / Navigation3 dependency topology rather than to duplicate CarBroz declarations. Both iOS Arm64 and iOS Simulator Arm64 compile successfully with this graph.

These warnings are recorded as an upstream dependency-family transition observation. CarBroz must not add exclusions, forced versions, dependency substitutions, downgrades, or warning suppression solely to hide them. They become actionable only if a supported target fails, upstream guidance changes, or dependency provenance demonstrates a CarBroz-owned duplicate.

### Slice 17.2 pilot acceptance criteria

- All neutral KMP publications succeed into the build-local repository.
- `:foundation:time` resolves from Maven coordinates when published consumption is enabled.
- `:app:composition` compiles/tests on Android and Desktop and compiles for both iOS targets using the artifact path.
- The default project-dependency path remains green during the pilot.
- No simultaneous direct project + artifact declaration exists for `foundation:time` in `:app:composition`.
- The local repository is absent from normal dependency resolution and restricted to the canonical foundation group when enabled.
- CI continuously proves publish -> Partner artifact consumption on Android, Desktop, iOS Arm64, and iOS Simulator Arm64.
- No external repository, credentials, or product assumptions are introduced.

### Slice 17.2 audit decision

Published consumption remains opt-in after the first pilot. It must not become the default until adoption is expanded as a controlled release-train boundary and the repository-wide regression gate proves that boundary without relying on stale local artifacts. Future adoption must migrate coherent dependency groups with dependency-insight evidence rather than toggling arbitrary modules independently.

## Slice 17.3 — Leaf runtime-support foundation adoption

The next adoption unit is the smallest closed runtime-support leaf cluster already consumed directly by `:app:composition`:

- `:foundation:lifecycle`
- `:foundation:time`
- `:foundation:security`
- `:foundation:observability`

Each module has no production dependency on another CarBroz module. Higher-level consumers such as `:foundation:session` and `:runtime:application` remain project dependencies in this slice because their internal CarBroz dependency closures are larger and must not be partially migrated without a separate evidence-backed adoption decision.

When `-Pcarbroz.foundation.consumePublished=true` is enabled, Partner requests all four leaf modules through canonical Maven coordinates. When the property is absent, all four continue to resolve as project dependencies. The two modes remain mutually exclusive for each direct dependency.

The build-local Maven repository must be cleaned before every aggregate foundation publication. Published-boundary verification must therefore prove artifacts generated by the current source/version and must never succeed because a stale artifact from a previous run remains in `build/foundation-repository`.

### Slice 17.3 acceptance criteria

- Aggregate local publication starts from an empty build-local foundation repository.
- `lifecycle`, `time`, `security`, and `observability` resolve through `com.carbroz.foundation:*:1.0.0-alpha01` when published mode is enabled.
- Their default Partner declarations remain project dependencies when published mode is disabled.
- Higher-level modules remain unchanged; this slice does not migrate `session`, `runtime:application`, navigation, SDUI, data, or platform modules.
- Partner Android host tests, Desktop tests, iOS Arm64 compilation, and iOS Simulator Arm64 compilation pass through published mode.
- The normal repository-wide project-dependency regression remains green.
- CI exercises the same clean publication and published-consumer path.
- No stale artifact, duplicate direct dependency, external repository, credential, or product-specific foundation assumption is introduced.

## Slice 17.4 — Closed runtime-support consumers

After the Slice 17.3 leaf cluster was frozen, two directly consumed modules form the second-tier runtime-support boundary:

- `:foundation:session`, whose CarBroz production dependencies are `:foundation:security` and `:foundation:time`.
- `:runtime:application`, whose frozen bootstrap target dependency closure includes `:foundation:lifecycle`, `:foundation:observability`, `:foundation:time`, and canonical `:foundation:session` because the focused application startup use case invokes `SessionStore.restore()` rather than duplicating session policy.

The bootstrap architecture freeze therefore expands the application module's previous dependency closure by one existing canonical owner; it does not create a new session abstraction or a product-specific session module. The publication boundary remains closed because `foundation:session` is already part of this same governed second-tier release-train slice and itself depends only on the already-adopted security/time leaf modules.

Published mode consumes `session` and `runtime:application` through the canonical foundation release train while default development mode preserves project dependencies. Dependency insight must prove both selected artifacts and their transitive foundation closure. Navigation, SDUI, data, platform, action, and binding modules remain project-bound in this slice.

The focused bootstrap refactor must update the actual Gradle dependency graph and published dependency metadata together. Documentation alone does not satisfy this acceptance criterion.

### Slice 17.4 acceptance criteria

- `session` resolves as `com.carbroz.foundation:session:1.0.0-alpha01` in published mode.
- `runtime:application` resolves as `com.carbroz.foundation:application:1.0.0-alpha01` in published mode.
- `runtime:application` may depend on the canonical `foundation:session` artifact/project for startup restoration; it must not introduce a parallel session port solely to preserve an outdated dependency graph.
- The complete CarBroz transitive dependency closure of both modules remains inside the governed foundation release train.
- Partner Android host tests, Desktop tests, iOS Arm64 compilation, and iOS Simulator Arm64 compilation pass through published mode.
- Repository-wide `check` remains green in default project mode.
- No broader runtime/data/platform migration is introduced by this governance correction.

## Slice 17.5 — Remaining direct leaf foundation adoption

The remaining direct foundation dependencies of `:app:composition` were re-inspected after Slice 17.4. The next closed cluster is the complete set of direct leaf foundation modules that own no production dependency on another CarBroz module:

- `:foundation:configuration`
- `:foundation:architecture`
- `:foundation:navigation`
- `:foundation:adaptive`
- `:foundation:design-system`
- `:foundation:capabilities`
- `:foundation:analytics`

`configuration` remains semantically distinct because `AppConfiguration` is part of composition's host-facing public API. Published mode must therefore preserve the existing `api(...)` exposure rather than reducing it to an implementation dependency. The other six remain implementation dependencies.

Navigation's upstream Compose/Navigation3 dependency-family warnings remain governed by the existing upstream-transition decision; migrating navigation to the published boundary must not introduce exclusions, forced versions, substitutions, downgrades, or warning suppression solely to silence those warnings.

This slice intentionally does not adopt `:platform:background`, `:runtime:action`, `:runtime:binding`, `:runtime:sdui`, or any `:data:*` module. Those modules have larger CarBroz dependency closures and require separate source-of-truth inspection after this leaf boundary is proven.

### Slice 17.5 acceptance criteria

- All seven remaining direct leaf foundation modules resolve through canonical `com.carbroz.foundation:*:1.0.0-alpha01` coordinates when published mode is enabled.
- `configuration` remains exposed with `api(...)` in both project and published modes.
- Default development mode continues to consume the same seven modules as Gradle projects.
- No direct leaf foundation module remains project-bound in `:app:composition` after the slice.
- Partner Android host tests, Desktop tests, iOS Arm64 compilation, and iOS Simulator Arm64 compilation pass through published mode.
- Repository-wide default-mode `check` remains green.
- Existing known upstream Compose/KLIB warnings are not converted into CarBroz-owned suppression or dependency overrides.
- Runtime, data, and platform modules outside the already-frozen Slice 17.4 boundary remain project dependencies.

## Slice 17.1 acceptance criteria

- One canonical foundation group and version exist.
- Every neutral module receives the same coordinates from the root build.
- Product-specific modules do not inherit those coordinates.
- A repository verification task fails on coordinate drift.
- No module-local duplicate version owner exists.
- Existing Android/iOS/Desktop build behavior remains unchanged.
