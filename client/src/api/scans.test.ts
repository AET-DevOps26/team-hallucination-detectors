import axios, { AxiosResponse } from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "./client";
import {
  createWebsite,
  getScan,
  getScanComparison,
  listFindings,
  listScans,
  listWebsites,
  rescanScan,
  triggerScan,
} from "./scans";

vi.mock("../constants/auth", () => ({ devAuthenticated: false, devSession: undefined }));

/** Captures the request axios would have sent and returns a scripted body. */
function stub(data: unknown) {
  let request: { url?: string; method?: string; body?: unknown } = {};
  apiClient.defaults.adapter = vi.fn().mockImplementation((config) => {
    request = { url: config.url, method: config.method, body: config.data ? JSON.parse(config.data) : undefined };
    return Promise.resolve({
      data,
      status: 200,
      statusText: "OK",
      headers: {},
      config,
    } satisfies AxiosResponse);
  });
  return () => request;
}

describe("scans API", () => {
  afterEach(() => {
    apiClient.defaults.adapter = axios.defaults.adapter;
  });

  it("listWebsites GETs /api/v1/websites", async () => {
    const seen = stub([{ id: 1 }]);

    const result = await listWebsites();

    expect(seen()).toMatchObject({ url: "/api/v1/websites", method: "get" });
    expect(result).toEqual([{ id: 1 }]);
  });

  it("createWebsite POSTs the url to /api/v1/websites", async () => {
    const seen = stub({ id: 1, url: "https://shop.example.org" });

    await createWebsite("https://shop.example.org");

    expect(seen()).toMatchObject({
      url: "/api/v1/websites",
      method: "post",
      body: { url: "https://shop.example.org" },
    });
  });

  it("listScans GETs the website's scan history", async () => {
    const seen = stub([]);

    await listScans(7);

    expect(seen()).toMatchObject({ url: "/api/v1/websites/7/scans", method: "get" });
  });

  it("triggerScan POSTs the scan request to the website's scans endpoint", async () => {
    const seen = stub({ id: 42, status: "Pending" });

    await triggerScan(7, { checks: ["https"], crawlDepth: 0, includeSubdomains: false });

    expect(seen()).toMatchObject({
      url: "/api/v1/websites/7/scans",
      method: "post",
      body: { checks: ["https"], crawlDepth: 0, includeSubdomains: false },
    });
  });

  it("getScan GETs /api/v1/scans/:id", async () => {
    const seen = stub({ id: 42 });

    await getScan(42);

    expect(seen()).toMatchObject({ url: "/api/v1/scans/42", method: "get" });
  });

  it("listFindings GETs the scan's findings", async () => {
    const seen = stub([]);

    await listFindings(42);

    expect(seen()).toMatchObject({ url: "/api/v1/scans/42/findings", method: "get" });
  });

  it("rescanScan POSTs to the scan's rescan endpoint", async () => {
    const seen = stub({ id: 43 });

    await rescanScan(42);

    expect(seen()).toMatchObject({ url: "/api/v1/scans/42/rescan", method: "post" });
  });

  it("getScanComparison GETs the scan's comparison endpoint", async () => {
    const seen = stub({ comparable: true });

    await getScanComparison(42);

    expect(seen()).toMatchObject({ url: "/api/v1/scans/42/comparison", method: "get" });
  });
});
