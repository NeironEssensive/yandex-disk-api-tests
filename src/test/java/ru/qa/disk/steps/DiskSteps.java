package ru.qa.disk.steps;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import ru.qa.disk.api.OperationsApi;
import ru.qa.disk.api.ResourcesApi;
import ru.qa.disk.api.TrashApi;
import ru.qa.disk.api.UploadApi;
import ru.qa.disk.config.TestConfig;
import ru.qa.disk.core.HttpStatus;
import ru.qa.disk.model.Link;
import ru.qa.disk.model.Operation;
import ru.qa.disk.model.Resource;
import ru.qa.disk.util.Await;
import ru.qa.disk.util.Playground;
import ru.qa.disk.util.TestData;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Прикладные шаги над API — то, что в тесте относится к подготовке данных и проверке состояния.
 *
 * <p>Разделение сознательное: классы в {@code api} только отправляют запросы и ничего не проверяют,
 * а шаги здесь уже утверждают, что подготовка прошла успешно. Благодаря этому падение в
 * arrange-части теста читается как «не удалось создать папку», а не как невнятный NPE ниже.
 */
public class DiskSteps {

    private final ResourcesApi resources = new ResourcesApi();
    private final UploadApi upload = new UploadApi();
    private final TrashApi trash = new TrashApi();
    private final OperationsApi operations = new OperationsApi();

    @Step("Создать папку {path}")
    public Resource createFolder(String path) {
        resources.createFolder(path).then().statusCode(HttpStatus.CREATED);
        return meta(path);
    }

    @Step("Загрузить файл {path}")
    public Resource uploadFile(String path, byte[] content) {
        Link link = upload.getUploadLink(path, true)
                .then().statusCode(HttpStatus.OK)
                .extract().as(Link.class);
        upload.uploadContent(link.href(), content)
                .then().statusCode(HttpStatus.CREATED);
        return meta(path);
    }

    public Resource uploadTextFile(String path, String text) {
        return uploadFile(path, TestData.bytesOf(text));
    }

    @Step("Прочитать метаинформацию о {path}")
    public Resource meta(String path) {
        return resources.getMeta(path)
                .then().statusCode(HttpStatus.OK)
                .extract().as(Resource.class);
    }

    @Step("Проверить, что ресурса {path} на Диске больше нет")
    public void assertAbsent(String path) {
        resources.getMeta(path).then().statusCode(HttpStatus.NOT_FOUND);
    }

    @Step("Удалить {path} безвозвратно")
    public void deletePermanently(String path) {
        Response response = resources.delete(path, true);
        assertThat(response.statusCode())
                .as("код ответа при безвозвратном удалении " + path)
                .isIn(HttpStatus.NO_CONTENT, HttpStatus.ACCEPTED);
        awaitCompletion(response);
    }

    /** Уборка после теста: если удалить не удалось, тест из-за этого падать не должен. */
    public void deleteQuietly(String path) {
        try {
            awaitCompletion(resources.delete(path, true));
        } catch (Exception | AssertionError ignored) {
            // данные могли быть удалены самим тестом — это нормально
        }
    }

    /** Если API ответил 202, дожидается завершения асинхронной операции. */
    @Step("Дождаться завершения операции, если она асинхронная")
    public void awaitCompletion(Response response) {
        if (response.statusCode() != HttpStatus.ACCEPTED) {
            return;
        }
        String operationId = response.as(Link.class).asyncOperationId()
                .orElseThrow(() -> new AssertionError(
                        "Ответ 202 без идентификатора операции: " + response.asString()));
        awaitOperation(operationId);
    }

    @Step("Дождаться успешного завершения операции {operationId}")
    public Operation awaitOperation(String operationId) {
        Operation operation = Await.until(
                "операция " + operationId,
                TestConfig.asyncTimeout(),
                TestConfig.asyncPollInterval(),
                () -> operations.getStatus(operationId)
                        .then().statusCode(HttpStatus.OK)
                        .extract().as(Operation.class),
                result -> !result.isInProgress());
        assertThat(operation.status())
                .as("итоговый статус асинхронной операции")
                .isEqualTo(Operation.SUCCESS);
        return operation;
    }

    /**
     * Ищет в Корзине ресурс, удалённый из {@code originalPath}.
     *
     * <p>Путь в Корзине не совпадает с исходным, поэтому сначала пробуем очевидный
     * {@code trash:/<имя>}, а если там оказался другой ресурс — просматриваем список
     * по полю {@code origin_path}.
     */
    @Step("Найти в Корзине ресурс, удалённый из {originalPath}")
    public Resource findInTrash(String originalPath) {
        String name = Playground.name(originalPath);
        Response direct = trash.list("trash:/" + name, Map.of());
        if (direct.statusCode() == HttpStatus.OK) {
            Resource candidate = direct.as(Resource.class);
            if (originalPath.equals(candidate.originPath())) {
                return candidate;
            }
        }
        Resource trashRoot = trash.list("trash:/", Map.of("limit", 200))
                .then().statusCode(HttpStatus.OK)
                .extract().as(Resource.class);
        return trashRoot.embedded().items().stream()
                .filter(item -> originalPath.equals(item.originPath()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "В Корзине не нашёлся ресурс, удалённый из " + originalPath));
    }
}
