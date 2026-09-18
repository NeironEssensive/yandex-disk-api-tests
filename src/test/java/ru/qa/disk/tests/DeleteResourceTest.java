package ru.qa.disk.tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qa.disk.api.ResourcesApi;
import ru.qa.disk.api.TrashApi;
import ru.qa.disk.core.ApiSpec;
import ru.qa.disk.core.HttpStatus;
import ru.qa.disk.model.ApiError;
import ru.qa.disk.model.Resource;
import ru.qa.disk.util.Playground;
import ru.qa.disk.util.TestData;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DELETE — удаление ресурсов и работа с Корзиной.
 *
 * <p>Тесты удаляют только то, что сами создали: очистка Корзины целиком не выполняется,
 * потому что прогон идёт на живом аккаунте.
 */
@Feature("Удаление и Корзина")
@DisplayName("DELETE /v1/disk/resources и Корзина")
class DeleteResourceTest extends BaseResourceTest {

    private final ResourcesApi resourcesApi = new ResourcesApi();
    private final TrashApi trashApi = new TrashApi();

    @Test
    @Story("Удаление в Корзину")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Удаляет файл в Корзину: 204, на Диске файла нет, в Корзине есть")
    void deletesFileToTrash() {
        String filePath = inTestFolder(TestData.fileName("to-trash", "txt"));
        Resource file = steps.uploadTextFile(filePath, TestData.textContent("deletesFileToTrash"));

        resourcesApi.delete(filePath, false).then().statusCode(HttpStatus.NO_CONTENT);

        steps.assertAbsent(filePath);
        Resource inTrash = steps.findInTrash(filePath);
        assertThat(inTrash.originPath()).as("исходный путь удалённого файла").isEqualTo(filePath);
        assertThat(inTrash.md5()).as("содержимое в Корзине то же").isEqualTo(file.md5());
        assertThat(inTrash.path()).as("путь в Корзине").startsWith("trash:/");

        trashApi.delete(inTrash.path()).then().statusCode(HttpStatus.NO_CONTENT);
    }

    @Test
    @Story("Восстановление из Корзины")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Восстанавливает файл из Корзины: 201 и файл снова на Диске")
    void restoresFileFromTrash() {
        String filePath = inTestFolder(TestData.fileName("restored", "txt"));
        Resource file = steps.uploadTextFile(filePath, TestData.textContent("restoresFileFromTrash"));
        resourcesApi.delete(filePath, false).then().statusCode(HttpStatus.NO_CONTENT);
        Resource inTrash = steps.findInTrash(filePath);

        Response response = trashApi.restore(inTrash.path(), Map.of("overwrite", false));

        assertThat(response.statusCode()).as("код ответа при восстановлении")
                .isIn(HttpStatus.CREATED, HttpStatus.ACCEPTED);
        steps.awaitCompletion(response);
        Resource restored = steps.meta(filePath);
        assertThat(restored.md5()).as("содержимое после восстановления").isEqualTo(file.md5());
    }

    @Test
    @Story("Безвозвратное удаление")
    @DisplayName("Удаление с permanently=true не оставляет файл в Корзине")
    void deletesFilePermanently() {
        String filePath = inTestFolder(TestData.fileName("permanent", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("deletesFilePermanently"));

        resourcesApi.delete(filePath, true).then().statusCode(HttpStatus.NO_CONTENT);

        steps.assertAbsent(filePath);
        trashApi.list("trash:/" + Playground.name(filePath), Map.of())
                .then().statusCode(HttpStatus.NOT_FOUND);
    }

    @Test
    @Story("Удаление папки")
    @DisplayName("Удаление непустой папки выполняется асинхронно и удаляет вложенные файлы")
    void deletesNonEmptyFolder() {
        String folderPath = inTestFolder(TestData.folderName("with-files"));
        steps.createFolder(folderPath);
        String innerFile = Playground.path(folderPath, "inner.txt");
        steps.uploadTextFile(innerFile, TestData.textContent("deletesNonEmptyFolder"));

        // Непустая папка удаляется в фоне: API отвечает 202 и ссылкой на операцию.
        Response response = resourcesApi.delete(folderPath, Map.of("permanently", true, "force_async", true));
        response.then().statusCode(HttpStatus.ACCEPTED);
        steps.awaitCompletion(response);

        steps.assertAbsent(folderPath);
        steps.assertAbsent(innerFile);
    }

    @Test
    @Story("Негативные проверки")
    @DisplayName("Удаление несуществующего ресурса возвращает 404")
    void deleteOfMissingResourceReturnsNotFound() {
        Response response = resourcesApi.delete(inTestFolder(TestData.fileName("ghost", "txt")), false);

        response.then().statusCode(HttpStatus.NOT_FOUND);
        ApiError error = response.as(ApiError.class);
        assertThat(error.error()).as("машинный код ошибки").isNotBlank();
        assertThat(error.message()).as("сообщение об ошибке").isNotBlank();
    }

    @Test
    @Story("Авторизация")
    @DisplayName("Без токена файл не удаляется: 401 и файл остаётся на Диске")
    void withoutTokenReturnsUnauthorized() {
        String filePath = inTestFolder(TestData.fileName("protected", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("withoutTokenReturnsUnauthorized"));

        new ResourcesApi(ApiSpec::anonymous).delete(filePath, true)
                .then().statusCode(HttpStatus.UNAUTHORIZED);

        assertThat(steps.meta(filePath).path()).as("файл на месте").isEqualTo(filePath);
    }
}
