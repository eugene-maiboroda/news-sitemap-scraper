package com.article.adapter.author;

import com.article.adapter.http.HttpContentLoader;
import com.article.adapter.shared.ParseUtils;
import com.article.bootstrap.CollectorProps;
import com.article.domain.Author;
import com.article.domain.DownloadResult;
import com.article.port.AuthorAvatarDownloadPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Same HTTP-GET-and-write approach as ImageDownloadAdapter, self-contained rather than sharing an
 * engine with it — VideoDownloadAdapter already shows this project keeps one adapter per media
 * type instead of forcing a shared "downloader" abstraction. The file name is built with
 * ParseUtils.avatarFilePrefix(author.id()), the same method AuthorExportMapper already uses for
 * the export JSON's avatarId, so a downloaded file's name always matches what was exported.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorAvatarDownloadAdapter implements AuthorAvatarDownloadPort {

    private final HttpContentLoader contentLoader;
    private final CollectorProps collectorProps;

    @Override
    public DownloadResult download(Author author) {
        try {
            Path dir = Path.of(collectorProps.avatarsDir());
            String fileName = ParseUtils.avatarFilePrefix(author.id()) + ParseUtils.resolveExtension(author.avatarUrl());
            Path target = dir.resolve(fileName);

            if (Files.exists(target)) {
                log.debug("[AVATAR][SKIP] {}", fileName);
                return DownloadResult.downloaded(target.toString(), Files.size(target));
            }

            Files.createDirectories(dir);
            byte[] bytes = contentLoader.download(author.avatarUrl());

            if (bytes == null || bytes.length == 0) {
                log.warn("[AVATAR][EMPTY] authorId={} url={}", author.id(), author.avatarUrl());
                return DownloadResult.failed();
            }

            Files.write(target, bytes);
            log.info("[AVATAR][OK] {} ({} bytes)", fileName, bytes.length);
            return DownloadResult.downloaded(target.toString(), bytes.length);
        } catch (Exception e) {
            log.warn("[AVATAR][FAIL] authorId={} url={}", author.id(), author.avatarUrl(), e);
            return DownloadResult.failed();
        }
    }
}
