package com.zilch.interview.client;

import com.zilch.interview.config.properties.RestClientsProperties;
import com.zilch.interview.dto.card.CardValidationRequestDTO;
import com.zilch.interview.dto.card.CardValidationResponseDTO;
import com.zilch.interview.exception.CardServiceUnavailableException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardClient {

    private static final String CIRCUIT_BREAKER_NAME = "cardService";

    private final RestClient cardRestClient;
    private final RestClientsProperties restClientsProperties;

    @Bulkhead(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "validateCardFallback")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "validateCardFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public CardValidationResponseDTO validateCard(CardValidationRequestDTO requestDTO) {
        return cardRestClient.post()
                .uri(restClientsProperties.card().getEndpoints().cardValidation())
                .body(requestDTO)
                .retrieve()
                .body(CardValidationResponseDTO.class);
    }

    private CardValidationResponseDTO validateCardFallback(CardValidationRequestDTO requestDTO, Exception exception) {
        log.error("Fallback triggered for card service. cardToken: {}, cause: {}",
                requestDTO.cardToken(), exception.getClass().getSimpleName(), exception);
        throw new CardServiceUnavailableException(
                "Card service is currently unavailable. Please try again later.", exception);
    }
}
