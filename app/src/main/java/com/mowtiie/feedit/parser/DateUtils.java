package com.mowtiie.feedit.parser;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateUtils {

    private DateUtils() {
    }

    private static final Pattern TRAILING_UT = Pattern.compile("\\bUT\\b$");

    public static Long parse(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return null;
        }
        String value = rawDate.trim();

        Long result = parseRfc1123(value);
        if (result != null) {
            return result;
        }
        result = parseIso8601(value);
        if (result != null) {
            return result;
        }
        return parseLenientFallback(value);
    }

    private static Long parseRfc1123(String value) {
        try {
            Matcher matcher = TRAILING_UT.matcher(value);
            String normalized = matcher.find() ? matcher.replaceAll("UTC") : value;
            ZonedDateTime zdt = ZonedDateTime.parse(normalized, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zdt.toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Long parseIso8601(String value) {
        try {
            OffsetDateTime odt = OffsetDateTime.parse(value);
            return odt.toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Long parseLenientFallback(String value) {
        String[] patterns = {
                "EEE, d MMM yyyy HH:mm:ss z",
                "d MMM yyyy HH:mm:ss z",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern, Locale.US);
                sdf.setLenient(true);
                return sdf.parse(value).getTime();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static long nowMillis() {
        return Instant.now().toEpochMilli();
    }
}
