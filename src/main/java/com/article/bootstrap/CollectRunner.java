package com.article.bootstrap;

import com.article.application.ArticleCollectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CollectRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CollectRunner.class);

    private final ArticleCollectorService collectorService;
    private final CollectorProps props;

    public CollectRunner(ArticleCollectorService collectorService, CollectorProps props) {
        this.collectorService = collectorService;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        List<String> sitemapUrls = props.sitemapUrls();

        if (sitemapUrls == null || sitemapUrls.isEmpty()) {
            log.warn("No sitemap URLs configured. Add urls to collector.sitemap-urls in application.properties");
            return;
        }

        collectorService.run(sitemapUrls);
    }
}
