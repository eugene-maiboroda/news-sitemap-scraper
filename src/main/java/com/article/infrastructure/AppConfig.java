package com.article.infrastructure;

import com.article.application.PipelineRunSettings.Snapshot;
import com.article.application.PipelineRunSettings;
import com.article.bootstrap.properties.CollectorProps;
import com.article.bootstrap.properties.StepsProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.type.LogicalType;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService articleExecutor(CollectorProps props) {
        return Executors.newFixedThreadPool(props.parseThreads());
    }

    @Bean
    @Profile("server")
    public JsonMapperBuilderCustomizer pipelineJson() {
        return builder -> builder
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .withCoercionConfig(LogicalType.Textual, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }

    @Bean
    @Profile("server")
    public OpenApiCustomizer pipelineOpenApi(PipelineRunSettings settings, StepsProps steps) {
        return api -> {
            Snapshot defaults = settings.defaults();
            Map<String, Object> discovery = new LinkedHashMap<>();
            discovery.put("enabled", false);
            discovery.put("collectionBatchId", "");
            discovery.put("sitemapUrls", List.of(""));
            discovery.put("batchSize", steps.discoverLinks().batchSize());
            discovery.put("sitemapDelayMs", defaults.collector().sitemapDelayMs());
            configureRequest(api, "/pipeline/discovery", "DiscoveryRequest", discovery,
                    List.of("collectionBatchId", "sitemapUrls"));

            Map<String, Object> parsing = new LinkedHashMap<>();
            parsing.put("enabled", false);
            parsing.put("batchSize", steps.parsePages().batchSize());
            parsing.put("parseThreads", defaults.collector().parseThreads());
            parsing.put("extractConnatixVideos", defaults.extractConnatixVideos());
            parsing.put("browserEnabled", defaults.browser().enabled());
            parsing.put("browserHeadless", defaults.browser().headless());
            parsing.put("browserTimeoutMs", defaults.browser().timeoutMs());
            configureRequest(api, "/pipeline/parsing", "ParsingRequest", parsing, List.of());

            Map<String, Object> images = new LinkedHashMap<>();
            images.put("enabled", false);
            images.put("imagesDir", "");
            images.put("batchSize", steps.downloadImages().batchSize());
            images.put("parseThreads", defaults.collector().parseThreads());
            configureRequest(api, "/pipeline/download-images", "DownloadImagesRequest", images, List.of("imagesDir"));

            Map<String, Object> videos = new LinkedHashMap<>();
            videos.put("enabled", false);
            videos.put("videosDir", "");
            videos.put("batchSize", steps.downloadVideos().batchSize());
            videos.put("parseThreads", defaults.collector().parseThreads());
            videos.put("timeoutSeconds", defaults.video().timeoutMs());
            configureRequest(api, "/pipeline/download-videos", "DownloadVideosRequest", videos, List.of("videosDir"));

            Map<String, Object> authors = new LinkedHashMap<>();
            authors.put("enabled", false);
            authors.put("avatarsDir", "");
            authors.put("batchSize", steps.processAuthors().batchSize());
            authors.put("parseThreads", defaults.collector().parseThreads());
            configureRequest(api, "/pipeline/process-authors", "ProcessAuthorsRequest", authors, List.of("avatarsDir"));

            Map<String, Object> authorExport = new LinkedHashMap<>();
            authorExport.put("enabled", false);
            authorExport.put("exportDir", "");
            configureRequest(api, "/pipeline/export-authors", "ExportAuthorsRequest", authorExport, List.of("exportDir"));

            Map<String, Object> export = new LinkedHashMap<>();
            export.put("enabled", false);
            export.put("exportDir", "");
            export.put("batchSize", steps.exportJson().batchSize());
            String batch = defaults.export().collectionBatchId();
            export.put("collectionBatchId", batch == null ? "" : batch);
            export.put("mode", defaults.export().mode().name());
            configureRequest(api, "/pipeline/export-json", "ExportJsonRequest", export, List.of("exportDir"));

            Operation stop = api.getPaths().get("/pipeline/stop").getPost();
            stop.getResponses().addApiResponse("200", statusResponse("Повторний Stop або немає активної роботи"));
            stop.getResponses().addApiResponse("202", statusResponse("Запитано м’яку зупинку; очікуйте STOPPED/FAILED"));
        };
    }

    private void configureRequest(
            OpenAPI api,
            String path,
            String name,
            Map<String, Object> example,
            List<String> requiredWhenEnabled
    ) {
        Schema<?> schema = api.getComponents().getSchemas().get(name);
        Map<String, Schema> ordered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : example.entrySet()) {
            String field = entry.getKey();
            Schema<?> property = (Schema<?>) schema.getProperties().get(field);
            if (field.equals("enabled")) {
                property.setDescription("true — запустити крок; false — нічого не запускати. Не зупиняє активну роботу.");
            } else if (requiredWhenEnabled.contains(field)) {
                String description = "Обов’язково за enabled=true; defaults немає. ";
                if (field.equals("sitemapUrls")) {
                    description += "Список річних HTTP(S) sitemap NYPost.";
                } else if (field.equals("collectionBatchId")) {
                    description += "Назва оновлення тільки для нових URL.";
                } else {
                    description += "Директорія на машині застосунку.";
                }
                property.setDescription(description);
            } else {
                property.setDefault(entry.getValue());
                String description = "Можна пропустити: значення з application.properties. ";
                if (field.equals("mode")) {
                    description += "CATEGORY / TOPICS / TAGS; TAGS — спеціальний Sports Betting, не всі теги.";
                } else if (field.equals("collectionBatchId")) {
                    description += "Порожній рядок — всі статті; значення — точний batch.";
                } else if (field.equals("timeoutSeconds")) {
                    description += "Очікування yt-dlp у секундах.";
                }
                property.setDescription(description);
            }
            ordered.put(field, property);
        }
        schema.setProperties(ordered);
        schema.setRequired(List.of("enabled"));

        Operation operation = api.getPaths().get(path).getPost();
        operation.setDescription("Один крок у фоні. Директорії належать машині застосунку. "
                + "batchSize — розмір пакета, не ліміт всього запуску. "
                + "Налаштуйте обов’язкові поля та enabled=true; enabled=false нічого не запускає.");
        operation.getRequestBody().getContent().values().forEach(media -> media.setExample(example));
        operation.getResponses().addApiResponse("200", statusResponse("enabled=false: поточний стан без запуску"));
        operation.getResponses().addApiResponse("202", statusResponse("Прийнятий запуск у фоні"));
        operation.getResponses().addApiResponse("409", statusResponse("Зайнято: запит не зберігається, черги немає"));
        operation.getResponses().addApiResponse("400", errorResponse("INVALID_REQUEST", "Request validation failed"));
        operation.getResponses().addApiResponse("500", errorResponse("INTERNAL_ERROR", "Internal server error"));
    }

    private ApiResponse statusResponse(String description) {
        MediaType media = new MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/PipelineStatus"));
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", media));
    }

    private ApiResponse errorResponse(String code, String message) {
        MediaType media = new MediaType()
                .example(Map.of("error", Map.of("code", code, "message", message)));
        return new ApiResponse()
                .description(message)
                .content(new Content().addMediaType("application/json", media));
    }

}
