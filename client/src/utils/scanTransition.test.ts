import { describe, expect, it } from "vitest";
import type { ApiScanComparison } from "../api/scans";
import { summarizeScanTransition } from "./scanTransition";

type ComparisonFinding = ApiScanComparison["findings"][number];

function finding(
  changeStatus: ComparisonFinding["changeStatus"],
  severity: ComparisonFinding["severity"],
): ComparisonFinding {
  return {
    changeStatus,
    severity,
    check: "headers",
    title: `${changeStatus} ${severity}`,
    affected: "https://shop.example.org/",
    suggestedFix: "Fix it.",
    effort: { level: "Low", estimate: "1-2 hours" },
  };
}

function comparison(overrides: Partial<ApiScanComparison>): ApiScanComparison {
  return {
    scanId: 42,
    comparable: true,
    message: "",
    summary: { fixed: 0, stillPresent: 0, newlyIntroduced: 0 },
    findings: [],
    actionPlan: [],
    ...overrides,
  };
}

describe("summarizeScanTransition", () => {
  it("treats an incomparable scan as the baseline", () => {
    expect(summarizeScanTransition(comparison({ comparable: false }))).toEqual({
      kind: "baseline",
    });
  });

  it("reports no change when only still-present findings exist", () => {
    const result = summarizeScanTransition(
      comparison({ findings: [finding("Still present", "High")] }),
    );
    expect(result).toEqual({ kind: "no-change" });
  });

  it("buckets fixed and newly introduced findings by severity, worst-first", () => {
    const result = summarizeScanTransition(
      comparison({
        findings: [
          finding("Fixed", "Medium"),
          finding("Fixed", "Medium"),
          finding("Still present", "High"),
          finding("Newly introduced", "Low"),
          finding("Fixed", "Critical"),
        ],
      }),
    );

    expect(result).toEqual({
      kind: "changes",
      fixed: [
        { severity: "Critical", count: 1 },
        { severity: "Medium", count: 2 },
      ],
      introduced: [{ severity: "Low", count: 1 }],
    });
  });
});
