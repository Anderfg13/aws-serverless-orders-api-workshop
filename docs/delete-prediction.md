# Prediction — `DELETE /orders/{id}`

Written **before** asking the AI assistant to implement the feature and committed
before any change to the code (see git history: this file's commit precedes the
`feat: add delete order ...` commit).

## Expected behavior

| Request | Result |
|---------|--------|
| `DELETE /orders/ORD-001` (exists) | `204 No Content`, empty body |
| `DELETE /orders/ORD-999` (missing) | `404 Not Found` + `ORDER_NOT_FOUND` error JSON |

## Questions

**Which Java classes should change?**

* `OrderRepository` — new `deleteById(String id)` operation (returns whether something was removed).
* `InMemoryOrderRepository` — implement it.
* `OrderService` — new `deleteOrder(String id)`.
* `OrdersApiHandler` — new `DELETE` branch in the `/orders/{id}` route.

Not expected to change: `Order`, `CreateOrderRequest`, the exceptions, the
Architecture A handlers (`mapping` package) and `ApiResponses` (it already has `noContent()`).

**Do I need a new Lambda function?**
No. The proxy integration uses a single Lambda for the whole API; routing is done
inside `OrdersApiHandler`.

**Does the API Gateway configuration need to change?**
Yes, but only one thing: add a `DELETE` method on the `/orders/{id}` resource, using
Lambda proxy integration pointing to the existing function, and redeploy the stage.
No mapping templates and no status-code configuration are needed, because the adapter
decides the status code.

**Where should the deletion logic reside?**
In the application layer: `OrderService.deleteOrder` decides that a missing order is an
error (`OrderNotFoundException`); the repository only knows how to remove an entry.

**Where should the decision to return 204 or 404 reside?**
In the HTTP adapter (`OrdersApiHandler`). The service only reports a business outcome
(deleted, or `OrderNotFoundException`); translating that into `204` or `404` is an HTTP concern.
The `404` path already exists (the adapter maps `OrderNotFoundException`), so only the
success path (`204`) is new.

**Which tests should be added?**

* `OrderServiceTest`: delete existing order; deleted order can no longer be retrieved;
  delete missing order throws `OrderNotFoundException`; blank id is rejected.
* `OrdersApiHandlerTest`: `DELETE` existing returns `204` with an empty body; `DELETE`
  missing returns `404` with `ORDER_NOT_FOUND`; a `GET` after a successful `DELETE` returns `404`.
