package com.zilch.interview.client;

import com.zilch.interview.config.properties.RestClientsProperties;
import com.zilch.interview.dto.balance.UserBalanceResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BalanceClient {

    private final RestClient balanceRestClient;
    private final RestClientsProperties restClientsProperties;

    public UserBalanceResponseDTO getUserBalance(UUID userId) {
        return balanceRestClient.get()
                .uri(restClientsProperties.balance().getEndpoints().userBalance(), userId)
                .retrieve()
                .body(UserBalanceResponseDTO.class);
    }
}
