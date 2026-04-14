package com.zilch.interview.config.properties.dummyDomain;

import com.zilch.interview.config.properties.RestClientProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DummyDomainClientProperties extends RestClientProperties<DummyDomainEndpoints> {
}
