package com.zilch.interview.client;

import com.zilch.interview.config.properties.RestClientsProperties;
import com.zilch.interview.dto.card.CardValidationRequestDTO;
import com.zilch.interview.dto.card.CardValidationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CardClient {

    private final RestClient balanceRestClient;
    private final RestClientsProperties restClientsProperties;

    public CardValidationResponseDTO validateCard(CardValidationRequestDTO requestDTO) {
        return balanceRestClient.post()
                .uri(restClientsProperties.card().getEndpoints().cardValidation())
                .body(requestDTO)
                .retrieve()
                .body(CardValidationResponseDTO.class);
    }
}
