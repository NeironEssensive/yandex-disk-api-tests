package ru.qa.disk.tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qa.disk.api.PublicApi;
import ru.qa.disk.core.ApiSpec;
import ru.qa.disk.core.HttpStatus;
import ru.qa.disk.model.Link;
import ru.qa.disk.model.Resource;
import ru.qa.disk.util.TestData;

import static org.assertj.core.api.Assertions.assertThat;

/** PUT — публикация ресурсов: {@code /v1/disk/resources/publish} и {@code /unpublish}. */
@Feature("Публикация")
@DisplayName("PUT /v1/disk/resources/publish и /unpublish")
class PublishPutTest extends BaseResourceTest {

    private final PublicApi publicApi = new PublicApi();

    @Test
    @Story("Публикация файла")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Публикует файл: 200 и в метаданных появляются public_url и public_key")
    void publishesFile() {
        String filePath = inTestFolder(TestData.fileName("published", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("publishesFile"));

        Response response = publicApi.publish(filePath);

        response.then().statusCode(HttpStatus.OK);
        Link link = response.as(Link.class);
        assertThat(link.href()).as("ссылка на метаданные ресурса").isNotBlank();
        assertThat(link.method()).as("метод для ссылки").isEqualTo("GET");

        Resource published = steps.meta(filePath);
        assertThat(published.isPublished()).as("ресурс опубликован").isTrue();
        assertThat(published.publicUrl()).as("публичная ссылка").startsWith("https://");
        assertThat(published.publicKey()).as("ключ публичного ресурса").isNotBlank();
    }

    @Test
    @Story("Публикация файла")
    @DisplayName("Метаданные публичного ресурса доступны по public_key")
    void readsPublicMetaByPublicKey() {
        String filePath = inTestFolder(TestData.fileName("shared", "txt"));
        Resource file = steps.uploadTextFile(filePath, TestData.textContent("readsPublicMetaByPublicKey"));
        publicApi.publish(filePath).then().statusCode(HttpStatus.OK);
        String publicKey = steps.meta(filePath).publicKey();

        Resource publicResource = publicApi.getPublicMeta(publicKey)
                .then().statusCode(HttpStatus.OK)
                .extract().as(Resource.class);

        assertThat(publicResource.name()).as("имя публичного ресурса").isEqualTo(file.name());
        assertThat(publicResource.md5()).as("содержимое публичного ресурса").isEqualTo(file.md5());
        assertThat(publicResource.publicKey()).as("public_key в ответе").isEqualTo(publicKey);
    }

    @Test
    @Story("Публикация папки")
    @DisplayName("Публикует папку: в публичных метаданных видно её содержимое")
    void publishesFolderWithContent() {
        String folderPath = inTestFolder(TestData.folderName("public-folder"));
        steps.createFolder(folderPath);
        steps.uploadTextFile(folderPath + "/inside.txt", TestData.textContent("publishesFolderWithContent"));

        publicApi.publish(folderPath).then().statusCode(HttpStatus.OK);

        String publicKey = steps.meta(folderPath).publicKey();
        Resource publicFolder = publicApi.getPublicMeta(publicKey)
                .then().statusCode(HttpStatus.OK)
                .extract().as(Resource.class);
        assertThat(publicFolder.isDir()).as("публичный ресурс — папка").isTrue();
        assertThat(publicFolder.embedded().names()).as("содержимое публичной папки")
                .containsExactly("inside.txt");
    }

    @Test
    @Story("Снятие публикации")
    @DisplayName("Снимает публикацию: 200 и public_url исчезает из метаданных")
    void unpublishesFile() {
        String filePath = inTestFolder(TestData.fileName("unpublished", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("unpublishesFile"));
        publicApi.publish(filePath).then().statusCode(HttpStatus.OK);
        assertThat(steps.meta(filePath).isPublished()).as("файл опубликован до снятия").isTrue();

        publicApi.unpublish(filePath).then().statusCode(HttpStatus.OK);

        Resource unpublished = steps.meta(filePath);
        assertThat(unpublished.publicUrl()).as("публичная ссылка").isNull();
        assertThat(unpublished.publicKey()).as("ключ публичного ресурса").isNull();
    }

    @Test
    @Story("Негативные проверки")
    @DisplayName("Публикация несуществующего ресурса возвращает 404")
    void publishOfMissingResourceReturnsNotFound() {
        publicApi.publish(inTestFolder(TestData.fileName("ghost", "txt")))
                .then().statusCode(HttpStatus.NOT_FOUND);
    }

    @Test
    @Story("Авторизация")
    @DisplayName("Без токена публикация возвращает 401 и ресурс остаётся приватным")
    void withoutTokenReturnsUnauthorized() {
        String filePath = inTestFolder(TestData.fileName("private", "txt"));
        steps.uploadTextFile(filePath, TestData.textContent("withoutTokenReturnsUnauthorized"));

        new PublicApi(ApiSpec::anonymous).publish(filePath)
                .then().statusCode(HttpStatus.UNAUTHORIZED);

        assertThat(steps.meta(filePath).isPublished()).as("ресурс не опубликован").isFalse();
    }
}
