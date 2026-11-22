// SPDX-License-Identifier: MPL-2.0

/*
 * Link previews for game, event, and giveaway pages, as a Cloudflare Pages Function in advanced mode.
 *
 * _routes.json sends only /games/*, /game/*, /events/*, and /giveaway/* here; every other address is a static file and never
 * runs this code. For those pages it serves the app's usual page with the game's or giveaway's own title,
 * description, and image in its head, so chat apps, social networks, and search engines show what a shared link is
 * about. The app itself loads from the same page as before.
 *
 * - Data comes from the public API, through the edge cache. The extracted preview is kept per data center for a day
 *   (games) or an hour (giveaways and unknown IDs), so repeated previews rarely leave the edge.
 * - The page itself is never cached here: it names the current deployment's bundle, which a later deployment replaces.
 * - Any API failure serves the app's page with a generic preview. An address the app cannot open, or an unknown ID,
 *   gets the same page with status 404, so it never counts as a page of its own.
 * - Game and giveaway pages ask not to be indexed: they are for previews, while the fixed pages carry search traffic.
 *
 * The build replaces the two placeholders below. Without a backend URL, as in builds with sample data, every page gets
 * the generic preview.
 */

const BACKEND_BASE_URL = "__BACKEND_BASE_URL__";
const PUBLIC_ORIGIN = "__PUBLIC_ORIGIN__";

const SITE_NAME = "Braindance";
const META_START = "<!-- page-meta:start -->";
const META_END = "<!-- page-meta:end -->";
const DEFAULT_DESCRIPTION =
    "Discover popular and upcoming games, explore similar titles, and find free game giveaways in one place.";
const DEFAULT_IMAGE_PATH = "/og-image.png";

// The app opens only positive 32-bit IDs, without a trailing slash; see webPathToRoute in BrowserHistory.kt.
const MAX_ID = 2147483647;
const ROUTES = [
    { pattern: /^\/games\/([1-9][0-9]{0,9})$/, kind: "game", prefix: "games" },
    { pattern: /^\/events\/([1-9][0-9]{0,9})$/, kind: "event", prefix: "events" },
    // Game links published before the /games/ address; their canonical address is the current one.
    { pattern: /^\/game\/([1-9][0-9]{0,9})$/, kind: "game", prefix: "games" },
    { pattern: /^\/giveaway\/([1-9][0-9]{0,9})$/, kind: "giveaway", prefix: "giveaway" },
];

const API_TIMEOUT_MS = 3000;
const DESCRIPTION_MAX_LENGTH = 200;
const TITLE_MAX_LENGTH = 120;
const GAME_TTL_SECONDS = 24 * 60 * 60;
const GIVEAWAY_TTL_SECONDS = 60 * 60;
const MISSING_TTL_SECONDS = 60 * 60;

// IGDB serves every image under one name at several sizes; the API sends thumbnails.
const IGDB_IMAGE =
    /^(?:https:)?\/\/images\.igdb\.com\/igdb\/image\/upload\/t_[a-z0-9_]+\/([A-Za-z0-9_-]+)\.(?:jpg|png|webp)$/;
const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

export default {
    async fetch(request, env, context) {
        if (request.method !== "GET" && request.method !== "HEAD") {
            return env.ASSETS.fetch(request);
        }
        const url = new URL(request.url);
        try {
            return await previewPage(request, url, env, context);
        } catch {
            // Whatever went wrong, the visitor still gets the app.
            return env.ASSETS.fetch(new URL("/", url));
        }
    },
};

async function previewPage(request, url, env, context) {
    const shell = await env.ASSETS.fetch(new URL("/", url));
    if (!shell.ok) return shell;
    const page = await shell.text();

    const target = matchRoute(url.pathname);
    let status = 200;
    let meta;
    if (target === null) {
        status = 404;
        meta = notFoundMeta();
    } else {
        const preview = await loadPreview(target, url, context);
        if (preview.status === "missing") {
            status = 404;
            meta = notFoundMeta();
        } else {
            meta = preview.status === "found" ? preview.meta : genericMeta();
            // An edition's or update's page shows its game, so search engines index the game's address instead.
            const canonicalId = target.kind === "game" ? (meta.canonicalId ?? target.id) : target.id;
            meta.canonical = `${PUBLIC_ORIGIN}/${target.prefix}/${canonicalId}`;
        }
    }

    const headers = new Headers(shell.headers);
    // The body differs from the static page, so its validators and length no longer apply.
    headers.delete("content-length");
    headers.delete("etag");
    headers.delete("last-modified");
    headers.set("content-type", "text/html; charset=utf-8");
    headers.set("cache-control", "public, max-age=0, must-revalidate");
    if (url.hostname.endsWith(".pages.dev")) headers.set("x-robots-tag", "noindex");

    const body = request.method === "HEAD" ? null : withMeta(page, renderMeta(meta));
    return new Response(body, { status, headers });
}

