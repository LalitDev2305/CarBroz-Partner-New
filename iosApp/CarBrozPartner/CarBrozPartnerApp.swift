import SwiftUI
import CarBrozShared

@main
struct CarBrozPartnerApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        let environment = Bundle.main.object(forInfoDictionaryKey: "CarBrozEnvironment") as? String ?? ""
        let apiBaseUrl = Bundle.main.object(forInfoDictionaryKey: "CarBrozApiBaseUrl") as? String ?? ""
        let versionName = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
        let versionCodeString = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? ""
        let versionCode = Int64(versionCodeString) ?? 0
        let applicationId = Bundle.main.bundleIdentifier ?? ""

        IosApplicationBootstrapKt.initializeCarBrozIosApplication(
            environment: environment,
            apiBaseUrl: apiBaseUrl,
            versionName: versionName,
            versionCode: versionCode,
            applicationId: applicationId
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { phase in
            switch phase {
            case .active:
                AppLifecycleBridge.shared.moveToForeground()
            case .background:
                AppLifecycleBridge.shared.moveToBackground()
            case .inactive:
                break
            @unknown default:
                break
            }
        }
    }
}
