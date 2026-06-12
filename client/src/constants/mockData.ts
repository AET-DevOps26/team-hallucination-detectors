import { TeamMember } from "../types/domain";

// Websites, scans, and findings come from the real API (useAnalysisState).
// Team management has no backend yet, so members remain client-side data.
export const initialMembers: TeamMember[] = [
  { id: "member-1", email: "owner@example.com", role: "Owner" },
  { id: "member-2", email: "security@example.com", role: "Member" },
];
