package ru.qa.disk.model;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Объект {@code Link} — его возвращают операции создания, копирования, публикации и загрузки.
 *
 * <p>Для асинхронных операций (код 202) {@code href} указывает на статус операции:
 * {@code .../v1/disk/operations?id=<id>}. Поэтому здесь же лежит разбор идентификатора.
 */
public record Link(
        String href,
        String method,
        Boolean templated,
        String operationId
) {

    private static final Pattern OPERATION_ID = Pattern.compile("operations[/?](?:id=)?([\\w.-]+)");

    /** Идентификатор асинхронной операции — из поля или из {@code href}. */
    public Optional<String> asyncOperationId() {
        if (operationId != null && !operationId.isBlank()) {
            return Optional.of(operationId);
        }
        if (href == null) {
            return Optional.empty();
        }
        Matcher matcher = OPERATION_ID.matcher(href);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
