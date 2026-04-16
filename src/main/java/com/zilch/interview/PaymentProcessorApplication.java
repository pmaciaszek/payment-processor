package com.zilch.interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PaymentProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentProcessorApplication.class, args);
    }

}
