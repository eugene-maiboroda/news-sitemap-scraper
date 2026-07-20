package com.article.adapter.export.resolver;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.export.model.JsonExportTarget;
import com.article.adapter.export.model.PreparedArticleExport;
import com.article.domain.article.TaxonomyLink;
import com.article.domain.article.ArticleMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopicExportTargetResolverTest {

    private final TopicExportTargetResolver resolver = new TopicExportTargetResolver();

    @Test
    void buildsOneTargetForEveryTopicLevel() {
        PreparedArticleExport article = article(List.of(
                new TaxonomyLink("Sports", "/sports"),
                new TaxonomyLink("NFL", "/nfl")
        ), "Sports");

        List<JsonExportTarget> targets = resolver.resolve(article);

        assertEquals(2, targets.size());
        assertEquals("/sports", targets.get(0).topicUrl());
        assertEquals(List.of("Sports"), targets.get(0).hierarchy());
        assertEquals("/nfl", targets.get(1).topicUrl());
        assertEquals(List.of("Sports", "NFL"), targets.get(1).hierarchy());
    }

    @Test
    void preservesStandaloneUrlTopicWithNullName() {
        PreparedArticleExport article = article(
                java.util.Collections.singletonList(new TaxonomyLink(null, "/sports-plus")),
                "Post Sports+"
        );

        List<JsonExportTarget> targets = resolver.resolve(article);

        assertEquals(1, targets.size());
        assertEquals(List.of("Post Sports+"), targets.getFirst().hierarchy());
    }

    private PreparedArticleExport article(List<TaxonomyLink> topics, String mainCategory) {
        return new PreparedArticleExport(
                ArticleExportDto.builder().id("article-1").build(),
                new ArticleMetadata(topics, List.of(), mainCategory)
        );
    }
}
