package ru.qa.disk.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import ru.qa.disk.util.Playground;

/**
 * База для тестов, которым нужны файлы и папки на Диске.
 *
 * <p>Каждый тест получает собственную папку и удаляет её за собой, поэтому тесты не зависят
 * от порядка запуска и не наследуют состояние друг от друга. Имя папки содержит класс и метод —
 * если тест упал, понятно, чьи данные остались на Диске.
 */
public abstract class BaseResourceTest extends BaseApiTest {

    /** Папка текущего теста, например {@code disk:/qa-autotests/CopyMoveTest.copyFile-1a2b3c4d}. */
    protected String testFolder;

    @BeforeEach
    void createTestFolder(TestInfo testInfo) {
        testFolder = steps.createFolder(Playground.folderFor(testInfo)).path();
    }

    @AfterEach
    void removeTestFolder() {
        steps.deleteQuietly(testFolder);
    }

    /** Путь к ресурсу внутри папки текущего теста. */
    protected String inTestFolder(String name) {
        return Playground.path(testFolder, name);
    }
}
