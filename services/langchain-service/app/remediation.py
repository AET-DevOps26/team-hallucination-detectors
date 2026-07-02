from dataclasses import dataclass
from typing import Optional


SUPPORTED_BUILDERS = ("Generic", "Cursor", "Lovable", "v0", "Bolt", "Replit")
SUPPORTED_MODES = (
    "Quick fix",
    "Detailed implementation",
    "Explain and fix",
    "Verification only",
)


@dataclass(frozen=True)
class RemediationGuidance:
    issue_type: str
    secure_behavior: str
    likely_targets: str
    implementation_notes: str
    verification_steps: str
    rollback_note: str
    risk_note: str


DEFAULT_GUIDANCE = RemediationGuidance(
    issue_type="General security finding",
    secure_behavior="The affected surface should no longer expose the reported weakness while existing user flows keep working.",
    likely_targets="Review the route, endpoint, file, deployment setting, or middleware connected to the affected target.",
    implementation_notes="Make the smallest safe change that resolves the finding and preserves current behavior.",
    verification_steps=(
        "Rescan the site in VibeShield. Also manually check the affected URL or path and confirm the issue is no longer visible."
    ),
    rollback_note="If the change breaks an existing flow, revert the last change and apply a narrower fix.",
    risk_note="Changing security behavior can affect routing, authentication, headers, or third-party integrations.",
)


def guidance_for(title: str, check: str, summary: str, impact: str) -> RemediationGuidance:
    text = " ".join([title, check, summary, impact]).lower()

    if "strict-transport-security" in text or "hsts" in text:
        return RemediationGuidance(
            issue_type="Missing HSTS header",
            secure_behavior="All HTTPS responses should include a Strict-Transport-Security header with an appropriate max-age.",
            likely_targets="Hosting custom headers, reverse proxy config, server middleware, CDN rules, or framework header config.",
            implementation_notes=(
                "Add Strict-Transport-Security on HTTPS responses only. Start with max-age=31536000 and avoid includeSubDomains "
                "unless subdomains are confirmed to support HTTPS."
            ),
            verification_steps=(
                "Request the HTTPS URL and confirm the Strict-Transport-Security response header is present. Then rescan in VibeShield."
            ),
            rollback_note="Remove or reduce the HSTS header if it blocks access to required subdomains or non-HTTPS environments.",
            risk_note="HSTS can lock browsers to HTTPS for the configured duration, so subdomain settings should be chosen carefully.",
        )

    if "content-security-policy" in text or "csp" in text:
        return RemediationGuidance(
            issue_type="Missing or weak Content-Security-Policy",
            secure_behavior="The site should send a Content-Security-Policy header that limits script, style, image, connect, and frame sources.",
            likely_targets="Framework header config, server middleware, hosting custom headers, CDN rules, or meta/header configuration.",
            implementation_notes=(
                "Start with a conservative policy such as default-src 'self' and add only the external sources the app actually uses. "
                "Avoid unsafe-inline and broad wildcards unless there is a documented reason."
            ),
            verification_steps=(
                "Load the affected page, confirm the Content-Security-Policy header is present, check the browser console for blocked required assets, "
                "and rescan in VibeShield."
            ),
            rollback_note="If legitimate scripts or assets break, roll back the last policy edit and add only the missing trusted source.",
            risk_note="A too-strict CSP can break scripts, styles, embeds, or API calls; a too-loose CSP may not improve security.",
        )

    if "x-content-type-options" in text or "nosniff" in text:
        return RemediationGuidance(
            issue_type="Missing X-Content-Type-Options header",
            secure_behavior="Responses should include X-Content-Type-Options: nosniff.",
            likely_targets="Server middleware, framework header config, reverse proxy, CDN, or hosting custom headers.",
            implementation_notes="Add the nosniff header globally for normal web responses.",
            verification_steps="Request the affected URL and confirm X-Content-Type-Options: nosniff is present, then rescan in VibeShield.",
            rollback_note="If a legacy asset stops loading, check and fix its Content-Type rather than removing the security header.",
            risk_note="This is usually low risk, but incorrectly typed legacy assets may need their MIME type fixed.",
        )

    if "referrer-policy" in text:
        return RemediationGuidance(
            issue_type="Missing Referrer-Policy header",
            secure_behavior="The site should set a Referrer-Policy that avoids leaking full URLs to external sites.",
            likely_targets="Server middleware, framework header config, reverse proxy, CDN, or hosting custom headers.",
            implementation_notes="Use a policy such as strict-origin-when-cross-origin unless the app has a stricter requirement.",
            verification_steps="Request the affected URL and confirm the Referrer-Policy header is present, then rescan in VibeShield.",
            rollback_note="If analytics or integrations lose required attribution data, adjust the policy deliberately instead of removing it entirely.",
            risk_note="Changing referrer behavior can affect analytics, affiliate tracking, or some third-party integrations.",
        )

    if "permissions-policy" in text:
        return RemediationGuidance(
            issue_type="Missing Permissions-Policy header",
            secure_behavior="Browser features such as camera, microphone, and geolocation should be disabled unless the app needs them.",
            likely_targets="Server middleware, framework header config, reverse proxy, CDN, or hosting custom headers.",
            implementation_notes="Start with camera=(), microphone=(), geolocation=() and only allow features the app explicitly uses.",
            verification_steps="Request the affected URL and confirm the Permissions-Policy header is present, then rescan in VibeShield.",
            rollback_note="If a required browser feature stops working, allow only that feature for the smallest necessary scope.",
            risk_note="Blocking browser permissions can break product features that legitimately use camera, microphone, or geolocation.",
        )

    if "http" in text and ("https" in text or "unencrypted" in text or "redirect" in text):
        return RemediationGuidance(
            issue_type="HTTP reachable without HTTPS enforcement",
            secure_behavior="All HTTP traffic should permanently redirect to the HTTPS version of the same URL.",
            likely_targets="Hosting redirect settings, reverse proxy, CDN rules, load balancer, or server middleware.",
            implementation_notes="Add a permanent 301 redirect from http:// to https:// while preserving path and query string.",
            verification_steps=(
                "Open the http:// URL, confirm it redirects to https://, confirm the page still loads, and rescan in VibeShield."
            ),
            rollback_note="If redirect loops occur, revert the redirect rule and check proxy/forwarded-proto handling before retrying.",
            risk_note="Incorrect redirect rules can create loops or break callbacks that depend on exact URLs.",
        )

    if "secret" in text or "token" in text or "api key" in text or "client bundle" in text:
        return RemediationGuidance(
            issue_type="Secret exposed to the client",
            secure_behavior="Secrets, API keys, and private tokens should not be shipped in client-side JavaScript or public assets.",
            likely_targets="Environment variables, client bundle code, build config, server/API routes, secrets manager, or hosting secrets.",
            implementation_notes=(
                "Move the secret to server-side environment/secrets storage. Replace direct client usage with a server endpoint if the browser needs the result."
            ),
            verification_steps=(
                "Rebuild and redeploy, search the browser bundle/network responses for the secret, rotate the exposed credential, and rescan in VibeShield."
            ),
            rollback_note="If moving the secret breaks functionality, keep the secret server-side and adjust the client to call a backend endpoint.",
            risk_note="Exposed credentials may already be compromised; rotation is usually required, not just code removal.",
        )

    if "admin" in text or "login" in text:
        return RemediationGuidance(
            issue_type="Public admin or login path exposure",
            secure_behavior="Admin and login surfaces should require the intended authentication and should not expose debug or default panels.",
            likely_targets="Routing, auth middleware, admin framework config, deployment routes, or access-control settings.",
            implementation_notes="Ensure the route requires authentication/authorization, remove default panels, and avoid security through obscurity alone.",
            verification_steps=(
                "Open the affected path in a private browser session, confirm unauthenticated access is blocked, then rescan in VibeShield."
            ),
            rollback_note="If legitimate users cannot access the area, revert and apply a narrower role/auth check.",
            risk_note="Auth changes can lock out users or expose admin actions if implemented incompletely.",
        )

    if "sensitive file" in text or "backup" in text or ".env" in text or "robots" in text:
        return RemediationGuidance(
            issue_type="Sensitive file or backup exposed",
            secure_behavior="Sensitive files, backups, source maps, and environment files should not be publicly reachable.",
            likely_targets="Public/static directory, build output, hosting ignore rules, CDN cache, object storage, or repository artifacts.",
            implementation_notes="Remove the file from public assets, block direct access, purge caches, and ensure future builds do not publish it.",
            verification_steps="Request the affected path and confirm it returns 404 or 403, purge CDN cache if used, then rescan in VibeShield.",
            rollback_note="If blocking the path affects required public assets, narrow the deny rule to only the sensitive file patterns.",
            risk_note="If secrets were exposed in the file, rotate them even after the file is removed.",
        )

    return DEFAULT_GUIDANCE


