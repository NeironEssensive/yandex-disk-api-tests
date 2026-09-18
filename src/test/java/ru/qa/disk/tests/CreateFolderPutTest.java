package ru.qa.disk.tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qa.disk.api.ResourcesApi;
import ru.qa.disk.core.ApiSpec;
import ru.qa.disk.core.Endpoints;
import ru.qa.disk.core.HttpStatus;
import ru.qa.disk.model.ApiError;
import ru.qa.disk.model.Link;
import ru.qa.disk.model.Resource;
import ru.qa.disk.util.Playground;
import ru.qa.disk.util.TestData;

import static org.assertj.core.api.Assertions.assertThat;

/** PUT — создание папок: {@code PUT /v1/disk/resources}. */
@Feature("Папки")
@DisplayName("PUT /v1/disk/resources — создание папки")
class CreateFolderPutTest extends BaseResourceTest {

    private final ResourcesApi resourcesApi = new ResourcesApi();

    @Test
    @Story("Создание папки")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Создаёт папку: 201, ссылка на метаданные и папка видна на Диске")
    void createsFolder() {
        String folderName = TestData.folderName("new-folder");
        String folderPath = inTestFolder(folderName);

        Response response = resourcesApi.createFolder(folderPath);

        response.then().statusCode(HttpStatus.CREATED);
        Link link = response.as(Link.class);
        assertThat(link.href()).as("ссылка на метаданные созданной папки").contains(Endpoints.RESOURCES);
        assertThat(link.method()).as("метод, которым следует идти по ссылке").isEqualTo("GET");
        assertThat(link.templated()).as("ссылка не шаблонная").isFalse();

        Resource created = steps.meta(folderPath);
        assertThat(created.name()).as("имя папки").isEqualTo(folderName);
        assertThat(created.path()).as("путь папки").isEqualTo(folderPath);
        assertThat(created.isDir()).as("тип ресурса — папка").isTrue();
        assertThat(created.created()).as("дата создания").isNotNull();
    }

    @Test
    @Story("Создание папки")
    @DisplayName("Вложенная папка появляется в содержимом родительской")
    void createsNestedFolder() {
        String parentPath = inTestFolder(TestData.folderName("parent"));
        steps.createFolder(parentPath);
        String childPath = Playground.path(parentPath, "child");

        resourcesApi.createFolder(childPath).then().statusCode(HttpStatus.CREATED);

        Resource parent = steps.meta(parentPath);
        assertThat(parent.embedded()).as("содержимое родительской папки").isNotNull();
        assertThat(parent.embedded().names()).as("имена вложенных ресурсов").containsExactly("child");
        assertThat(parent.embedded().total()).as("всего вложенных ресурсов").isEqualTo(1);
    }

    @Test
    @Story("Негативные проверки")
    @DisplayName("Повторное создание той же папки возвращает 409")
    void duplicateFolderReturnsConflict() {
        String folderPath = inTestFolder(TestData.folderName("duplicate"));
        resourcesApi.createFolder(folderPath).then().statusCode(HttpStatus.CREATED);

        Response response = resourcesApi.createFolder(folderPath);

        response.then().statusCode(HttpStatus.CONFLICT);
        ApiError error = response.as(ApiError.class);
        assertThat(error.error()).as("машинный код ошибки").isNotBlank();
        assertThat(error.description()).as("описание ошибки").isNotBlank();
    }

    @Test
    @Story("Негативные проверки")
    @DisplayName("Создание папки без существующей родительской возвращает 409")
    void missingParentReturnsConflict() {
        String orphanPath = Playground.path(inTestFolder(TestData.folderName("ghost")), "child");

        resourcesApi.createFolder(orphanPath).then().statusCode(HttpStatus.CONFLICT);

        steps.assertAbsent(orphanPath);
    }

    @Test
    @Story("Негативные проверки")
    @DisplayName("Создание папки на месте существующего файла возвращает 409")
    void folderOverExistingFileReturnsConflict() {
        String filePath = inTestFolder(TestData.fileName("occupied", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("folderOverExistingFileReturnsConflict"));

        resourcesApi.createFolder(filePath).then().statusCode(HttpStatus.CONFLICT);

        assertThat(steps.meta(filePath).isFile()).as("файл остался файлом").isTrue();
    }

    @Test
    @Story("Авторизация")
    @DisplayName("Без токена папка не создаётся: 401 и на Диске ничего не появилось")
    void withoutTokenReturnsUnauthorized() {
        String folderPath = inTestFolder(TestData.folderName("unauthorized"));

        new ResourcesApi(ApiSpec::anonymous).createFolder(folderPath)
                .then().statusCode(HttpStatus.UNAUTHORIZED);

        steps.assertAbsent(folderPath);
    }
}
