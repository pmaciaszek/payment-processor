package com.zilch.interview.utils.base;

import com.zilch.interview.repository.UserDeviceRepository;
import com.zilch.interview.repository.UserRepository;
import com.zilch.interview.repository.UserTransferRepository;
import com.zilch.interview.utils.configs.RestTestClientConfiguration;
import com.zilch.interview.utils.configs.WiremockConfiguration;
import com.zilch.interview.utils.rest.RestTestClient;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@EnableWireMock(
        @ConfigureWireMock(
                configurationCustomizers = WiremockConfiguration.class))
@Import({RestTestClientConfiguration.class})
public abstract class IntegrationTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("test-db")
            .withUsername("sa")
            .withPassword("sa")
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(PostgreSQLContainer.class)));

    static {
        POSTGRESQL_CONTAINER.start();
    }

    protected static final String PAYMENTS_ENDPOINT = "/v1/payments";

    @Autowired
    protected RestTestClient restTestClient;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserDeviceRepository userDeviceRepository;

    @Autowired
    protected UserTransferRepository userTransferRepository;
}
