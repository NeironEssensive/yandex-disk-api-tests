package ru.qa.disk.model;

import java.util.List;

/** Содержимое папки — объект {@code ResourceList} внутри поля {@code _embedded}. */
public record ResourceList(
        String path,
        String sort,
        String publicKey,
        Integer limit,
        Integer offset,
        Integer total,
        List<Resource> items
) {

    public List<String> names() {
        return items == null ? List.of() : items.stream().map(Resource::name).toList();
    }

    public List<String> paths() {
        return items == null ? List.of() : items.stream().map(Resource::path).toList();
    }
}
