package com.zilch.interview.utils.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URI;

@RequiredArgsConstructor
public class RestTestClient {

    private final TestRestTemplate restTemplate;

    public <T>ResponseEntity<T> get(URI uri, Class<T> responseClass) {
        return restTemplate.exchange(uri, HttpMethod.GET, null, responseClass);
    }
}
