package com.zilch.interview.controller;

import com.zilch.interview.utils.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class TestControllerTest extends IntegrationTest {

    @Test
    void shouldReturnCorrectName() {
        var result = restTestClient.get(URI.create("/test/john"), String.class);

        assertThat(result)
                .returns("john", ResponseEntity::getBody);
    }
}