/** The page kind and ID at [path], or null for an address the app cannot open. */
function matchRoute(path) {
    for (const route of ROUTES) {
        const match = route.pattern.exec(path);
        if (match) {
            const id = Number(match[1]);
            return id <= MAX_ID ? { kind: route.kind, prefix: route.prefix, id } : null;
        }
    }
    return null;
}

/**
 * The preview of [target]: `found` with its meta, `missing` when the API knows no such ID, or `unavailable` when it
 * could not be asked or did not answer usefully. Found and missing results are kept in this data center's cache.
 */
async function loadPreview(target, url, context) {
    if (!BACKEND_BASE_URL.startsWith("https://")) return { status: "unavailable" };
    const cache = globalThis.caches?.default;
    // A key on this site's own address, under a path no page uses; the cache is shared with the zone.
    const cacheKey = new Request(`${url.origin}/__preview-cache/${target.kind}/${target.id}`);
    const cached = cache ? await cache.match(cacheKey) : undefined;
    if (cached) return cached.json();

    const preview = await fetchPreview(target);
    if (cache && preview.status !== "unavailable") {
        const ttl =
            preview.status === "missing"
                ? MISSING_TTL_SECONDS
                : target.kind === "game"
                  ? GAME_TTL_SECONDS
                  : GIVEAWAY_TTL_SECONDS;
        const stored = new Response(JSON.stringify(preview), {
            headers: { "content-type": "application/json", "cache-control": `max-age=${ttl}` },
        });
        const write = cache.put(cacheKey, stored);
        if (context?.waitUntil) context.waitUntil(write);
        else await write;
    }
    return preview;
}

async function fetchPreview(target) {
    const apiUrl =
        target.kind === "game"
            ? `${BACKEND_BASE_URL}/v1/games/details?id=${target.id}`
            : target.kind === "event"
              ? `${BACKEND_BASE_URL}/v1/events`
              : `${BACKEND_BASE_URL}/v1/giveaways/${target.id}`;
    let response;
    try {
        response = await fetch(apiUrl, {
            // The app's own origin, so the request matches the API's public edge cache like the app's requests do.
            headers: { accept: "application/json", origin: PUBLIC_ORIGIN },
            signal: AbortSignal.timeout(API_TIMEOUT_MS),
        });
    } catch {
        return { status: "unavailable" };
    }
    if (target.kind === "giveaway" && response.status === 404) return { status: "missing" };
    if (!response.ok) return { status: "unavailable" };
    let data;
    try {
        data = await response.json();
    } catch {
        return { status: "unavailable" };
    }
    if (target.kind === "game") {
        // The API answers an unknown game with an empty list.
        if (!Array.isArray(data)) return { status: "unavailable" };
        if (data.length === 0) return { status: "missing" };
        const meta = gameMeta(data[0]);
        return meta ? { status: "found", meta } : { status: "unavailable" };
    }
    if (target.kind === "event") {
        if (!Array.isArray(data)) return { status: "unavailable" };
        const event = data.find(item => item?.id === Number(target.id));
        const name = text(event?.name, TITLE_MAX_LENGTH);
        // An event outside the current feed may still have a games list.
        if (!name) return { status: "found", meta: genericMeta() };
        return {
            status: "found",
            meta: {
                ...genericMeta(),
                title: `${name} | ${SITE_NAME}`,
                socialTitle: name,
                description: `Games featured at ${name}.`,
                image: igdbImage(event.logo?.url, "t_screenshot_big"),
            },
        };
    }
    const meta = giveawayMeta(data);
    return meta ? { status: "found", meta } : { status: "unavailable" };
}

/**
 * The preview of an API game, or null without a name. A shared edition or update keeps its own name and cover, so the
 * link card shows what was shared; only its canonical address is its game's.
 */
function gameMeta(game) {
    const name = text(game?.name, TITLE_MAX_LENGTH);
    if (!name) return null;
    // The cover is the game's own key art; a game without one gets Braindance's share image.
    const cover = igdbImage(game.cover?.url, "t_cover_big_2x");
    const year = releaseYear(game.first_release_date);
    const summary = text(game.summary, Infinity);
    return {
        title: `${name} | ${SITE_NAME}`,
        socialTitle: name,
        description: summary
            ? truncate(summary, DESCRIPTION_MAX_LENGTH)
            : `${name}${year ? ` (${year})` : ""}: platforms, genres, where to buy, and similar games on ${SITE_NAME}.`,
        image: cover,
        imageAlt: cover ? `The cover of ${name}` : null,
        // Covers are portrait, which wide link cards would crop, so they get the compact card.
        largeImage: false,
        index: false,
        canonicalId: gameId(game.canonical_id),
    };
}

/** [value] when it is an ID the app opens, otherwise null. */
function gameId(value) {
    return Number.isInteger(value) && value > 0 && value <= MAX_ID ? value : null;
}

