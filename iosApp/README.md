# CarBroz Partner iOS Host

`iosApp` is the native Apple application shell for CarBroz Partner.

## Ownership

The iOS host owns only unavoidable Apple-platform concerns:

- SwiftUI application/process entry point
- Xcode project, bundle/signing and deployment configuration
- native launch screen and Apple resources
- linking the `CarBrozShared` Kotlin Multiplatform framework
- future entitlements, background modes, notification hooks and Apple SDK bridges

Reusable application architecture, business rules and Compose UI remain in Kotlin Multiplatform modules. Native SwiftUI views must not become a second feature-screen architecture.

## Application flow

`CarBrozPartnerApp` -> `ContentView` -> Kotlin `MainViewController()` -> shared `CarBrozApp()`.

The Xcode target runs `:app:shared:embedAndSignAppleFrameworkForXcode` before compiling the Swift host so the framework matches the active Xcode SDK, architecture and configuration.

## Identity and platform baseline

- Product: `CarBroz Partner`
- Bundle identifier: `com.carbroz.partner`
- Deployment target: iOS 15.0
- Devices: iPhone and iPad
- Shared framework: `CarBrozShared`

## Verification from Windows

Windows can compile both Kotlin/Native Apple targets and therefore validates the shared iOS bridge and all `iosMain` Kotlin implementations:

```powershell
.\gradlew.bat :app:shared:compileKotlinIosArm64 :app:shared:compileKotlinIosSimulatorArm64 --warning-mode all --stacktrace
```

Windows cannot execute Xcode, Swift compilation, simulator runtime tests, signing or real-device tests.

## Required macOS gate

On macOS with Xcode installed:

```bash
./gradlew :app:shared:compileKotlinIosArm64 :app:shared:compileKotlinIosSimulatorArm64
xcodebuild -project iosApp/CarBrozPartner.xcodeproj -scheme CarBrozPartner -sdk iphonesimulator -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 16' build
```

A valid Apple Development Team must be selected in Xcode before physical-device/archive signing. Future iOS-specific capabilities must include both their Kotlin/native adapter tests where possible and Xcode simulator/device verification.
