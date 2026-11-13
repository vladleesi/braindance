// SPDX-License-Identifier: MPL-2.0

import FirebaseCore
import FirebaseCrashlytics
import SwiftUI

@main
struct iOSApp: App {
	init() {
		startCrashReporting()
	}

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}

/// Starts Firebase, and with it Crashlytics, in Release builds that bundle the ignored `GoogleService-Info.plist`.
/// Debug builds and builds without the file, such as contributor builds, run without Firebase.
private func startCrashReporting() {
	#if !DEBUG
	guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else { return }
	FirebaseApp.configure()
	#endif
}
