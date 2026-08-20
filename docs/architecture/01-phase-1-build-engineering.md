# Phase 1 — Repository, Gradle & Build Engineering

## Objective
Establish the fresh CarBroz Partner multiplatform repository skeleton and quality gates without reusing prior implementation code.

## Identity
- Application name: `CarBroz Partner`
- Android application ID: `com.carbroz.partner`
- iOS bundle identifier: `com.carbroz.partner`
- Product packages: `com.carbroz.partner...`
- Product-neutral foundation packages: `com.carbroz.foundation...`

## Required targets
- Android
- iOS device ARM64
- iOS Simulator ARM64
- Desktop JVM

## Freeze gate
Phase 1 is frozen only after two review passes confirm:
1. Gradle/settings/version catalog resolve without hidden project repositories.
2. Android host builds.
3. Desktop target compiles and its host is runnable where runtime environment is available.
4. iOS simulator framework links on macOS CI.
5. Foundation tests pass.
6. Static-analysis/formatting policy is enforced, not merely declared.
7. Coverage tooling is executable and reports are generated for supported JVM/Android test targets.
8. CI uses pinned supported JDK/Gradle/AGP combinations.
9. No product business module exists in the foundation baseline.
10. No platform host owns reusable Kotlin architecture.
11. Repository naming/package/application identity is consistent.
12. Second-pass challenge review has no unresolved critical/high finding.

## Known tool limitation
Kover measures JVM/Android-host test coverage; Kotlin/Native coverage requires a separate strategy. The 90% project quality rule therefore applies through meaningful supported coverage metrics plus mandatory native/platform behavior tests rather than pretending Kover covers iOS binaries.
