package com.article.adapter.export.resolver;

import com.article.adapter.export.dto.ArticleExportDto;
import com.article.adapter.export.model.PreparedArticleExport;
import com.article.domain.article.TaxonomyLink;
import com.article.domain.article.ArticleMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SportsBettingExportTargetResolverTest {

    private final SportsBettingExportTargetResolver resolver =
            new SportsBettingExportTargetResolver();

    @Test
    void routesArticleToEveryMatchingLeague() {
        PreparedArticleExport article = new PreparedArticleExport(
                ArticleExportDto.builder().id("article-1").build(),
                new ArticleMetadata(
                        List.of(),
                        List.of(
                                new TaxonomyLink("Sports Betting", "/sports-betting"),
                                new TaxonomyLink("NFL Betting", "/nfl-betting"),
                                new TaxonomyLink("NBA Betting", "/nba-betting")
                        ),
                        null
                )
        );

        assertEquals(2, resolver.resolve(article).size());
    }
}
