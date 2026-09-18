package ru.qa.disk.util;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Небольшой помощник для ожидания состояния — вместо {@code Thread.sleep} на глаз.
 *
 * <p>Используется для асинхронных операций Диска: API отвечает 202 и предлагает опрашивать
 * статус операции, пока он не станет {@code success}.
 */
public final class Await {

    private Await() {
    }

    /**
     * Опрашивает {@code supplier}, пока результат не удовлетворит условию.
     *
     * @throws AssertionError если условие не выполнилось за отведённое время
     */
    public static <T> T until(String description,
                              Duration timeout,
                              Duration pollInterval,
                              Supplier<T> supplier,
                              Predicate<T> condition) {
        Instant deadline = Instant.now().plus(timeout);
        T lastResult = null;
        int attempts = 0;
        while (Instant.now().isBefore(deadline)) {
            attempts++;
            lastResult = supplier.get();
            if (condition.test(lastResult)) {
                return lastResult;
            }
            sleep(pollInterval);
        }
        throw new AssertionError(String.format(
                "Условие не выполнилось за %d с (%s). Попыток: %d, последний результат: %s",
                timeout.toSeconds(), description, attempts, lastResult));
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание прервано", e);
        }
    }
}
