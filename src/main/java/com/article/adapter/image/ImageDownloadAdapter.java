package com.article.adapter.image;

import com.article.adapter.http.HttpContentLoader;
import com.article.adapter.shared.ParseUtils;
import com.article.domain.ArticleImageRef;
import com.article.domain.DownloadResult;
import com.article.bootstrap.CollectorProps;
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
    public DownloadResult download(ArticleImageRef ref) {
        try {
            Path dir = Path.of(collectorProps.imagesDir(), ParseUtils.categorySlug(ref.category()));
            String fileName = ParseUtils.imageFilePrefix(ref.category(), ref.articleId(), ref.position()) + resolveExtension(ref.imageUrl());
            Path target = dir.resolve(fileName);

            if (Files.exists(target)) {
                log.debug("[IMAGE][SKIP] {}", fileName);
                return DownloadResult.downloaded(target.toString(), Files.size(target));
            }

            Files.createDirectories(dir);
            byte[] bytes = contentLoader.download(ref.imageUrl());

            if (bytes == null || bytes.length == 0) {
                log.warn("[IMAGE][EMPTY] id={} url={}", ref.articleId(), ref.imageUrl());
                return DownloadResult.failed();
            }

            Files.write(target, bytes);
            log.info("[IMAGE][OK] {} ({} bytes)", fileName, bytes.length);
            return DownloadResult.downloaded(target.toString(), bytes.length);
        } catch (Exception e) {
            log.warn("[IMAGE][FAIL] id={} url={}", ref.articleId(), ref.imageUrl(), e);
            return DownloadResult.failed();
        }
    }

    private String resolveExtension(String url) {
        int question = url.indexOf('?');
        String clean = question == -1 ? url : url.substring(0, question);

        int dot = clean.lastIndexOf('.');
        return dot == -1 ? ".jpg" : clean.substring(dot);
    }
}
