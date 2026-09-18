package ru.qa.disk.model;

/** Статус асинхронной операции: {@code GET /v1/disk/operations/{id}}. */
public record Operation(String status) {

    public static final String SUCCESS = "success";
    public static final String FAILED = "failed";
    public static final String IN_PROGRESS = "in-progress";

    public boolean isSuccess() {
        return SUCCESS.equals(status);
    }

    public boolean isFailed() {
        return FAILED.equals(status);
    }

    public boolean isInProgress() {
        return IN_PROGRESS.equals(status);
    }
}
