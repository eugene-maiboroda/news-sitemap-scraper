package com.article.adapter.image;

import com.article.adapter.http.HttpContentLoader;
import com.article.adapter.shared.FileNaming;
import com.article.bootstrap.properties.StorageProps;
import com.article.application.dto.ImageDownloadData;
import com.article.application.dto.DownloadResult;
import com.article.port.out.ImageDownloadPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageDownloadAdapter implements ImageDownloadPort {

    private final HttpContentLoader contentLoader;
    private final StorageProps storageProps;

    @Override
    public DownloadResult download(ImageDownloadData data) {
        Path temporaryFile = null;
        try {
            Path dir = Path.of(storageProps.imagesDir(), FileNaming.categorySlug(data.category()));
            String fileName = FileNaming.imageFilePrefix(
                    data.category(),
                    data.articleId(),
                    data.position()
            ) + FileNaming.resolveExtension(data.imageUrl());
            Path target = dir.resolve(fileName);

            if (Files.exists(target)) {
                log.debug("[IMAGE][SKIP] {}", fileName);
                return DownloadResult.downloaded(target.toString(), Files.size(target));
            }

            Files.createDirectories(dir);
            byte[] bytes = contentLoader.download(data.imageUrl());

            if (bytes == null || bytes.length == 0) {
                log.warn("[IMAGE][EMPTY] id={} url={}", data.articleId(), data.imageUrl());
                return DownloadResult.failed();
            }

            temporaryFile = dir.resolve(fileName + ".part");
            Files.write(temporaryFile, bytes);
            moveToTarget(temporaryFile, target);
            log.info("[IMAGE][OK] {} ({} bytes)", fileName, bytes.length);
            return DownloadResult.downloaded(target.toString(), bytes.length);
        } catch (Exception e) {
            deleteQuietly(temporaryFile);
            log.warn("[IMAGE][FAIL] id={} url={}", data.articleId(), data.imageUrl(), e);
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
            log.debug("[IMAGE] Could not delete temporary file {}", path);
        }
    }
}
