package com.article.adapter.article.mapper;

import com.article.adapter.article.persistence.entity.ArticleEntity;
import com.article.adapter.article.persistence.entity.LinkArticleEntity;
import com.article.domain.article.Article;
import com.article.domain.article.ArticleLink;
import com.article.domain.article.ArticleMetadata;
import com.article.domain.article.ArticleWithMetadata;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.*;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface ArticleMapper {

    @BeanMapping(unmappedSourcePolicy = ReportingPolicy.IGNORE)
    Article toArticle(ArticleEntity entity);

    default ArticleWithMetadata toArticleWithMetadata(ArticleEntity entity) {
        return new ArticleWithMetadata(toArticle(entity), new ArticleMetadata(
                entity.getTopics() != null ? entity.getTopics() : List.of(),
                entity.getTags() != null ? entity.getTags() : List.of(),
                entity.getMainCategory()
        ));
    }

    default List<ArticleWithMetadata> toArticlesWithMetadata(List<ArticleEntity> entities) {
        return entities.stream().map(this::toArticleWithMetadata).toList();
    }

    @Mapping(target = "topics", source = "metadata.topics")
    @Mapping(target = "tags", source = "metadata.tags")
    @Mapping(target = "mainCategory", source = "metadata.mainCategory")
    @Mapping(target = "topicsExtracted", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    ArticleEntity toEntity(Article article, ArticleMetadata metadata);

    @BeanMapping(ignoreUnmappedSourceProperties = {"id", "status", "errorMessage"})
    ArticleLink toArticleLink(LinkArticleEntity entity);

    List<ArticleLink> toArticleLinks(List<LinkArticleEntity> entities);
}
