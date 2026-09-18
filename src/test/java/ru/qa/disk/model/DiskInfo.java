package ru.qa.disk.model;

/**
 * Ответ {@code GET /v1/disk/} — общая информация о Диске пользователя.
 *
 * @see <a href="https://yandex.ru/dev/disk-api/doc/ru/reference/capacity">Данные о Диске</a>
 */
public record DiskInfo(
        Long totalSpace,
        Long usedSpace,
        Long trashSize,
        Long maxFileSize,
        Long revision,
        Boolean isPaid,
        Boolean unlimitedAutouploadEnabled,
        SystemFolders systemFolders,
        User user
) {

    /** Свободное место в байтах. */
    public long freeSpace() {
        return totalSpace - usedSpace;
    }
}
