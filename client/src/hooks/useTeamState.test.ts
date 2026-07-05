import { act, renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { initialMembers } from "../constants/mockData";
import { useTeamState } from "./useTeamState";

describe("useTeamState", () => {
  it("starts with the mock team roster", () => {
    const { result } = renderHook(() => useTeamState());

    expect(result.current.members).toEqual(initialMembers);
  });

  it("invites a new member as Pending without touching existing members", () => {
    const { result } = renderHook(() => useTeamState());

    act(() => {
      result.current.inviteMember("new-hire@example.com");
    });

    expect(result.current.members).toHaveLength(initialMembers.length + 1);
    const invited = result.current.members.at(-1);
    expect(invited?.email).toBe("new-hire@example.com");
    expect(invited?.role).toBe("Pending");
    expect(result.current.members.slice(0, initialMembers.length)).toEqual(initialMembers);
  });

  it("assigns each invited member a distinct id, even invited in the same millisecond", () => {
    // inviteMember derives the id from Date.now(); fake timers pin the clock so
    // this test would flake (or silently pass) without proving the ids differ.
    vi.useFakeTimers();
    const { result } = renderHook(() => useTeamState());

    act(() => {
      result.current.inviteMember("first@example.com");
    });
    vi.advanceTimersByTime(1);
    act(() => {
      result.current.inviteMember("second@example.com");
    });

    const invitedIds = result.current.members.slice(-2).map((member) => member.id);
    expect(new Set(invitedIds).size).toBe(2);
  });

  afterEach(() => {
    vi.useRealTimers();
  });
});
