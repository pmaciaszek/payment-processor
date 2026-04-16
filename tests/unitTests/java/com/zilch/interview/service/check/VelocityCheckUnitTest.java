package com.zilch.interview.service.check;

import com.zilch.interview.config.properties.ServicesProperties;
import com.zilch.interview.config.properties.VelocityCheckProperties;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.model.CheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VelocityCheckUnitTest {

    @Mock
    private ServicesProperties servicesProperties;

    @InjectMocks
    private VelocityCheck velocityCheck;

    @BeforeEach
    void setUp() {
        GlobalRequestCounter.reset();
    }

    @Test
    void shouldReturnOkWhenRequestCountIsWithinLimit() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        var velocityProps = new VelocityCheckProperties(2);
        
        when(servicesProperties.velocityCheck()).thenReturn(velocityProps);

        // when & then
        assertThat(velocityCheck.check(requestDTO)).returns(true, CheckResult::valid);
        assertThat(velocityCheck.check(requestDTO)).returns(true, CheckResult::valid);
    }

    @Test
    void shouldReturnFailWhenRequestCountExceedsLimit() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        var velocityProps = new VelocityCheckProperties(1);
        
        when(servicesProperties.velocityCheck()).thenReturn(velocityProps);

        // when
        velocityCheck.check(requestDTO);
        var result = velocityCheck.check(requestDTO);

        // then
        assertThat(result)
                .returns(false, CheckResult::valid)
                .returns("Too many requests", CheckResult::reason);
    }

    @Test
    void shouldReturnValidationStage() {
        // when
        var stage = velocityCheck.getCheckStage();

        // then
        assertThat(stage).isEqualTo(CheckStage.VALIDATION);
    }
}
