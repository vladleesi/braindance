// SPDX-License-Identifier: MPL-2.0


import Foundation
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    let onThemeOverrideChanged: (ColorScheme?) -> Void

    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {
        // ignore
    }		

    func makeUIViewController(context: Context) -> some UIViewController {
        BraindanceAppIOSKt.mainViewController { lightOverride in
            onThemeOverrideChanged(lightOverride.map { $0.boolValue ? .light : .dark })
        }
    }
}
