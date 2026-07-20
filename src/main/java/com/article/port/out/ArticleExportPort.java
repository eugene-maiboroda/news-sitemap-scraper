package com.article.port.out;

import com.article.application.dto.ArticleExportBatch;

public interface ArticleExportPort {

    void export(ArticleExportBatch batch);

    default void complete() {
    }
}
