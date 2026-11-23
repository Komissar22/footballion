package com.footballion.footballion.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class TextNormalizer {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");

    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String lower = input.trim().toLowerCase(Locale.ROOT)
                .replace("ø", "o")
                .replace("œ", "oe")
                .replace("æ", "ae")
                .replace("ß", "ss")
                .replace("ł", "l")
                .replace("đ", "d")
                .replace("ð", "d")
                .replace("þ", "th")
                .replace("ı", "i");

        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(decomposed).replaceAll("");
    }
}
