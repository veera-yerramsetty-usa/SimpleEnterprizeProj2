package org.sample.simpleenterprizeproj2.util;

public final class SanitizationUtils {

    private SanitizationUtils() {}

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]*>", "").trim();
    }

    public static String escapeWildcards(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("%", "\\%").replace("_", "\\_");
    }
}
