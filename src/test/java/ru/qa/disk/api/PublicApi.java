package ru.qa.disk.api;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import ru.qa.disk.core.Endpoints;

import java.util.Map;
import java.util.function.Supplier;

/** Публикация ресурсов и чтение метаданных публичного ресурса по {@code public_key}. */
public class PublicApi extends BaseApi {

    public PublicApi() {
        super();
    }

    public PublicApi(Supplier<RequestSpecification> specification) {
        super(specification);
    }

    @Step("PUT /v1/disk/resources/publish?path={path} — опубликовать ресурс")
    public Response publish(String path) {
        return request().queryParam("path", path).put(Endpoints.PUBLISH);
    }

    @Step("PUT /v1/disk/resources/unpublish?path={path} — снять публикацию")
    public Response unpublish(String path) {
        return request().queryParam("path", path).put(Endpoints.UNPUBLISH);
    }

    @Step("GET /v1/disk/public/resources — метаданные публичного ресурса")
    public Response getPublicMeta(String publicKey) {
        return getPublicMeta(publicKey, Map.of());
    }

    @Step("GET /v1/disk/public/resources с параметрами {params}")
    public Response getPublicMeta(String publicKey, Map<String, ?> params) {
        return request()
                .queryParam("public_key", publicKey)
                .queryParams(params)
                .get(Endpoints.PUBLIC_RESOURCES);
    }
}
