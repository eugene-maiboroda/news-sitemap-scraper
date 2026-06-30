package com.article.adapter.http;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class HttpContentLoader {

    private final RestClient restClient;

    public String loadText(String url) {
        return execute(() -> restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class), url);
    }

    public Document loadDocument(String url) {
        String html = execute(() -> restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class), url);

        return Jsoup.parse(html, url);
    }

    public byte[] download(String url) {
        return execute(() -> restClient.get()
                .uri(url)
                .retrieve()
                .body(byte[].class), url);
    }

    private <T> T execute(Supplier<T> supplier, String url) {
        try {
            return supplier.get();
        } catch (ResourceAccessException e) {
            throw new RuntimeException("Failed to connect: " + url, e);

        } catch (RestClientException e) {
            throw new RuntimeException("Failed to execute request: " + url, e);
        }
    }
}
