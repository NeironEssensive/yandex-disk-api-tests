package ru.qa.disk.api;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import ru.qa.disk.core.Endpoints;

import java.util.function.Supplier;

/**
 * Статус асинхронной операции: {@code GET /v1/disk/operations/<id>}.
 *
 * <p>Асинхронными становятся операции с непустыми папками (копирование, перемещение,
 * удаление) и загрузка файла из интернета — API отвечает 202 и ссылкой на операцию.
 */
public class OperationsApi extends BaseApi {

    public OperationsApi() {
        super();
    }

    public OperationsApi(Supplier<RequestSpecification> specification) {
        super(specification);
    }

    @Step("GET /v1/disk/operations/{operationId} — статус операции")
    public Response getStatus(String operationId) {
        return request().get(Endpoints.OPERATIONS, operationId);
    }
}
