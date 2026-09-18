package ru.qa.disk.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Random;
import java.util.UUID;

/** Генерация тестовых данных: уникальные имена, содержимое файлов, контрольные суммы. */
public final class TestData {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Random RANDOM = new Random();

    private TestData() {
    }

    /** Короткий уникальный суффикс, чтобы тесты не мешали друг другу на одном аккаунте. */
    public static String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String fileName(String prefix, String extension) {
        return prefix + "-" + uniqueSuffix() + "." + extension;
    }

    public static String folderName(String prefix) {
        return prefix + "-" + uniqueSuffix();
    }

    /** Текстовое содержимое с пометкой, по которому видно, какой тест создал файл. */
    public static String textContent(String marker) {
        return "Автотест API Яндекс Диска" + System.lineSeparator()
                + "marker: " + marker + System.lineSeparator()
                + "created: " + LocalDateTime.now().format(TIMESTAMP) + System.lineSeparator();
    }

    public static byte[] bytesOf(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /** MD5 в hex — API отдаёт его в поле {@code md5} у файла, удобно сверять содержимое. */
    public static String md5Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 недоступен в этой JVM", e);
        }
    }
}
