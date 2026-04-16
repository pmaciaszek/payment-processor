package com.zilch.interview.client;

import com.zilch.interview.config.properties.RestClientsProperties;
import com.zilch.interview.dto.balance.UserBalanceResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BalanceClient {

    public static final String CURRENCY_QUERY_PARAM = "currency";

    private final RestClient balanceRestClient;
    private final RestClientsProperties restClientsProperties;

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
}
