package com.article.adapter.http;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HttpHeadersProvider {
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/138.0.0.0 Safari/537.36";

    private static final Map<String, String> DEFAULT_HEADERS = Map.of(
            "User-Agent", USER_AGENT,
            "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language", "en-US,en;q=0.9"
    );

    public Map<String, String> defaultHeaders() {
        return DEFAULT_HEADERS;
    }
}
