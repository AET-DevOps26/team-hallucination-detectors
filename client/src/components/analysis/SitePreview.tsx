import { useEffect, useRef, useState } from "react";
import { Skeleton } from "../ui/Skeleton";
import { hostnameOf } from "../../utils/url";

type SitePreviewProps = {
  url: string;
};

/**
 * Live preview of the scanned site inside a faux browser chrome. Cross-origin
 * framing can't be reliably inspected from JS (X-Frame-Options / CSP
 * frame-ancestors block silently and onLoad still fires), so we treat the
 * iframe as best-effort: show a shimmer until load, then reveal it, and always
 * offer "Open live site" plus a note in case the frame is blank. Isolated here
 * so a future backend screenshot endpoint can drop in without touching callers.
 */
export function SitePreview({ url }: SitePreviewProps) {
  const [loaded, setLoaded] = useState(false);
  const host = hostnameOf(url);
  // Re-arm the shimmer whenever the target URL changes (e.g. after a rescan).
  const frameKey = useRef(url);
  frameKey.current = url;

  useEffect(() => {
    setLoaded(false);
  }, [url]);

  return (
    <div className="overflow-hidden rounded-xl border border-line bg-surface shadow-card">
      {/* Browser chrome */}
      <div className="flex items-center gap-3 border-b border-line bg-elevated px-4 py-2.5">
        <div className="flex gap-1.5">
          <span className="h-3 w-3 rounded-full bg-red-400/70" />
          <span className="h-3 w-3 rounded-full bg-amber-400/70" />
          <span className="h-3 w-3 rounded-full bg-emerald-400/70" />
        </div>
        <div className="flex min-w-0 flex-1 items-center gap-2 rounded-md border border-line bg-surface px-3 py-1.5 text-xs text-muted">
          <svg className="h-3.5 w-3.5 shrink-0" fill="none" stroke="currentColor" strokeWidth={1.7} viewBox="0 0 24 24">
            <path d="M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Zm0 0c2.5-2.5 3.75-5.5 3.75-9S14.5 5.5 12 3m0 18c-2.5-2.5-3.75-5.5-3.75-9S9.5 5.5 12 3M3.5 9h17M3.5 15h17" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          <span className="truncate">{host}</span>
        </div>
        <a
          className="shrink-0 rounded-md px-2.5 py-1.5 text-xs font-semibold text-primary transition hover:bg-primary/10"
          href={url}
          rel="noreferrer"
          target="_blank"
        >
          Open live site ↗
        </a>
      </div>

      {/* Viewport */}
      <div className="relative aspect-[16/10] w-full bg-elevated">
        {!loaded && (
          <div className="absolute inset-0 p-4">
            <Skeleton className="h-full w-full" />
          </div>
        )}
        <iframe
          className={`h-full w-full border-0 transition-opacity duration-500 ${loaded ? "opacity-100" : "opacity-0"}`}
          key={url}
          loading="lazy"
          onLoad={() => setLoaded(true)}
          referrerPolicy="no-referrer"
          sandbox="allow-scripts allow-same-origin"
          src={url}
          title={`Preview of ${host}`}
        />
      </div>
    </div>
  );
}
