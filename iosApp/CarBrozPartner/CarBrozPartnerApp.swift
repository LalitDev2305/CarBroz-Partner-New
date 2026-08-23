import SwiftUI
import CarBrozShared

@main
struct CarBrozPartnerApp: App {
    @Environment(\.scenePhase) private var scenePhase

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
