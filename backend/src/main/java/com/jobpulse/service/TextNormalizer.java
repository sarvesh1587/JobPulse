package com.jobpulse.service;

import java.util.Locale;

/** Lower-cased, trimmed, single-spaced text used for dedup keys and case-insensitive search columns. */
public final class TextNormalizer {

    private TextNormalizer() {}

    public static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /** Trims and turns blank strings into null, so "" is never stored. */
    public static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
