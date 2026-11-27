package com.article.application;

import com.article.bootstrap.properties.BrowserProps;
import com.article.bootstrap.properties.CollectorProps;
import com.article.bootstrap.properties.ExportProps;
import com.article.bootstrap.properties.StepsProps;
import com.article.bootstrap.properties.StorageProps;
import com.article.bootstrap.properties.VideoProps;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@Getter
public class PipelineRunSettings {

    private final Snapshot defaults;

    private volatile Snapshot active;
    private volatile boolean stopRequested;

    public PipelineRunSettings(
            CollectorProps collector,
            StorageProps storage,
            ExportProps export,
            BrowserProps browser,
            VideoProps video,
            StepsProps steps
    ) {
        boolean extractConnatixVideos = steps.extractConnatixVideos() != null
                && steps.extractConnatixVideos().enabled();
        defaults = new Snapshot(collector, storage, export, browser, video, extractConnatixVideos);
    }

    public Snapshot defaults() {
        return defaults;
    }

    public Snapshot current() {
        Snapshot snapshot = active;
        return snapshot == null ? defaults : snapshot;
    }

    public synchronized void activate(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "Run settings must not be null");
        if (active != null) {
            throw new IllegalStateException("Run settings are already active");
        }

        stopRequested = false;
        active = snapshot;
    }

    public synchronized void clear() {
        active = null;
        stopRequested = false;
    }

    public synchronized void requestStop() {
        if (active != null) {
            stopRequested = true;
        }
    }

    @Builder(toBuilder = true)
    public record Snapshot(

            CollectorProps collector,

            StorageProps storage,

            ExportProps export,

            BrowserProps browser,

            VideoProps video,

            boolean extractConnatixVideos
    ) {

        public Snapshot {
            Objects.requireNonNull(collector, "Collector settings must not be null");
            Objects.requireNonNull(storage, "Storage settings must not be null");
            Objects.requireNonNull(export, "Export settings must not be null");
            Objects.requireNonNull(browser, "Browser settings must not be null");
            Objects.requireNonNull(video, "Video settings must not be null");

            List<String> sitemapUrls = collector.sitemapUrls();
            if (sitemapUrls != null) {
                collector = new CollectorProps(
                        collector.parseThreads(),
                        collector.sitemapDelayMs(),
                        List.copyOf(sitemapUrls),
                        collector.collectionBatchId()
                );
            }
        }
    }
}
