# Wallet Transfer Service (Spring Boot)

A portfolio-ready backend service that simulates wallet funding and money transfers with idempotency support.

## Tech Stack

- Java 17
- Spring Boot (Web, Data JPA, Validation, Actuator)
- H2 in-memory database
- OpenAPI via springdoc
- JUnit + MockMvc + Mockito

## Features

- Create user and auto-provision a wallet
- Deposit funds to wallet
- Transfer funds between wallets
- Idempotent transfer API using `X-Idempotency-Key`
- Centralized API error handling and request validation
- Structured logging with SLF4J
- Integration and unit tests
- Coverage reporting with JaCoCo

## Run Locally

```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`.

OpenAPI docs:
- `http://localhost:8080/swagger-ui/index.html`

Health check:
- `http://localhost:8080/actuator/health`

## API Quickstart

### 1) Create users

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Alex Doe","email":"alex@example.com"}'
```

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Jamie Doe","email":"jamie@example.com"}'
```

### 2) Deposit into wallet 1

```bash
curl -X POST http://localhost:8080/api/v1/wallets/1/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount":200.00}'
```

### 3) Transfer from wallet 1 to wallet 2

```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: tx-abc-123" \
  -d '{"fromWalletId":1,"toWalletId":2,"amount":50.00}'
```

### 4) Repeat the same transfer request safely

Re-send the exact same request with the same `X-Idempotency-Key`.  
The API returns the original transfer response and does not charge twice.

## Test

```bash
mvn test
```

## Coverage (JaCoCo)

Generate tests + coverage report:

```bash
mvn clean verify
```

Open report:
- `target/site/jacoco/index.html`

## Resume Line (suggested)

Built a Spring Boot wallet transfer service with layered architecture (`Controller -> Service -> Repository`), implementing idempotent money transfers with JPA transaction management, centralized exception handling, and structured SLF4J logging; documented APIs via OpenAPI/Swagger and added JaCoCo-based coverage reporting with 13 unit/integration tests.
