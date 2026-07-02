import { apiClient } from "./client";
import type { Finding } from "../types/domain";

/**
 * Client for VibeShield's GenAI capability. The gateway routes /langchain/* to
 * the Python LangChain service (see gateway/nginx.conf), so these calls share
 * the same origin as the rest of the API — no separate base URL needed.
 */

/** The finding fields the GenAI service needs to write a fix prompt. */
export type AiBuilder = "Generic" | "Cursor" | "Lovable" | "v0" | "Bolt" | "Replit";
export type FixPromptMode =
  | "Quick fix"
  | "Detailed implementation"
  | "Explain and fix"
  | "Verification only";

export type FixPromptInput = Pick<
  Finding,
  "title" | "severity" | "checkLabel" | "affected" | "summary" | "impact"
> & {
  builder: AiBuilder;
  mode: FixPromptMode;
  changeStatus?: string;
};

export type FixPromptPackage = {
  prompt: string;
  verificationPrompt: string;
  expectedResult: string;
  riskNote: string;
  rollbackNote: string;
  likelyTargets: string;
  issueType: string;
  builder: AiBuilder;
  mode: FixPromptMode;
};

/**
 * Asks the GenAI service to turn one finding into a single prompt the user can
 * paste straight into their AI builder. LLM generation routinely outruns the
 * default 5s client timeout, so we widen it just for this call.
 */
export async function generateFixPrompt(
  finding: FixPromptInput,
): Promise<FixPromptPackage> {
  const { data } = await apiClient.post<FixPromptPackage>(
    "/langchain/fix-prompt",
    {
      builder: finding.builder,
      mode: finding.mode,
      title: finding.title,
      severity: finding.severity,
      check: finding.checkLabel,
      affected: finding.affected,
      summary: finding.summary,
      impact: finding.impact,
      changeStatus: finding.changeStatus,
    },
    { timeout: 30000 },
  );

  return data;
}
