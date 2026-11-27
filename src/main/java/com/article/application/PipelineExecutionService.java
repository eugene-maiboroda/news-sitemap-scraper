package com.article.application;

import com.article.application.PipelineRunSettings.Snapshot;
import com.article.application.dto.PipelineStatus;
import com.article.application.dto.PipelineStatus.Status;
import com.article.application.dto.PipelineStatus.Step;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineExecutionService {

    private static final String FAILURE_MESSAGE = "Pipeline step failed; see application logs for details";

    private final DiscoverLinksUseCase discoverLinksUseCase;
    private final ParseArticlesUseCase parseArticlesUseCase;
    private final DownloadImagesUseCase downloadImagesUseCase;
    private final DownloadVideosUseCase downloadVideosUseCase;
    private final AuthorUseCase authorUseCase;
    private final ExportArticlesUseCase exportArticlesUseCase;
    private final ParallelExecutor parallelExecutor;
    private final PipelineRunSettings runSettings;

    private final ExecutorService coordinator = Executors.newSingleThreadExecutor();

    private PipelineStatus currentStatus = PipelineStatus.idle();
    private boolean closed;

    public synchronized PipelineStatus status() {
        return currentStatus;
    }

    public synchronized PipelineStatus start(Step step, int batchSize, Snapshot settings) {
        if (isActive()) {
            throw new BusyException(currentStatus);
        }
        if (closed) {
            throw new IllegalStateException("Pipeline execution service is shutting down");
        }
        Objects.requireNonNull(step, "Step must not be null");
        if (step != Step.EXPORT_AUTHORS && batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be greater than zero");
        }

        runSettings.activate(settings);
        PipelineStatus accepted = PipelineStatus.running(step, Instant.now());
        currentStatus = accepted;

        try {
            coordinator.execute(() -> execute(step, batchSize, settings));
        } catch (RuntimeException | Error e) {
            finish(e);
            throw e;
        }
        return accepted;
    }

    public synchronized PipelineStatus stop() {
        if (currentStatus.status() == Status.RUNNING) {
            runSettings.requestStop();
            currentStatus = currentStatus.stopping();
        }
        return currentStatus;
    }

    @PreDestroy
    public void shutdown() {
        synchronized (this) {
            closed = true;
            stop();
            coordinator.shutdown();
        }

        boolean interrupted = false;
        boolean terminated = false;
        while (!terminated) {
            try {
                terminated = coordinator.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isActive() {
        return currentStatus.status() == Status.RUNNING || currentStatus.status() == Status.STOPPING;
    }

    private void execute(Step step, int batchSize, Snapshot settings) {
        Throwable failure = null;
        boolean executorStarted = false;
        try {
            if (!runSettings.isStopRequested()) {
                parallelExecutor.beginRun(settings.collector().parseThreads());
                executorStarted = true;
                if (!runSettings.isStopRequested()) {
                    executeStep(step, batchSize, settings);
                }
            }
        } catch (RuntimeException | Error e) {
            failure = e;
        }

        if (executorStarted) {
            try {
                parallelExecutor.endRun();
            } catch (RuntimeException | Error e) {
                if (failure == null) {
                    failure = e;
                } else if (failure != e) {
                    failure.addSuppressed(e);
                }
            }
        }
        finish(failure);
    }

    private void executeStep(Step step, int batchSize, Snapshot settings) {
        switch (step) {
            case DISCOVERY -> discoverLinksUseCase.discoverLinks(settings.collector().sitemapUrls(), batchSize);
            case PARSING -> parseArticlesUseCase.parsePages(batchSize);
            case DOWNLOAD_IMAGES -> downloadImagesUseCase.downloadImages(batchSize);
            case DOWNLOAD_VIDEOS -> downloadVideosUseCase.downloadVideos(batchSize);
            case PROCESS_AUTHORS -> authorUseCase.processAuthors(batchSize);
            case EXPORT_AUTHORS -> authorUseCase.exportAuthors();
            case EXPORT_JSON -> exportArticlesUseCase.exportArticles(batchSize);
        }
    }

    private synchronized void finish(Throwable failure) {
        boolean stopped = runSettings.isStopRequested();
        runSettings.clear();
        Instant finishedAt = Instant.now();

        if (failure != null) {
            log.error("Pipeline step {} failed", currentStatus.step(), failure);
            currentStatus = currentStatus.failed(finishedAt, FAILURE_MESSAGE);
        } else if (stopped) {
            currentStatus = currentStatus.stopped(finishedAt);
        } else {
            currentStatus = currentStatus.completed(finishedAt);
        }
    }

    @Getter
    public static class BusyException extends IllegalStateException {

        private final PipelineStatus status;

        public BusyException(PipelineStatus status) {
            super("Pipeline is already running");
            this.status = status;
        }
    }
}
