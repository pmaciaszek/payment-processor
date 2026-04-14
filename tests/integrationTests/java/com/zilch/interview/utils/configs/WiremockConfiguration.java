package com.zilch.interview.utils.configs;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.WireMockConfigurationCustomizer;

@TestConfiguration
public class WiremockConfiguration implements WireMockConfigurationCustomizer {

    private static final String WIREMOCK_MAPPING_JSON_LOCATION = "tests/integrationTests/resources";

    @Override
    public void customize(WireMockConfiguration configuration, ConfigureWireMock options) {
        configuration.withRootDirectory(WIREMOCK_MAPPING_JSON_LOCATION)
                .extensions(new NoKeepAliveTransformer())
                .globalTemplating(true)
                .notifier(new ConsoleNotifier(true));
    }

    private static class NoKeepAliveTransformer implements ResponseDefinitionTransformerV2 {

        @Override
        public String getName() {
            return "keep-alive-disabler";
        }

        @Override
        public ResponseDefinition transform(ServeEvent serveEvent) {
            return ResponseDefinitionBuilder
                    .like(serveEvent.getResponseDefinition())
                    .withHeader("Connection", "close")
                    .build();
        }
    }
}
