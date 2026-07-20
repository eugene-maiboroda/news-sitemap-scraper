package com.article.adapter.author;

import com.article.adapter.http.HttpContentLoader;
import com.article.adapter.shared.FileNaming;
import com.article.bootstrap.properties.StorageProps;
import com.article.domain.author.Author;
import com.article.application.dto.DownloadResult;
import com.article.port.out.AuthorAvatarDownloadPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Same HTTP-GET-and-write approach as ImageDownloadAdapter, self-contained rather than sharing an
 * engine with it — VideoDownloadAdapter already shows this project keeps one adapter per media
 * type instead of forcing a shared "downloader" abstraction. The file name is built with
 * FileNaming.avatarFilePrefix(author.id()), the same method AuthorExportMapper already uses for
 * the export JSON's avatarId, so a downloaded file's name always matches what was exported.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorAvatarDownloadAdapter implements AuthorAvatarDownloadPort {

    private final HttpContentLoader contentLoader;
    private final StorageProps storageProps;

    @Override
    public DownloadResult download(Author author) {
        Path temporaryFile = null;
        try {
            Path dir = Path.of(storageProps.avatarsDir());
            String fileId = FileNaming.avatarFilePrefix(author.id());
            String fileName = fileId + FileNaming.resolveExtension(author.avatarUrl());
            Path target = dir.resolve(fileName);

            if (Files.exists(target)) {
                log.debug("[AVATAR][SKIP] {}", fileName);
                return DownloadResult.downloaded(target.toString(), Files.size(target), fileId);
            }

            Files.createDirectories(dir);
            byte[] bytes = contentLoader.download(author.avatarUrl());

            if (bytes == null || bytes.length == 0) {
                log.warn("[AVATAR][EMPTY] authorId={} url={}", author.id(), author.avatarUrl());
                return DownloadResult.failed();
            }

            temporaryFile = dir.resolve(fileName + ".part");
            Files.write(temporaryFile, bytes);
            moveToTarget(temporaryFile, target);
            log.info("[AVATAR][OK] {} ({} bytes)", fileName, bytes.length);
            return DownloadResult.downloaded(target.toString(), bytes.length, fileId);
        } catch (Exception e) {
            deleteQuietly(temporaryFile);
            log.warn("[AVATAR][FAIL] authorId={} url={}", author.id(), author.avatarUrl(), e);
            return DownloadResult.failed();
        }
    }

    private void moveToTarget(Path source, Path target) throws java.io.IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
            log.debug("[AVATAR] Could not delete temporary file {}", path);
        }
    }
}
