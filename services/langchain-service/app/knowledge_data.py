"""Curated grounding content for the fix-prompt RAG knowledge base.

Each entry is a short, cited security write-up keyed to one of VibeShield's
scanner check types (see ScanOption in client/src/types/domain.ts). This
module is just the source of truth for the text and citations — it's embedded
and loaded into Postgres offline by app/embed_knowledge.py, not read directly
at request time.
"""

KNOWLEDGE_ENTRIES: list[dict[str, str]] = [
    {
        "check_type": "https",
        "source": "OWASP A02:2021 - Cryptographic Failures; MDN Strict-Transport-Security",
        "title": "HTTPS enforcement (HSTS)",
        "content": (
            "Sites reachable over plain HTTP expose all traffic - including session "
            "cookies and form data - to interception and downgrade attacks on the "
            "network path. Redirect all HTTP traffic to HTTPS and send a "
            "Strict-Transport-Security response header (e.g. max-age=31536000; "
            "includeSubDomains) so browsers refuse to fall back to HTTP even if a "
            "user types the bare domain."
        ),
    },
    {
        "check_type": "https",
        "source": "MDN Mixed content; OWASP A02:2021 - Cryptographic Failures",
        "title": "Mixed content on HTTPS pages",
        "content": (
            "A page served over HTTPS that loads scripts, stylesheets, or images "
            "over plain HTTP ('mixed content') lets an attacker on the network "
            "tamper with those subresources even though the main page looks secure. "
            "Serve every asset the page loads - scripts, styles, images, fonts, "
            "iframes - over HTTPS as well."
        ),
    },
    {
        "check_type": "headers",
        "source": "OWASP Secure Headers Project; MDN Content-Security-Policy; CWE-693",
        "title": "Content-Security-Policy",
        "content": (
            "Without a Content-Security-Policy header, the browser will execute any "
            "inline or third-party script injected into the page, which is what "
            "turns a single injection point into a full cross-site-scripting (XSS) "
            "exploit. Add a CSP header that restricts script-src to the site's own "
            "origin and known trusted hosts, and avoid 'unsafe-inline' where possible."
        ),
    },
    {
        "check_type": "headers",
        "source": "MDN X-Frame-Options; CWE-1021",
        "title": "Clickjacking protection (X-Frame-Options / frame-ancestors)",
        "content": (
            "Without X-Frame-Options or a CSP frame-ancestors directive, another "
            "site can load this page inside a hidden iframe and trick users into "
            "clicking real buttons on it (clickjacking). Send `X-Frame-Options: "
            "DENY` (or SAMEORIGIN if the app is legitimately framed by itself), or "
            "an equivalent frame-ancestors CSP directive."
        ),
    },
    {
        "check_type": "adminPaths",
        "source": "OWASP A01:2021 - Broken Access Control; CWE-284",
        "title": "Exposed admin or login surface",
        "content": (
            "An admin or internal login page reachable at a predictable path (e.g. "
            "/admin, /wp-admin) with no additional access restriction gives every "
            "visitor - not just staff - a direct path to brute-force or "
            "credential-stuff. Require authentication before any admin route "
            "responds with real content, and consider network-level restriction "
            "(IP allowlist, VPN) for the most sensitive paths."
        ),
    },
    {
        "check_type": "adminPaths",
        "source": "OWASP A07:2021 - Identification and Authentication Failures",
        "title": "Default or unauthenticated admin credentials",
        "content": (
            "Admin panels shipped with a default username/password, or with no "
            "login at all, are one of the most common ways a site builder's back "
            "office ends up publicly writable. Require a strong, unique login for "
            "every admin surface before shipping, and enforce it server-side, not "
            "just by hiding a UI link."
        ),
    },
    {
        "check_type": "secrets",
        "source": "CWE-798: Use of Hard-Coded Credentials; OWASP A02:2021",
        "title": "Hard-coded API keys or secrets in client-side code",
        "content": (
            "An API key, database credential, or signing secret embedded in "
            "client-side JavaScript ships to every visitor's browser and is "
            "trivially recoverable from the page source or network tab. Move any "
            "secret-bearing call behind a server endpoint the client calls "
            "instead, and rotate the exposed key once it's removed."
        ),
    },
    {
        "check_type": "secrets",
        "source": "CWE-540: Inclusion of Sensitive Information in Source Code",
        "title": "Secrets committed in version-controlled config",
        "content": (
            "A committed .env file or config with real credentials stays in git "
            "history even after later deletion, so anyone with repo access (or a "
            "leaked clone) can recover it. Use the AI builder's secret or "
            "environment-variable feature instead of committing credentials, and "
            "rotate any key that was ever committed."
        ),
    },
    {
        "check_type": "sensitiveFiles",
        "source": "CWE-538: Insertion of Sensitive Information into Externally-Accessible File; OWASP A05:2021",
        "title": "Exposed .git directory or backup files",
        "content": (
            "A publicly reachable .git directory, .env file, or database backup "
            "(.sql, .zip) lets anyone reconstruct source history, credentials, or "
            "user data without needing any other vulnerability. Remove these paths "
            "from the deployed output and block them at the web-server or CDN "
            "level; treat any secret found inside them as compromised."
        ),
    },
    {
        "check_type": "sensitiveFiles",
        "source": "OWASP A05:2021 - Security Misconfiguration",
        "title": "Directory listing enabled",
        "content": (
            "If the web server lists a directory's contents when there's no index "
            "file, an attacker can browse the file tree and find anything not "
            "meant to be public. Disable directory listing at the web-server or "
            "hosting level."
        ),
    },
    {
        "check_type": "cookies",
        "source": "CWE-614: Sensitive Cookie Without 'Secure' Attribute; CWE-1004",
        "title": "Missing Secure and HttpOnly cookie flags",
        "content": (
            "A session cookie without the Secure flag can be sent over plain HTTP "
            "if an attacker forces a downgrade, and without HttpOnly it's readable "
            "by any JavaScript on the page - including injected XSS payloads. Set "
            "both flags on every cookie that carries a session or auth token."
        ),
    },
    {
        "check_type": "cookies",
        "source": "OWASP Session Management Cheat Sheet; MDN Set-Cookie SameSite",
        "title": "Missing SameSite attribute",
        "content": (
            "A cookie with no SameSite attribute (or SameSite=None without Secure) "
            "is sent along with cross-site requests, which is what makes "
            "cross-site request forgery (CSRF) possible against "
            "session-authenticated actions. Set SameSite=Lax (or Strict where the "
            "app's navigation flow allows it) on session cookies."
        ),
    },
]
