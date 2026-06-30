package com.article.port;

import com.article.domain.Article;

import java.util.List;

public interface ExportPort {

    void export(List<Article> articles);
}
