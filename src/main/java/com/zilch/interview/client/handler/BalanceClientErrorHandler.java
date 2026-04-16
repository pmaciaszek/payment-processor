package com.zilch.interview.client.handler;

import com.zilch.interview.dto.balance.BalanceErrorResponseDTO;
import com.zilch.interview.exception.BalanceResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceClientErrorHandler implements ResponseErrorHandler {

    private final ObjectMapper objectMapper;

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        throw new BalanceResponseException(readErrorBody(response));
    }

    private BalanceErrorResponseDTO readErrorBody(ClientHttpResponse response) throws IOException {
        var stringBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return objectMapper.readValue(stringBody, BalanceErrorResponseDTO.class);
        } catch (JacksonException exception) {
            log.error("Could not deserialize balance domain error {}", stringBody, exception);
            return new BalanceErrorResponseDTO(
                    response.getStatusCode().value(),
                    "Unrecognized error occurred.");
        }
    }


}
