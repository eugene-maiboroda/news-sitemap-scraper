package com.article.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
@Component
public class ParallelExecutor {

    public <T> void execute(List<T> items, Consumer<T> consumer, int threads) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (T item : items) {
                futures.add(executor.submit(() -> consumer.accept(item)));
            }
            for (Future<?> f : futures) {
                f.get();
            }

        } catch (InterruptedException e) {
            log.warn("Parsing interrupted");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Unexpected execution error", e);
        } finally {
            shutdownExecutor(executor);
            try {
                if (!executor.awaitTermination(2, TimeUnit.HOURS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                log.warn("Forcing executor shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

