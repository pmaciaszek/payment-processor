package com.zilch.interview.utils.configs;

import com.zilch.interview.utils.rest.RestTestClient;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class RestTestClientConfiguration {

    @Bean
    public RestTestClient restTestClient(TestRestTemplate testRestTemplate) {
        return new RestTestClient(testRestTemplate);
    }
}
