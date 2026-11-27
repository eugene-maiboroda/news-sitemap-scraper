package com.article.application.dto;

import java.time.Instant;
import java.util.Objects;

public record PipelineStatus(

        Step step,

        Status status,

        Instant startedAt,

        Instant finishedAt,

        String errorMessage
) {

    public PipelineStatus {
        Objects.requireNonNull(status, "Status must not be null");

        if (status == Status.IDLE) {
            if (step != null || startedAt != null || finishedAt != null || errorMessage != null) {
                throw new IllegalArgumentException("IDLE must not contain run details");
            }
        } else {
            Objects.requireNonNull(step, "Step must not be null");
            Objects.requireNonNull(startedAt, "Start time must not be null");
            boolean active = status == Status.RUNNING || status == Status.STOPPING;
            if (active && finishedAt != null) {
                throw new IllegalArgumentException("An active run must not have a finish time");
            }
            if (!active && finishedAt == null) {
                throw new IllegalArgumentException("A finished run must have a finish time");
            }
        }

        if (status == Status.FAILED) {
            if (errorMessage == null || errorMessage.isBlank()) {
                throw new IllegalArgumentException("FAILED must contain an error message");
            }
        } else if (errorMessage != null) {
            throw new IllegalArgumentException("Only FAILED may contain an error message");
        }
    }

    public static PipelineStatus idle() {
        return new PipelineStatus(null, Status.IDLE, null, null, null);
    }

    public static PipelineStatus running(Step step, Instant startedAt) {
        return new PipelineStatus(step, Status.RUNNING, startedAt, null, null);
    }

    public PipelineStatus stopping() {
        return new PipelineStatus(step, Status.STOPPING, startedAt, null, null);
    }

    public PipelineStatus completed(Instant finishedAt) {
        return new PipelineStatus(step, Status.COMPLETED, startedAt, finishedAt, null);
    }

    public PipelineStatus stopped(Instant finishedAt) {
        return new PipelineStatus(step, Status.STOPPED, startedAt, finishedAt, null);
    }

    public PipelineStatus failed(Instant finishedAt, String errorMessage) {
        return new PipelineStatus(step, Status.FAILED, startedAt, finishedAt, errorMessage);
    }

    public enum Step {

        DISCOVERY,

        PARSING,

        DOWNLOAD_IMAGES,

        DOWNLOAD_VIDEOS,

        PROCESS_AUTHORS,

        EXPORT_AUTHORS,

        EXPORT_JSON
    }

    public enum Status {

        IDLE,

        RUNNING,

        STOPPING,

        COMPLETED,

        STOPPED,

        FAILED
    }
}
