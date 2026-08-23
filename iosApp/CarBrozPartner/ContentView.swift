import SwiftUI
import UIKit
import CarBrozShared

struct ContentView: View {
    var body: some View {
        ComposeRootView()
            .ignoresSafeArea()
    }
}

private struct ComposeRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // The shared Compose root owns its own UI state.
    }
}
