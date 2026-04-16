# Payment Processor System

An advanced payment orchestration and processing system designed for high reliability, resilience, and transaction security.

## 🚀 Key Features

*   **Payment Orchestration:** Management of the full transaction lifecycle from validation to finalization with an external provider.
*   **Idempotency Mechanism:** Protection against double processing of the same transaction using `OperationLockService`.
*   **Asynchronous Validation:** Parallel execution of security and business checks (Velocity Check, Device Check, User Check) using `CompletableFuture`.
*   **Check System (Chain of Responsibility):**
    *   `VelocityCheck`: Transaction limits over time.
    *   `BalanceCheck`: Verification of user funds (integration with external Balance Service).
    *   `DeviceCheck` & `UserCheck`: Verification of account and device status.
    *   `PaymentMethodCheck`: Validation of specific payment methods (Card, BLIK).
*   **Error Handling:** A global exception handling system providing consistent API responses.
*   **Persistence:** Tracking transfer statuses (PENDING, CAPTURED, FAILED) in the database.

## 🏗 Architecture

The project is built on **Spring Boot 4** and **Java 21**.

### Key Components:
*   `PaymentOrchestrator`: Main entry point, providing locking and idempotency.
*   `PaymentService`: Implements business logic and communication with API clients.
*   `PaymentRequestValidatorService`: Manages the asynchronous validation process.
*   `TransferPersistenceService`: Responsible for secure transaction state persistence in the DB.
*   `OperationLockService`: Cache-based implementation of the locking mechanism.

## 🛠 Technologies

*   **Framework:** Spring Boot 4.x
*   **Language:** Java 21+
*   **Database:** PostgreSQL (handled via Spring Data JPA)
*   **Migrations:** Liquibase
*   **Testing:** JUnit 5, Mockito, AssertJ, Wiremock (integration tests)
*   **Others:** Lombok, Jackson

## 🚦 Getting Started

### Requirements
*   JDK 17 or newer
*   Gradle

### Running the application
```bash
./gradlew bootRun
```

### Running tests
The application has an extensive set of unit and integration tests:
```bash
./gradlew test         # All tests
./gradlew unitTest     # Unit tests only
./gradlew integrationTest # Integration tests only
```

## 📝 API Endpoints

Main entry point for payments:
`POST /api/v1/payments`

Example Request Body (Card):
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 100.50,
  "currency": "PLN",
  "paymentMethod": {
    "type": "CARD",
    "token": "tok_123456789"
  },
  "merchantId": "mer_001",
  "orderId": "order_abc123",
  "deviceId": "dev_987654"
}
```

## 🛡 Security Mechanisms

1.  **Timeouts:** Each validation check has a defined time limit (default: 5s).
2.  **Transactional Rollback:** The system ensures data consistency in case of external service failures.
3.  **Strict Validation:** Use of Bean Validation annotations (`@Valid`) and dedicated validators for card tokens and amounts.

---
