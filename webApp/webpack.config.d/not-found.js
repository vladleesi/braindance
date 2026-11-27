// SPDX-License-Identifier: MPL-2.0

{
    if (config.devServer) {
        const path = require("node:path");
        const resources = path.resolve(__dirname, "../../../../webApp/build/processedResources/wasmJs/main");
        // Webpack's development server serves the app shell for the same browser routes as the Pages build.
        const screens = new Set([
            "/", "/popular", "/anticipated", "/events", "/giveaways", "/discover", "/releases", "/news",
            "/library", "/profile",
        ]);
        const detail = /^\/(?:games|game|events|giveaway)\/([1-9][0-9]{0,9})$/;

        config.devServer.setupMiddlewares = (middlewares) => {
            middlewares.push({
                name: "braindance-document-fallback",
                middleware(req, res, next) {
                    if (req.method !== "GET" && req.method !== "HEAD") return next();
                    const pathname = req.path;
                    const id = detail.exec(pathname)?.[1];
                    if (screens.has(pathname) || (id && Number(id) <= 2147483647)) {
                        return res.sendFile("index.html", { root: resources });
                    }
                    return res.status(404).sendFile("404.html", { root: resources });
                },
            });
            return middlewares;
        };
    }
}
