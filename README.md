# Payment GW

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker

## How to Run
Run order:

```bash
docker compose up -d
./gradlew bootRun
# Optional override
./gradlew bootRun --args='--bank.url=http://localhost:8080/payments'
open http://localhost:8090/swagger-ui/index.html
```

`bank.url` property override for folks running the simulator on another host:
```text
java -Dbank.url=http://localhost:8080/payments -jar ...
```
### Example POST:
#### 1) Happy-path POST
```bash
curl -X POST http://localhost:8090/api/payment \
  -H 'Content-Type: application/json' \
  -d '{
    "card_number":"4111111111111111",
    "expiry_month":12,
    "expiry_year":2027,
    "currency":"GBP",
    "amount":1200,
    "cvv":"123"
  }' -i
```
#### Response: Authorized
- 201 Created
- Location: /api/payment/99b6bf60-7695-440b-802d-2873132d5743
- JSON body with "status": "Authorized" (last digit 1 → bank “authorized”).
```json
{"id":"99b6bf60-7695-440b-802d-2873132d5743","status":"Authorized","cardNumberLastFour":1111,"expiryMonth":12,"expiryYear":2027,"currency":"GBP","amount":1200}
```

#### 2) Decline path
Use an even last digit:
```bash
curl -X POST http://localhost:8090/api/payment \
  -H 'Content-Type: application/json' \
  -d '{
    "card_number":"4111111111111112",
    "expiry_month":12,
    "expiry_year":2027,
    "currency":"GBP",
    "amount":1200,
    "cvv":"123"
  }' -i
```
#### Response: Declined
- when the acquiring bank simulator declines the payment, e.g. card number ending in an even digit
- Expected: 201 with `status`: `Declined`:
```json
{"id":"2935d238-248b-4e97-ac49-829f293fc7b9","status":"Declined","cardNumberLastFour":1112,"expiryMonth":12,"expiryYear":2027,"currency":"GBP","amount":1200}
```
3) Bank unavailable path

Use last digit 0 (PAN ends with 0) → 502 Bad Gateway:
```bash
curl -X POST http://localhost:8090/api/payment \
  -H 'Content-Type: application/json' \
  -d '{
    "card_number":"4111111111111110",
    "expiry_month":12,
    "expiry_year":2027,
    "currency":"GBP",
    "amount":1200,
    "cvv":"123"
  }' -i
```
Expected: 502 Bad Gateway (not persisted) with message:
```json
{
  "message": "Bank temporarily unavailable"
}
```

4) Validation error (gateway rejects without calling bank)

Bad currency or expired card:
```bash
curl -X POST http://localhost:8090/api/payment \
  -H 'Content-Type: application/json' \
  -d '{
    "card_number":"4111111111111111",
    "expiry_month":1,
    "expiry_year":2020,
    "currency":"XXX",
    "amount":0,
    "cvv":"12"
  }' -i
```

#### Response: 400 Bad Request (validation error)
- when the gateway rejects invalid input before calling the bank, e.g. bad PAN length, expired card, unsupported currency, etc.
- Expected: 400 Bad Request with an ErrorResponse message
- First reason given when there are multiple reasons

**Response (HTTP 400):**
```json
{
  "message": "Invalid CVV"
}
```
### Example GET (retrieve a payment)

Use the `id` returned from the POST response:

```bash
curl -X GET http://localhost:8090/api/payment/{id}
```

Replace {id} with the UUID value, e.g.:
```bash
curl -X GET http://localhost:8090/api/payment/99b6bf60-7695-440b-802d-2873132d5743
```


## Design Notes
- Validation rules, masking, bank mapping, error codes, didn’t modify imposters/ or .editorconfig.
- Avoid logging sensitive fields (never PAN/CVV)
- Currency is validated against an allow-list of three ISO-4217 codes (GBP, EUR, USD). Other codes are rejected with a 400 Bad Request. This keeps the scope simple while demonstrating validation logic.
- The spec mentions “Rejected” as a payment outcome. In this implementation, invalid requests are rejected with HTTP 400 and an error payload instead of returning a PaymentResponse with status:"Rejected". I chose REST-friendly semantics and did not persist failed validations.

### Status mapping

| Condition                                         | Status      | Notes                                                                 |
|---------------------------------------------------|-------------|----------------------------------------------------------------------|
| All request fields valid, bank returns `authorized: true`  | Authorized  | Payment accepted by acquiring bank. Auth code stored (not exposed). |
| All request fields valid, bank returns `authorized: false` | Declined    | Payment declined by acquiring bank.                                 |
| All request fields valid, bank returns `503` (card ends in 0) | Error → surfaced as 502 Bad Gateway | Bank unavailable; not persisted as a payment. |
| Request validation fails (bad PAN length, expired card, unsupported currency, CVV invalid, amount ≤ 0) |  Rejected (HTTP 400) | Gateway rejects request without calling the bank; not persisted. |