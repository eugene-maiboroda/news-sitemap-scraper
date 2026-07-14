package com.article.adapter.video;

import com.article.adapter.shared.ParseUtils;
import com.article.bootstrap.CollectorProps;
import com.article.domain.ArticleVideoRef;
import com.article.domain.DownloadResult;
import com.article.domain.status.VideoPlatform;
import com.article.port.VideoDownloadPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Downloads video files via the external {@code yt-dlp} binary (a Java {@link ProcessBuilder}
 * invocation, mirroring how the Python reference script shells out to the same tool) — no
 * Playwright, no HTTP scraping, all inputs already come from the {@code videos} table. Supports
 * only JW Player ({@link ArticleVideoRef#m3u8Url()}) and YouTube ({@link ArticleVideoRef#youtubeVideoId()});
 * Connatix is out of scope and never resolves to a URL here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoDownloadAdapter implements VideoDownloadPort {

    private final CollectorProps collectorProps;

    @Override
    public DownloadResult download(ArticleVideoRef ref) {
        String url = resolveUrl(ref);
        if (url == null) {
            log.warn("[VIDEO][SKIP] Missing source data id={} platform={}", ref.id(), ref.platform());
            return DownloadResult.failed();
        }

        try {
            Path dir = Path.of(collectorProps.videosDir(), ParseUtils.categorySlug(ref.category()));
            String prefix = ParseUtils.videoFilePrefix(
                    ref.category(), ref.articleId(), ref.platform().name(), ParseUtils.shortId(ref.id()));
            Path target = dir.resolve(prefix + ".mp4");

            if (isValidFile(target)) {
                log.debug("[VIDEO][SKIP] {}", target);
                return DownloadResult.downloaded(target.toString(), Files.size(target));
            }

            Files.createDirectories(dir);

            List<String> command = List.of(
                    collectorProps.ytDlpCommand(),
                    url,
                    "-o", dir.resolve(prefix + ".%(ext)s").toString(),
                    "--no-playlist",
                    "--merge-output-format", "mp4");

            log.info(ref.platform() == VideoPlatform.YOUTUBE ? "Downloading YouTube:\n{}" : "Downloading JW video:\n{}", url);
            log.info("Executing:\n{}", String.join(" ", command));

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            closeQuietly(process.getOutputStream());
            drainOutput(process);

            int timeoutSeconds = collectorProps.videoDownloadTimeoutSeconds();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("yt-dlp timed out after {} seconds", timeoutSeconds);
                return DownloadResult.failed();
            }

            log.info("yt-dlp finished with exit code: {}", process.exitValue());
            if (process.exitValue() == 0 && isValidFile(target)) {
                return DownloadResult.downloaded(target.toString(), Files.size(target));
            }
            return DownloadResult.failed();
        } catch (Exception e) {
            log.warn("[VIDEO][FAIL] id={} url={}", ref.articleId(), url, e);
            return DownloadResult.failed();
        }
    }

    private String resolveUrl(ArticleVideoRef ref) {
        return switch (ref.platform()) {
            case JW_TOP -> StringUtils.hasText(ref.m3u8Url()) ? ref.m3u8Url() : null;
            case YOUTUBE -> StringUtils.hasText(ref.youtubeVideoId())
                    ? "https://www.youtube.com/watch?v=" + ref.youtubeVideoId()
                    : null;
            case CONNATIX_BOTTOM -> null;
        };
    }

    private boolean isValidFile(Path path) throws IOException {
        return Files.exists(path) && Files.isRegularFile(path) && Files.size(path) > 0;
    }

    /** We never write to the child process's stdin — close it immediately so a pipe we're not
     *  using can't be mistaken by yt-dlp/ffmpeg for "more input coming" and left open forever. */
    private void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            log.debug("[yt-dlp] failed to close process stdin", e);
        }
    }

    /** Must run concurrently with {@link Process#waitFor}, not after — an unread stdout/stderr
     *  buffer can fill up and deadlock the process before it ever exits. */
    private void drainOutput(Process process) {
        Thread drain = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[yt-dlp] {}", line);
                }
            } catch (IOException e) {
                log.debug("[yt-dlp] output stream closed", e);
            }
        }, "yt-dlp-output-drain");
        drain.setDaemon(true);
        drain.start();
    }
}
