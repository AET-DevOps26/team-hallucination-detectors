export type ApiState =
  | { status: "loading" }
  | { status: "success"; message: string }
  | { status: "error"; error: string };

export type Session = {
  username: string;
  email?: string;
  token?: string;
};

export type AuthMode = "login" | "register" | "reset";
export type Severity = "Critical" | "High" | "Medium" | "Low" | "Info";
export type FindingStatus = "Open" | "Fixed" | "Ignored";

export type ScanOption =
  | "crawl"
  | "https"
  | "headers"
  | "adminPaths"
  | "secrets"
  | "sensitiveFiles";

export type Site = {
  id: string;
  name: string;
  url: string;
};

export type Finding = {
  id: string;
  title: string;
  severity: Severity;
  status: FindingStatus;
  affected: string;
  summary: string;
  impact: string;
  check: string;
  reason?: string;
};

export type Analysis = {
  id: string;
  siteId: string;
  siteName: string;
  url: string;
  createdAt: string;
  status: "Pending" | "Running" | "Completed" | "Failed";
  selectedScans: ScanOption[];
  crawlDepth: number;
  includeSubdomains: boolean;
  findings: Finding[];
};

export type TeamMember = {
  id: string;
  email: string;
  role: "Owner" | "Member" | "Pending";
};

export type NewAnalysisInput = {
  url: string;
  selectedScans: ScanOption[];
  crawlDepth: number;
  includeSubdomains: boolean;
};
