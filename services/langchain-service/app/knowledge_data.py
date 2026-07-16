"""Curated grounding content for the fix-prompt RAG knowledge base.

Each entry is a short, cited security write-up keyed to one of VibeShield's
scanner check types (see ScanOption in client/src/types/domain.ts). This
module is just the source of truth for the text and citations — it's embedded
and loaded into Postgres offline by app/embed_knowledge.py, not read directly
at request time.

Granularity matters: there is one entry per distinct finding *variant* the
scanner can emit (see the checks in services/scanner-service .../checks/), not
one generic entry per check type. With several candidates per check type,
the embedding similarity search in app/retrieval.py genuinely selects the
chunks matching the concrete finding (e.g. the Referrer-Policy entry for a
Referrer-Policy finding, not the CSP entry) — a coarser corpus would collapse
retrieval into a lookup table. `crawl` has no entries because page discovery
never produces findings.
"""

KNOWLEDGE_ENTRIES: list[dict[str, str]] = [
    # ------------------------------------------------------------- https ---
    {
        "check_type": "https",
        "source": "OWASP A02:2021 - Cryptographic Failures",
        "title": "Site reachable over unencrypted HTTP",
        "content": (
            "A site that answers over plain HTTP without redirecting to HTTPS "
            "exposes all traffic - including session cookies and form data - to "
            "interception and tampering by anyone on the network path. Redirect "
            "all HTTP requests to HTTPS with a permanent (301/308) redirect at "
            "the web-server or hosting level, so no page is ever served "
            "unencrypted."
        ),
    },
    {
        "check_type": "https",
        "source": "OWASP A02:2021 - Cryptographic Failures; CWE-319",
        "title": "HTTPS not available at all",
        "content": (
            "A site with no working HTTPS endpoint sends every page, credential, "
            "and cookie in cleartext (CWE-319: Cleartext Transmission of "
            "Sensitive Information). Enable TLS on the hosting platform - most "
            "modern hosts provision a free certificate (e.g. Let's Encrypt) "
            "automatically once the domain is connected - and then redirect all "
            "HTTP traffic to the new HTTPS endpoint."
        ),
    },
    {
        "check_type": "https",
        "source": "MDN Strict-Transport-Security; OWASP HSTS Cheat Sheet",
        "title": "Missing Strict-Transport-Security (HSTS) header",
        "content": (
            "Without a Strict-Transport-Security header, browsers will still try "
            "plain HTTP when a user types the bare domain, leaving a window for "
            "downgrade and cookie-stealing attacks even though HTTPS exists. "
            "Send `Strict-Transport-Security: max-age=31536000; includeSubDomains` "
            "on every HTTPS response so browsers refuse to fall back to HTTP for "
            "a year."
        ),
    },
    # ----------------------------------------------------------- headers ---
    {
        "check_type": "headers",
        "source": "OWASP Secure Headers Project; MDN Content-Security-Policy; CWE-693",
        "title": "Missing Content-Security-Policy header",
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
            "clicking real buttons on it (clickjacking, CWE-1021). Send "
            "`X-Frame-Options: DENY` (or SAMEORIGIN if the app is legitimately "
            "framed by itself), or an equivalent frame-ancestors CSP directive."
        ),
    },
    {
        "check_type": "headers",
        "source": "MDN X-Content-Type-Options; OWASP Secure Headers Project",
        "title": "Missing X-Content-Type-Options header",
        "content": (
            "Without `X-Content-Type-Options: nosniff`, browsers may second-guess "
            "a response's declared content type and execute an uploaded file or "
            "mislabeled asset as script or HTML (MIME sniffing). Send the header "
            "with the value `nosniff` on every response; it has no legitimate "
            "downside and most frameworks and CDNs expose a single setting for it."
        ),
    },
    {
        "check_type": "headers",
        "source": "MDN Referrer-Policy; OWASP Secure Headers Project",
        "title": "Missing Referrer-Policy header",
        "content": (
            "Without a Referrer-Policy header, browsers send the full page URL - "
            "which can contain tokens, IDs, or private paths - to every external "
            "site a user clicks through to, and to every third-party resource the "
            "page loads. Send `Referrer-Policy: strict-origin-when-cross-origin` "
            "(the modern default) so cross-site destinations only ever see the "
            "site's origin, never the full path or query string."
        ),
    },
    {
        "check_type": "headers",
        "source": "MDN Permissions-Policy; OWASP Secure Headers Project",
        "title": "Missing Permissions-Policy header",
        "content": (
            "Without a Permissions-Policy header, powerful browser features like "
            "camera, microphone, and geolocation stay available to the page and "
            "anything embedded in it. Send a header that disables what the site "
            "doesn't use, e.g. `Permissions-Policy: camera=(), microphone=(), "
            "geolocation=()`, and explicitly allow only the features the site "
            "actually needs. This is hardening (low severity), not an active "
            "vulnerability on its own."
        ),
    },
    # -------------------------------------------------------- adminPaths ---
    {
        "check_type": "adminPaths",
        "source": "OWASP A07:2021 - Identification and Authentication Failures; WordPress Hardening Guide",
        "title": "WordPress admin or login page publicly reachable",
        "content": (
            "A publicly reachable /wp-admin or /wp-login.php is a constant target "
            "for automated credential-stuffing and brute-force campaigns that scan "
            "the whole internet for WordPress logins. Restrict those paths to "
            "known IPs at the hosting or proxy level where possible, enforce a "
            "strong unique password plus two-factor authentication for every "
            "admin account, and add login rate-limiting."
        ),
    },
    {
        "check_type": "adminPaths",
        "source": "OWASP A05:2021 - Security Misconfiguration; OWASP A01:2021 - Broken Access Control",
        "title": "phpMyAdmin or database console publicly reachable",
        "content": (
            "A database administration console (e.g. phpMyAdmin) exposed to the "
            "internet gives attackers a direct interface to read, modify, or "
            "delete the entire database if its credentials are weak, default, or "
            "ever leaked. Remove public access entirely, or restrict it to known "
            "IPs or a VPN at the hosting/proxy level, and make sure it never uses "
            "default credentials."
        ),
    },
    {
        "check_type": "adminPaths",
        "source": "OWASP A01:2021 - Broken Access Control; CWE-284",
        "title": "Admin panel publicly reachable at a predictable path",
        "content": (
            "An admin surface reachable at a predictable path (e.g. /admin, "
            "/administrator) with no additional access restriction tells every "
            "visitor exactly where to aim credential-stuffing and brute-force "
            "attempts (CWE-284: Improper Access Control). Require authentication "
            "server-side before any admin route responds with real content - "
            "hiding the UI link is not protection - and consider IP allowlisting "
            "or VPN-only access for the most sensitive panels."
        ),
    },
    {
        "check_type": "adminPaths",
        "source": "OWASP Authentication Cheat Sheet; CWE-307",
        "title": "Public login page without brute-force protection",
        "content": (
            "A login form at a predictable path is normal for most sites, but "
            "without rate-limiting it can be brute-forced indefinitely (CWE-307: "
            "Improper Restriction of Excessive Authentication Attempts). Make sure "
            "failed login attempts are rate-limited or trigger a temporary lockout "
            "after repeated failures, and that the form is only ever served and "
            "submitted over HTTPS."
        ),
    },
    # ----------------------------------------------------------- secrets ---
    {
        "check_type": "secrets",
        "source": "CWE-798: Use of Hard-Coded Credentials; Stripe API key security docs",
        "title": "Stripe secret key exposed in client-side code",
        "content": (
            "A Stripe secret key (sk_live_...) in code that ships to the browser "
            "lets anyone who views the page source create charges, issue refunds, "
            "and read payment data as the site owner. Remove it from all "
            "client-side code immediately and roll the key in the Stripe "
            "dashboard - a key that ever shipped to a browser must be treated as "
            "compromised. Only the publishable key (pk_...) belongs client-side; "
            "secret-key calls must move behind a server endpoint."
        ),
    },
    {
        "check_type": "secrets",
        "source": "CWE-798: Use of Hard-Coded Credentials; AWS IAM best practices",
        "title": "AWS access key exposed in client-side code",
        "content": (
            "An AWS access key ID (AKIA...) in code the browser downloads is "
            "routinely harvested by automated scanners within minutes; combined "
            "with its secret key it grants whatever the underlying IAM identity "
            "can do. Deactivate the exposed key in the AWS IAM console, issue a "
            "new one that only the server holds, and move any AWS calls behind a "
            "server endpoint the client calls instead."
        ),
    },
    {
        "check_type": "secrets",
        "source": "CWE-321: Use of Hard-coded Cryptographic Key",
        "title": "Private key exposed in client-side code",
        "content": (
            "A private key block (-----BEGIN PRIVATE KEY-----) in code that ships "
            "to the browser can no longer be considered secret - anyone can "
            "impersonate whatever that key authenticated or decrypt whatever it "
            "protected. Remove it from client-side code immediately, generate a "
            "fresh key pair, and replace the compromised key everywhere it was "
            "used; simply deleting it from the page does not un-leak it."
        ),
    },
    {
        "check_type": "secrets",
        "source": "Supabase Row Level Security docs; OWASP A01:2021 - Broken Access Control",
        "title": "Supabase project configuration exposed client-side (RLS)",
        "content": (
            "A Supabase project URL plus anon key in client-side code is expected "
            "- that key is designed to ship to browsers - but it means anyone can "
            "query the database directly with it. The actual security boundary is "
            "Row Level Security: enable RLS on every table in the Supabase "
            "dashboard and add policies granting only the access each table "
            "needs. A table without RLS is readable and writable by anyone "
            "holding the anon key, which is everyone who visits the site."
        ),
    },
    # ---------------------------------------------------- sensitiveFiles ---
    {
        "check_type": "sensitiveFiles",
        "source": "CWE-538; OWASP A05:2021 - Security Misconfiguration",
        "title": "Environment file (.env) publicly downloadable",
        "content": (
            "A publicly downloadable .env file typically hands over database "
            "passwords, API keys, and signing secrets in one request - full "
            "backend compromise without any other vulnerability. Configure the "
            "web server or hosting platform to never serve dotfiles, remove the "
            "file from the public/deployed folder, and rotate every credential it "
            "contained; once it was reachable, every secret in it is compromised."
        ),
    },
    {
        "check_type": "sensitiveFiles",
        "source": "CWE-538: Insertion of Sensitive Information into Externally-Accessible File",
        "title": "Git repository metadata (.git) exposed",
        "content": (
            "A publicly reachable .git directory (e.g. /.git/config or /.git/HEAD "
            "responding with real content) lets attackers reconstruct the entire "
            "source history with standard tools, including any credentials ever "
            "committed. Block access to the .git path at the web-server, proxy, or "
            "CDN level - or deploy build output instead of a repository checkout - "
            "and treat any secret that ever lived in the repo as compromised."
        ),
    },
    {
        "check_type": "sensitiveFiles",
        "source": "CWE-538; OWASP A05:2021 - Security Misconfiguration",
        "title": "macOS folder metadata (.DS_Store) exposed",
        "content": (
            ".DS_Store files are macOS Finder metadata that list the names of "
            "other files in the same folder, helping attackers discover hidden or "
            "unlinked content such as backups or draft pages. Delete .DS_Store "
            "files from the deployed output and add them to the deployment ignore "
            "list. Low severity on its own - it discloses structure, not content."
        ),
    },
    # ----------------------------------------------------------- cookies ---
    {
        "check_type": "cookies",
        "source": "CWE-614: Sensitive Cookie Without 'Secure' Attribute; MDN Set-Cookie",
        "title": "Cookies set without the Secure attribute",
        "content": (
            "A cookie without the Secure attribute will also be sent over plain "
            "HTTP if an unencrypted connection is ever available, letting anyone "
            "on the network read it (CWE-614). Add `Secure` to every Set-Cookie "
            "response on an HTTPS site - most frameworks expose a single "
            "configuration flag that applies it to all cookies."
        ),
    },
    {
        "check_type": "cookies",
        "source": "CWE-1004: Sensitive Cookie Without 'HttpOnly' Flag; MDN Set-Cookie",
        "title": "Cookies set without the HttpOnly attribute",
        "content": (
            "A cookie without HttpOnly is readable by any JavaScript running on "
            "the page, so a single injected script (XSS) can steal it outright "
            "(CWE-1004). Add `HttpOnly` to every cookie the site's own JavaScript "
            "doesn't need to read - session and login cookies almost never need "
            "to be script-readable."
        ),
    },
    {
        "check_type": "cookies",
        "source": "OWASP Session Management Cheat Sheet; MDN Set-Cookie SameSite",
        "title": "Cookies set without a SameSite attribute",
        "content": (
            "A cookie with no SameSite attribute may be attached to requests that "
            "originate from other sites, which is what makes cross-site request "
            "forgery (CSRF) against session-authenticated actions possible. Add "
            "`SameSite=Lax` (or `Strict` for the most sensitive cookies) to every "
            "Set-Cookie response; `SameSite=None` is only legitimate for "
            "deliberate cross-site embedding and then must be combined with "
            "`Secure`."
        ),
    },
    # -------------------------------------------------------------- cors ---
    {
        "check_type": "cors",
        "source": "OWASP Testing for CORS Misconfiguration; CWE-942",
        "title": "CORS reflects any origin and allows credentials",
        "content": (
            "Reflecting the request's Origin header back in "
            "Access-Control-Allow-Origin while also sending "
            "Access-Control-Allow-Credentials: true lets any website make "
            "authenticated requests with a visitor's logged-in session and read "
            "the responses - effectively account takeover for anyone who visits a "
            "malicious page while logged in (CWE-942). Replace the reflection "
            "with a fixed allow-list of exact trusted origins; browsers forbid "
            "the wildcard `*` with credentials precisely because this combination "
            "is unsafe."
        ),
    },
    {
        "check_type": "cors",
        "source": "OWASP Testing for CORS Misconfiguration; CWE-942",
        "title": "CORS reflects any origin",
        "content": (
            "Echoing whatever Origin a request sends back in "
            "Access-Control-Allow-Origin - instead of checking it against a list "
            "- lets any website read cross-origin responses from this site, "
            "leaking data that wasn't meant to be public (CWE-942: Permissive "
            "Cross-domain Policy). Replace the dynamic reflection with a fixed "
            "allow-list of the exact origins that legitimately need cross-origin "
            "access. A bare `Access-Control-Allow-Origin: *` without credentials "
            "is a separate, deliberate pattern for public APIs and is not the "
            "same flaw."
        ),
    },
]
