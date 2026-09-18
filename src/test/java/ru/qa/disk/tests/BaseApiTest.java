package ru.qa.disk.tests;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.qa.disk.core.PlaygroundLifecycle;
import ru.qa.disk.steps.DiskSteps;

/**
 * База для тестов, которые ходят в реальное API.
 *
 * <p>Тег {@code api} позволяет отделить их от офлайн-тестов:
 * {@code mvn test -Dgroups=api} или {@code mvn test -Dgroups=offline}.
 */
@Tag("api")
@Epic("REST API Яндекс Диска")
@ExtendWith(PlaygroundLifecycle.class)
public abstract class BaseApiTest {

    protected final DiskSteps steps = new DiskSteps();
}
