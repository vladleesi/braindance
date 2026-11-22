// SPDX-License-Identifier: MPL-2.0

// Tests for the link-preview worker in webApp/src/wasmJsMain/resources/_worker.js.
// Run with: node --test webApp/src/edgeTest/*.test.mjs
// They load the worker as the build writes it, with its placeholders filled in, and call its fetch handler with stubs
// for the static assets, the API, and the edge cache.

import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { afterEach, beforeEach, describe, it } from "node:test";

const PUBLIC_ORIGIN = "https://app.example.com";
const API = "https://api.example.com";
const SHELL = `<!DOCTYPE html>
<html lang="en">
<head>
    <!-- page-meta:start -->
    <title>Braindance: find what to play next</title>
    <meta name="description" content="Home">
    <link rel="canonical" href="${PUBLIC_ORIGIN}/">
    <!-- page-meta:end -->
</head>
<body><script src="webApp.0123abcd.js"></script></body>
</html>`;

const source = await readFile(new URL("../wasmJsMain/resources/_worker.js", import.meta.url), "utf8");

async function loadWorker(backend = API) {
    const code = source.replaceAll("__BACKEND_BASE_URL__", backend).replaceAll("__PUBLIC_ORIGIN__", PUBLIC_ORIGIN);
    const module = await import(`data:text/javascript;base64,${Buffer.from(code).toString("base64")}`);
    return module.default;
}

let apiRequests;
let apiResponses;
let cacheEntries;
const realFetch = globalThis.fetch;

beforeEach(() => {
    apiRequests = [];
    apiResponses = new Map();
    cacheEntries = new Map();
    globalThis.fetch = async (url, init) => {
        apiRequests.push({ url: String(url), init });
        const respond = apiResponses.get(String(url));
        if (!respond) throw new TypeError("network error");
        return respond();
    };
    globalThis.caches = {
        default: {
            match: async (request) => cacheEntries.get(request.url)?.clone(),
            put: async (request, response) => void cacheEntries.set(request.url, response),
        },
    };
});

afterEach(() => {
    globalThis.fetch = realFetch;
    delete globalThis.caches;
});

function env(shell = SHELL) {
    return {
        ASSETS: {
            fetch: async (input) => {
                const url = new URL(input instanceof Request ? input.url : input);
                if (input instanceof Request && input.method === "POST") return new Response(null, { status: 405 });
                assert.equal(url.pathname, "/", "the worker serves the app's start page");
                return new Response(shell, {
                    headers: {
                        "content-type": "text/html; charset=utf-8",
                        "content-length": String(shell.length),
                        etag: '"static"',
                        "x-frame-options": "DENY",
                    },
                });
            },
        },
    };
}

function json(body, status = 200) {
    return () => new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

async function open(path, { method = "GET", host = PUBLIC_ORIGIN, backend } = {}) {
    const worker = await loadWorker(backend);
    const response = await worker.fetch(new Request(`${host}${path}`, { method }), env(), { waitUntil() {} });
    return { response, html: method === "HEAD" ? "" : await response.text() };
}

const meta = (html, name) =>
    html.match(new RegExp(`<meta (?:name|property)="${name}" content="([^"]*)">`))?.[1] ?? null;
const title = (html) => html.match(/<title>([^<]*)<\/title>/)?.[1] ?? null;
const canonical = (html) => html.match(/<link rel="canonical" href="([^"]*)">/)?.[1] ?? null;

const witcher = {
    id: 1942,
    name: "The Witcher 3: Wild Hunt",
    summary: "A story-driven open world RPG.",
    first_release_date: 1431993600,
    cover: { url: "//images.igdb.com/igdb/image/upload/t_thumb/co1wyy.jpg" },
    screenshots: [{ url: "//images.igdb.com/igdb/image/upload/t_thumb/sctj8u.jpg" }],
};

