import { act, renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import { LlmProviderProvider } from "./LlmProviderProvider";
import {
  DEFAULT_LLM_PROVIDER,
  LLM_PROVIDER_STORAGE_KEY,
  readStoredProvider,
  useLlmProvider,
} from "./useLlmProvider";

describe("readStoredProvider", () => {
  afterEach(() => {
    window.localStorage.clear();
  });

  it("defaults to Logos when nothing is stored", () => {
    expect(readStoredProvider()).toBe(DEFAULT_LLM_PROVIDER);
  });

  it("returns the stored provider when it is a known value", () => {
    window.localStorage.setItem(LLM_PROVIDER_STORAGE_KEY, "openai");

    expect(readStoredProvider()).toBe("openai");
  });

  it("falls back to the default for an unrecognized stored value", () => {
    window.localStorage.setItem(LLM_PROVIDER_STORAGE_KEY, "bogus");

    expect(readStoredProvider()).toBe(DEFAULT_LLM_PROVIDER);
  });
});

describe("useLlmProvider", () => {
  afterEach(() => {
    window.localStorage.clear();
  });

  it("throws when used outside an LlmProviderProvider", () => {
    expect(() => renderHook(() => useLlmProvider())).toThrow(
      "useLlmProvider must be used within an LlmProviderProvider",
    );
  });

  it("provides the stored provider and persists updates", () => {
    window.localStorage.setItem(LLM_PROVIDER_STORAGE_KEY, "openai");

    const { result } = renderHook(() => useLlmProvider(), {
      wrapper: LlmProviderProvider,
    });

    expect(result.current.provider).toBe("openai");

    act(() => {
      result.current.setProvider("logos");
    });

    expect(result.current.provider).toBe("logos");
    expect(window.localStorage.getItem(LLM_PROVIDER_STORAGE_KEY)).toBe("logos");
  });
});