/** The preview of an API giveaway, or null without a title. */
function giveawayMeta(giveaway) {
    const title = text(giveaway?.title, TITLE_MAX_LENGTH);
    if (!title) return null;
    const worth = text(giveaway.worth, 20);
    const platforms = text(giveaway.platforms, 80);
    const ends = endDate(giveaway.end_date);
    const ended = typeof giveaway.status === "string" && giveaway.status.toLowerCase() === "expired";
    const facts = ended
        ? "This giveaway has ended."
        : `Free${worth && worth !== "N/A" ? ` (worth ${worth})` : ""}${platforms ? ` on ${platforms}` : ""}` +
          `${ends ? ` until ${ends}` : ""}.`;
    const description = text(giveaway.description, Infinity);
    const image =
        typeof giveaway.image === "string" && /^https:\/\/[^\s"'<>]+$/.test(giveaway.image) ? giveaway.image : null;
    return {
        title: `${title} | ${SITE_NAME}`,
        socialTitle: title,
        description: truncate(description ? `${facts} ${description}` : facts, DESCRIPTION_MAX_LENGTH),
        image,
        imageAlt: image ? title : null,
        largeImage: Boolean(image),
        index: false,
    };
}

function genericMeta() {
    return {
        title: SITE_NAME,
        socialTitle: SITE_NAME,
        description: DEFAULT_DESCRIPTION,
        image: null,
        imageAlt: null,
        largeImage: true,
        index: false,
    };
}

function notFoundMeta() {
    return { ...genericMeta(), title: `Page not found | ${SITE_NAME}` };
}

/** The head tags for [meta]; every value is escaped, since it comes from the API. */
function renderMeta(meta) {
    const image = meta.image ?? `${PUBLIC_ORIGIN}${DEFAULT_IMAGE_PATH}`;
    const imageAlt = meta.image ? meta.imageAlt : `${SITE_NAME} logo`;
    const tags = [
        `<title>${escapeHtml(meta.title)}</title>`,
        `<meta name="description" content="${escapeHtml(meta.description)}">`,
    ];
    if (!meta.index) tags.push(`<meta name="robots" content="noindex">`);
    if (meta.canonical) tags.push(`<link rel="canonical" href="${escapeHtml(meta.canonical)}">`);
    tags.push(
        `<meta property="og:site_name" content="${SITE_NAME}">`,
        `<meta property="og:type" content="website">`,
        `<meta property="og:title" content="${escapeHtml(meta.socialTitle)}">`,
        `<meta property="og:description" content="${escapeHtml(meta.description)}">`,
    );
    if (meta.canonical) tags.push(`<meta property="og:url" content="${escapeHtml(meta.canonical)}">`);
    tags.push(`<meta property="og:image" content="${escapeHtml(image)}">`);
    if (imageAlt) tags.push(`<meta property="og:image:alt" content="${escapeHtml(imageAlt)}">`);
    const card = meta.image && !meta.largeImage ? "summary" : "summary_large_image";
    tags.push(`<meta name="twitter:card" content="${card}">`);
    return tags.map((tag) => `    ${tag}`).join("\n");
}

/** [page] with [tags] between the page-meta markers, or unchanged when it has no such block. */
function withMeta(page, tags) {
    const start = page.indexOf(META_START);
    const end = page.indexOf(META_END, start);
    if (start === -1 || end === -1) return page;
    return `${page.slice(0, start + META_START.length)}\n${tags}\n    ${page.slice(end)}`;
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

/** [value] as one line of text at most [maxLength] long, or null when it is not a non-blank string. */
function text(value, maxLength) {
    if (typeof value !== "string") return null;
    const line = value.replace(/\s+/g, " ").trim();
    if (!line) return null;
    return Number.isFinite(maxLength) ? truncate(line, maxLength) : line;
}

/** [value] cut at a word boundary to at most [maxLength] characters, ending with an ellipsis when cut. */
function truncate(value, maxLength) {
    if (value.length <= maxLength) return value;
    const cut = value.slice(0, maxLength - 1);
    const space = cut.lastIndexOf(" ");
    return `${(space > maxLength / 2 ? cut.slice(0, space) : cut).replace(/[\s,.;:]+$/, "")}…`;
}

function igdbImage(url, size) {
    if (typeof url !== "string") return null;
    const match = IGDB_IMAGE.exec(url);
    return match ? `https://images.igdb.com/igdb/image/upload/${size}/${match[1]}.jpg` : null;
}

function releaseYear(seconds) {
    if (!Number.isFinite(seconds)) return null;
    const year = new Date(seconds * 1000).getUTCFullYear();
    return Number.isFinite(year) ? year : null;
}

/** A GamerPower end date such as "2026-12-31 23:59:00" as "Dec 31, 2026", or null for "N/A" or anything else. */
function endDate(value) {
    const match = typeof value === "string" ? /^(\d{4})-(\d{2})-(\d{2})[ T]/.exec(value) : null;
    if (!match) return null;
    const month = Number(match[2]);
    const day = Number(match[3]);
    if (month < 1 || month > 12 || day < 1 || day > 31) return null;
    return `${MONTHS[month - 1]} ${day}, ${match[1]}`;
}
