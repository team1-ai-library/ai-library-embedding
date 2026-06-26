package com.nhnacadmey.book_embeddings.util;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class TextPreprocessor {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[^가-힣a-zA-Z0-9\\s]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    public static String preprocess(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        String decoded = text
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");

        String cleaned = HTML_TAG.matcher(decoded).replaceAll(" ");
        cleaned = SPECIAL_CHAR.matcher(cleaned).replaceAll("");
        cleaned = MULTI_SPACE.matcher(cleaned).replaceAll(" ");

        return cleaned.trim().toLowerCase();
    }
}