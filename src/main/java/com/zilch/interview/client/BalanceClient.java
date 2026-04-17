package com.zilch.interview.client;

import com.zilch.interview.config.properties.RestClientsProperties;
import com.zilch.interview.dto.balance.UserBalanceResponseDTO;
import com.zilch.interview.exception.BalanceServiceUnavailableException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceClient {

    public static final String CURRENCY_QUERY_PARAM = "currency";
    private static final String CIRCUIT_BREAKER_NAME = "balanceService";

    private final RestClient balanceRestClient;
    private final RestClientsProperties restClientsProperties;

    @Bulkhead(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserBalanceFallback")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserBalanceFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public UserBalanceResponseDTO getUserBalance(UUID userId, String currency) {
        var uri = UriComponentsBuilder
                .fromPath(restClientsProperties.balance().getEndpoints().userBalance())
                .queryParam(CURRENCY_QUERY_PARAM, currency)
                .build(userId);

        return balanceRestClient.get()
                .uri(uri)
                .retrieve()
                .body(UserBalanceResponseDTO.class);
    }

    private UserBalanceResponseDTO getUserBalanceFallback(UUID userId, String currency, Exception exception) {
        log.error("Fallback triggered for balance service. userId: {}, currency: {}, cause: {}",
                userId, currency, exception.getClass().getSimpleName(), exception);
        throw new BalanceServiceUnavailableException(
                "Balance service is currently unavailable. Please try again later.", exception);
    }
}
