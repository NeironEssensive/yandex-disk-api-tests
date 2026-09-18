package ru.qa.disk.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Настройки прогона.
 *
 * <p>Значения ищутся в следующем порядке, каждый следующий источник перекрывает предыдущий:
 * <ol>
 *     <li>{@code src/test/resources/application.properties} — значения по умолчанию;</li>
 *     <li>файл {@code .env} в корне проекта — удобно локально, в git не попадает;</li>
 *     <li>переменные окружения, например {@code YANDEX_DISK_TOKEN};</li>
 *     <li>системные свойства, например {@code -Dyandex.disk.token=...}.</li>
 * </ol>
 *
 * <p>Имя ключа и имя переменной окружения связаны однозначно:
 * {@code yandex.disk.token} &lt;-&gt; {@code YANDEX_DISK_TOKEN}.
 *
 * <p>Токен не хранится в репозитории и не попадает ни в логи, ни в отчёт Allure
 * (см. {@link ru.qa.disk.core.AllureRequestFilter}).
 */
public final class TestConfig {

    public static final String TOKEN = "yandex.disk.token";
    public static final String BASE_URI = "yandex.disk.base.uri";
    public static final String PLAYGROUND_ROOT = "yandex.disk.playground.root";
    public static final String ASYNC_TIMEOUT_SECONDS = "async.timeout.seconds";
    public static final String ASYNC_POLL_INTERVAL_MS = "async.poll.interval.ms";

    private static final List<String> KNOWN_KEYS =
            List.of(TOKEN, BASE_URI, PLAYGROUND_ROOT, ASYNC_TIMEOUT_SECONDS, ASYNC_POLL_INTERVAL_MS);

    private static final String PROPERTIES_RESOURCE = "application.properties";
    private static final String ENV_FILE = ".env";

    private static final Map<String, String> SETTINGS = load();

    private TestConfig() {
    }

    public static String baseUri() {
        return require(BASE_URI);
    }

    public static String token() {
        return require(TOKEN);
    }

    /** {@code true}, если токен передан; иначе API-тесты пропускаются с понятным сообщением. */
    public static boolean hasToken() {
        return !get(TOKEN).isBlank();
    }

    /** Корневая папка на Диске, внутри которой создаются все тестовые данные. */
    public static String playgroundRoot() {
        return require(PLAYGROUND_ROOT);
    }

    /** Сколько ждать завершения асинхронной операции Диска. */
    public static Duration asyncTimeout() {
        return Duration.ofSeconds(requireLong(ASYNC_TIMEOUT_SECONDS));
    }

    /** Пауза между опросами статуса асинхронной операции. */
    public static Duration asyncPollInterval() {
        return Duration.ofMillis(requireLong(ASYNC_POLL_INTERVAL_MS));
    }

    public static String get(String key) {
        return SETTINGS.getOrDefault(key, "");
    }

    private static String require(String key) {
        String value = get(key);
        if (value.isBlank()) {
            throw new IllegalStateException("Не задан обязательный параметр '" + key
                    + "'. Передайте его через -D" + key + "=..., переменную окружения "
                    + envName(key) + " или файл .env в корне проекта");
        }
        return value;
    }

    private static long requireLong(String key) {
        String value = require(key);
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Параметр '" + key + "' должен быть числом, получено: " + value, e);
        }
    }

    private static Map<String, String> load() {
        Map<String, String> settings = new LinkedHashMap<>();
        putAll(settings, fromClasspath());
        putAll(settings, fromEnvFile());
        for (String key : KNOWN_KEYS) {
            putIfNotBlank(settings, key, System.getenv(envName(key)));
        }
        for (String key : KNOWN_KEYS) {
            putIfNotBlank(settings, key, System.getProperty(key));
        }
        return settings;
    }

    private static Properties fromClasspath() {
        Properties properties = new Properties();
        try (InputStream in = TestConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_RESOURCE)) {
            if (in != null) {
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать " + PROPERTIES_RESOURCE, e);
        }
        return properties;
    }

    /**
     * Файл {@code .env} в формате {@code YANDEX_DISK_TOKEN=y0_Ag...} — самый удобный способ
     * передать токен локально, потому что файл лежит в {@code .gitignore}.
     */
    private static Properties fromEnvFile() {
        Properties properties = new Properties();
        Path envFile = Path.of(ENV_FILE);
        if (!Files.isRegularFile(envFile)) {
            return properties;
        }
        try {
            for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                int separator = trimmed.indexOf('=');
                if (trimmed.isEmpty() || trimmed.startsWith("#") || separator < 1) {
                    continue;
                }
                String name = trimmed.substring(0, separator).trim();
                String value = unquote(trimmed.substring(separator + 1).trim());
                properties.setProperty(propertyName(name), value);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать файл " + ENV_FILE, e);
        }
        return properties;
    }

    private static void putAll(Map<String, String> settings, Properties properties) {
        properties.stringPropertyNames()
                .forEach(name -> putIfNotBlank(settings, name, properties.getProperty(name)));
    }

    private static void putIfNotBlank(Map<String, String> settings, String key, String value) {
        if (value != null && !value.isBlank()) {
            settings.put(key, value.trim());
        }
    }

    private static String unquote(String value) {
        boolean quoted = value.length() > 1
                && ((value.charAt(0) == '"' && value.endsWith("\""))
                || (value.charAt(0) == '\'' && value.endsWith("'")));
        return quoted ? value.substring(1, value.length() - 1) : value;
    }

    /** {@code yandex.disk.token} -&gt; {@code YANDEX_DISK_TOKEN}. */
    public static String envName(String propertyKey) {
        return propertyKey.replace('.', '_').toUpperCase();
    }

    /** {@code YANDEX_DISK_TOKEN} -&gt; {@code yandex.disk.token}. */
    public static String propertyName(String envName) {
        return envName.replace('_', '.').toLowerCase();
    }
}
