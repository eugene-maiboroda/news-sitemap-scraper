package com.article.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ParallelExecutor {

    private final ExecutorService executor;

    public <T> void execute(List<T> items, Consumer<T> consumer) {
        try {
            List<Future<?>> futures = new ArrayList<>(items.size());
            for (T item : items) {
                futures.add(executor.submit(() -> consumer.accept(item)));
            }
            for (Future<?> f : futures) {
                f.get();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Parallel execution was interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Parallel task failed", e.getCause());
        }
    }

    public <T, R> List<R> map(List<T> items, Function<T, R> function) {
        try {
            List<Future<R>> futures = new ArrayList<>();
            for (T item : items) {
                futures.add(executor.submit(() -> function.apply(item)));
            }
            List<R> results = new ArrayList<>(items.size());
            for (Future<R> f : futures) {
                results.add(f.get());
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Parallel mapping was interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Parallel mapping task failed", e.getCause());
        }
    }
}