describe("game pages", () => {
    it("describe the game with its cover and keep the app's page", async () => {
        apiResponses.set(`${API}/v1/games/details?id=1942`, json([witcher]));
        const { response, html } = await open("/games/1942");

        assert.equal(response.status, 200);
        assert.equal(title(html), "The Witcher 3: Wild Hunt | Braindance");
        assert.equal(meta(html, "description"), "A story-driven open world RPG.");
        assert.equal(meta(html, "og:title"), "The Witcher 3: Wild Hunt");
        assert.equal(
            meta(html, "og:image"),
            "https://images.igdb.com/igdb/image/upload/t_cover_big_2x/co1wyy.jpg",
        );
        assert.equal(meta(html, "og:image:alt"), "The cover of The Witcher 3: Wild Hunt");
        assert.equal(meta(html, "twitter:card"), "summary");
        assert.equal(meta(html, "robots"), "noindex");
        assert.equal(canonical(html), `${PUBLIC_ORIGIN}/games/1942`);
        assert.match(html, /<script src="webApp\.0123abcd\.js"><\/script>/);
        assert.equal(html.match(/<title>/g).length, 1, "the start page's own tags are replaced");
        assert.equal(response.headers.get("x-frame-options"), "DENY");
        assert.equal(response.headers.get("etag"), null);
        assert.equal(response.headers.get("content-length"), null);
        assert.equal(response.headers.get("cache-control"), "public, max-age=0, must-revalidate");
    });

    it("ask the API's public edge cache like the app does", async () => {
        apiResponses.set(`${API}/v1/games/details?id=1942`, json([witcher]));
        await open("/games/1942");

        assert.equal(apiRequests.length, 1);
        assert.equal(apiRequests[0].init.headers.origin, PUBLIC_ORIGIN);
    });

    it("give a legacy address the current canonical address", async () => {
        apiResponses.set(`${API}/v1/games/details?id=1942`, json([witcher]));
        const { html } = await open("/game/1942");

        assert.equal(canonical(html), `${PUBLIC_ORIGIN}/games/1942`);
    });

    it("point an edition's or update's page at its game, and keep its own preview", async () => {
        const update = {
            id: 230102,
            name: "The Witcher 3: Wild Hunt - Free Next-Gen Update",
            kind: "update",
            canonical_id: 1942,
            canonical: witcher,
        };
        apiResponses.set(`${API}/v1/games/details?id=230102`, json([update]));
        const { html } = await open("/games/230102");

        assert.equal(title(html), "The Witcher 3: Wild Hunt - Free Next-Gen Update | Braindance");
        assert.equal(canonical(html), `${PUBLIC_ORIGIN}/games/1942`);
    });

    it("keep a game's own address when its canonical ID is missing or invalid", async () => {
        for (const [id, canonicalId] of [[11, undefined], [12, 0], [13, "1942"], [14, 2147483648]]) {
            apiResponses.set(
                `${API}/v1/games/details?id=${id}`,
                json([{ id, name: "Game", canonical_id: canonicalId }]),
            );
            const { html } = await open(`/games/${id}`);
            assert.equal(canonical(html), `${PUBLIC_ORIGIN}/games/${id}`, String(canonicalId));
        }
    });

    it("use Braindance's share image without a cover, and describe a game without a summary", async () => {
        apiResponses.set(
            `${API}/v1/games/details?id=7`,
            json([{ id: 7, name: "Tetris", first_release_date: 455587200, screenshots: witcher.screenshots }]),
        );
        const { html } = await open("/games/7");

        assert.equal(meta(html, "og:image"), `${PUBLIC_ORIGIN}/og-image.png`);
        assert.equal(meta(html, "og:image:alt"), "Braindance logo");
        assert.equal(meta(html, "twitter:card"), "summary_large_image");
        assert.equal(
            meta(html, "description"),
            "Tetris (1984): platforms, genres, where to buy, and similar games on Braindance.",
        );
    });

    it("escape everything the API sends and ignore images from other hosts", async () => {
        apiResponses.set(
            `${API}/v1/games/details?id=8`,
            json([
                {
                    name: `"><script>alert(1)</script>`,
                    summary: "Fish & <b>chips</b>",
                    cover: { url: "https://evil.example/t_thumb/x.jpg" },
                },
            ]),
        );
        const { html } = await open("/games/8");

        assert.doesNotMatch(html, /<script>alert/);
        assert.equal(title(html), "&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt; | Braindance");
        assert.equal(meta(html, "description"), "Fish &amp; &lt;b&gt;chips&lt;/b&gt;");
        assert.equal(meta(html, "og:image"), `${PUBLIC_ORIGIN}/og-image.png`);
    });

    it("cut a long summary at a word", async () => {
        apiResponses.set(
            `${API}/v1/games/details?id=9`,
            json([{ name: "Long", summary: "word ".repeat(100) }]),
        );
        const { html } = await open("/games/9");
        const description = meta(html, "description");

        assert.ok(description.length <= 200);
        assert.ok(description.endsWith("word…"));
    });
});

describe("missing and invalid pages", () => {
    it("answer an unknown game with 404 and remember it", async () => {
        apiResponses.set(`${API}/v1/games/details?id=5`, json([]));
        const first = await open("/games/5");
        const second = await open("/games/5");

        assert.equal(first.response.status, 404);
        assert.equal(title(first.html), "Page not found | Braindance");
        assert.equal(meta(first.html, "robots"), "noindex");
        assert.equal(canonical(first.html), null);
        assert.equal(second.response.status, 404);
        assert.equal(apiRequests.length, 1, "the second preview comes from the cache");
    });

    for (const path of ["/games/0", "/games/abc", "/games/2147483648", "/games/12/", "/giveaway/-1"]) {
        it(`answer ${path} with 404 without asking the API`, async () => {
            const { response, html } = await open(path);

            assert.equal(response.status, 404);
            assert.equal(meta(html, "robots"), "noindex");
            assert.equal(apiRequests.length, 0);
        });
    }
});

