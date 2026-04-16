package com.zilch.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import com.zilch.interview.exception.RequestBodyHashCreationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class IdempotencyKeyFactoryServiceUnitTest {

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private IdempotencyKeyFactoryService factoryService;

    private static final String REQUEST_ID = "req-123";
    private static final String REQUEST_BODY = "{\"amount\": 100}";

    @Test
    void shouldCreateIdempotencyKeyWithCorrectHash() {
        // when
        var result = factoryService.createIdempotencyKey(REQUEST_ID, REQUEST_BODY);

        // then
        assertAll(
                () -> assertThat(result.key()).isEqualTo(REQUEST_ID),
                () -> assertThat(result.requestBodyHash())
                        .isEqualTo("e68c3b009d25de798db98eab43f4ebbbffc456f6e42b74678b85a5371dd34cfb")
        );
    }

    @Test
    void shouldThrowRequestBodyHashCreationExceptionWhenSerializationFails() {
        // given
        var bodyObject = new Object();
        when(objectMapper.writeValueAsString(bodyObject)).thenThrow(new RuntimeException("Serialization failed"));

        // when & then
        assertThatThrownBy(() -> factoryService.createIdempotencyKey(REQUEST_ID, bodyObject))
                .isInstanceOf(RequestBodyHashCreationException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldReturnSameHashForSameInput() {
        // when
        var key1 = factoryService.createIdempotencyKey(REQUEST_ID, REQUEST_BODY);
        var key2 = factoryService.createIdempotencyKey(REQUEST_ID, REQUEST_BODY);

        // then
        assertThat(key1.requestBodyHash()).isEqualTo(key2.requestBodyHash());
    }

    @Test
    void shouldReturnDifferentHashWhenJsonOrderIsChanged() {
        // given
        var body1 = Map.of("field1", "value1", "field2", "value2");
        var body2 = Map.of("field2", "value2", "field1", "value1");

        // when
        var key1 = factoryService.createIdempotencyKey(REQUEST_ID, body1);
        var key2 = factoryService.createIdempotencyKey(REQUEST_ID, body2);

        // then
        assertThat(key1.requestBodyHash()).isEqualTo(key2.requestBodyHash());
    }
}
