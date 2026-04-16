package com.zilch.interview.utils.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URI;

@RequiredArgsConstructor
public class RestTestClient {

    private final TestRestTemplate restTemplate;

    public <T> ResponseEntity<T> get(URI uri, Class<T> responseClass) {
        return restTemplate.exchange(uri, HttpMethod.GET, null, responseClass);
    }

    public <T, R> ResponseEntity<R> post(String url, String requestId, T body, Class<R> responseClass) {
        var headers = new HttpHeaders();
        headers.add("X-Request-Id", requestId);
        return post(url, headers, body, responseClass);
    }

    public <T, R> ResponseEntity<R> post(String url, HttpHeaders headers, T body, Class<R> responseClass) {
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), responseClass);
    }
}
