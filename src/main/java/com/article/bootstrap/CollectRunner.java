package com.article.bootstrap;

import com.article.port.in.ArticlePipelineUseCase;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!server")
@RequiredArgsConstructor
public class CollectRunner implements CommandLineRunner {

    private final ArticlePipelineUseCase collectionUseCase;

    @Override
    public void run(String... args) {
        collectionUseCase.run();
    }
}
