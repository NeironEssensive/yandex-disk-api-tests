package ru.qa.disk.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.qa.disk.config.TestConfig;
import ru.qa.disk.core.JsonMapperFactory;
import ru.qa.disk.model.ApiError;
import ru.qa.disk.model.DiskInfo;
import ru.qa.disk.model.Link;
import ru.qa.disk.model.Operation;
import ru.qa.disk.model.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Офлайн-проверки: разбор сохранённых ответов API в модели и соответствие JSON-схемам.
 *
 * <p>Зачем это нужно отдельно от API-тестов:
 * <ul>
 *     <li>маппинг snake_case в camelCase, {@code _embedded} и даты проверяются без сети,
 *         поэтому такая ошибка ловится за секунды и не выглядит как «упал API»;</li>
 *     <li>эти тесты проходят без токена, значит {@code mvn test} что-то проверяет
 *         даже у того, кто только что клонировал репозиторий;</li>
 *     <li>фикстуры заодно документируют, как реально выглядят ответы Диска.</li>
 * </ul>
 */
@Tag("offline")
@Feature("Модели и контракты")
@DisplayName("Разбор ответов API без обращения к сети")
class JsonMappingOfflineTest {

    @Test
    @Story("Данные о Диске")
    @DisplayName("Ответ GET /v1/disk/ разбирается в модель DiskInfo")
    void mapsDiskInfo() throws JsonProcessingException {
        DiskInfo diskInfo = JsonMapperFactory.mapper().readValue(fixture("disk-info.json"), DiskInfo.class);

        assertThat(diskInfo.totalSpace()).as("total_space").isEqualTo(10_737_418_240L);
        assertThat(diskInfo.usedSpace()).as("used_space").isEqualTo(45_248_937L);
        assertThat(diskInfo.trashSize()).as("trash_size").isEqualTo(4938L);
        assertThat(diskInfo.maxFileSize()).as("max_file_size").isEqualTo(53_687_091_200L);
        assertThat(diskInfo.isPaid()).as("is_paid").isFalse();
        assertThat(diskInfo.freeSpace()).as("свободное место").isEqualTo(10_692_169_303L);
        assertThat(diskInfo.systemFolders().applications()).as("system_folders.applications")
                .isEqualTo("disk:/Приложения");
        assertThat(diskInfo.user().displayName()).as("user.display_name").isEqualTo("QA Autotest");
        assertThat(diskInfo.user().uid()).as("user.uid").isEqualTo("1130000000000000");
    }

    @Test
    @Story("Устойчивость к изменениям API")
    @DisplayName("Неизвестные поля в ответе не ломают разбор")
    void ignoresUnknownFields() throws JsonProcessingException {
        // В фикстуре system_folders содержит больше папок, чем описано в модели.
        DiskInfo diskInfo = JsonMapperFactory.mapper().readValue(fixture("disk-info.json"), DiskInfo.class);

        assertThat(diskInfo.systemFolders().downloads()).as("известное поле разобрано")
                .isEqualTo("disk:/Загрузки/");
    }

    @Test
    @Story("Метаданные ресурса")
    @DisplayName("Ответ GET /v1/disk/resources разбирается вместе с _embedded и датами")
    void mapsResourceWithEmbedded() throws JsonProcessingException {
        Resource folder = JsonMapperFactory.mapper().readValue(fixture("resource-folder.json"), Resource.class);

        assertThat(folder.isDir()).as("тип ресурса").isTrue();
        assertThat(folder.path()).as("path").isEqualTo("disk:/qa-autotests/demo");
        assertThat(folder.created()).as("created как дата")
                .isEqualTo(OffsetDateTime.parse("2026-09-18T10:15:30+00:00"));
        assertThat(folder.customProperties()).as("custom_properties")
                .containsEntry("qa_owner", "autotest");

        assertThat(folder.embedded()).as("_embedded").isNotNull();
        assertThat(folder.embedded().total()).as("_embedded.total").isEqualTo(2);
        assertThat(folder.embedded().names()).as("имена вложенных ресурсов")
                .containsExactly("report.txt", "nested");

        Resource file = folder.embedded().items().get(0);
        assertThat(file.isFile()).as("вложенный ресурс — файл").isTrue();
        assertThat(file.size()).as("size").isEqualTo(1024L);
        assertThat(file.md5()).as("md5").isEqualTo("e2fc714c4727ee9395f324cd2e7f331f");
        assertThat(file.mimeType()).as("mime_type").isEqualTo("text/plain");
    }

