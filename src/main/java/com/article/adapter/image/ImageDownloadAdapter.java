package com.article.adapter.image;

import com.article.adapter.http.HttpContentLoader;
import com.article.adapter.shared.ParseUtils;
import com.article.domain.ArticleImageRef;
import com.article.infrastructure.CollectorProps;
import com.article.port.ImageDownloadPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageDownloadAdapter implements ImageDownloadPort {

    private final HttpContentLoader contentLoader;
    private final CollectorProps collectorProps;

    @Override
    public void download(ArticleImageRef ref) {
        try {
            Path dir = Path.of(collectorProps.imagesDir());
            String fileName = ParseUtils.imageFilePrefix(ref.category(), ref.id()) + resolveExtension(ref.imageUrl());
            Path target = dir.resolve(fileName);

            if (Files.exists(target)) {
                log.debug("[IMAGE][SKIP] {}", fileName);
                return;
            }

            Files.createDirectories(dir);
            byte[] bytes = contentLoader.download(ref.imageUrl());

            if (bytes == null || bytes.length == 0) {
                log.warn("[IMAGE][EMPTY] id={} url={}", ref.id(), ref.imageUrl());
                return;
            }

            Files.write(target, bytes);
            log.info("[IMAGE][OK] {} ({} bytes)", fileName, bytes.length);
        } catch (Exception e) {
            log.warn("[IMAGE][FAIL] id={} url={}", ref.id(), ref.imageUrl(), e);
        }
    }

    private String resolveExtension(String url) {
        int dot = url.lastIndexOf('.');
        return dot != -1 ? url.substring(dot) : ".jpg";
    }
}
