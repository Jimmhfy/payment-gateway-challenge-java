# Payment Gateway
This is a payment gateway service to validate and transfer payment card detail to acquiring bank. 

In pre-production environment, acquiring bank simulator mocking service will be used to mock the bank response.

An Optional field of Idempotency key will be checked in request header to prevent double payment from customer.

### Key Features
* **Pre-production Mocking**: Includes an acquiring bank simulator for easy testing.
* **Pre-validation**: Validates card details based on specific business rules before reaching out to the bank.
* **Idempotency**: Supports optional `Idempotency-Key` in request headers to prevent accidental double-charging.

## Project Flow
1. Process a Payment
   1. Request: Merchant sends a `POST` payment request. ([Example Request](#process-a-card-payment-post-payment))
   2. Validation: Service validates the request against the ([Rule of Payment Card Detail](#rule-of-payment-card-detail))
   3. Execution: 
      1. If the Idempotency-Key matches a previously processed request, it returns the cached result. 
      2. Otherwise, it forwards the request to the acquiring bank ([Simulator response](#card-simulation))
   4. Response: Returns a unique payment_id, masked card details, and status (`Authorized` or `Declined`).
2. Retrieve the payment result by payment id
   1. Request: Merchant sends a `GET` request using the payment_id ([Example Request](#retrieve-a-processed-payment-get-paymentid))
   2. Response: Returns the historical payment result if it exists

For more API detail, please head up to ([Swagger API Document](#swagger-api-document))

## Library Version

| Library          | Version |
|------------------|---------|
| Java JDK         | 17      |
| Spring Boot      | 3.1.5   |
| OpenAPI Swagger  | 2.2.0   |
| Gradle           | 8.2.1   |
| Docker Compose   | 5.5.1   |
| Docker           |         |

## Quick Start
### Docker - Start the all services
`docker-compose up`

| Service / Container | Port | Description                                                  |
|---------------------|------|--------------------------------------------------------------|
| bank_simulator      | 8080 | A acquiring bank simulator for validating card detail        |
| payment_gateway     | 8090 | A payment gateway service for merchant to pay by credit card |
### TroubleShoot
#### If you face gradle cache lock issue, please try `./gradlew --stop` then `docker compose up` again.
#### Or to run them independently ([Debug locally](#debug-locally))

# API Endpoint
Sample HTTP request is available under `src/test/resources/paymentRequest.http` for easy execution via IDE
## Functionality
| Method | Path            | Description                  |
|--------|-----------------|------------------------------|
| POST   | /payment        | Process a card payment       |
| GET    | /payment/{id}   | Retrieve a processed payment |

### Process a card payment `POST /payment`
- Request (Without Idempotency Key):
```bash
curl -X POST http://localhost:8090/payment \
  -H "Content-Type: application/json" \
  -d '{
    "currency": "GBP",
    "amount": 100,
    "cvv": "111",
    "card_number": "4321432143214321",
    "expiry_month": 1,
    "expiry_year": 2027
  }'
```
- Request (With Idempotency Key):
```bash
curl -X POST http://localhost:8090/payment \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-12345" \
  -d '{
    "currency": "GBP",
    "amount": 100,
    "cvv": "111",
    "card_number": "4321432143214321",
    "expiry_month": 1,
    "expiry_year": 2027
  }'
```
#### Expected Response
```
{
  "id": "977b2dd6-e085-4cc5-a142-e16fd6f7c7ef",
  "status": "Authorized",
  "cardNumberLastFour": 4321,
  "expiryMonth": 1,
  "expiryYear": 2027,
  "currency": "GBP",
  "amount": 100
}
```


### Retrieve a processed payment `GET /payment/{id}`
```bash
curl -X GET http://localhost:8090/payment/977b2dd6-e085-4cc5-a142-e16fd6f7c7ef
```
#### Expected Response
```
{
  "id": "977b2dd6-e085-4cc5-a142-e16fd6f7c7ef",
  "status": "Authorized",
  "cardNumberLastFour": 4321,
  "expiryMonth": 1,
  "expiryYear": 2027,
  "currency": "GBP",
  "amount": 100
}
```
## Business Rules & Simulation
### Rule of Payment Card Detail
The service enforces the following rules before sending data to the bank:

| Field              | Rule                                             |
|--------------------|--------------------------------------------------|
| Card number        | Between 14-19 characters long numeric characters |
| Expiry month       | Value must be between 1-12                       |
| Expiry year (YYYY) | Value must be in the future                      |
| Currency           | GBP or USD or EUR                                |
| Amount             | Positive integer amount in minor currency unit   |
|                    | for example 1050 refer to 10.50                  |
| CVV                | Card verification value (3–4 digits)             |

### Card Simulation
The mocked bank determines the payment result based on the last digit of the card number:

| Last digit of the card number | Result              |
|-------------------------------|---------------------|
| Odd (1, 3, 5, 7, 9)           | Authorized          |
| Even (2, 4, 6, 8)             | Declined            |
| Zero (0)                      | Service Unavailable |

## Observability
| Path                                      | Description                                   |
|-------------------------------------------|-----------------------------------------------|
| http://localhost:8090/actuator/health     | Endpoint to show service health               |
| http://localhost:8090/actuator/metrics    | Endpoint to show application metric           |
| http://localhost:8090/actuator/prometheus | Endpoint to show metric in prometheus format  |

## Swagger API Document
| URL Path                                    | Description                                |
|---------------------------------------------|--------------------------------------------|
| http://localhost:8090/swagger-ui/index.html | Endpoint to swagger API to show API detail |

## Project Structure
* **`configuration/`** - Configuration setups for external API client (`RestTemplate`) configurations.
* **`controller/`** - Handles RESTsAPI routing and status codes.
* **`enums/`** - Definitions used across the domain (`PaymentStatus`).
* **`exception/`** - Contains all customized exception based on the business logic.
* **`model/`** - Contains Date Models used across the project.
* **`repository/`** - Data access layer (currently implemented as an in-memory store for simplicity).
* **`service/`** - Core business logic and orchestration: processes payments, communicates with the acquiring Bank, and saves records.
* **`validator/`** - Contains custom business rule validations like card payment validation.

## Debug locally
### Run the bank simulator in docker
```bash
docker-compose up bank_simulator
```
### Run the payment gateway standalone (port 8090)
`./gradlew bootRun`
### Execute unit tests
`./gradlew test`
### To Stop Gradle
`./gradlew --stop`