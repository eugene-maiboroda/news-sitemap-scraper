package com.article.infrastructure;

import com.article.adapter.export.resolver.CategoryExportTargetResolver;
import com.article.adapter.export.resolver.ExportTargetResolver;
import com.article.adapter.export.resolver.SportsBettingExportTargetResolver;
import com.article.adapter.export.resolver.TopicExportTargetResolver;
import com.article.bootstrap.properties.ExportProps;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExportConfig {

    @Bean
    ExportTargetResolver exportTargetResolver(ExportProps props) {
        return switch (props.mode()) {
            case CATEGORY -> new CategoryExportTargetResolver();
            case TOPICS -> new TopicExportTargetResolver();
            case TAGS -> new SportsBettingExportTargetResolver();
        };
    }
}
