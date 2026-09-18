package ru.qa.disk.api;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import ru.qa.disk.core.Endpoints;

import java.util.function.Supplier;

/** {@code GET /v1/disk/} — данные о Диске пользователя. */
public class DiskInfoApi extends BaseApi {

    public DiskInfoApi() {
        super();
    }

    public DiskInfoApi(Supplier<RequestSpecification> specification) {
        super(specification);
    }

    @Step("GET /v1/disk/ — общая информация о Диске")
    public Response getDiskInfo() {
        return request().get(Endpoints.DISK);
    }

    @Step("GET /v1/disk/?fields={fields} — только выбранные поля")
    public Response getDiskInfo(String fields) {
        return request().queryParam("fields", fields).get(Endpoints.DISK);
    }
}
