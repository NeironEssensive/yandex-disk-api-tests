package ru.qa.disk.model;

import java.util.List;

/**
 * Плоские списки файлов: {@code GET /v1/disk/resources/files} и
 * {@code GET /v1/disk/resources/last-uploaded}. В обоих случаях в ответе только
 * {@code items} и параметры выборки, папки в такие списки не попадают.
 */
public record FlatResourceList(
        List<Resource> items,
        Integer limit,
        Integer offset
) {

    public List<String> paths() {
        return items == null ? List.of() : items.stream().map(Resource::path).toList();
    }
}
