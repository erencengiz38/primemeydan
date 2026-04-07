package com.meydan.meydan.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class XssSanitizer {

    private static final Safelist BASIC_SAFELIST = Safelist.basic();

    /**
     * Temel XSS koruması - HTML taglarını tamamen kaldırır
     * Sadece plain text bırakır
     */
    public String sanitizeBasic(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // HTML taglarını temizle, sadece plain text bırak
        return Jsoup.clean(input, Safelist.none());
    }

    /**
     * Temel HTML taglarına izin veren sanitization
     * <b>, <i>, <u> gibi basit format tagları korunur
     */
    public String sanitizeBasicHtml(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        return Jsoup.clean(input, BASIC_SAFELIST);
    }

    /**
     * Link ve temel format taglarına izin veren sanitization
     */
    public String sanitizeWithLinks(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        Safelist safelist = Safelist.basicWithImages()
                .addTags("a")
                .addAttributes("a", "href", "target", "rel")
                .addProtocols("a", "href", "http", "https", "mailto");

        return Jsoup.clean(input, safelist);
    }

    /**
     * XSS tehlikesi içerip içermediğini kontrol eder
     */
    public boolean containsXss(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String cleaned = Jsoup.clean(input, Safelist.none());
        return !cleaned.equals(input);
    }

    /**
     * String'in uzunluğunu sınırlandırır ve XSS temizler
     */
    public String sanitizeAndLimit(String input, int maxLength) {
        if (input == null) {
            return null;
        }

        String sanitized = sanitizeBasic(input);

        if (sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength);
        }

        return sanitized;
    }
}

