import axios, { AxiosResponse } from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { FixPromptInput } from "./genai";
import { generateFixPrompt } from "./genai";
import { apiClient } from "./client";

vi.mock("../constants/auth", () => ({ devAuthenticated: false, devSession: undefined }));

const finding: FixPromptInput = {
  title: "Missing Content-Security-Policy header",
  severity: "Medium",
  checkLabel: "Security headers",
  affected: "https://shop.example.org/",
  summary: "No CSP header was found.",
  impact: "Increases the blast radius of any injected script.",
};

describe("generateFixPrompt", () => {
  afterEach(() => {
    apiClient.defaults.adapter = axios.defaults.adapter;
  });

  it("posts the finding mapped to the langchain fix-prompt contract with defaults", async () => {
    let seenConfig: { url?: string; body?: unknown; timeout?: number } = {};
    apiClient.defaults.adapter = vi.fn().mockImplementation((config) => {
      seenConfig = { url: config.url, body: JSON.parse(config.data), timeout: config.timeout };
      return Promise.resolve({
        data: { prompt: "Fix it like this." },
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      } satisfies AxiosResponse);
    });

    const prompt = await generateFixPrompt(finding);

    expect(seenConfig.url).toBe("/langchain/fix-prompt");
    expect(seenConfig.body).toEqual({
      title: finding.title,
      severity: finding.severity,
      check: finding.checkLabel,
      affected: finding.affected,
      summary: finding.summary,
      impact: finding.impact,
      builder: "Generic",
      provider: "logos",
    });
    expect(seenConfig.timeout).toBe(30000);
    expect(prompt).toBe("Fix it like this.");
  });

  it("passes through an explicit builder and provider", async () => {
    let seenBody: { builder?: string; provider?: string } = {};
    apiClient.defaults.adapter = vi.fn().mockImplementation((config) => {
      seenBody = JSON.parse(config.data);
      return Promise.resolve({
        data: { prompt: "prompt" },
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      } satisfies AxiosResponse);
    });

    await generateFixPrompt(finding, "Cursor", "openai");

    expect(seenBody.builder).toBe("Cursor");
    expect(seenBody.provider).toBe("openai");
  });
});
