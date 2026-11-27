package com.article.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ParallelExecutorTest {

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final ParallelExecutor parallelExecutor = new ParallelExecutor(executorService, mock(PipelineRunSettings.class));

    @AfterEach
    void shutdownExecutor() {
        executorService.shutdownNow();
    }

    @Test
    void mapsAllItemsInInputOrder() {
        assertEquals(
                List.of(2, 4, 6),
                parallelExecutor.map(List.of(1, 2, 3), value -> value * 2)
        );
    }

    @Test
    void propagatesTaskFailure() {
        assertThrows(
                IllegalStateException.class,
                () -> parallelExecutor.execute(List.of("article"), ignored -> {
                    throw new IllegalArgumentException("parse failed");
                })
        );
    }
}
