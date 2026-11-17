package com.article.adapter.image.persistence;

import com.article.domain.ArticleImageRef;
import com.article.port.persistence.ImagePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImagePersistenceService implements ImagePersistencePort {

    private final ImageRepository repository;

    @Override
    public List<ArticleImageRef> findPending(int limit) {
        return repository.findPending(PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public void markDownloaded(String id, String localFilePath, long fileSize) {
        repository.markDownloaded(id, localFilePath, fileSize);
    }

    @Override
    @Transactional
    public void markFailed(String id) {
        repository.markFailed(id);
    }
}
