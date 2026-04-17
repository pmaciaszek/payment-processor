package com.zilch.interview.service.check;

import com.zilch.interview.config.properties.ServicesProperties;
import com.zilch.interview.config.properties.VelocityCheckProperties;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.model.CheckResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.zilch.interview.utils.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VelocityCheckUnitTest {

    @Mock
    private ServicesProperties servicesProperties;

    @Mock
    private GlobalRequestCounter globalRequestCounter;

    @InjectMocks
    private VelocityCheck velocityCheck;

    @Test
    void shouldReturnOkWhenRequestCountIsWithinLimit() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        var velocityProps = new VelocityCheckProperties(2, 60L);

        when(servicesProperties.velocityCheck()).thenReturn(velocityProps);
        when(globalRequestCounter.increment(requestDTO.userId())).thenReturn(1);

        // when
        var result = velocityCheck.check(requestDTO);

        // then
        assertThat(result.valid()).isTrue();
    }

    @Test
    void shouldReturnFailWhenRequestCountExceedsLimit() {
        // given
        var requestDTO = getPaymentDTORequestBuilder().build();
        var velocityProps = new VelocityCheckProperties(1, 60L);

        when(servicesProperties.velocityCheck()).thenReturn(velocityProps);
        when(globalRequestCounter.increment(requestDTO.userId())).thenReturn(2);

        // when
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
