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
import ru.qa.disk.core.HttpStatus;
import ru.qa.disk.model.ApiError;
import ru.qa.disk.model.Resource;
import ru.qa.disk.util.Playground;
import ru.qa.disk.util.TestData;

import static org.assertj.core.api.Assertions.assertThat;

/** POST — копирование и перемещение: {@code /v1/disk/resources/copy} и {@code /move}. */
@Feature("Копирование и перемещение")
@DisplayName("POST /v1/disk/resources/copy и /move")
class CopyMovePostTest extends BaseResourceTest {

    private final ResourcesApi resourcesApi = new ResourcesApi();

    @Test
    @Story("Копирование")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Копирует файл: 201, оба файла на месте и с одинаковым содержимым")
    void copiesFile() {
        String sourcePath = inTestFolder(TestData.fileName("source", "txt"));
        Resource source = steps.uploadTextFile(sourcePath, TestData.textContent("copiesFile"));
        String copyPath = inTestFolder(TestData.fileName("copy", "txt"));

        resourcesApi.copy(sourcePath, copyPath, false).then().statusCode(HttpStatus.CREATED);

        Resource copy = steps.meta(copyPath);
        assertThat(copy.md5()).as("MD5 копии совпадает с исходным файлом").isEqualTo(source.md5());
        assertThat(copy.size()).as("размер копии").isEqualTo(source.size());
        assertThat(steps.meta(sourcePath).path()).as("исходный файл остался на месте").isEqualTo(sourcePath);
    }

    @Test
    @Story("Копирование")
    @DisplayName("Копирование на занятый путь: 409 без overwrite и 201 с overwrite=true")
    void copyToExistingPath() {
        String sourcePath = inTestFolder(TestData.fileName("source", "txt"));
        Resource source = steps.uploadTextFile(sourcePath, TestData.textContent("новое содержимое для перезаписи"));
        String targetPath = inTestFolder(TestData.fileName("target", "txt"));
        steps.uploadTextFile(targetPath, TestData.textContent("старое"));

        Response conflict = resourcesApi.copy(sourcePath, targetPath, false);
        conflict.then().statusCode(HttpStatus.CONFLICT);
        assertThat(conflict.as(ApiError.class).error()).as("код ошибки конфликта").isNotBlank();

        resourcesApi.copy(sourcePath, targetPath, true).then().statusCode(HttpStatus.CREATED);
        assertThat(steps.meta(targetPath).md5()).as("файл перезаписан копией").isEqualTo(source.md5());
    }

    @Test
    @Story("Копирование")
    @DisplayName("Копирование непустой папки: содержимое копируется целиком")
    void copiesNonEmptyFolder() {
        String sourceFolder = inTestFolder(TestData.folderName("with-content"));
        steps.createFolder(sourceFolder);
        steps.uploadTextFile(Playground.path(sourceFolder, "first.txt"), TestData.textContent("first"));
        steps.uploadTextFile(Playground.path(sourceFolder, "second.txt"), TestData.textContent("second"));
        String copyFolder = inTestFolder(TestData.folderName("copied"));

        // Непустая папка копируется асинхронно: возможен и 201, и 202 со ссылкой на операцию.
        Response response = resourcesApi.copy(sourceFolder, copyFolder, false);
        assertThat(response.statusCode()).as("код ответа при копировании папки")
                .isIn(HttpStatus.CREATED, HttpStatus.ACCEPTED);
        steps.awaitCompletion(response);

        Resource copy = steps.meta(copyFolder);
        assertThat(copy.isDir()).as("копия — папка").isTrue();
        assertThat(copy.embedded().names()).as("содержимое копии")
                .containsExactlyInAnyOrder("first.txt", "second.txt");
    }

    @Test
    @Story("Перемещение")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Перемещает файл: 201, по новому пути файл есть, по старому — нет")
    void movesFile() {
        String sourcePath = inTestFolder(TestData.fileName("before-move", "txt"));
        Resource source = steps.uploadTextFile(sourcePath, TestData.textContent("movesFile"));
        String targetPath = inTestFolder(TestData.fileName("after-move", "txt"));

        resourcesApi.move(sourcePath, targetPath, false).then().statusCode(HttpStatus.CREATED);

        Resource moved = steps.meta(targetPath);
        assertThat(moved.md5()).as("содержимое не изменилось").isEqualTo(source.md5());
        assertThat(moved.resourceId()).as("это тот же ресурс").isEqualTo(source.resourceId());
        steps.assertAbsent(sourcePath);
    }

    @Test
    @Story("Перемещение")
    @DisplayName("Перемещение непустой папки переносит её содержимое")
    void movesNonEmptyFolder() {
        String sourceFolder = inTestFolder(TestData.folderName("to-move"));
        steps.createFolder(sourceFolder);
        steps.uploadTextFile(Playground.path(sourceFolder, "inner.txt"), TestData.textContent("inner"));
        String targetFolder = inTestFolder(TestData.folderName("moved"));

        Response response = resourcesApi.move(sourceFolder, targetFolder, false);
        assertThat(response.statusCode()).as("код ответа при перемещении папки")
                .isIn(HttpStatus.CREATED, HttpStatus.ACCEPTED);
        steps.awaitCompletion(response);

        assertThat(steps.meta(targetFolder).embedded().names()).as("содержимое перемещённой папки")
                .containsExactly("inner.txt");
        steps.assertAbsent(sourceFolder);
    }

    @Test
    @Story("Негативные проверки")
    @DisplayName("Копирование несуществующего ресурса возвращает 404")
    void copyOfMissingResourceReturnsNotFound() {
        String missingPath = inTestFolder(TestData.fileName("missing", "txt"));

        Response response = resourcesApi.copy(missingPath, inTestFolder("copy.txt"), false);

        response.then().statusCode(HttpStatus.NOT_FOUND);
        assertThat(response.as(ApiError.class).error()).as("код ошибки").isNotBlank();
    }

    @Test
    @Story("Авторизация")
    @DisplayName("Без токена копирование возвращает 401 и копия не создаётся")
    void withoutTokenReturnsUnauthorized() {
        String sourcePath = inTestFolder(TestData.fileName("protected", "txt"));
        steps.uploadTextFile(sourcePath, TestData.textContent("withoutTokenReturnsUnauthorized"));
        String copyPath = inTestFolder(TestData.fileName("stolen", "txt"));

        new ResourcesApi(ApiSpec::anonymous).copy(sourcePath, copyPath, false)
                .then().statusCode(HttpStatus.UNAUTHORIZED);

        steps.assertAbsent(copyPath);
    }
}
