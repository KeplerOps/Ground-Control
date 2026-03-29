package com.keplerops.groundcontrol.domain.requirements.service;

/** Shared CSV formatting utilities with formula-injection protection. */
public final class CsvUtils {

    private CsvUtils() {}

    /**
     * Escapes a value for safe inclusion in a CSV cell.
     *
     * <p>Guards against CSV formula injection by prefixing dangerous leading characters
     * ({@code = + - @ TAB CR}) with a single quote. Values containing commas, quotes,
     * or newlines are enclosed in double-quotes with internal quotes doubled.
     */
    public static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String safe = value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
