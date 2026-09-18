package ru.qa.disk.api;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import ru.qa.disk.core.Endpoints;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Корзина: {@code /v1/disk/trash/resources}.
 *
 * <p>Пути в Корзине начинаются с {@code trash:/} и не совпадают с исходными: у ресурса
 * появляется поле {@code origin_path}, по которому его и нужно искать.
 *
 * <p>Метод «очистить Корзину целиком» здесь сознательно не реализован: тесты работают
 * на живом аккаунте и не должны удалять то, что положил туда не тест.
 */
public class TrashApi extends BaseApi {

    public TrashApi() {
        super();
    }

    public TrashApi(Supplier<RequestSpecification> specification) {
        super(specification);
    }

    @Step("GET /v1/disk/trash/resources?path={path} — содержимое Корзины")
    public Response list(String path, Map<String, ?> params) {
        return request()
                .queryParam("path", path)
                .queryParams(params)
                .get(Endpoints.TRASH_RESOURCES);
    }

    @Step("PUT /v1/disk/trash/resources/restore?path={trashPath} — восстановить из Корзины")
    public Response restore(String trashPath, Map<String, ?> params) {
        return request()
                .queryParam("path", trashPath)
                .queryParams(params)
                .put(Endpoints.TRASH_RESTORE);
    }

    @Step("DELETE /v1/disk/trash/resources?path={trashPath} — удалить из Корзины безвозвратно")
    public Response delete(String trashPath) {
        return request()
                .queryParam("path", trashPath)
                .delete(Endpoints.TRASH_RESOURCES);
    }
}
