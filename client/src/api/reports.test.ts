import axios, { AxiosResponse } from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "./client";
import { downloadReport, getReportData } from "./reports";

vi.mock("../constants/auth", () => ({ devAuthenticated: false, devSession: undefined }));

function stubBlobResponse(blob: Blob, headers: Record<string, string> = {}) {
  apiClient.defaults.adapter = vi.fn().mockImplementation((config) =>
    Promise.resolve({
      data: blob,
      status: 200,
      statusText: "OK",
      headers,
      config,
    } satisfies AxiosResponse),
  );
}

describe("getReportData", () => {
  afterEach(() => {
    apiClient.defaults.adapter = axios.defaults.adapter;
  });

  it("GETs the scan's report data endpoint", async () => {
    let seenUrl: string | undefined;
    apiClient.defaults.adapter = vi.fn().mockImplementation((config) => {
      seenUrl = config.url;
      return Promise.resolve({
        data: { scanId: 42 },
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      } satisfies AxiosResponse);
    });

    const data = await getReportData("42");

    expect(seenUrl).toBe("/api/v1/scans/42/report/data");
    expect(data).toEqual({ scanId: 42 });
  });
});

describe("downloadReport", () => {
  const createObjectURL = vi.fn(() => "blob:mock-url");
  const revokeObjectURL = vi.fn();
  const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});

  beforeEach(() => {
    // jsdom doesn't implement the Blob URL APIs; downloadReport needs both.
    URL.createObjectURL = createObjectURL;
    URL.revokeObjectURL = revokeObjectURL;
  });

  afterEach(() => {
    apiClient.defaults.adapter = axios.defaults.adapter;
    createObjectURL.mockClear();
    revokeObjectURL.mockClear();
    clickSpy.mockClear();
  });

  it.each([
    ["summary-html" as const, "summary.html"],
    ["summary-pdf" as const, "summary.pdf"],
    ["full-pdf" as const, "full.pdf"],
  ])("requests the %s report as a blob from the correct path", async (kind, path) => {
    let seenUrl: string | undefined;
    let seenResponseType: string | undefined;
    apiClient.defaults.adapter = vi.fn().mockImplementation((config) => {
      seenUrl = config.url;
      seenResponseType = config.responseType;
      return Promise.resolve({
        data: new Blob(["content"]),
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      } satisfies AxiosResponse);
    });

    await downloadReport("42", kind);

    expect(seenUrl).toBe(`/api/v1/scans/42/report/${path}`);
    expect(seenResponseType).toBe("blob");
  });

  it("names the download from the Content-Disposition header when present", async () => {
    stubBlobResponse(new Blob(["content"]), {
      "content-disposition": 'attachment; filename="my-report.pdf"',
    });

    await downloadReport("42", "full-pdf");

    expect(clickSpy).toHaveBeenCalledTimes(1);
    const anchor = clickSpy.mock.instances[0] as HTMLAnchorElement;
    expect(anchor.download).toBe("my-report.pdf");
  });

  it("falls back to a generated filename when there is no Content-Disposition header", async () => {
    stubBlobResponse(new Blob(["content"]));

    await downloadReport("42", "summary-pdf");

    const anchor = clickSpy.mock.instances[0] as HTMLAnchorElement;
    expect(anchor.download).toBe("vibeshield-scan-42-summary.pdf");
  });

  it("revokes the object URL after triggering the download", async () => {
    stubBlobResponse(new Blob(["content"]));

    await downloadReport("42", "summary-html");

    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:mock-url");
  });

  it("does not leave the temporary anchor element in the document", async () => {
    stubBlobResponse(new Blob(["content"]));

    await downloadReport("42", "summary-html");

    const anchor = clickSpy.mock.instances[0] as HTMLAnchorElement;
    expect(document.body.contains(anchor)).toBe(false);
  });
});
