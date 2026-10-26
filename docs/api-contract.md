# Orders API — Contract

Designed **before** writing any Java code. The contract is the same for both
architectures (API Gateway mapping templates and Lambda proxy integration).

## Resource model

| Resource | URL | Meaning |
|----------|-----|---------|
| Orders collection | `/orders` | The set of all orders |
| Order | `/orders/{id}` | One specific order |

## Order representation

```json
{
  "id": "ORD-001",
  "customerId": "CUST-42",
  "product": "MacBook Air",
  "quantity": 1
}
```

## Error representation (all errors)

```json
{
  "error": "ORDER_NOT_FOUND",
  "message": "Order ORD-999 does not exist"
}
```

* `error` — stable, machine-readable code (clients can `switch` on it).
* `message` — human-readable explanation (may change, never parsed by clients).

## Operations

### 1. Create an order — `POST /orders`

| Item | Value |
|------|-------|
| Path / query params | none |
| Request body | `{"customerId": "CUST-42", "product": "MacBook Air", "quantity": 1}` |
| Success | **201 Created**, body = created order (with generated `id`), header `Location: /orders/{id}` |
| `400 Bad Request` | `INVALID_ORDER` — missing/blank `customerId` or `product`, `quantity <= 0` |
| `400 Bad Request` | `INVALID_JSON` — body missing or not valid JSON (proxy integration) |
| `500 Internal Server Error` | `INTERNAL_ERROR` — unexpected failure |

### 2. Retrieve an order — `GET /orders/{id}`

| Item | Value |
|------|-------|
| Path param | `id` — order identifier, e.g. `ORD-001` |
| Query params | none |
| Success | **200 OK**, body = order |
| `404 Not Found` | `ORDER_NOT_FOUND` |
| `500 Internal Server Error` | `INTERNAL_ERROR` |

### 3. Find orders by customer — `GET /orders?customerId={customerId}`

| Item | Value |
|------|-------|
| Path params | none |
| Query param | `customerId` (required) |
| Success | **200 OK**, body = JSON array of orders. A customer with no orders yields `[]` (it is a valid, empty search result — not a 404) |
| `400 Bad Request` | `INVALID_ORDER` — `customerId` missing or blank |
| `500 Internal Server Error` | `INTERNAL_ERROR` |

### 4. Delete an order — `DELETE /orders/{id}` *(final challenge)*

| Item | Value |
|------|-------|
| Path param | `id` |
| Success | **204 No Content**, empty body |
| `404 Not Found` | `ORDER_NOT_FOUND` |
| `500 Internal Server Error` | `INTERNAL_ERROR` |

## Status code summary

| Code | Used when |
|------|-----------|
| 200 OK | Successful `GET` |
| 201 Created | Successful `POST` |
| 204 No Content | Successful `DELETE` |
| 400 Bad Request | Invalid input |
| 404 Not Found | Order (or route) does not exist |
| 500 Internal Server Error | Anything unexpected; generic message, details only in logs |
