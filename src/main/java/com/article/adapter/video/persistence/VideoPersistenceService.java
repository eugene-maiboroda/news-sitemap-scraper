package com.article.adapter.video.persistence;

import com.article.domain.ArticleVideoRef;
import com.article.port.persistence.VideoPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoPersistenceService implements VideoPersistencePort {

    private final VideoRepository repository;

    @Override
    public List<ArticleVideoRef> findPending(int limit) {
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
