package com.article.infrastructure;

import com.article.adapter.article.persistence.entity.ArticleEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "collector.discovery-source", havingValue = "AMERICAN_HUNTER")
@EnableJpaRepositories(basePackages = "com.article", excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX, pattern = {
                "com[.]article[.]adapter[.]article[.]persistence[.]ArticleRepository",
                "com[.]article[.]adapter[.]image[.]persistence[.]ImageRepository",
                "com[.]article[.]adapter[.]video[.]persistence[.]VideoRepository"
        }))
class HunterPersistenceConfig {

    // Hunter uses JDBC for articles and media. The excluded repositories query NYPost categories.
    @Bean
    PersistenceManagedTypes persistenceManagedTypes(ResourceLoader resourceLoader) {
        return new PersistenceManagedTypesScanner(resourceLoader,
                className -> !ArticleEntity.class.getName().equals(className))
                .scan("com.article");
    }
}
