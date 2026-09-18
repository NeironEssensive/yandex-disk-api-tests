package ru.qa.disk.model;

/** Владелец Диска — приходит внутри {@link DiskInfo}. */
public record User(
        String uid,
        String login,
        String displayName,
        String country,
        Boolean isChild
) {
}
