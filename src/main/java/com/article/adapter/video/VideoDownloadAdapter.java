package com.article.adapter.video;

import com.article.adapter.shared.ParseUtils;
import com.article.bootstrap.properties.StorageProps;
import com.article.bootstrap.properties.VideoProps;
import com.article.domain.ArticleVideoRef;
import com.article.domain.DownloadResult;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoDownloadAdapter implements VideoDownloadPort {

    private final StorageProps storageProps;
    private final VideoProps videoProps;

    @Override
    public DownloadResult download(ArticleVideoRef ref) {
        String url = resolveUrl(ref);
        if (url == null) {
            log.warn("[VIDEO][SKIP] Missing HTTP(S) video URL id={}", ref.id());
            return DownloadResult.failed();
        }

        try {
            Path dir = Path.of(storageProps.videosDir(), ParseUtils.categorySlug(null));
            String prefix = ParseUtils.videoFilePrefix(
                    null, ref.articleId(), "video", ParseUtils.shortId(ref.id()));
            Path target = dir.resolve(prefix + ".mp4");

            if (isValidFile(target)) {
                log.debug("[VIDEO][SKIP] {}", target);
                return DownloadResult.downloaded(target.toString(), Files.size(target));
            }

            Files.createDirectories(dir);

            List<String> command = List.of(
                    videoProps.ytDlpCommand(),
                    url,
                    "-o", dir.resolve(prefix + ".%(ext)s").toString(),
                    "--no-playlist",
                    "--merge-output-format", "mp4");

            log.info("Downloading video {}", url);
            log.info("Executing:\n{}", String.join(" ", command));

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            closeQuietly(process.getOutputStream());
            drainOutput(process);

            int timeoutSeconds = videoProps.timeoutMs();
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
        String sourceUrl = ref.sourceUrl();
        if (sourceUrl != null && (sourceUrl.startsWith("https://") || sourceUrl.startsWith("http://"))) {
            return sourceUrl;
        }
        if (StringUtils.hasText(ref.youtubeVideoId())) {
            return "https://www.youtube.com/watch?v=" + ref.youtubeVideoId();
        }
        return null;
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
