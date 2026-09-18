package ru.qa.disk.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Единый {@link ObjectMapper} для тестов: его же использует REST Assured, поэтому офлайн-тесты
 * маппинга проверяют ровно ту конфигурацию, которая работает в тестах против реального API.
 *
 * <p>API отдаёт поля в snake_case ({@code total_space}), модели описаны в camelCase —
 * связывает их {@link PropertyNamingStrategies#SNAKE_CASE}. Неизвестные поля игнорируются:
 * API развивается, и новое поле в ответе не должно ломать тесты.
 */
public final class JsonMapperFactory {

    private static final ObjectMapper MAPPER = build();

    private JsonMapperFactory() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    private static ObjectMapper build() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
    }
}
