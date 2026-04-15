package com.zilch.interview.config.properties.dummyDomain;

import com.zilch.interview.config.properties.RestClientProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.validation.annotation.Validated;

@Data
@EqualsAndHashCode(callSuper = true)
@Validated
public class DummyDomainClientProperties extends RestClientProperties<DummyDomainEndpoints> {
}
