package com.zilch.interview.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RestClientProperties<T> {

    @NotNull
    @NotBlank
    private String host;

    @NotNull
    private T endpoints;
}
