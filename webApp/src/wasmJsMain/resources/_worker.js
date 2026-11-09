// SPDX-License-Identifier: MPL-2.0

// Cloudflare Pages worker. The build's `_routes.json` runs it only for the content-hashed Wasm modules; every other
// path is a plain static asset, which costs no Functions requests. Pages compresses on the fly at a low Brotli level,
// so this serves the build's maximum-quality copy (`<module>.wasm.br`) instead, about a fifth smaller.
// Cloudflare passes a Brotli response through to browsers that accept Brotli and decodes it for any that do not.
// Pages applies `_headers` only to static responses, so the response sets its own caching and security headers.
// If the project's Functions quota runs out, Pages serves the plain module as long as the project stays on
// "Fail open", the default.

const year = 31536000;

export default {
    async fetch(request, env) {
        const url = new URL(request.url);
        if (request.method === 'GET' && url.pathname.endsWith('.wasm')) {
            try {
                const compressed = await brotliCopy(url, env);
                if (compressed) return compressed;
            } catch (error) {
                // The plain module is the same file, so a failure here costs only the smaller download, never the
                // app. Logs only the hashed path and the error; Cloudflare keeps them only in its live log stream.
                console.error(`Serving the plain module for ${url.pathname}: ${error}`);
            }
        }
        return env.ASSETS.fetch(request);
    },
};

/** Returns the module's Brotli copy, or null when the build has none. */
async function brotliCopy(url, env) {
    const compressed = await env.ASSETS.fetch(new URL(`${url.pathname}.br`, url.origin));
    const contentType = compressed.headers.get('Content-Type') ?? '';
    // A missing copy falls back to the single-page app's HTML, so only a non-HTML success is the module.
    if (!compressed.ok || contentType.startsWith('text/html')) return null;
    return new Response(compressed.body, {
        headers: {
            'Content-Type': 'application/wasm',
            'Content-Encoding': 'br',
            'Cache-Control': `public, max-age=${year}, immutable`,
            Vary: 'Accept-Encoding',
            'X-Content-Type-Options': 'nosniff',
            'Strict-Transport-Security': `max-age=${year}`,
        },
        // The body is already Brotli; sending it as is keeps the runtime from compressing it again.
        encodeBody: 'manual',
    });
}
