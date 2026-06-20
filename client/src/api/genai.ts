import { apiClient } from "./client";
import type { Finding } from "../types/domain";

/**
 * Client for VibeShield's GenAI capability. The gateway routes /langchain/* to
 * the Python LangChain service (see gateway/nginx.conf), so these calls share
 * the same origin as the rest of the API — no separate base URL needed.
 */

/** The finding fields the GenAI service needs to write a fix prompt. */
export type FixPromptInput = Pick<
  Finding,
  "title" | "severity" | "check" | "affected" | "summary" | "impact"
>;

/**
 * Asks the GenAI service to turn one finding into a single prompt the user can
 * paste straight into their AI builder. LLM generation routinely outruns the
 * default 5s client timeout, so we widen it just for this call.
 */
export async function generateFixPrompt(
  finding: FixPromptInput,
): Promise<string> {
  const { data } = await apiClient.post<{ prompt: string }>(
    "/langchain/fix-prompt",
    {
      title: finding.title,
      severity: finding.severity,
      check: finding.check,
      affected: finding.affected,
      summary: finding.summary,
      impact: finding.impact,
    },
    { timeout: 30000 },
  );

  return data.prompt;
}
