package com.article.application.dto;

import com.article.domain.Article;
import com.article.domain.ArticleLinks;
import com.article.domain.Image;
import com.article.domain.Video;

import java.util.List;

public record ExportData(
        String category,
        List<Article> articles,
        List<Image> images,
        List<Video> videos,
        List<ArticleLinks> articleLinks,
        TopicExportContext topicContext
) {}
