package com.article.infrastructure;

import com.article.bootstrap.BrowserProps;
import com.article.bootstrap.CollectorProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    RestClient restClient() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService articleExecutor(CollectorProps props) {
        return Executors.newFixedThreadPool(props.parseThreads());
    }

    // Lazy: Playwright/Chromium must not start just because the app booted — only when a
    // Connatix extraction actually runs (collector.steps.extract-connatix-videos=true).
    // Most runs (discover-links, download-images, export-json, JW-only parsing) never touch this.
    @Bean(destroyMethod = "close")
    @Lazy
    public Playwright playwright() {
        return Playwright.create();
    }

    @Bean(destroyMethod = "close")
    @Lazy
    public Browser browser(Playwright playwright, BrowserProps props) {
        return playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(props.headless()));
    }
}
