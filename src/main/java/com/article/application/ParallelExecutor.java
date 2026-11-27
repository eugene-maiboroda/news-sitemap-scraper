package com.article.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ParallelExecutor {

    private final ExecutorService executor;
    private final PipelineRunSettings runSettings;

    private volatile ExecutorService runExecutor;

    public synchronized void beginRun(int threads) {
        if (runExecutor != null) {
            throw new IllegalStateException("Run executor is already active");
        }
        runExecutor = Executors.newFixedThreadPool(threads);
    }

    public synchronized void endRun() {
        if (runExecutor == null) {
            return;
        }

        runExecutor.shutdown();
        boolean interrupted = false;
        boolean terminated = false;
        while (!terminated) {
            try {
                terminated = runExecutor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        runExecutor = null;

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public <T> void execute(List<T> items, Consumer<T> consumer) {
        map(items, item -> {
            consumer.accept(item);
            return Boolean.TRUE;
        });
    }

    public <T, R> List<R> map(List<T> items, Function<T, R> function) {
        ExecutorService activeExecutor = runExecutor;
        if (activeExecutor == null) {
            activeExecutor = executor;
        }

        List<Future<Optional<R>>> futures = new ArrayList<>(items.size());
        Throwable failure = null;
        try {
            for (T item : items) {
                if (runSettings.isStopRequested()) {
                    break;
                }
                futures.add(activeExecutor.submit(() -> {
                    if (runSettings.isStopRequested()) {
                        return Optional.empty();
                    }
                    return Optional.of(function.apply(item));
                }));
            }
        } catch (RuntimeException | Error e) {
            failure = e;
        }

        return awaitResults(futures, failure);
    }

    private <R> List<R> awaitResults(List<Future<Optional<R>>> futures, Throwable failure) {
        List<R> results = new ArrayList<>(futures.size());
        boolean interrupted = false;

        for (Future<Optional<R>> future : futures) {
            boolean finished = false;
            while (!finished) {
                try {
                    future.get().ifPresent(results::add);
                    finished = true;
                } catch (InterruptedException e) {
                    interrupted = true;
                    failure = collectFailure(failure, e);
                } catch (ExecutionException e) {
                    failure = collectFailure(failure, e.getCause());
                    finished = true;
                } catch (RuntimeException e) {
                    failure = collectFailure(failure, e);
                    finished = true;
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        if (failure != null) {
            throw new IllegalStateException("Parallel task failed", failure);
        }
        return results;
    }

    private Throwable collectFailure(Throwable failure, Throwable nextFailure) {
        if (failure == null) {
            return nextFailure;
        }
        if (failure != nextFailure) {
            failure.addSuppressed(nextFailure);
        }
        return failure;
    }
}
