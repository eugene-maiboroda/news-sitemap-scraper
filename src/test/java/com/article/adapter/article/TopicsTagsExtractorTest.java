package com.article.adapter.article;

import com.article.domain.article.ArticleMetadata;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TopicsTagsExtractorTest {

    private final TopicsTagsExtractor extractor = new TopicsTagsExtractor(new MainCategoryResolver());

    @Test
    void extractsAllTopicsAndTagsWhilePreservingNullableFields() {
        String html = """
                <article>
                  <header class="article-header">
                    <div class="article-header__top">
                      <a class="flag flag--stamp" href="https://nypost.com/sports/">Sports</a>
                      <div class="flag flag--stamp">Named topic without URL</div>
                      <a class="flag flag--stamp" href="https://nypost.com/sports-plus/">
                        <svg></svg>
                      </a>
                    </div>
                  </header>
                  <footer class="article-footer">
                    <div class="tag-list">
                      <a rel="tag" href="https://nypost.com/tag/nfl/">NFL</a>
                      <a rel="tag" href="">Name without URL</a>
                      <a rel="tag" href=""> </a>
                    </div>
                  </footer>
                </article>
                """;

        ArticleMetadata result = extractor.extract(Jsoup.parse(html));

        assertEquals(3, result.topics().size());
        assertEquals("Sports", result.topics().get(0).name());
        assertEquals("https://nypost.com/sports/", result.topics().get(0).url());
        assertEquals("Named topic without URL", result.topics().get(1).name());
        assertNull(result.topics().get(1).url());
        assertNull(result.topics().get(2).name());
        assertEquals("https://nypost.com/sports-plus/", result.topics().get(2).url());

        assertEquals(2, result.tags().size());
        assertEquals("NFL", result.tags().get(0).name());
        assertEquals("https://nypost.com/tag/nfl/", result.tags().get(0).url());
        assertEquals("Name without URL", result.tags().get(1).name());
        assertNull(result.tags().get(1).url());
        assertEquals("Sports", result.mainCategory());
    }
}
