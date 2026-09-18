package ru.qa.disk.model;

/** Абсолютные пути системных папок Диска (набор зависит от языка аккаунта). */
public record SystemFolders(
        String applications,
        String downloads,
        String photostream,
        String screenshots,
        String scans
) {
}
