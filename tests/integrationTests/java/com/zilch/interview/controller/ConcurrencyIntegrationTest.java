package com.zilch.interview.controller;

import com.zilch.interview.dto.PaymentResponseDTO;
import com.zilch.interview.entity.UserDeviceEntity;
import com.zilch.interview.entity.UserDeviceId;
import com.zilch.interview.entity.UserEntity;
import com.zilch.interview.entity.UserTransferEntity;
import com.zilch.interview.enums.TransferStatus;
import com.zilch.interview.enums.UserAccountStatus;
import com.zilch.interview.utils.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static com.zilch.interview.utils.provider.PaymentRequestDTOProvider.getPaymentDTORequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ConcurrencyIntegrationTest extends IntegrationTest {

    @Test
    void shouldHandleConcurrentRequestsWithSameIdempotencyKey() throws Exception {
        // given
        var idempotencyKey = UUID.randomUUID().toString();
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .build();

        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            // when
            var tasks = IntStream.range(0, 10)
                    .<Callable<ResponseEntity<PaymentResponseDTO>>>mapToObj(i ->
                            () -> restTestClient.post(PAYMENTS_ENDPOINT, idempotencyKey, requestDTO, PaymentResponseDTO.class))
                    .toList();

            var results = executor.invokeAll(tasks);

            // then
            var transactionIds = results.stream()
                    .map(this::getBody)
                    .filter(body -> body != null && body.success())
                    .map(PaymentResponseDTO::transactionId)
                    .distinct()
                    .toList();

            assertThat(transactionIds)
                    .hasSize(1);

            assertThat(userTransferRepository.findAll())
                    .singleElement()
                    .satisfies(transfer -> assertThat(transfer.getStatus())
                            .isEqualTo(TransferStatus.CAPTURED));
        }
    }

    @Test
    void shouldHandleConcurrentRequestsWithDifferentIdempotencyKeys() throws Exception {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .build();

        try (ExecutorService executor = Executors.newFixedThreadPool(5)) {
            // when
            var tasks = IntStream.range(0, 5)
                    .<Callable<ResponseEntity<PaymentResponseDTO>>>mapToObj(i ->
                            () -> restTestClient.post(PAYMENTS_ENDPOINT, UUID.randomUUID().toString(), requestDTO, PaymentResponseDTO.class))
                    .toList();

            var results = executor.invokeAll(tasks);

            // then
            var successfulResponses = results.stream()
                    .map(this::getBody)
                    .filter(body -> body != null && body.success())
                    .toList();

            var uniqueTransactionIds = successfulResponses.stream()
                    .map(PaymentResponseDTO::transactionId)
                    .distinct()
                    .toList();
            assertAll(
                    () -> assertThat(successfulResponses)
                            .hasSize(5),
                    () -> assertThat(uniqueTransactionIds)
                            .hasSize(5),
                    () -> assertThat(userTransferRepository.findAll())
                            .hasSize(5));
        }
    }

    @Test
    void shouldRejectConcurrentRequestsExceedingVelocityLimit() throws Exception {
        // given
        var requestDTO = getPaymentDTORequestBuilder()
                .userId(user.getId())
                .deviceId(DEVICE_ID)
                .build();

        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            // when
            var tasks = IntStream.range(0, 10)
                    .<Callable<ResponseEntity<String>>>mapToObj(i ->
                            () -> restTestClient.post(PAYMENTS_ENDPOINT, UUID.randomUUID().toString(), requestDTO, String.class))
                    .toList();

            var results = executor.invokeAll(tasks);

            // then
            var statusCodes = results.stream()
                    .map(this::getStatusCode)
                    .toList();

            var successCount = statusCodes.stream().filter(s -> s == HttpStatus.OK).count();
            var badRequestCount = statusCodes.stream().filter(s -> s == HttpStatus.BAD_REQUEST).count();

            assertAll(
                    () -> assertThat(successCount)
                            .isLessThanOrEqualTo(5),
                    () -> assertThat(badRequestCount)
                            .isGreaterThanOrEqualTo(5));
        }
    }

    @Test
    void shouldHandleConcurrentPaymentsForDifferentUsers() throws Exception {
        // given
        var users = IntStream.range(0, 5)
                .mapToObj(i -> {
                    var u = userRepository.save(UserEntity.builder()
                            .status(UserAccountStatus.ACTIVE)
                            .build());
                    userDeviceRepository.save(UserDeviceEntity.builder()
                            .id(new UserDeviceId(u.getId(), "device-user-" + i))
                            .trusted(true)
                            .build());
                    return u;
                })
                .toList();

        try (ExecutorService executor = Executors.newFixedThreadPool(5)) {
            // when
            var tasks = users.stream()
                    .<Callable<ResponseEntity<PaymentResponseDTO>>>map(u -> () -> {
                        var req = getPaymentDTORequestBuilder()
                                .userId(u.getId())
                                .deviceId("device-user-" + users.indexOf(u))
                                .build();
                        return restTestClient.post(PAYMENTS_ENDPOINT, req, PaymentResponseDTO.class);
                    })
                    .toList();

            var results = executor.invokeAll(tasks);

            // then
            var allSucceeded = results.stream()
                    .map(this::getBody)
                    .allMatch(body -> body != null && body.success());

            assertAll(
                    () -> assertThat(allSucceeded).isTrue(),
                    () -> assertThat(userTransferRepository.findAll())
                            .hasSize(5)
                            .allSatisfy(entity -> assertThat(entity)
                                    .returns(TransferStatus.CAPTURED, UserTransferEntity::getStatus)));
        }
    }

    private PaymentResponseDTO getBody(Future<ResponseEntity<PaymentResponseDTO>> future) {
        try {
            return future.get().getBody();
        } catch (Exception e) {
            return null;
        }
    }

    private HttpStatus getStatusCode(Future<ResponseEntity<String>> future) {
        try {
            return (HttpStatus) future.get().getStatusCode();
        } catch (Exception e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
