#!/usr/bin/env python3
# SPDX-License-Identifier: MPL-2.0

"""Check the configured edge with dummy credentials; never prints response bodies or secret headers."""

import sys
import time
import urllib.error
import urllib.parse
import urllib.request


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def main():
    require(len(sys.argv) == 3, "Usage: check-cloudflare-cache.py HTTPS_API_ORIGIN HTTPS_WEB_ORIGIN")
    base, web_origin = sys.argv[1:]
    for value in (base, web_origin):
        parsed = urllib.parse.urlsplit(value)
        require(
            parsed.scheme == "https" and parsed.hostname and not parsed.username
            and not parsed.password and not parsed.path and not parsed.query and not parsed.fragment,
            "Arguments must be exact HTTPS origins without a trailing slash",
        )
    opener = urllib.request.build_opener(NoRedirect())
    opener.addheaders = [("User-Agent", "BraindanceCacheCheck/1.0")]

    def request(path, method="GET", headers=None):
        # Keep a burst of uncached verification keys below the existing four-per-second IGDB limit.
        if path.startswith("/v1/games/"):
            time.sleep(0.3)
        req = urllib.request.Request(base + path, headers=headers or {}, method=method)
        try:
            response = opener.open(req, timeout=40)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            status = response.code
            result = response.headers
            # Drain the bounded public response for connection completion; do not log/store its body.
            require(len(response.read(3 * 1024 * 1024 + 1)) <= 3 * 1024 * 1024, "Unexpected response size")
        require(result.get("CF-Ray"), "Response did not pass through Cloudflare")
        cache_status = result.get("CF-Cache-Status", "").upper()
        print(f"{method} {path.split('?')[0]}: HTTP {status}, CF-Cache-Status={cache_status or 'absent'}")
        return status, result, cache_status

    def verify_public(path, origin=None, expect_miss=False):
        saw_hit = False
        for attempt in range(5):
            status, headers, cache_status = request(path, headers={"Origin": origin} if origin else {})
            require(status == 200, "Public data unavailable; verify origin, upstream, TLS and WAF")
            if expect_miss and attempt == 0:
                require(cache_status == "MISS", "Fresh public query did not produce a MISS")
            require(headers.get("Cache-Control") == "public, max-age=1200", "Expected a 20-minute public TTL")
            require(not headers.get("Set-Cookie"), "Public response contains a cookie")
            require(headers.get("Access-Control-Allow-Origin") == origin, "Cached CORS variant mismatch")
            require("origin" in {value.strip().lower() for value in headers.get("Vary", "").split(",")},
                    "Missing Vary: Origin")
            require(headers.get("Access-Control-Allow-Credentials") is None, "Credentialed CORS is enabled")
            require(headers.get("X-Origin-Verify") is None, "Origin verification header leaked")
            require(headers.get("X-Public-Cache-Eligible") is None, "Cache attestation header leaked")
            if cache_status == "HIT":
                require(headers.get("Age") is not None, "Cache HIT has no Age")
                saw_hit = True
                break
            time.sleep(1)
        require(saw_hit, "No warm HIT; check cache/attestation rules, Origin Cache Control and Development Mode")

    # A supported timestamp creates a fresh cache key without purging unrelated cached data.
    fresh_path = f"/v1/games/anticipated?currentTimestamp={int(time.time())}&pageSize=1"
    verify_public(fresh_path, expect_miss=True)

    public_paths = [
        "/v1/giveaways",
        "/v1/games/details?id=42",
        "/v1/games/anticipated?pageSize=20",
        "/v1/games/popularity?type=34&pageSize=40",
        "/v1/games/popular?ids=42,43&pageSize=20",
    ]
    for path in public_paths:
        verify_public(path)
        verify_public(path, web_origin)
        verify_public(path)  # Browser CORS headers must not leak into the absent-Origin variant.

    cases = [
        ("/v1/giveaways", "GET", {"Authorization": "Bearer dummy-cdn-check"}),
        ("/v1/giveaways", "GET", {"aUtHoRiZaTiOn": ""}),
        ("/v1/giveaways", "GET", {"Cookie": "session=dummy-cdn-check"}),
        ("/v1/giveaways", "GET", {"Cookie": ""}),
        ("/v1/giveaways", "GET", {"X-User-Id": "dummy-user"}),
        ("/v1/giveaways", "GET", {"X-Api-Key": "dummy-key"}),
        ("/v1/giveaways", "GET", {"X-Auth-Token": "dummy-token"}),
        ("/v1/giveaways", "GET", {"X-Session-Id": "dummy-session"}),
        ("/v1/giveaways", "GET", {"Proxy-Authorization": "dummy"}),
        ("/v1/giveaways", "GET", {"X-Forwarded-Host": "evil.example"}),
        ("/v1/giveaways", "GET", {"X-Original-URL": "/v1/favorites"}),
        ("/v1/giveaways", "GET", {"X-HTTP-Method-Override": "POST"}),
        ("/v1/giveaways", "GET", {"Origin": "https://evil.example"}),
        ("/v1/giveaways", "GET", {"Authorization": "Bearer dummy", "X-Public-Cache-Eligible": "1"}),
        ("/v1/giveaways", "GET", {"Forwarded": "host=evil.example", "X-Public-Cache-Eligible": "1"}),
        ("/v1/giveaways?token=dummy", "GET", {}),
        ("/v1/giveaways?a=1&a=2", "GET", {}),
        ("/v1/giveaways/image?url=https%3A%2F%2Fevil.example%2Foffers%2Fa.png", "GET", {}),
        ("/v1/giveaways/file.css", "GET", {}),
        ("/v1/favorites/file.css", "GET", {}),
        ("/health", "GET", {}),
        ("/v1/games/details?id=42&token=dummy", "GET", {}),
        ("/v1/games/details?id=42&id=43", "GET", {}),
        ("/v1/games/details?id=42", "GET", {"Authorization": "Bearer dummy"}),
        ("/v1/games/details?id=42", "GET", {"Cookie": "session=dummy"}),
    ]
    cases.extend(("/v1/giveaways", method, {}) for method in ("POST", "PUT", "PATCH", "DELETE", "HEAD"))
    cases.extend(("/v1/games/details?id=42", method, {}) for method in ("POST", "PUT", "PATCH", "DELETE", "HEAD"))
    for path, method, request_headers in cases:
        for _ in range(2):
            status, headers, cache_status = request(path, method, request_headers)
            directives = {item.strip().lower() for item in headers.get("Cache-Control", "").split(",")}
            require({"private", "no-store"} <= directives, "Excluded response lost private/no-store directives")
            require(cache_status in ("BYPASS", "DYNAMIC"), "Excluded request entered the shared-cache path")
            require(headers.get("Age") is None, "Excluded request has cached Age")
            require(headers.get("X-Origin-Verify") is None, "Origin verification header leaked")
            require(headers.get("X-Public-Cache-Eligible") is None, "Cache attestation header leaked")
            require(headers.get("Authorization") is None, "Authorization response header leaked")
            origin = request_headers.get("Origin")
            if origin:
                expected = web_origin if origin == web_origin else None
                require(headers.get("Access-Control-Allow-Origin") == expected, "CORS origin mismatch")
                require(headers.get("Access-Control-Allow-Credentials") is None, "Credentialed CORS is enabled")

    # The entry can expire during the bypass probes; refill it and verify the absent-Origin variant again.
    verify_public("/v1/giveaways")
    print("PASS: public GET/CORS variants and excluded requests stayed separated at this edge.")
    print("Check Cloud Run request logs separately to confirm HIT requests did not contact the origin.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, urllib.error.URLError, TimeoutError) as error:
        # Do not print the exception: it can contain a request URL or provider diagnostic information.
        print("FAIL: edge regression check failed; inspect the last request and dashboard settings.", file=sys.stderr)
        if isinstance(error, RuntimeError):
            print(str(error), file=sys.stderr)
        sys.exit(1)
