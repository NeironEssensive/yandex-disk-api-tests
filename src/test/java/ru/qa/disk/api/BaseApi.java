package ru.qa.disk.api;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import ru.qa.disk.core.ApiSpec;

import java.util.function.Supplier;

/**
 * Общая часть клиентов API: откуда брать спецификацию запроса.
 *
 * <p>По умолчанию это запрос с валидным токеном. Для негативных проверок тот же клиент
 * создаётся с другой спецификацией, например {@code new ResourcesApi(ApiSpec::anonymous)} —
 * так негативные тесты ходят по тем же методам, что и позитивные, без дублирования кода.
 */
public abstract class BaseApi {

    private final Supplier<RequestSpecification> specification;

    protected BaseApi() {
        this(ApiSpec::authorized);
    }

    protected BaseApi(Supplier<RequestSpecification> specification) {
        this.specification = specification;
    }

    protected RequestSpecification request() {
        return RestAssured.given().spec(specification.get());
    }
}
