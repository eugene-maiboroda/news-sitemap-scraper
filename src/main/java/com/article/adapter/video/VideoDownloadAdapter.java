package com.article.adapter.video;

import com.article.adapter.shared.FileNaming;
import com.article.application.PipelineRunSettings;
import com.article.application.dto.DownloadResult;
import com.article.application.dto.VideoDownloadData;
import com.article.bootstrap.properties.VideoProps;
import com.article.domain.media.VideoPlatform;
import com.article.port.out.VideoDownloadPort;
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
 * only JW Player ({@link VideoDownloadData#m3u8Url()}) and YouTube ({@link VideoDownloadData#youtubeVideoId()});
 * Connatix is out of scope and never resolves to a URL here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoDownloadAdapter implements VideoDownloadPort {

    private final PipelineRunSettings runSettings;
    private final VideoProps videoProps;

    @Override
    public DownloadResult download(VideoDownloadData data) {
        String url = resolveUrl(data);
        if (url == null) {
            log.warn("[VIDEO][SKIP] Missing source data id={} platform={}", data.id(), data.platform());
            return DownloadResult.failed();
        }

        try {
            Path dir = Path.of(runSettings.current().storage().videosDir(), FileNaming.categorySlug(data.category()));
            String prefix = FileNaming.videoFilePrefix(
                    data.category(),
                    data.articleId(),
                    data.platform().name(),
                    FileNaming.shortId(data.id())
            );
            Path target = dir.resolve(prefix + ".mp4");

            if (isValidFile(target)) {
                log.debug("[VIDEO][SKIP] {}", target);
                return DownloadResult.downloaded(target.toString(), Files.size(target));
            }

            Files.createDirectories(dir);

            List<String> processCommand = List.of(
                    videoProps.ytDlpCommand(),
                    url,
                    "-o", dir.resolve(prefix + ".%(ext)s").toString(),
                    "--no-playlist",
                    "--merge-output-format", "mp4");

            log.info(data.platform() == VideoPlatform.YOUTUBE
                    ? "Downloading YouTube:\n{}"
                    : "Downloading JW video:\n{}", url);
            log.info("Executing:\n{}", String.join(" ", processCommand));

            Process process = new ProcessBuilder(processCommand)
                    .redirectErrorStream(true)
                    .start();
            closeQuietly(process.getOutputStream());
            drainOutput(process);

            int timeoutSeconds = runSettings.current().video().timeoutMs();
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
            log.warn("[VIDEO][FAIL] id={} url={}", data.articleId(), url, e);
            return DownloadResult.failed();
        }
    }

    private String resolveUrl(VideoDownloadData data) {
        return switch (data.platform()) {
            case JW_TOP -> StringUtils.hasText(data.m3u8Url()) ? data.m3u8Url() : null;
            case YOUTUBE -> StringUtils.hasText(data.youtubeVideoId())
                    ? "https://www.youtube.com/watch?v=" + data.youtubeVideoId()
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
