package com.article.adapter.pipeline;

import com.article.application.PipelineRunSettings.Snapshot;
import com.article.bootstrap.properties.BrowserProps;
import com.article.bootstrap.properties.CollectorProps;
import com.article.bootstrap.properties.ExportMode;
import com.article.bootstrap.properties.ExportProps;
import com.article.bootstrap.properties.StorageProps;
import com.article.bootstrap.properties.VideoProps;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class PipelineRequests {

    private PipelineRequests() {
    }

    public record DiscoveryRequest(

            Boolean enabled,

            String collectionBatchId,

            List<String> sitemapUrls,

            Integer batchSize,

            Long sitemapDelayMs
    ) {

        public int batchSize(int defaultValue) {
            return positiveInt(batchSize, defaultValue, "batchSize");
        }

        public Snapshot settings(Snapshot defaults) {
            String batchId = requiredText(collectionBatchId, "collectionBatchId");
            List<String> urls = requiredSitemapUrls(sitemapUrls);
            long delay = sitemapDelayMs == null ? defaults.collector().sitemapDelayMs() : sitemapDelayMs;
            if (delay < 0) {
                throw new IllegalArgumentException("sitemapDelayMs must not be negative");
            }

            CollectorProps collector = new CollectorProps(defaults.collector().parseThreads(), delay, urls, batchId);
            return defaults.toBuilder()
                    .collector(collector)
                    .build();
        }
    }

    public record ParsingRequest(

            Boolean enabled,

            Integer batchSize,

            Integer parseThreads,

            Boolean extractConnatixVideos,

            Boolean browserEnabled,

            Boolean browserHeadless,

            Integer browserTimeoutMs
    ) {

        public int batchSize(int defaultValue) {
            return positiveInt(batchSize, defaultValue, "batchSize");
        }

        public Snapshot settings(Snapshot defaults) {
            CollectorProps collector = collectorWithThreads(defaults.collector(), parseThreads);
            BrowserProps originalBrowser = defaults.browser();
            boolean enabledBrowser = browserEnabled == null ? originalBrowser.enabled() : browserEnabled;
            boolean headless = browserHeadless == null ? originalBrowser.headless() : browserHeadless;
            int timeout = positiveInt(browserTimeoutMs, originalBrowser.timeoutMs(), "browserTimeoutMs");
            boolean connatix = extractConnatixVideos == null
                    ? defaults.extractConnatixVideos() : extractConnatixVideos;
            if (connatix && !enabledBrowser) {
                throw new IllegalArgumentException("extractConnatixVideos requires browserEnabled=true");
            }

            return defaults.toBuilder()
                    .collector(collector)
                    .browser(new BrowserProps(enabledBrowser, headless, timeout))
                    .extractConnatixVideos(connatix)
                    .build();
        }
    }

    public record DownloadImagesRequest(

            Boolean enabled,

            String imagesDir,

            Integer batchSize,

            Integer parseThreads
    ) {

        public int batchSize(int defaultValue) {
            return positiveInt(batchSize, defaultValue, "batchSize");
        }

        public Snapshot settings(Snapshot defaults) {
            String directory = requiredText(imagesDir, "imagesDir");
            StorageProps storage = defaults.storage();

            return defaults.toBuilder()
                    .collector(collectorWithThreads(defaults.collector(), parseThreads))
                    .storage(new StorageProps(storage.exportDir(), directory, storage.videosDir(), storage.avatarsDir()))
                    .build();
        }
    }

    public record DownloadVideosRequest(

            Boolean enabled,

            String videosDir,

            Integer batchSize,

            Integer parseThreads,

            Integer timeoutSeconds
    ) {

        public int batchSize(int defaultValue) {
            return positiveInt(batchSize, defaultValue, "batchSize");
        }

        public Snapshot settings(Snapshot defaults) {
            String directory = requiredText(videosDir, "videosDir");
            StorageProps storage = defaults.storage();
            VideoProps video = defaults.video();
            int timeout = positiveInt(timeoutSeconds, video.timeoutMs(), "timeoutSeconds");

            return defaults.toBuilder()
                    .collector(collectorWithThreads(defaults.collector(), parseThreads))
                    .storage(new StorageProps(storage.exportDir(), storage.imagesDir(), directory, storage.avatarsDir()))
                    .video(new VideoProps(timeout, video.ytDlpCommand()))
                    .build();
        }
    }

    public record ProcessAuthorsRequest(

            Boolean enabled,

            String avatarsDir,

            Integer batchSize,

            Integer parseThreads
    ) {

        public int batchSize(int defaultValue) {
            return positiveInt(batchSize, defaultValue, "batchSize");
        }

        public Snapshot settings(Snapshot defaults) {
            String directory = requiredText(avatarsDir, "avatarsDir");
            StorageProps storage = defaults.storage();

            return defaults.toBuilder()
                    .collector(collectorWithThreads(defaults.collector(), parseThreads))
                    .storage(new StorageProps(storage.exportDir(), storage.imagesDir(), storage.videosDir(), directory))
                    .build();
        }
    }

    public record ExportAuthorsRequest(

            Boolean enabled,

            String exportDir
    ) {

        public Snapshot settings(Snapshot defaults) {
            String directory = requiredText(exportDir, "exportDir");
            StorageProps storage = defaults.storage();

            return defaults.toBuilder()
                    .storage(new StorageProps(directory, storage.imagesDir(), storage.videosDir(), storage.avatarsDir()))
                    .build();
        }
    }

    public record ExportJsonRequest(

            Boolean enabled,

            String exportDir,

            Integer batchSize,

            String collectionBatchId,

            ExportMode mode
    ) {

        public int batchSize(int defaultValue) {
            return positiveInt(batchSize, defaultValue, "batchSize");
        }

        public Snapshot settings(Snapshot defaults) {
            String directory = requiredText(exportDir, "exportDir");
            StorageProps storage = defaults.storage();
            ExportProps export = defaults.export();
            ExportMode resolvedMode = mode == null ? export.mode() : mode;
            String batchId = collectionBatchId == null ? export.collectionBatchId() : collectionBatchId;

            return defaults.toBuilder()
                    .storage(new StorageProps(directory, storage.imagesDir(), storage.videosDir(), storage.avatarsDir()))
                    .export(new ExportProps(resolvedMode, batchId))
                    .build();
        }
    }

    private static int positiveInt(Integer value, int defaultValue, String field) {
        int resolved = value == null ? defaultValue : value;
        if (resolved <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return resolved;
    }

    private static CollectorProps collectorWithThreads(CollectorProps collector, Integer parseThreads) {
        int threads = positiveInt(parseThreads, collector.parseThreads(), "parseThreads");
        return new CollectorProps(threads, collector.sitemapDelayMs(), collector.sitemapUrls(), collector.collectionBatchId());
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static List<String> requiredSitemapUrls(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("sitemapUrls is required");
        }

        List<String> urls = new ArrayList<>(values.size());
        for (String value : values) {
            String url = requiredText(value, "sitemapUrls");
            URI uri = URI.create(url);
            boolean http = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            if (!http || uri.getHost() == null) {
                throw new IllegalArgumentException("sitemapUrls must contain HTTP(S) URLs");
            }
            urls.add(url);
        }
        return urls;
    }
}
