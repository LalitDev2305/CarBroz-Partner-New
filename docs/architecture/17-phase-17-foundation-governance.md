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

The two declaration paths are mutually exclusive at `:app:composition`; the same responsibility must never be directly declared simultaneously as both a project dependency and a published artifact. CI must publish the neutral release train first and then execute Partner Android/Desktop tests through the published mode so the artifact boundary cannot silently regress.

### Slice 17.2 pilot acceptance criteria

- All neutral KMP publications succeed into the build-local repository.
- `:foundation:time` resolves from Maven coordinates when published consumption is enabled.
- `:app:composition` compiles/tests on Android and Desktop and compiles for both iOS targets using the artifact path.
- The default project-dependency path remains green during the pilot.
- No simultaneous direct project + artifact declaration exists for `foundation:time` in `:app:composition`.
- The local repository is absent from normal dependency resolution and restricted to the canonical foundation group when enabled.
- CI continuously proves publish -> Partner artifact consumption.
- No external repository, credentials, or product assumptions are introduced.

### Slice 17.2 audit decision

Published consumption remains opt-in after the first pilot. It must not become the default until adoption is expanded as a controlled release-train boundary and the repository-wide regression gate proves that boundary without relying on stale local artifacts. Future adoption must migrate coherent dependency groups with dependency-insight evidence rather than toggling arbitrary modules independently.

## Slice 17.1 acceptance criteria

- One canonical foundation group and version exist.
- Every neutral module receives the same coordinates from the root build.
- Product-specific modules do not inherit those coordinates.
- A repository verification task fails on coordinate drift.
- No module-local duplicate version owner exists.
- Existing Android/iOS/Desktop build behavior remains unchanged.
