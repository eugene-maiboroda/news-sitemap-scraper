package com.article.bootstrap;

import com.article.application.ArticleCollectorService;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CollectRunner implements CommandLineRunner {

    private final ArticleCollectorService collectorService;

    @Override
    public void run(String... args) {
        collectorService.run();
    }
}
