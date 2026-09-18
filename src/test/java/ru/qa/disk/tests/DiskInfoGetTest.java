package ru.qa.disk.tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qa.disk.api.DiskInfoApi;
import ru.qa.disk.core.ApiSpec;
import ru.qa.disk.core.HttpStatus;
import ru.qa.disk.model.ApiError;
import ru.qa.disk.model.DiskInfo;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/** GET — данные о Диске: {@code GET /v1/disk/}. */
@Feature("Информация о Диске")
@DisplayName("GET /v1/disk/ — данные о Диске")
class DiskInfoGetTest extends BaseApiTest {

    private final DiskInfoApi diskInfoApi = new DiskInfoApi();

    @Test
    @Story("Чтение квоты")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Возвращает 200 и непротиворечивые данные о квоте")
    void returnsConsistentQuota() {
        DiskInfo diskInfo = diskInfoApi.getDiskInfo()
                .then().statusCode(HttpStatus.OK)
                .extract().as(DiskInfo.class);

        assertThat(diskInfo.totalSpace()).as("общий объём Диска").isPositive();
        assertThat(diskInfo.usedSpace()).as("занятое место").isNotNegative();
        assertThat(diskInfo.usedSpace()).as("занято не больше, чем всего")
                .isLessThanOrEqualTo(diskInfo.totalSpace());
        assertThat(diskInfo.trashSize()).as("размер Корзины").isNotNegative();
        assertThat(diskInfo.freeSpace()).as("свободное место").isNotNegative();
        assertThat(diskInfo.systemFolders()).as("системные папки").isNotNull();
        assertThat(diskInfo.systemFolders().applications()).as("папка приложений")
                .isNotBlank()
                .startsWith("disk:/");
    }

    @Test
    @Story("Чтение квоты")
    @DisplayName("Ответ соответствует JSON-схеме")
    void matchesJsonSchema() {
        diskInfoApi.getDiskInfo()
                .then().statusCode(HttpStatus.OK)
                .body(matchesJsonSchemaInClasspath("schemas/disk-info.json"));
    }

    @Test
    @Story("Параметр fields")
    @DisplayName("Параметр fields оставляет в ответе только запрошенные поля")
    void fieldsParameterLimitsResponse() {
        diskInfoApi.getDiskInfo("total_space,used_space")
                .then().statusCode(HttpStatus.OK)
                .body("total_space", notNullValue())
                .body("used_space", notNullValue())
                .body("trash_size", nullValue())
                .body("system_folders", nullValue());
    }

    @Test
    @Story("Авторизация")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Без токена возвращает 401 и тело с описанием ошибки")
    void withoutTokenReturnsUnauthorized() {
        Response response = new DiskInfoApi(ApiSpec::anonymous).getDiskInfo();

        response.then().statusCode(HttpStatus.UNAUTHORIZED);
        ApiError error = response.as(ApiError.class);
        assertThat(error.error()).as("машинный код ошибки").isNotBlank();
        assertThat(error.message()).as("сообщение об ошибке").isNotBlank();
    }

    @Test
    @Story("Авторизация")
    @DisplayName("С невалидным токеном возвращает 401")
    void withInvalidTokenReturnsUnauthorized() {
        new DiskInfoApi(() -> ApiSpec.withToken("definitely-not-a-valid-token"))
                .getDiskInfo()
                .then().statusCode(HttpStatus.UNAUTHORIZED);
    }
}
