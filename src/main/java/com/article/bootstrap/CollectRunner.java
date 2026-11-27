package com.article.bootstrap;

import com.article.port.in.ArticlePipelineUseCase;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnNotWebApplication
@RequiredArgsConstructor
public class CollectRunner implements CommandLineRunner {

    private final ArticlePipelineUseCase collectionUseCase;

    @Override
    public void run(String... args) {
        collectionUseCase.run();
    }
}
