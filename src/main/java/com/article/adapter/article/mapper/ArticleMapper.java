package com.article.adapter.article.mapper;

import com.article.adapter.article.persistence.entity.ArticleEntity;
import com.article.adapter.article.persistence.entity.ArticleLinkEntity;
import com.article.domain.Article;
import com.article.domain.ArticleLink;
import com.article.infrastructure.MapStructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface ArticleMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = "createdAt")
    Article toArticle(ArticleEntity entity);

    List<Article> toArticles(List<ArticleEntity> entities);

    @Mapping(target = "createdAt", ignore = true)
    ArticleEntity toEntity(Article article);

    @BeanMapping(ignoreUnmappedSourceProperties = {"id", "parsed"})
    ArticleLink toArticleLink(ArticleLinkEntity entity);

    List<ArticleLink> toArticleLinks(List<ArticleLinkEntity> entities);

    default ArticleLinkEntity toLinkEntity(ArticleLink link) {
        return new ArticleLinkEntity(link.teamName(), link.articleUrl(), link.articleDate());
    }
}
