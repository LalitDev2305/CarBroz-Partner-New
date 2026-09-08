# CarBroz Partner iOS Host

`iosApp` is the native Apple application shell for CarBroz Partner.

## Ownership

The iOS host owns only unavoidable Apple-platform concerns:

- SwiftUI application/process entry point
- Xcode project, bundle/signing and deployment configuration
- linking the `CarBrozShared` Kotlin Multiplatform framework produced by `:app:composition`
- entitlements, declared background-task identifiers, notification hooks and Apple SDK configuration that must live in the native target

Reusable application architecture, business rules, navigation state, SDUI runtime and product UI remain in Kotlin Multiplatform/Compose Multiplatform modules. Native SwiftUI views must not become a second feature-screen architecture. Platform-specific code is added only when the common implementation cannot satisfy the platform requirement.

## Application flow

`CarBrozPartnerApp` -> `ContentView` -> Kotlin `MainViewController()` -> shared `CarBrozApp()`.

The Xcode target runs `:app:composition:embedAndSignAppleFrameworkForXcode` before compiling the Swift host so the framework matches the active Xcode SDK, architecture and configuration.

## Environment model

The native host preserves the same explicit three-environment model as the shared configuration layer and Android host:

| Xcode configuration | CarBroz environment | Default API base URL | Bundle identifier | Kotlin framework type |
| --- | --- | --- | --- | --- |
| `Debug` | `development` | `https://development.invalid` | `com.carbroz.partner` | auto-detected Debug |
| `Staging` | `staging` | `https://staging.invalid` | `com.carbroz.partner.staging` | explicit `Release` |
| `Release` | `production` | `https://production.invalid` | `com.carbroz.partner` | auto-detected Release |

`CarBrozEnvironment` and `CarBrozApiBaseUrl` are read from `Info.plist` build settings. `CarBrozPartnerApp` passes those values, bundle version/build number and bundle identifier into the common application bootstrap. No environment selection or backend URL is re-decided in shared startup/Splash code.

`Staging` sets `KOTLIN_FRAMEWORK_BUILD_TYPE=Release` because custom Xcode configuration names are not automatically classifiable by the Kotlin Multiplatform direct-integration task.

## Background execution

Semantic scheduling and continuous-execution contracts live in `:platform:background`. The iOS adapter uses `BGTaskScheduler` and reads `BGTaskSchedulerPermittedIdentifiers` from this target's `Info.plist`; identifiers must be added there at the same time as their corresponding `BackgroundTaskHandler` is registered in the canonical Koin composition graph. Arbitrary transient task payloads are intentionally unsupported on iOS: handlers reconstruct work from durable application state.

Generic unlimited continuous execution is not available on iOS. Operations that legitimately qualify for an Apple background mode or continued-processing API must add an operation-specific native adapter rather than emulating an Android-style foreground service.

## Launch and splash policy

Product splash/startup UI is owned by shared Compose Multiplatform code. The iOS host does not define a branded/product splash screen. Any OS-mandated launch presentation must remain minimal and non-product-specific; the first intentional product screen is rendered by shared Compose code.

The Swift host forwards the initial active scene state as well as later active/background transitions into the common lifecycle bridge. Duplicate foreground delivery is intentionally harmless because the shared Splash/runtime path is idempotent.

## Identity and platform baseline

- Product: `CarBroz Partner`
- Production bundle identifier: `com.carbroz.partner`
- Staging bundle identifier: `com.carbroz.partner.staging`
- Deployment target: iOS 15.0
- Devices: iPhone and iPad
- Shared framework: `CarBrozShared`

## Verification from Windows

Windows can compile both Kotlin/Native Apple targets and therefore validates the shared iOS bridge and all `iosMain` Kotlin implementations:

```powershell
.\gradlew.bat :app:composition:compileKotlinIosArm64 :app:composition:compileKotlinIosSimulatorArm64 --warning-mode all --stacktrace
```

Windows cannot execute Xcode, Swift compilation, simulator runtime tests, signing or real-device tests.

## Required macOS gate

On macOS with Xcode installed, verify both native host configurations used before production release:

```bash
./gradlew :app:composition:compileKotlinIosArm64 :app:composition:compileKotlinIosSimulatorArm64

xcodebuild \
  -project iosApp/CarBrozPartner.xcodeproj \
  -scheme CarBrozPartner \
  -sdk iphonesimulator \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build

xcodebuild \
  -project iosApp/CarBrozPartner.xcodeproj \
  -scheme CarBrozPartner \
  -sdk iphonesimulator \
  -configuration Staging \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

CI executes these unsigned simulator builds after the shared Kotlin/Native and published-foundation gates. A valid Apple Development Team is still required for physical-device/archive signing. iOS-specific capabilities must include their Kotlin/native adapter tests where possible and Xcode simulator/device verification.
