package com.article.adapter.export;

import com.article.adapter.export.mapper.ArticleExportMapper;
import com.article.adapter.export.model.PreparedArticleExport;
import com.article.domain.article.Article;
import com.article.domain.article.ArticleMetadata;
import com.article.domain.article.ArticleWithMetadata;
import com.article.domain.media.Video;
import com.article.application.dto.ArticleExportBatch;
import com.article.domain.media.VideoPlatform;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArticleExportAssemblerTest {

    private final ArticleExportAssembler assembler =
            new ArticleExportAssembler(new ArticleExportMapper());

    @Test
    void associatesMediaAndBuildsYoutubeUrlOnce() {
        Article article = Article.builder()
                .id("article-1")
                .category("Sports")
                .title("Title")
                .content("Content")
                .build();
        Video youtube = Video.builder()
                .id("video-1")
                .articleId(article.id())
                .platform(VideoPlatform.YOUTUBE)
                .youtubeVideoId("abc123")
                .build();
        ArticleMetadata metadata = new ArticleMetadata(List.of(), List.of(), "Sports");

        List<PreparedArticleExport> result = assembler.assemble(new ArticleExportBatch(
                List.of(new ArticleWithMetadata(article, metadata)),
                List.of(),
                List.of(youtube)
        ));

        assertEquals(1, result.size());
        assertEquals(
                "https://www.youtube.com/watch?v=abc123",
                result.getFirst().article().videos().getFirst().url()
        );
    }
}
