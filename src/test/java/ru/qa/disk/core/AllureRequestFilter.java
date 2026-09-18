package ru.qa.disk.core;

import io.qameta.allure.Allure;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * Прикладывает запрос и ответ к шагу в отчёте Allure.
 *
 * <p>Штатный {@code AllureRestAssured} логирует заголовки как есть, то есть вместе с
 * {@code Authorization: OAuth <токен>} — а отчёт обычно уезжает в артефакты CI и открыт всей команде.
 * Поэтому здесь свой фильтр: значение токена маскируется, тело большого/бинарного ответа
 * не прикладывается целиком.
 */
public class AllureRequestFilter implements OrderedFilter {

    private static final int MAX_BODY_LENGTH = 20_000;
    private static final String MASKED_VALUE = "OAuth ***";

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext context) {
        Response response = context.next(requestSpec, responseSpec);
        attach(requestSpec, response);
        return response;
    }

    /** Фильтр должен быть последним в цепочке, чтобы видеть итоговый запрос. */
    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    private void attach(FilterableRequestSpecification requestSpec, Response response) {
        // Отчёт не должен быть причиной падения теста, поэтому любые проблемы здесь игнорируем.
        try {
            if (Allure.getLifecycle().getCurrentTestCaseOrStep().isEmpty()) {
                return;
            }
            Allure.addAttachment(
                    requestSpec.getMethod() + " " + requestSpec.getURI(),
                    "text/plain",
                    renderRequest(requestSpec),
                    ".txt");
            Allure.addAttachment(
                    "Ответ " + response.statusCode(),
                    "application/json",
                    renderResponse(response),
                    ".json");
        } catch (Exception ignored) {
            // отчёт не критичен для результата проверки
        }
    }

    private String renderRequest(FilterableRequestSpecification requestSpec) {
        StringBuilder text = new StringBuilder()
                .append(requestSpec.getMethod()).append(' ').append(requestSpec.getURI()).append('\n');
        for (Header header : requestSpec.getHeaders()) {
            String value = ApiSpec.AUTH_HEADER.equalsIgnoreCase(header.getName()) ? MASKED_VALUE : header.getValue();
            text.append(header.getName()).append(": ").append(value).append('\n');
        }
        Object body = requestSpec.getBody();
        if (body instanceof byte[] bytes) {
            text.append("\n<двоичные данные, ").append(bytes.length).append(" байт>\n");
        } else if (body != null) {
            text.append('\n').append(truncate(String.valueOf(body))).append('\n');
        }
        return text.toString();
    }

    private String renderResponse(Response response) {
        String contentType = response.getContentType();
        if (contentType != null && !contentType.contains("json") && !contentType.startsWith("text")) {
            return "{\"note\": \"тело ответа не текстовое: " + contentType + "\"}";
        }
        String body = response.getBody().asString();
        return body == null || body.isBlank() ? "{}" : truncate(body);
    }

    private String truncate(String body) {
        return body.length() <= MAX_BODY_LENGTH
                ? body
                : body.substring(0, MAX_BODY_LENGTH) + "... (обрезано)";
    }
}
