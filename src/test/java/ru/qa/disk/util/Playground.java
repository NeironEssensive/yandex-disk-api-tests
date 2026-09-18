package ru.qa.disk.util;

import org.junit.jupiter.api.TestInfo;
import ru.qa.disk.config.TestConfig;

import java.lang.reflect.Method;

/**
 * Пути внутри тестовой «песочницы» на Диске.
 *
 * <p>Все тестовые данные лежат под одной корневой папкой (по умолчанию
 * {@code disk:/qa-autotests}), а каждый тест работает в своей подпапке вида
 * {@code disk:/qa-autotests/CopyMoveTest.copyFile-1a2b3c4d}. Это даёт изоляцию тестов
 * и гарантирует, что после прогона на аккаунте не останется мусора.
 */
public final class Playground {

    private Playground() {
    }

    public static String root() {
        return TestConfig.playgroundRoot();
    }

    /** Уникальная папка для конкретного теста — имя видно в отчёте и в самом Диске. */
    public static String folderFor(TestInfo testInfo) {
        String testClass = testInfo.getTestClass().map(Class::getSimpleName).orElse("UnknownTest");
        String testMethod = testInfo.getTestMethod().map(Method::getName).orElse("unknownMethod");
        return path(root(), testClass + "." + testMethod + "-" + TestData.uniqueSuffix());
    }

    public static String path(String parent, String name) {
        return parent.endsWith("/") ? parent + name : parent + "/" + name;
    }

    /** Имя ресурса из полного пути: {@code disk:/a/b/file.txt} -&gt; {@code file.txt}. */
    public static String name(String fullPath) {
        int lastSlash = fullPath.lastIndexOf('/');
        return lastSlash < 0 ? fullPath : fullPath.substring(lastSlash + 1);
    }
}
