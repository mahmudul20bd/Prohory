package com.cyberaware.backend.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlExtractor {

    // অ্যাডভান্সড রেগুলার এক্সপ্রেশন যা যেকোনো লিংক বা লুকানো আইপি অ্যাড্রেস ধরতে পারে
    private static final String URL_REGEX = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))";

    public static List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return urls;
        }

        Pattern pattern = Pattern.compile(URL_REGEX);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String url = matcher.group();
            // স্ক্যান করার সুবিধার জন্য http:// যুক্ত না থাকলে তা বসিয়ে দেওয়া
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            urls.add(url);
        }
        return urls;
    }
}