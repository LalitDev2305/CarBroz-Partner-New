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

Internal publication/consumption will be introduced only after this canonical coordinate identity passes repository verification. Until then, existing project dependencies remain the build mechanism and must not be duplicated by an additional publication path.

## Slice 17.1 acceptance criteria

- One canonical foundation group and version exist.
- Every neutral module receives the same coordinates from the root build.
- Product-specific modules do not inherit those coordinates.
- A repository verification task fails on coordinate drift.
- No module-local duplicate version owner exists.
- Existing Android/iOS/Desktop build behavior remains unchanged.

