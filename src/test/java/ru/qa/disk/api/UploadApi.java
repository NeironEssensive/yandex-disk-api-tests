package ru.qa.disk.api;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import ru.qa.disk.core.AllureRequestFilter;
import ru.qa.disk.core.Endpoints;

import java.util.function.Supplier;

/**
 * Загрузка файлов на Диск.
 *
 * <p>Штатный сценарий двухшаговый: {@code GET /v1/disk/resources/upload} отдаёт временную
 * ссылку, и уже по ней файл отправляется методом PUT. Второй способ — попросить Диск
 * скачать файл из интернета: {@code POST /v1/disk/resources/upload?url=...}, ответ 202
 * и асинхронная операция.
 */
public class UploadApi extends BaseApi {

    public UploadApi() {
        super();
    }

    public UploadApi(Supplier<RequestSpecification> specification) {
        super(specification);
    }

    @Step("GET /v1/disk/resources/upload?path={path}&overwrite={overwrite} — получить ссылку для загрузки")
    public Response getUploadLink(String path, boolean overwrite) {
        return request()
                .queryParam("path", path)
                .queryParam("overwrite", overwrite)
                .get(Endpoints.UPLOAD);
    }

    /**
     * Отправка содержимого файла по временной ссылке.
     *
     * <p>Ссылка уже содержит подписанные query-параметры, поэтому кодирование URL
     * обязательно отключить: иначе REST Assured закодирует ссылку повторно и подпись сломается.
     * Токен здесь не нужен — доступ даёт сама ссылка.
     */
    @Step("PUT по ссылке загрузки — отправить содержимое файла")
    public Response uploadContent(String uploadHref, byte[] content) {
        return RestAssured.given()
                .urlEncodingEnabled(false)
                .filter(new AllureRequestFilter())
                .contentType(ContentType.BINARY)
                .body(content)
                .put(uploadHref);
    }

    @Step("POST /v1/disk/resources/upload — попросить Диск скачать файл по ссылке в {path}")
    public Response uploadFromUrl(String sourceUrl, String path) {
        return request()
                .queryParam("url", sourceUrl)
                .queryParam("path", path)
                .post(Endpoints.UPLOAD);
    }
}
