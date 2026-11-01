// SPDX-License-Identifier: MPL-2.0

import SwiftUI
import shared

struct ContentView: View {
	var body: some View {
		ComposeView()
			// Compose handles system insets; retain SwiftUI's keyboard avoidance.
			.ignoresSafeArea(.container)
	}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}
