import { useState } from "react";
import { initialMembers } from "../constants/mockData";
import { TeamMember } from "../types/domain";

export function useTeamState() {
  const [members, setMembers] = useState<TeamMember[]>(initialMembers);

  function inviteMember(emailAddress: string) {
    setMembers((current) => [
      ...current,
      { id: `member-${Date.now()}`, email: emailAddress, role: "Pending" },
    ]);
  }

  return { inviteMember, members };
}
