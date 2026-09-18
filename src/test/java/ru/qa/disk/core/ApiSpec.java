package ru.qa.disk.core;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import ru.qa.disk.config.TestConfig;

/**
 * Спецификации запросов к API Диска.
 *
 * <p>Здесь же один раз настраивается глобальный {@link RestAssured#config}:
 * <ul>
 *     <li>заголовок {@code Authorization} добавлен в blacklist — токен не утечёт
 *         в консольный лог при падении теста;</li>
 *     <li>при неуспешной валидации ответа REST Assured печатает запрос и ответ,
 *         поэтому причину падения видно сразу в выводе Maven;</li>
 *     <li>JSON разбирается тем же ObjectMapper, что и в офлайн-тестах маппинга.</li>
 * </ul>
 */
public final class ApiSpec {

    public static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "OAuth ";

    private static boolean initialized;

    private ApiSpec() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        RestAssured.config = RestAssuredConfig.config()
                .logConfig(LogConfig.logConfig()
                        .blacklistHeader(AUTH_HEADER)
                        .enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL))
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> JsonMapperFactory.mapper()));
        initialized = true;
    }

    /** Запрос с валидным токеном из настроек — основной вариант для позитивных проверок. */
    public static RequestSpecification authorized() {
        return withToken(TestConfig.token());
    }

    /** Запрос с произвольным токеном — нужен для негативных проверок авторизации. */
    public static RequestSpecification withToken(String token) {
        return base().addHeader(AUTH_HEADER, TOKEN_PREFIX + token).build();
    }

    /** Запрос вовсе без заголовка авторизации — ожидаем 401. */
    public static RequestSpecification anonymous() {
        return base().build();
    }

    private static RequestSpecBuilder base() {
        init();
        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.baseUri())
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRequestFilter());
    }
}
