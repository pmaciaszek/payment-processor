package com.zilch.interview.client.handler;

import com.zilch.interview.dto.balance.BalanceErrorResponseDTO;
import com.zilch.interview.exception.BalanceResponseException;
import com.zilch.interview.exception.BalanceServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
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
        var statusCode = response.getStatusCode();
        var body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        if (statusCode.is4xxClientError()) {
            throw new BalanceResponseException(readErrorBody(statusCode, body));
        }
        log.error("Balance service responded with status {} for request {} {}.", statusCode,
                method,
                url,
                StructuredArguments.kv("responseBody", body));
        throw new BalanceServiceUnavailableException("Balance service error: " + statusCode);
    }

    private BalanceErrorResponseDTO readErrorBody(HttpStatusCode statusCode, String responseBody) {
        try {
            return objectMapper.readValue(responseBody, BalanceErrorResponseDTO.class);
        } catch (JacksonException exception) {
            log.error("Could not deserialize balance domain error {}", responseBody, exception);
            return new BalanceErrorResponseDTO(
                    statusCode.value(),
                    "Unrecognized error occurred.");
        }
    }


}
