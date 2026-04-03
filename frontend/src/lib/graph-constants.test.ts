import { describe, expect, it } from "vitest";
import {
  PRIORITY_COLORS,
  SERIES_COLORS,
  STATUS_COLORS,
  WAVE_COLORS,
  getColorMap,
  getNodeColor,
  getSeries,
} from "./graph-constants";

describe("getSeries", () => {
  it("extracts series letter from a valid UID", () => {
    expect(getSeries("GC-A001")).toBe("A");
    expect(getSeries("GC-Q005")).toBe("Q");
    expect(getSeries("GC-Z999")).toBe("Z");
  });

  it("returns '?' for non-matching UIDs", () => {
    expect(getSeries("bad")).toBe("?");
    expect(getSeries("")).toBe("?");
    expect(getSeries("REQ-001")).toBe("?");
  });
});

describe("getNodeColor", () => {
  const req = { uid: "GC-A001", priority: "MUST", status: "ACTIVE", wave: 2 };

  it("returns series color by default", () => {
    expect(getNodeColor(req, "series")).toBe(SERIES_COLORS.A);
  });

  it("returns priority color", () => {
    expect(getNodeColor(req, "priority")).toBe(PRIORITY_COLORS.MUST);
  });

  it("returns status color", () => {
    expect(getNodeColor(req, "status")).toBe(STATUS_COLORS.ACTIVE);
  });

  it("returns wave color", () => {
    expect(getNodeColor(req, "wave")).toBe(WAVE_COLORS["2"]);
  });

  it("returns fallback for unknown values", () => {
    const unknown = {
      entityType: "REQUIREMENT",
      uid: "GC-A001",
      priority: "NOPE",
      status: "ACTIVE",
      wave: 0,
    };
    expect(getNodeColor(unknown, "priority")).toBe("#6c7ee1");
  });
});

describe("getColorMap", () => {
  it("returns priority colors for priority scheme", () => {
    expect(getColorMap("priority")).toBe(PRIORITY_COLORS);
  });

  it("returns status colors for status scheme", () => {
    expect(getColorMap("status")).toBe(STATUS_COLORS);
  });

  it("returns series colors for series scheme", () => {
    expect(getColorMap("series")).toBe(SERIES_COLORS);
  });

  it("returns wave-prefixed keys for wave scheme", () => {
    const map = getColorMap("wave");
    expect(map["Wave 1"]).toBe(WAVE_COLORS["1"]);
    expect(map["Wave 5"]).toBe(WAVE_COLORS["5"]);
    expect(Object.keys(map).every((k) => k.startsWith("Wave "))).toBe(true);
  });
});
