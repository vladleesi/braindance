// SPDX-License-Identifier: MPL-2.0


import Foundation
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {
        // ignore
    }		

    func makeUIViewController(context: Context) -> some UIViewController {
        BraindanceAppIOSKt.mainViewController()
    }
}
