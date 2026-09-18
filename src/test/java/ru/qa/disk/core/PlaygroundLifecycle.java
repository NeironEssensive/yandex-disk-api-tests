package ru.qa.disk.core;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ru.qa.disk.api.ResourcesApi;
import ru.qa.disk.config.TestConfig;

/**
 * Готовит окружение для тестов против реального API.
 *
 * <ul>
 *     <li>если токен не передан — весь класс тестов пропускается с подсказкой,
 *         как его получить (падать с NPE в такой ситуации бессмысленно);</li>
 *     <li>корневая папка песочницы создаётся один раз на прогон;</li>
 *     <li>после последнего теста песочница удаляется безвозвратно — на аккаунте
 *         не остаётся ни тестовых файлов, ни мусора в Корзине.</li>
 * </ul>
 */
public class PlaygroundLifecycle implements BeforeAllCallback {

    private static final String NO_TOKEN_MESSAGE = """
            Не задан OAuth-токен Яндекс Диска, тесты против реального API пропущены.
            Получите токен для тестового (не личного) аккаунта на https://yandex.ru/dev/disk/poligon/
            и передайте его одним из способов:
              * файл .env в корне проекта:  YANDEX_DISK_TOKEN=y0_Ag...
              * переменная окружения:       YANDEX_DISK_TOKEN
              * параметр Maven:             mvn test -Dyandex.disk.token=y0_Ag...
            Офлайн-тесты (mvn test -Dgroups=offline) токен не требуют.""";

    /** Чтобы подсказка про токен попала в лог Maven один раз, а не перед каждым классом. */
    private static boolean warningPrinted;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!TestConfig.hasToken()) {
            printWarningOnce();
        }
        Assumptions.assumeTrue(TestConfig.hasToken(), NO_TOKEN_MESSAGE);
        ApiSpec.init();
        context.getRoot()
                .getStore(ExtensionContext.Namespace.GLOBAL)
                .getOrComputeIfAbsent(PlaygroundRoot.class.getName(), key -> new PlaygroundRoot(), PlaygroundRoot.class);
    }

    private static synchronized void printWarningOnce() {
        if (!warningPrinted) {
            warningPrinted = true;
            System.out.println(System.lineSeparator() + NO_TOKEN_MESSAGE + System.lineSeparator());
        }
    }

    /**
     * Корневая папка песочницы. Живёт в глобальном хранилище JUnit, поэтому создаётся один раз,
     * а {@link #close()} вызывается при завершении всего прогона.
     */
    static class PlaygroundRoot implements ExtensionContext.Store.CloseableResource {

        private final ResourcesApi resources = new ResourcesApi();
        private final String root = TestConfig.playgroundRoot();

        PlaygroundRoot() {
            Response response = resources.createFolder(root);
            int status = response.statusCode();
            // 409 значит «папка уже есть» — это нормально, например при повторном прогоне.
            if (status != HttpStatus.CREATED && status != HttpStatus.CONFLICT) {
                throw new IllegalStateException("Не удалось подготовить папку " + root
                        + ": код " + status + ", тело " + response.asString());
            }
        }

        @Override
        public void close() {
            resources.delete(root, true);
        }
    }
}
