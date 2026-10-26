# Example requests and responses

Expected behavior of the API. `$URL` is the stage invoke URL
(e.g. `https://abc123.execute-api.us-east-1.amazonaws.com/prod`). These are the contract's expected
results (verified locally by unit tests); real captures are in the README screenshots.

## POST /orders → 201 Created

```bash
curl -i -X POST "$URL/orders" -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-42","product":"MacBook Air","quantity":1}'
```

```
HTTP/1.1 201 Created
Content-Type: application/json
Location: /orders/ORD-004

{"id":"ORD-004","customerId":"CUST-42","product":"MacBook Air","quantity":1}
```

(`Location` is only produced by Architecture B.)

## POST /orders (invalid) → 400 Bad Request

```bash
curl -i -X POST "$URL/orders" -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-42","product":"MacBook Air","quantity":0}'
```

```
HTTP/1.1 400 Bad Request
Content-Type: application/json

{"error":"INVALID_ORDER","message":"quantity must be greater than zero"}
```

## GET /orders/{id} → 200 OK

```bash
curl -i "$URL/orders/ORD-001"
```

```
HTTP/1.1 200 OK
Content-Type: application/json

{"id":"ORD-001","customerId":"CUST-42","product":"MacBook Air","quantity":1}
```

## GET /orders/{id} → 404 Not Found

```bash
curl -i "$URL/orders/ORD-999"
```

```
HTTP/1.1 404 Not Found
Content-Type: application/json

{"error":"ORDER_NOT_FOUND","message":"Order ORD-999 does not exist"}
```

## GET /orders?customerId= → 200 OK

```bash
curl -i "$URL/orders?customerId=CUST-42"
```

```
HTTP/1.1 200 OK
Content-Type: application/json

[{"id":"ORD-001","customerId":"CUST-42","product":"MacBook Air","quantity":1},
 {"id":"ORD-002","customerId":"CUST-42","product":"Keyboard","quantity":2}]
```

`customerId=CUST-100` (no orders) → `200` with `[]`. Missing/blank `customerId` → `400 INVALID_ORDER`.

## DELETE /orders/{id} → 204 No Content

```bash
curl -i -X DELETE "$URL/orders/ORD-003"
```

```
HTTP/1.1 204 No Content
```

A second `DELETE` of the same id → `404 Not Found` with `ORDER_NOT_FOUND`.

## Malformed JSON (Architecture B) → 400

```bash
curl -i -X POST "$URL/orders" -H "Content-Type: application/json" -d '{not json'
```

```
HTTP/1.1 400 Bad Request

{"error":"INVALID_JSON","message":"Request body is not valid JSON for an order"}
```

## Direct Lambda invocation (Architecture A functions)

`orders-get` with `{"id": "ORD-001"}`:

```json
{"id":"ORD-001","customerId":"CUST-42","product":"MacBook Air","quantity":1}
```

`orders-get` with `{"id": "ORD-999"}` (the function fails; the status code is added later by API Gateway):

```json
{
  "errorMessage": "{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}",
  "errorType": "com.example.orders.lambda.mapping.MappedApiException",
  "stackTrace": ["..."]
}
```
