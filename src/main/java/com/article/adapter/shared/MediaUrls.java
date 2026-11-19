package com.article.adapter.shared;

public final class MediaUrls {

    private MediaUrls() {
    }

    public static String withoutQueryOrFragment(String url) {
        return url.split("[?#]", 2)[0];
    }
}
