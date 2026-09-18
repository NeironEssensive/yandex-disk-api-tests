package ru.qa.disk.tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qa.disk.api.ResourcesApi;
import ru.qa.disk.core.ApiSpec;
import ru.qa.disk.core.HttpStatus;
import ru.qa.disk.model.Resource;
import ru.qa.disk.util.TestData;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PATCH — пользовательские свойства ресурса.
 *
 * <p>Метод не входил в обязательный набор задания, но это единственный способ изменить
 * метаданные ресурса, поэтому без него набор проверок был бы неполным.
 */
@Feature("Пользовательские свойства")
@DisplayName("PATCH /v1/disk/resources — custom_properties")
class CustomPropertiesPatchTest extends BaseResourceTest {

    private final ResourcesApi resourcesApi = new ResourcesApi();

    @Test
    @Story("Запись свойств")
    @DisplayName("Записывает custom_properties: 200 и свойства видны в метаданных")
    void writesCustomProperties() {
        String filePath = inTestFolder(TestData.fileName("with-properties", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("writesCustomProperties"));
        Map<String, Object> body = Map.of("custom_properties", Map.of(
                "qa_owner", "autotest",
                "qa_case", "PATCH custom_properties"));

        Response response = resourcesApi.patchCustomProperties(filePath, body);

        response.then().statusCode(HttpStatus.OK);
        Resource patched = response.as(Resource.class);
        assertThat(patched.customProperties()).as("свойства в ответе PATCH")
                .containsEntry("qa_owner", "autotest")
                .containsEntry("qa_case", "PATCH custom_properties");
        assertThat(steps.meta(filePath).customProperties()).as("свойства сохранились на Диске")
                .containsEntry("qa_owner", "autotest");
    }

    @Test
    @Story("Запись свойств")
    @DisplayName("Повторный PATCH перезаписывает набор свойств")
    void rewritesCustomProperties() {
        String filePath = inTestFolder(TestData.fileName("rewritten", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("rewritesCustomProperties"));
        resourcesApi.patchCustomProperties(filePath, Map.of("custom_properties", Map.of("stage", "first")))
                .then().statusCode(HttpStatus.OK);

        resourcesApi.patchCustomProperties(filePath, Map.of("custom_properties", Map.of("stage", "second")))
                .then().statusCode(HttpStatus.OK);

        assertThat(steps.meta(filePath).customProperties()).as("итоговое значение свойства")
                .containsEntry("stage", "second");
    }

    @Test
    @Story("Негативные проверки")
    @DisplayName("PATCH несуществующего ресурса возвращает 404")
    void patchOfMissingResourceReturnsNotFound() {
        resourcesApi.patchCustomProperties(
                        inTestFolder(TestData.fileName("ghost", "txt")),
                        Map.of("custom_properties", Map.of("qa_owner", "autotest")))
                .then().statusCode(HttpStatus.NOT_FOUND);
    }

    @Test
    @Story("Негативные проверки")
    @DisplayName("PATCH с телом, которое не является JSON-объектом, возвращает 400")
    void patchWithMalformedBodyReturnsBadRequest() {
        String filePath = inTestFolder(TestData.fileName("valid", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("patchWithMalformedBodyReturnsBadRequest"));

        resourcesApi.patchCustomProperties(filePath, "это не JSON")
                .then().statusCode(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Story("Авторизация")
    @DisplayName("Без токена свойства не меняются: 401")
    void withoutTokenReturnsUnauthorized() {
        String filePath = inTestFolder(TestData.fileName("protected", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("withoutTokenReturnsUnauthorized"));

        new ResourcesApi(ApiSpec::anonymous)
                .patchCustomProperties(filePath, Map.of("custom_properties", Map.of("hacked", "true")))
                .then().statusCode(HttpStatus.UNAUTHORIZED);

        assertThat(steps.meta(filePath).customProperties()).as("свойства не появились").isNull();
    }
}
