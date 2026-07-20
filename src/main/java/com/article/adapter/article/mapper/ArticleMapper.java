package com.article.adapter.article.mapper;

import com.article.adapter.article.persistence.entity.ArticleEntity;
import com.article.adapter.article.persistence.entity.LinkArticleEntity;
import com.article.domain.Article;
import com.article.domain.ArticleLink;
import com.article.domain.ArticleLinks;
import com.article.domain.ArticleWithMetadata;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface ArticleMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {
            "topics", "tags", "mainCategory", "topicsExtracted", "createdAt"
    })
    Article toArticle(ArticleEntity entity);

    List<Article> toArticles(List<ArticleEntity> entities);

    default ArticleWithMetadata toArticleWithMetadata(ArticleEntity entity) {
        return new ArticleWithMetadata(
                toArticle(entity),
                new ArticleLinks(
                        entity.getId(),
                        entity.getTopics() != null ? entity.getTopics() : List.of(),
                        entity.getTags() != null ? entity.getTags() : List.of()
                )
        );
    }

    default List<ArticleWithMetadata> toArticlesWithMetadata(List<ArticleEntity> entities) {
        return entities.stream().map(this::toArticleWithMetadata).toList();
    }

    @Mappings({
            @Mapping(target = "topics", ignore = true),
            @Mapping(target = "tags", ignore = true),
            @Mapping(target = "mainCategory", ignore = true),
            @Mapping(target = "topicsExtracted", ignore = true),
            @Mapping(target = "createdAt", ignore = true)
    })
    ArticleEntity toEntity(Article article);

    @BeanMapping(ignoreUnmappedSourceProperties = {"id", "status", "errorMessage"})
    ArticleLink toArticleLink(LinkArticleEntity entity);

    List<ArticleLink> toArticleLinks(List<LinkArticleEntity> entities);
}
