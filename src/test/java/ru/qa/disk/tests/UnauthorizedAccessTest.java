package ru.qa.disk.tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.qa.disk.api.DiskInfoApi;
import ru.qa.disk.api.PublicApi;
import ru.qa.disk.api.ResourcesApi;
import ru.qa.disk.api.TrashApi;
import ru.qa.disk.api.UploadApi;
import ru.qa.disk.core.ApiSpec;
import ru.qa.disk.core.HttpStatus;
import ru.qa.disk.model.ApiError;
import ru.qa.disk.util.Playground;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Единая проверка авторизации для всех методов сразу.
 *
 * <p>Тесту не нужен токен: он специально ходит без заголовка {@code Authorization}, поэтому
 * запускается даже на «чистой» машине, где токен не настроен — удобно как smoke-проверка
 * доступности API.
 */
@Tag("api")
@Feature("Авторизация")
@DisplayName("Запросы без OAuth-токена")
class UnauthorizedAccessTest {

    private static final String ANY_PATH = Playground.path(Playground.root(), "any-resource.txt");

    @BeforeAll
    static void initSpecs() {
        ApiSpec.init();
    }

    static Stream<Arguments> anonymousRequests() {
        return Stream.of(
                Arguments.of("GET /v1/disk/",
                        (Supplier<Response>) () -> new DiskInfoApi(ApiSpec::anonymous).getDiskInfo()),
                Arguments.of("GET /v1/disk/resources",
                        (Supplier<Response>) () -> new ResourcesApi(ApiSpec::anonymous).getMeta(ANY_PATH)),
                Arguments.of("PUT /v1/disk/resources",
                        (Supplier<Response>) () -> new ResourcesApi(ApiSpec::anonymous).createFolder(ANY_PATH)),
                Arguments.of("POST /v1/disk/resources/copy",
                        (Supplier<Response>) () -> new ResourcesApi(ApiSpec::anonymous)
                                .copy(ANY_PATH, ANY_PATH + ".copy", false)),
                Arguments.of("POST /v1/disk/resources/move",
                        (Supplier<Response>) () -> new ResourcesApi(ApiSpec::anonymous)
                                .move(ANY_PATH, ANY_PATH + ".moved", false)),
                Arguments.of("DELETE /v1/disk/resources",
                        (Supplier<Response>) () -> new ResourcesApi(ApiSpec::anonymous).delete(ANY_PATH, true)),
                Arguments.of("PATCH /v1/disk/resources",
                        (Supplier<Response>) () -> new ResourcesApi(ApiSpec::anonymous)
                                .patchCustomProperties(ANY_PATH, Map.of("custom_properties", Map.of("a", "b")))),
                Arguments.of("GET /v1/disk/resources/upload",
                        (Supplier<Response>) () -> new UploadApi(ApiSpec::anonymous).getUploadLink(ANY_PATH, false)),
                Arguments.of("PUT /v1/disk/resources/publish",
                        (Supplier<Response>) () -> new PublicApi(ApiSpec::anonymous).publish(ANY_PATH)),
                Arguments.of("GET /v1/disk/trash/resources",
                        (Supplier<Response>) () -> new TrashApi(ApiSpec::anonymous).list("trash:/", Map.of())));
    }

    @ParameterizedTest(name = "{0} без токена -> 401")
    @MethodSource("anonymousRequests")
    @Story("Все методы требуют токен")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("API отвечает 401 и телом с описанием ошибки")
    void returnsUnauthorized(String requestDescription, Supplier<Response> request) {
        Response response = request.get();

        response.then().statusCode(HttpStatus.UNAUTHORIZED);
        ApiError error = response.as(ApiError.class);
        assertThat(error.error()).as("машинный код ошибки для " + requestDescription).isNotBlank();
        assertThat(error.message()).as("сообщение об ошибке для " + requestDescription).isNotBlank();
    }
}
