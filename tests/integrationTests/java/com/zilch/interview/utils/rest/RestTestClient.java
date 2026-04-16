package com.zilch.interview.utils.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@RequiredArgsConstructor
public class RestTestClient {

    private final TestRestTemplate restTemplate;

    public <T, R> ResponseEntity<R> post(String url, T body, Class<R> responseClass) {
        return post(url, UUID.randomUUID().toString(), body, responseClass);
    }

    public <T, R> ResponseEntity<R> post(String url, String requestId, T body, Class<R> responseClass) {
        var headers = new HttpHeaders();
        headers.add("X-Request-Id", requestId);
        return post(url, headers, body, responseClass);
    }

    public <R> ResponseEntity<R> postWithoutRequestId(String url, Object body, Class<R> responseClass) {
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, new HttpHeaders()), responseClass);
    }

    public <R> ResponseEntity<R> postRawJson(String url, String jsonBody, Class<R> responseClass) {
        var headers = new HttpHeaders();
        headers.add("X-Request-Id", UUID.randomUUID().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(jsonBody, headers), responseClass);
    }

    private <T, R> ResponseEntity<R> post(String url, HttpHeaders headers, T body, Class<R> responseClass) {
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), responseClass);
    }
}
