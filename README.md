# VibeShield
Security scanning for sites built with AI — findings come with ready-to-paste prompts for your AI builder.

## What it does
A growing wave of non-technical builders are shipping live websites and small SaaS tools with AI-first tools like Lovable, Cursor, v0, Bolt, and Replit. The sites work, but they routinely leak API keys in client-side bundles, expose admin pages, ship without HTTPS or basic security headers, or sit on misconfigured backends like Supabase tables without RLS — and the owner has no way to tell. A traditional security report wouldn't help: they can't read or write the code it points at. VibeShield runs a surface-level scan of a registered site and shows the findings in a plain dashboard, sorted by severity. The leverage is in what comes next: each finding ships with a ready-to-paste prompt for the same AI builder that wrote the site, so the user fixes the problem by asking their tool, not by reading code.
