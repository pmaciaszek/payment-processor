package com.zilch.interview.client.handler;

import com.zilch.interview.dto.DummyDomainErrorResponseDTO;
import com.zilch.interview.exception.DummyDomainResponseException;
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
public class DummyDomainClientErrorHandler implements ResponseErrorHandler {

    private final ObjectMapper objectMapper;

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        ResponseErrorHandler.super.handleError(url, method, response);
        throw new DummyDomainResponseException(readErrorBody(response));
    }

    private DummyDomainErrorResponseDTO readErrorBody(ClientHttpResponse response) throws IOException {
        var stringBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return objectMapper.readValue(stringBody, DummyDomainErrorResponseDTO.class);
        } catch (JacksonException exception) {
            log.error("Could not deserialize dummy domain error {}", stringBody, exception);
            return new DummyDomainErrorResponseDTO(
                    response.getStatusCode().value(),
                    "Unrecognized error occurred.");
        }
    }


}