def builder_guidance(builder: str) -> str:
    if builder == "Cursor":
        return (
            "Assume you can inspect and edit the codebase. Search for the relevant route, middleware, config, or deployment header file, "
            "make the smallest safe code change, and mention files you changed."
        )
    if builder == "Lovable":
        return (
            "Phrase the instructions as product/app-builder changes. Ask Lovable to update app settings, server behavior, headers, routes, "
            "or environment variables as needed without requiring the user to edit code manually."
        )
    if builder == "v0":
        return (
            "Keep the instruction concise and implementation-oriented. If backend or hosting changes are needed, explicitly say that the fix "
            "may need to be applied outside the generated UI component."
        )
    if builder == "Bolt":
        return (
            "Assume the builder can modify the app and run checks. Ask it to update the relevant files/config, run the app, and verify the affected path."
        )
    if builder == "Replit":
        return (
            "Mention Replit Secrets for private values and ask it to restart/redeploy after changing env vars, server code, or headers."
        )
    return "Keep the prompt portable for any AI builder and avoid assuming a specific framework unless the finding gives evidence."


def mode_guidance(mode: str) -> str:
    if mode == "Quick fix":
        return "Keep the prompt short and action-first. Include only the essential context, fix request, and verification steps."
    if mode == "Detailed implementation":
        return "Ask for a detailed implementation plan, concrete changes, edge cases, verification steps, and rollback guidance."
    if mode == "Explain and fix":
        return "Briefly explain the issue in plain language first, then ask for the fix and verification steps."
    if mode == "Verification only":
        return "Do not ask for code changes. Generate only a verification prompt that checks whether the issue is fixed."
    return "Generate a practical fix prompt with verification steps."


def rescan_guidance(change_status: Optional[str]) -> str:
    if change_status == "Still present":
        return (
            "This finding was still present after a rescan. Tell the builder the previous attempt did not fully resolve it and ask it to verify the actual deployed behavior."
        )
    if change_status == "Newly introduced":
        return (
            "This finding is newly introduced compared with the previous completed scan. Ask the builder to look for recent changes that may have caused it."
        )
    if change_status == "Fixed":
        return "This finding appears fixed in the latest comparison. Focus on verification and avoid unnecessary changes."
    return "No previous scan comparison status is available for this finding."
