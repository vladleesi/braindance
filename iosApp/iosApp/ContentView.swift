// SPDX-License-Identifier: MPL-2.0

import SwiftUI
import shared

struct ContentView: View {
	var body: some View {
		ComposeView()
			// Compose handles system insets and the keyboard, as on Android. SwiftUI's keyboard avoidance would shrink
			// the view on top of Compose's keyboard inset and squeeze screens to their title.
			.ignoresSafeArea()
	}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}
