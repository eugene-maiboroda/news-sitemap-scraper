package com.article.adapter.pipeline;

import com.article.adapter.pipeline.PipelineRequests.DiscoveryRequest;
import com.article.adapter.pipeline.PipelineRequests.DownloadImagesRequest;
import com.article.adapter.pipeline.PipelineRequests.DownloadVideosRequest;
import com.article.adapter.pipeline.PipelineRequests.ExportAuthorsRequest;
import com.article.adapter.pipeline.PipelineRequests.ExportJsonRequest;
import com.article.adapter.pipeline.PipelineRequests.ParsingRequest;
import com.article.adapter.pipeline.PipelineRequests.ProcessAuthorsRequest;
import com.article.application.PipelineExecutionService;
import com.article.application.PipelineExecutionService.BusyException;
import com.article.application.PipelineRunSettings;
import com.article.application.PipelineRunSettings.Snapshot;
import com.article.application.dto.PipelineStatus;
import com.article.application.dto.PipelineStatus.Status;
import com.article.application.dto.PipelineStatus.Step;
import com.article.bootstrap.properties.StepsProps;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@Slf4j
@RestController
@Profile("server")
@RequestMapping(value = "/pipeline", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineExecutionService execution;
    private final PipelineRunSettings runSettings;
    private final StepsProps steps;

    @Operation(tags = "1. Discovery", summary = "Зібрати нові URL")
    @PostMapping(value = "/discovery", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineStatus> discovery(@RequestBody DiscoveryRequest request) {
        return start(request.enabled(), Step.DISCOVERY,
                () -> request.batchSize(steps.discoverLinks().batchSize()),
                () -> request.settings(runSettings.defaults()));
    }

    @Operation(tags = "2. Parsing", summary = "Обробити чергу NEW")
    @PostMapping(value = "/parsing", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineStatus> parsing(@RequestBody ParsingRequest request) {
        return start(request.enabled(), Step.PARSING,
                () -> request.batchSize(steps.parsePages().batchSize()),
                () -> request.settings(runSettings.defaults()));
    }

    @Operation(tags = "3. Images", summary = "Завантажити фото PENDING")
    @PostMapping(value = "/download-images", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineStatus> images(@RequestBody DownloadImagesRequest request) {
        return start(request.enabled(), Step.DOWNLOAD_IMAGES,
                () -> request.batchSize(steps.downloadImages().batchSize()),
                () -> request.settings(runSettings.defaults()));
    }

    @Operation(tags = "4. Videos", summary = "Завантажити відео PENDING")
    @PostMapping(value = "/download-videos", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineStatus> videos(@RequestBody DownloadVideosRequest request) {
        return start(request.enabled(), Step.DOWNLOAD_VIDEOS,
                () -> request.batchSize(steps.downloadVideos().batchSize()),
                () -> request.settings(runSettings.defaults()));
    }

    @Operation(tags = "5. Authors", summary = "Обробити авторів та їхні аватари")
    @PostMapping(value = "/process-authors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineStatus> authors(@RequestBody ProcessAuthorsRequest request) {
        return start(request.enabled(), Step.PROCESS_AUTHORS,
                () -> request.batchSize(steps.processAuthors().batchSize()),
                () -> request.settings(runSettings.defaults()));
    }

    @Operation(tags = "6. Author export", summary = "Створити AllAuthors.json")
    @PostMapping(value = "/export-authors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineStatus> exportAuthors(@RequestBody ExportAuthorsRequest request) {
        return start(request.enabled(), Step.EXPORT_AUTHORS, () -> 0,
                () -> request.settings(runSettings.defaults()));
    }

    @Operation(tags = "7. Article export", summary = "Створити JSON статей")
    @PostMapping(value = "/export-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineStatus> exportJson(@RequestBody ExportJsonRequest request) {
        return start(request.enabled(), Step.EXPORT_JSON,
                () -> request.batchSize(steps.exportJson().batchSize()),
                () -> request.settings(runSettings.defaults()));
    }

    @Operation(tags = "8. Control", summary = "Стан останнього запуску")
    @GetMapping("/status")
    public PipelineStatus status() {
        return execution.status();
    }

    @Operation(tags = "8. Control", summary = "Запитати м’яку зупинку")
    @PostMapping("/stop")
    public ResponseEntity<PipelineStatus> stop() {
        synchronized (execution) {
            boolean running = execution.status().status() == Status.RUNNING;
            PipelineStatus status = execution.stop();
            return ResponseEntity.status(running ? HttpStatus.ACCEPTED : HttpStatus.OK).body(status);
        }
    }

    @ExceptionHandler(BusyException.class)
    public ResponseEntity<PipelineStatus> busy(BusyException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getStatus());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> malformedJson() {
        return ResponseEntity.badRequest().body(error("INVALID_REQUEST", "Request validation failed",
                Map.of("request", "Malformed JSON or invalid field type")));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> invalidRequest(ResponseStatusException exception) {
        if (exception.getStatusCode() == HttpStatus.BAD_REQUEST) {
            String reason = exception.getReason();
            if (reason == null) {
                reason = "request is invalid";
            }
            String field = reason.split(" ", 2)[0];
            return ResponseEntity.badRequest().body(error("INVALID_REQUEST", "Request validation failed",
                    Map.of(field, reason)));
        }
        return frameworkError(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpectedFailure(Exception exception) {
        if (exception instanceof ErrorResponse response) {
            return frameworkError(response);
        }
        log.error("Pipeline HTTP request failed", exception);
        return ResponseEntity.internalServerError()
                .body(error("INTERNAL_ERROR", "Internal server error", Map.of()));
    }

    private ResponseEntity<PipelineStatus> start(
            Boolean enabled,
            Step step,
            IntSupplier batchSize,
            Supplier<Snapshot> settings
    ) {
        if (enabled == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "enabled is required");
        }
        if (!enabled) {
            return ResponseEntity.ok(execution.status());
        }

        int resolvedBatchSize;
        Snapshot resolvedSettings;
        try {
            resolvedBatchSize = batchSize.getAsInt();
            resolvedSettings = settings.get();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        PipelineStatus accepted = execution.start(step, resolvedBatchSize, resolvedSettings);
        return ResponseEntity.accepted().body(accepted);
    }

    private ResponseEntity<Map<String, Object>> frameworkError(ErrorResponse response) {
        boolean serverFailure = response.getStatusCode().is5xxServerError();
        String code = serverFailure ? "INTERNAL_ERROR" : "HTTP_ERROR";
        String message = serverFailure ? "Internal server error" : "Request could not be processed";
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(error(code, message, Map.of()));
    }

    private Map<String, Object> error(String code, String message, Map<String, String> fields) {
        if (fields.isEmpty()) {
            return Map.of("error", Map.of("code", code, "message", message));
        }
        return Map.of("error", Map.of("code", code, "message", message, "errors", fields));
    }
}
