export type UrlValidation =
  | { ok: true; url: string }
  | { ok: false; error: string };

/**
 * Validates and normalizes a user-entered target URL. Shared by the hero scan
 * box and the advanced analysis form so both enforce the same contract:
 * http/https only, trailing slash stripped.
 */
export function validateTargetUrl(input: string): UrlValidation {
  const trimmed = input.trim();
  if (!trimmed) {
    return { ok: false, error: "Enter a URL to scan." };
  }

  let parsed: URL;
  try {
    parsed = new URL(trimmed);
  } catch {
    return { ok: false, error: "Enter a valid URL." };
  }

  if (!["http:", "https:"].includes(parsed.protocol)) {
    return { ok: false, error: "Enter a valid http or https URL." };
  }

  return { ok: true, url: parsed.toString().replace(/\/$/, "") };
}

/** Best-effort hostname for display; falls back to the raw string. */
export function hostnameOf(url: string): string {
  try {
    return new URL(url).hostname;
  } catch {
    return url;
  }
}
