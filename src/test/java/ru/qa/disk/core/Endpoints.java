package ru.qa.disk.core;

/** Пути ресурсов REST API Яндекс Диска (v1). */
public final class Endpoints {

    public static final String DISK = "/v1/disk/";
    public static final String RESOURCES = "/v1/disk/resources";
    public static final String UPLOAD = "/v1/disk/resources/upload";
    public static final String DOWNLOAD = "/v1/disk/resources/download";
    public static final String COPY = "/v1/disk/resources/copy";
    public static final String MOVE = "/v1/disk/resources/move";
    public static final String PUBLISH = "/v1/disk/resources/publish";
    public static final String UNPUBLISH = "/v1/disk/resources/unpublish";
    public static final String FILES = "/v1/disk/resources/files";
    public static final String LAST_UPLOADED = "/v1/disk/resources/last-uploaded";
    public static final String PUBLIC_RESOURCES = "/v1/disk/public/resources";
    public static final String TRASH_RESOURCES = "/v1/disk/trash/resources";
    public static final String TRASH_RESTORE = "/v1/disk/trash/resources/restore";
    public static final String OPERATIONS = "/v1/disk/operations/{id}";

    private Endpoints() {
    }
}
