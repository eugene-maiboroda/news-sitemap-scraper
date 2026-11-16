package com.article.adapter.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpContentLoader {

    private final RestClient restClient;
    private final HttpHeadersProvider httpHeadersProvider;

    @Retryable(includes = HttpClientErrorException.TooManyRequests.class, multiplier = 2)
    public String loadText(String url) {
        return get(url, String.class);
    }

    @Retryable(includes = HttpClientErrorException.TooManyRequests.class, multiplier = 2)
    public byte[] download(String url) {
        return get(url, byte[].class);
    }

    private <T> T get(String url, Class<T> bodyType) {
        RestClient.RequestHeadersSpec<?> request = restClient.get().uri(url);
        httpHeadersProvider.defaultHeaders().forEach(request::header);

        ResponseEntity<T> response = execute(() -> request
                .retrieve()
                .toEntity(bodyType), url);
        log.info("{} -> {}", url, response.getStatusCode());
        return response.getBody();
    }

    private <T> T execute(Supplier<T> supplier, String url) {
        try {
            return supplier.get();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new RuntimeException("Failed to connect: " + url, e);

        } catch (RestClientException e) {
            throw new RuntimeException("Failed to execute request: " + url, e);
        }
    }
}
