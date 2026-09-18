package ru.qa.disk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Файл или папка на Диске (объект {@code Resource} в документации).
 *
 * <p>Приходит из {@code GET /v1/disk/resources}, а также внутри {@code _embedded} — поэтому
 * поле {@code _embedded} размечено явно: стратегия snake_case не превращает {@code embedded}
 * в {@code _embedded}.
 */
public record Resource(
        String name,
        String path,
        String type,
        String mimeType,
        String mediaType,
        Long size,
        String md5,
        String sha256,
        Long revision,
        String resourceId,
        OffsetDateTime created,
        OffsetDateTime modified,
        String originPath,
        String publicKey,
        String publicUrl,
        String file,
        String preview,
        Map<String, Object> customProperties,
        @JsonProperty("_embedded") ResourceList embedded
) {

    public static final String TYPE_DIR = "dir";
    public static final String TYPE_FILE = "file";

    public boolean isDir() {
        return TYPE_DIR.equals(type);
    }

    public boolean isFile() {
        return TYPE_FILE.equals(type);
    }

    public boolean isPublished() {
        return publicUrl != null && !publicUrl.isBlank();
    }
}
