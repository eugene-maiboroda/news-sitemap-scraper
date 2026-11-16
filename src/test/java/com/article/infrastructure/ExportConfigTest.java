package com.article.infrastructure;

import com.article.adapter.export.ArticleExportMapper;
import com.article.adapter.export.JsonExportAdapter;
import com.article.adapter.export.SportsBettingJsonExportAdapter;
import com.article.adapter.export.TopicJsonExportAdapter;
import com.article.bootstrap.properties.ExportPops;
import com.article.bootstrap.properties.StorageProps;
import com.article.port.ExportPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExportConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ExportTestConfiguration.class)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(StorageProps.class, () -> new StorageProps(null, null, null, null))
            .withBean(ArticleExportMapper.class, () -> mock(ArticleExportMapper.class));

    @Test
    void usesCategoryExporterByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ExportPort.class);
            assertThat(context.getBean(ExportPort.class)).isInstanceOf(JsonExportAdapter.class);
            assertThat(context.getBean(ExportPops.class).mode()).isNull();
        });
    }

    @Test
    void selectsCategoryExporter() {
        assertExporter("category", JsonExportAdapter.class);
    }

    @Test
    void selectsTopicExporter() {
        assertExporter("topics", TopicJsonExportAdapter.class);
    }

    @Test
    void selectsTagsExporter() {
        assertExporter("tags", SportsBettingJsonExportAdapter.class);
    }

    private void assertExporter(String mode, Class<? extends ExportPort> exporterClass) {
        contextRunner.withPropertyValues("export.mode=" + mode).run(context -> {
            assertThat(context).hasSingleBean(ExportPort.class);
            assertThat(context.getBean(ExportPort.class)).isInstanceOf(exporterClass);
            assertThat(context.getBean(ExportPops.class).mode()).isEqualTo(mode);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ExportPops.class)
    @Import({JsonExportAdapter.class, TopicJsonExportAdapter.class, SportsBettingJsonExportAdapter.class})
    static class ExportTestConfiguration {
    }
}
