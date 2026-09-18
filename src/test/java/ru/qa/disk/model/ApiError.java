package ru.qa.disk.model;

/**
 * Тело ошибки API Диска.
 *
 * <p>Например для несуществующего пути:
 * <pre>
 * {
 *   "message": "Не удалось найти запрошенный ресурс.",
 *   "description": "Resource not found.",
 *   "error": "DiskNotFoundError"
 * }
 * </pre>
 */
public record ApiError(
        String message,
        String description,
        String error
) {
}