describe("unavailable data", () => {
    for (const [name, respond] of [
        ["a server error", json({ error: "boom" }, 503)],
        ["a network error", null],
        ["a body that is not JSON", () => new Response("<html>", { status: 200 })],
        ["an unexpected shape", json({ id: 1 })],
    ]) {
        it(`keep the app's page with a generic preview after ${name}, and ask again next time`, async () => {
            if (respond) apiResponses.set(`${API}/v1/games/details?id=1942`, respond);
            const first = await open("/games/1942");
            await open("/games/1942");

            assert.equal(first.response.status, 200);
            assert.equal(title(first.html), "Braindance");
            assert.equal(canonical(first.html), `${PUBLIC_ORIGIN}/games/1942`);
            assert.equal(meta(first.html, "og:image"), `${PUBLIC_ORIGIN}/og-image.png`);
            assert.equal(apiRequests.length, 2, "failures are not cached");
        });
    }

    it("never ask the API when the build has no backend", async () => {
        const { response, html } = await open("/games/1942", { backend: "" });

        assert.equal(response.status, 200);
        assert.equal(title(html), "Braindance");
        assert.equal(apiRequests.length, 0);
    });
});

describe("event pages", () => {
    it("shows the event name and a canonical URL", async () => {
        apiResponses.set(`${API}/v1/events`, json([{ id: 4, name: "Sample Showcase" }]));
        const { response, html } = await open("/events/4");
        assert.equal(response.status, 200);
        assert.equal(meta(html, "og:title"), "Sample Showcase");
        assert.equal(canonical(html), `${PUBLIC_ORIGIN}/events/4`);
    });
});

describe("giveaway pages", () => {
    const giveaway = {
        id: 3850,
        title: "Knightica (Epic Games) Giveaway",
        worth: "$14.99",
        platforms: "PC, Epic Games Store",
        end_date: "2026-11-19 23:59:00",
        status: "Active",
        description: "Score Knightica for free.",
        image: "https://api.example.com/v1/giveaways/image?url=https%3A%2F%2Fwww.gamerpower.com%2Foffers%2F1b%2Fx.jpg",
    };

    it("say what is free, where, and until when", async () => {
        apiResponses.set(`${API}/v1/giveaways/3850`, json(giveaway));
        const { response, html } = await open("/giveaway/3850");

        assert.equal(response.status, 200);
        assert.equal(title(html), "Knightica (Epic Games) Giveaway | Braindance");
        assert.equal(
            meta(html, "description"),
            "Free (worth $14.99) on PC, Epic Games Store until Nov 19, 2026. Score Knightica for free.",
        );
        assert.equal(meta(html, "og:image"), giveaway.image.replaceAll("&", "&amp;"));
        assert.equal(canonical(html), `${PUBLIC_ORIGIN}/giveaway/3850`);
    });

    it("say when a giveaway has ended and leave out an unknown worth or end date", async () => {
        apiResponses.set(`${API}/v1/giveaways/1`, json({ ...giveaway, status: "Expired" }));
        apiResponses.set(`${API}/v1/giveaways/2`, json({ ...giveaway, worth: "N/A", end_date: "N/A" }));
        const ended = await open("/giveaway/1");
        const openEnded = await open("/giveaway/2");

        assert.match(meta(ended.html, "description"), /^This giveaway has ended\. /);
        assert.match(meta(openEnded.html, "description"), /^Free on PC, Epic Games Store\. /);
    });

    it("answer an unknown giveaway with 404", async () => {
        apiResponses.set(`${API}/v1/giveaways/404`, json({ error: "not found" }, 404));
        const { response } = await open("/giveaway/404");

        assert.equal(response.status, 404);
    });
});

describe("requests", () => {
    it("answer HEAD without a body", async () => {
        apiResponses.set(`${API}/v1/games/details?id=1942`, json([witcher]));
        const { response } = await open("/games/1942", { method: "HEAD" });

        assert.equal(response.status, 200);
        assert.equal(response.body, null);
    });

    it("leave other methods to the static site", async () => {
        const { response } = await open("/games/1942", { method: "POST" });

        assert.equal(response.status, 405);
        assert.equal(apiRequests.length, 0);
    });

    it("keep the pages.dev address out of search results", async () => {
        apiResponses.set(`${API}/v1/games/details?id=1942`, json([witcher]));
        const { response } = await open("/games/1942", { host: "https://abc123.example-project.pages.dev" });

        assert.equal(response.headers.get("x-robots-tag"), "noindex");
    });

    it("leave a page without the page-meta block unchanged", async () => {
        apiResponses.set(`${API}/v1/games/details?id=1942`, json([witcher]));
        const worker = await loadWorker();
        const plain = "<html><head><title>Plain</title></head></html>";
        const response = await worker.fetch(new Request(`${PUBLIC_ORIGIN}/games/1942`), env(plain), {});

        assert.equal(await response.text(), plain);
    });
});
