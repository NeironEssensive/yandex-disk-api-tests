package ru.qa.disk.tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qa.disk.api.ResourcesApi;
import ru.qa.disk.core.HttpStatus;
import ru.qa.disk.model.ApiError;
import ru.qa.disk.model.FlatResourceList;
import ru.qa.disk.model.Resource;
import ru.qa.disk.util.Playground;
import ru.qa.disk.util.TestData;

import java.util.List;
import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/** GET — чтение метаданных и списков: {@code /v1/disk/resources}, {@code /files}, {@code /last-uploaded}. */
@Feature("Чтение метаданных")
@DisplayName("GET /v1/disk/resources — метаданные и списки")
class ResourceListGetTest extends BaseResourceTest {

    private static final List<String> FILE_NAMES = List.of("alpha.txt", "beta.txt", "gamma.txt");

    private final ResourcesApi resourcesApi = new ResourcesApi();

    @Test
    @Story("Пагинация")
    @DisplayName("Параметры limit и offset выдают содержимое папки постранично")
    void limitAndOffsetPaginateFolderContent() {
        FILE_NAMES.forEach(name ->
                steps.uploadTextFile(Playground.path(testFolder, name), TestData.textContent(name)));

        Resource firstPage = resourcesApi.getMeta(testFolder, Map.of("limit", 2, "offset", 0, "sort", "name"))
                .then().statusCode(HttpStatus.OK)
                .extract().as(Resource.class);
        Resource secondPage = resourcesApi.getMeta(testFolder, Map.of("limit", 2, "offset", 2, "sort", "name"))
                .then().statusCode(HttpStatus.OK)
                .extract().as(Resource.class);

        assertThat(firstPage.embedded().total()).as("всего файлов в папке").isEqualTo(FILE_NAMES.size());
        assertThat(firstPage.embedded().limit()).as("limit в ответе").isEqualTo(2);
        assertThat(firstPage.embedded().offset()).as("offset в ответе").isZero();
        assertThat(firstPage.embedded().names()).as("первая страница").containsExactly("alpha.txt", "beta.txt");
        assertThat(secondPage.embedded().names()).as("вторая страница").containsExactly("gamma.txt");
    }

    @Test
    @Story("Сортировка")
    @DisplayName("Параметр sort=-name отдаёт содержимое в обратном порядке")
    void sortByNameDescending() {
        FILE_NAMES.forEach(name ->
                steps.uploadTextFile(Playground.path(testFolder, name), TestData.textContent(name)));

        Resource folder = resourcesApi.getMeta(testFolder, Map.of("sort", "-name"))
                .then().statusCode(HttpStatus.OK)
                .extract().as(Resource.class);

        assertThat(folder.embedded().sort()).as("сортировка в ответе").isEqualTo("-name");
        assertThat(folder.embedded().names()).as("порядок файлов")
                .containsExactly("gamma.txt", "beta.txt", "alpha.txt");
    }

    @Test
    @Story("Параметр fields")
    @DisplayName("Параметр fields оставляет в ответе только запрошенные поля")
    void fieldsParameterLimitsResponse() {
        String filePath = Playground.path(testFolder, "alpha.txt");
        steps.uploadTextFile(filePath, TestData.textContent("fieldsParameterLimitsResponse"));

        resourcesApi.getMeta(filePath, Map.of("fields", "name,size"))
                .then().statusCode(HttpStatus.OK)
                .body("name", notNullValue())
                .body("size", notNullValue())
                .body("md5", nullValue())
                .body("path", nullValue());
    }

    @Test
    @Story("Плоские списки")
    @DisplayName("GET /resources/last-uploaded содержит только что загруженный файл")
    void lastUploadedContainsFreshFile() {
        String filePath = Playground.path(testFolder, TestData.fileName("fresh", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("lastUploadedContainsFreshFile"));

        FlatResourceList lastUploaded = resourcesApi.getLastUploaded(Map.of("limit", 20))
                .then().statusCode(HttpStatus.OK)
                .extract().as(FlatResourceList.class);

        assertThat(lastUploaded.paths()).as("последние загруженные файлы").contains(filePath);
        assertThat(lastUploaded.items()).as("в плоском списке только файлы")
                .allMatch(Resource::isFile);
    }

    @Test
    @Story("Плоские списки")
    @DisplayName("GET /resources/files возвращает плоский список файлов без папок")
    void flatFileListContainsOnlyFiles() {
        steps.uploadTextFile(Playground.path(testFolder, "alpha.txt"), TestData.textContent("alpha"));

        FlatResourceList files = resourcesApi.getFlatFileList(Map.of("limit", 20))
                .then().statusCode(HttpStatus.OK)
                .extract().as(FlatResourceList.class);

        assertThat(files.limit()).as("limit в ответе").isEqualTo(20);
        assertThat(files.items()).as("список файлов").isNotEmpty();
        assertThat(files.items()).as("папок в плоском списке нет").allMatch(Resource::isFile);
    }

    @Test
    @Story("Контракт ответа")
    @DisplayName("Метаданные папки соответствуют JSON-схеме ресурса")
    void folderMetaMatchesJsonSchema() {
        steps.uploadTextFile(Playground.path(testFolder, "alpha.txt"), TestData.textContent("schema"));

        resourcesApi.getMeta(testFolder)
                .then().statusCode(HttpStatus.OK)
                .body(matchesJsonSchemaInClasspath("schemas/resource.json"));
    }

    @Test
    @Story("Негативные проверки")
    @DisplayName("Метаданные несуществующего ресурса: 404 и тело с описанием ошибки")
    void missingResourceReturnsNotFound() {
        Response response = resourcesApi.getMeta(inTestFolder(TestData.fileName("ghost", "txt")));

        response.then().statusCode(HttpStatus.NOT_FOUND);
        ApiError error = response.as(ApiError.class);
        assertThat(error.error()).as("машинный код ошибки").isNotBlank();
        assertThat(error.message()).as("сообщение об ошибке").isNotBlank();
        assertThat(error.description()).as("описание ошибки").isNotBlank();
    }
}
