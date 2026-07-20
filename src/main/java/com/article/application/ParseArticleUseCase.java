package com.article.application;

import com.article.bootstrap.properties.StepsProps;
import com.article.domain.ArticleLink;
import com.article.domain.ParseResult;
import com.article.port.ArticlePagePort;
import com.article.port.persistence.ArticlePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParseArticleUseCase {

    private final StepsProps stepsProps;
    private final ArticlePersistencePort articlePersistencePort;
    private final ArticlePagePort articlePagePort;
    private final ParallelExecutor parallelExecutor;
    private final ParsedMediaService parsedMediaService;

    public void parsePages() {
        log.info("Step: PARSE PAGES");

        int batchSize = stepsProps.parsePages().batchSize();
        log.info("Parsing pages | batchSize={}", batchSize);

        List<ArticleLink> batch;

//        batch.add(ArticleLink.builder()
//                .articleDate(LocalDate.EPOCH)
//                .articleUrl("https://nypost.com/2024/02/11/lifestyle/dear-abby-im-gay-but-women-keep-coming-on-to-me/")
//                .build());
        while (!(batch = articlePersistencePort.findNewBatch(batchSize)).isEmpty()) {
            parallelExecutor.execute(batch, this::parseSingle);
        }
        log.info("No more links to parse");
    }

    private void parseSingle(ArticleLink link) {
        long start = System.currentTimeMillis();
        switch (articlePagePort.parse(link)) {
            case ParseResult.Success success -> {
                articlePersistencePort.saveArticleAndMarkParsed(success.article(), link.articleUrl());
                parsedMediaService.saveImages(success.images());
                parsedMediaService.saveVideos(success.videos());
                log.info("[PARSED] {} ({} ms)", link.articleUrl(), System.currentTimeMillis() - start);
            }
            case ParseResult.Failure f -> {
                articlePersistencePort.markError(link.articleUrl(), f.error().reason());
                log.warn("[ERROR] {} | {} ({} ms)", link.articleUrl(), f.error().reason(), System.currentTimeMillis() - start);            }
        }
    }
}



//    } catch (
//HttpStatusCodeException e) {
//        log.error("[PARSE] HTTP {} for {}", e.getStatusCode().value(), url);
//        return ParseResult.error(url, "HTTP " + e.getStatusCode().value());
//        } catch (Exception e) {
//        log.error("[PARSE] Failed to fetch {}: {}", url, e.getMessage());
//        return ParseResult.error(url, e.getMessage());
//        }
//        }