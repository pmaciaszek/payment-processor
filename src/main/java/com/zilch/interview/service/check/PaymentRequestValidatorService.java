package com.zilch.interview.service.check;

import com.zilch.interview.dto.PaymentRequestDTO;
import com.zilch.interview.enums.CheckStage;
import com.zilch.interview.exception.ValidationCheckException;
import com.zilch.interview.model.CheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRequestValidatorService {

    private final List<PaymentRequestCheck> checks;

    public void runChecks(PaymentRequestDTO requestDTO) {
        performPreValidationChecks(requestDTO);
        performValidationChecks(requestDTO);
    }

    private void performPreValidationChecks(PaymentRequestDTO requestDTO) {
        getChecks(CheckStage.PRE_VALIDATION)
                .stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .forEach(check -> performPreValidationCheck(check, requestDTO));
    }

    private void performValidationChecks(PaymentRequestDTO requestDTO) {
        var parallelChecks = getChecks(CheckStage.VALIDATION).stream()
                .map(check -> CompletableFuture.supplyAsync(() -> check.check(requestDTO)))
                .toList();

        var results = CompletableFuture.allOf(parallelChecks.toArray(new CompletableFuture[0]))
                .thenApply(check -> parallelChecks.stream()
                        .map(CompletableFuture::join)
                        .toList())
                .join();

        results.stream()
                .filter(result -> !result.valid())
                .forEach(result -> logError(requestDTO.orderId(), result));

        if (!results.stream().allMatch(CheckResult::valid)) {
            var firstError = results.stream()
                    .filter(result -> !result.valid())
                    .findFirst()
                    .orElseThrow();
            throw ValidationCheckException.of(firstError);
        }
    }

    private void performPreValidationCheck(PaymentRequestCheck check, PaymentRequestDTO requestDTO) {
        var result = check.check(requestDTO);
        if (!result.valid()) {
            logError(requestDTO.orderId(), result);
            throw ValidationCheckException.of(result);
        }
    }

    private List<PaymentRequestCheck> getChecks(CheckStage stage) {
        return checks.stream()
                .filter(check -> check.getCheckStage() == stage)
                .toList();
    }

    private void logError(String orderId, CheckResult result) {
        log.error("Payment check failed for order {}: {}", orderId, result.reason());
    }
}

