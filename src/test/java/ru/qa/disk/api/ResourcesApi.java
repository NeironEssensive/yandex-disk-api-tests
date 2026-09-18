package ru.qa.disk.api;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import ru.qa.disk.core.AllureRequestFilter;
import ru.qa.disk.core.Endpoints;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Операции с файлами и папками на Диске: {@code /v1/disk/resources} и вложенные ресурсы.
 *
 * <p>Клиент намеренно возвращает «сырой» {@link Response} и ничего не проверяет:
 * решение о том, какой код ответа корректен, принимает тест. Значения параметров
 * кодируются REST Assured, поэтому пути передаются как есть: {@code disk:/папка/файл.txt}.
 */
public class ResourcesApi extends BaseApi {

    public ResourcesApi() {
        super();
    }

    public ResourcesApi(Supplier<RequestSpecification> specification) {
        super(specification);
    }

    @Step("GET /v1/disk/resources?path={path} — метаинформация о ресурсе")
    public Response getMeta(String path) {
        return getMeta(path, Map.of());
    }

    @Step("GET /v1/disk/resources?path={path} с параметрами {params}")
    public Response getMeta(String path, Map<String, ?> params) {
        return request()
                .queryParam("path", path)
                .queryParams(params)
                .get(Endpoints.RESOURCES);
    }

    @Step("PUT /v1/disk/resources?path={path} — создать папку")
    public Response createFolder(String path) {
        return request()
                .queryParam("path", path)
                .put(Endpoints.RESOURCES);
    }

    @Step("DELETE /v1/disk/resources?path={path}&permanently={permanently}")
    public Response delete(String path, boolean permanently) {
        return request()
                .queryParam("path", path)
                .queryParam("permanently", permanently)
                .delete(Endpoints.RESOURCES);
    }

    @Step("DELETE /v1/disk/resources?path={path} с параметрами {params}")
    public Response delete(String path, Map<String, ?> params) {
        return request()
                .queryParam("path", path)
                .queryParams(params)
                .delete(Endpoints.RESOURCES);
    }

    @Step("POST /v1/disk/resources/copy — копировать {from} в {to}")
    public Response copy(String from, String to, boolean overwrite) {
        return request()
                .queryParam("from", from)
                .queryParam("path", to)
                .queryParam("overwrite", overwrite)
                .post(Endpoints.COPY);
    }

    @Step("POST /v1/disk/resources/move — переместить {from} в {to}")
    public Response move(String from, String to, boolean overwrite) {
        return request()
                .queryParam("from", from)
                .queryParam("path", to)
                .queryParam("overwrite", overwrite)
                .post(Endpoints.MOVE);
    }

    @Step("PATCH /v1/disk/resources?path={path} — записать пользовательские свойства")
    public Response patchCustomProperties(String path, Object body) {
        return request()
                .contentType(ContentType.JSON)
                .queryParam("path", path)
                .body(body)
                .patch(Endpoints.RESOURCES);
    }

    @Step("GET /v1/disk/resources/files — плоский список файлов")
    public Response getFlatFileList(Map<String, ?> params) {
        return request().queryParams(params).get(Endpoints.FILES);
    }

    @Step("GET /v1/disk/resources/last-uploaded — последние загруженные файлы")
    public Response getLastUploaded(Map<String, ?> params) {
        return request().queryParams(params).get(Endpoints.LAST_UPLOADED);
    }

    @Step("GET /v1/disk/resources/download?path={path} — ссылка на скачивание")
    public Response getDownloadLink(String path) {
        return request().queryParam("path", path).get(Endpoints.DOWNLOAD);
    }

    /**
     * Скачивание содержимого по временной ссылке.
     *
     * <p>Как и при загрузке, ссылка подписана, поэтому повторное кодирование URL отключено.
     * Ссылка ведёт на отдельный хост и авторизации не требует.
     */
    @Step("GET по ссылке скачивания — получить содержимое файла")
    public Response downloadContent(String downloadHref) {
        return RestAssured.given()
                .urlEncodingEnabled(false)
                .filter(new AllureRequestFilter())
                .get(downloadHref);
    }
}
