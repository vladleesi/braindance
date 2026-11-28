// SPDX-License-Identifier: MPL-2.0

// Match ThemeChoice.wasmJs.kt before the page's loading screen paints.
{
    let light = false;
    try {
        const theme = localStorage.getItem("braindance.theme");
        const legacyLight = localStorage.getItem("braindance.lightTheme");
        if (theme === "light" || theme === "dark") {
            light = theme === "light";
        } else if (theme === "system") {
            light = !matchMedia("(prefers-color-scheme: dark)").matches;
        } else if (legacyLight === "true" || legacyLight === "false") {
            light = legacyLight === "true";
        }
    } catch {
        // Keep the default when storage is blocked.
    }
    if (light) {
        document.documentElement.dataset.theme = "light";
        document.querySelector('meta[name="theme-color"]').content = "#EAEBEE";
    }
}