    @Test
    @Story("Асинхронные операции")
    @DisplayName("Из ссылки на операцию извлекается её идентификатор")
    void extractsOperationIdFromLink() throws JsonProcessingException {
        Link link = JsonMapperFactory.mapper().readValue(fixture("link-async.json"), Link.class);

        assertThat(link.method()).as("метод").isEqualTo("GET");
        assertThat(link.templated()).as("templated").isFalse();
        assertThat(link.asyncOperationId()).as("идентификатор операции из href")
                .contains("f0a1b2c3d4e5f6a7b8c9");
    }

    @ParameterizedTest(name = "статус {0}")
    @CsvSource({"success, true, false", "failed, false, true", "in-progress, false, false"})
    @Story("Асинхронные операции")
    @DisplayName("Статусы операции распознаются корректно")
    void recognizesOperationStatuses(String status, boolean success, boolean failed) {
        Operation operation = new Operation(status);

        assertThat(operation.isSuccess()).as("success").isEqualTo(success);
        assertThat(operation.isFailed()).as("failed").isEqualTo(failed);
        assertThat(operation.isInProgress()).as("in-progress").isEqualTo(!success && !failed);
    }

    @Test
    @Story("Ошибки API")
    @DisplayName("Тело ошибки разбирается в модель ApiError")
    void mapsApiError() throws JsonProcessingException {
        ApiError error = JsonMapperFactory.mapper().readValue(fixture("error-not-found.json"), ApiError.class);

        assertThat(error.error()).as("error").isEqualTo("DiskNotFoundError");
        assertThat(error.description()).as("description").isEqualTo("Resource not found.");
        assertThat(error.message()).as("message").isNotBlank();
    }

    @Test
    @Story("JSON-схемы")
    @DisplayName("Фикстуры соответствуют JSON-схемам, которыми проверяются реальные ответы")
    void fixturesMatchSchemas() {
        MatcherAssert.assertThat(fixture("disk-info.json"),
                matchesJsonSchemaInClasspath("schemas/disk-info.json"));
        MatcherAssert.assertThat(fixture("resource-folder.json"),
                matchesJsonSchemaInClasspath("schemas/resource.json"));
    }

    @ParameterizedTest(name = "{0} <-> {1}")
    @CsvSource({
            "yandex.disk.token, YANDEX_DISK_TOKEN",
            "yandex.disk.base.uri, YANDEX_DISK_BASE_URI",
            "async.timeout.seconds, ASYNC_TIMEOUT_SECONDS"})
    @Story("Настройки")
    @DisplayName("Имя параметра и имя переменной окружения переводятся друг в друга однозначно")
    void propertyAndEnvNamesAreSymmetric(String propertyKey, String envName) {
        assertThat(TestConfig.envName(propertyKey)).as("имя переменной окружения").isEqualTo(envName);
        assertThat(TestConfig.propertyName(envName)).as("имя параметра").isEqualTo(propertyKey);
    }

    @Test
    @Story("Настройки")
    @DisplayName("Значения по умолчанию прочитаны из application.properties")
    void readsDefaultSettings() {
        assertThat(TestConfig.baseUri()).as("базовый адрес API").isEqualTo("https://cloud-api.yandex.net");
        assertThat(TestConfig.playgroundRoot()).as("корень песочницы").startsWith("disk:/");
        assertThat(TestConfig.asyncTimeout().toSeconds()).as("таймаут асинхронной операции").isPositive();
        assertThat(TestConfig.asyncPollInterval().toMillis()).as("интервал опроса").isPositive();
    }

    private static String fixture(String name) {
        String resource = "fixtures/" + name;
        try (InputStream in = JsonMappingOfflineTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Не найдена фикстура " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать фикстуру " + resource, e);
        }
    }
}
