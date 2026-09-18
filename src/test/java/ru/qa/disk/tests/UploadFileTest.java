package ru.qa.disk.tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qa.disk.api.ResourcesApi;
import ru.qa.disk.api.UploadApi;
import ru.qa.disk.core.ApiSpec;
import ru.qa.disk.core.HttpStatus;
import ru.qa.disk.model.Link;
import ru.qa.disk.model.Resource;
import ru.qa.disk.util.TestData;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Загрузка файлов — здесь задействованы сразу три метода:
 * {@code GET /resources/upload} (ссылка), {@code PUT} по ссылке (содержимое),
 * {@code POST /resources/upload} (скачать файл по ссылке из интернета).
 */
@Feature("Загрузка файлов")
@DisplayName("Загрузка файлов на Диск")
class UploadFileTest extends BaseResourceTest {

    private final UploadApi uploadApi = new UploadApi();
    private final ResourcesApi resourcesApi = new ResourcesApi();

    @Test
    @Story("Ссылка для загрузки")
    @DisplayName("GET /resources/upload возвращает 200 и временную ссылку с методом PUT")
    void returnsUploadLink() {
        String filePath = inTestFolder(TestData.fileName("upload-link", "txt"));

        Response response = uploadApi.getUploadLink(filePath, false);

        response.then().statusCode(HttpStatus.OK);
        Link link = response.as(Link.class);
        assertThat(link.href()).as("ссылка для загрузки").startsWith("https://");
        assertThat(link.method()).as("метод загрузки").isEqualTo("PUT");
        assertThat(link.operationId()).as("идентификатор операции загрузки").isNotBlank();
    }

    @Test
    @Story("Загрузка содержимого")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("PUT по ссылке загружает файл: 201, размер и MD5 на Диске совпадают с исходными")
    void uploadsFileByLink() {
        String filePath = inTestFolder(TestData.fileName("payload", "txt"));
        byte[] content = TestData.bytesOf(TestData.textContent("uploadsFileByLink"));

        Link link = uploadApi.getUploadLink(filePath, false)
                .then().statusCode(HttpStatus.OK)
                .extract().as(Link.class);
        uploadApi.uploadContent(link.href(), content)
                .then().statusCode(HttpStatus.CREATED);

        Resource uploaded = steps.meta(filePath);
        assertThat(uploaded.isFile()).as("тип ресурса — файл").isTrue();
        assertThat(uploaded.size()).as("размер файла").isEqualTo((long) content.length);
        assertThat(uploaded.md5()).as("MD5 содержимого").isEqualToIgnoringCase(TestData.md5Hex(content));
        assertThat(uploaded.mimeType()).as("MIME-тип").isNotBlank();
    }

    @Test
    @Story("Загрузка содержимого")
    @DisplayName("Загрузка с overwrite=true заменяет содержимое файла")
    void uploadWithOverwriteReplacesContent() {
        String filePath = inTestFolder(TestData.fileName("overwritten", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("первая версия"));
        byte[] newContent = TestData.bytesOf(TestData.textContent("вторая версия, заметно длиннее первой"));

        Link link = uploadApi.getUploadLink(filePath, true)
                .then().statusCode(HttpStatus.OK)
                .extract().as(Link.class);
        uploadApi.uploadContent(link.href(), newContent)
                .then().statusCode(HttpStatus.CREATED);

        Resource uploaded = steps.meta(filePath);
        assertThat(uploaded.size()).as("размер после перезаписи").isEqualTo((long) newContent.length);
        assertThat(uploaded.md5()).as("MD5 после перезаписи").isEqualToIgnoringCase(TestData.md5Hex(newContent));
    }

    @Test
    @Story("Негативные проверки")
    @DisplayName("Ссылка для существующего файла с overwrite=false возвращает 409")
    void uploadLinkForExistingFileReturnsConflict() {
        String filePath = inTestFolder(TestData.fileName("existing", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("uploadLinkForExistingFileReturnsConflict"));

        uploadApi.getUploadLink(filePath, false).then().statusCode(HttpStatus.CONFLICT);
    }

    @Test
    @Story("Загрузка по ссылке из интернета")
    @DisplayName("POST /resources/upload скачивает файл по ссылке: 202 и успешная асинхронная операция")
    void uploadsFromUrl() {
        // Источником служит файл, уже загруженный на Диск: ссылка на скачивание работает без
        // авторизации, поэтому тест не зависит от сторонних хостов.
        String sourcePath = inTestFolder(TestData.fileName("source", "txt"));
        byte[] content = TestData.bytesOf(TestData.textContent("uploadsFromUrl"));
        steps.uploadFile(sourcePath, content);
        String sourceUrl = resourcesApi.getDownloadLink(sourcePath)
                .then().statusCode(HttpStatus.OK)
                .extract().as(Link.class)
                .href();
        String targetPath = inTestFolder(TestData.fileName("from-url", "txt"));

        Response response = uploadApi.uploadFromUrl(sourceUrl, targetPath);

        response.then().statusCode(HttpStatus.ACCEPTED);
        String operationId = response.as(Link.class).asyncOperationId()
                .orElseThrow(() -> new AssertionError("В ответе 202 нет идентификатора операции"));
        steps.awaitOperation(operationId);

        Resource downloaded = steps.meta(targetPath);
        assertThat(downloaded.size()).as("размер скачанного файла").isEqualTo((long) content.length);
        assertThat(downloaded.md5()).as("MD5 скачанного файла").isEqualToIgnoringCase(TestData.md5Hex(content));
    }

    @Test
    @Story("Скачивание")
    @DisplayName("GET /resources/download отдаёт ссылку, по которой файл скачивается без изменений")
    void downloadsUploadedFile() {
        String filePath = inTestFolder(TestData.fileName("downloaded", "bin"));
        byte[] content = TestData.randomBytes(2048);
        steps.uploadFile(filePath, content);

        Link link = resourcesApi.getDownloadLink(filePath)
                .then().statusCode(HttpStatus.OK)
                .extract().as(Link.class);
        assertThat(link.method()).as("метод скачивания").isEqualTo("GET");
        byte[] downloaded = resourcesApi.downloadContent(link.href())
                .then().statusCode(HttpStatus.OK)
                .extract().asByteArray();

        assertThat(downloaded).as("скачанное содержимое совпадает с загруженным").isEqualTo(content);
    }

    @Test
    @Story("Авторизация")
    @DisplayName("Без токена ссылка для загрузки не выдаётся: 401")
    void withoutTokenReturnsUnauthorized() {
        new UploadApi(ApiSpec::anonymous)
                .getUploadLink(inTestFolder(TestData.fileName("unauthorized", "txt")), false)
                .then().statusCode(HttpStatus.UNAUTHORIZED);
    }
}
