# Guía paso a paso en AWS (consola web)

Todo lo de Java ya está hecho y probado localmente. Esta guía cubre lo que se hace en la consola de AWS.
Usa **una sola región** durante todo el laboratorio (ej. `us-east-1`) y **REST API** en API Gateway
(no "HTTP API": solo REST API soporta mapping templates / integración no-proxy).

---

## Paso 0 — Construir el JAR

```bash
mvn test
mvn package
```

El archivo a subir es `target/orders-api-1.0.0.jar` (≈3.4 MB, es un fat JAR con las dependencias).
---

## Parte II — Una función Lambda invocada manualmente

1. Consola AWS → **Lambda** → **Create function** → *Author from scratch*.
   * Name: `orders-create`
   * Runtime: **Java 17**
   * Architecture: x86_64
   * Permissions: **Change default execution role → Use an existing role → `LabRole`**.
     (En AWS Academy / Learner Lab no se permite `iam:CreateRole`; dejar el rol por defecto falla con "not authorized to perform: iam:CreateRole".)
2. En la pestaña **Code** → *Upload from* → **.zip or .jar file** → sube `target/orders-api-1.0.0.jar`.
3. En **Code → Runtime settings → Edit**, Handler:
   `com.example.orders.lambda.mapping.CreateOrderHandler::handleRequest`
4. **Configuration → General configuration → Edit**: Memory **512 MB**, Timeout **15 sec**
   (Java en arranque en frío es lento con 128 MB).
5. Pestaña **Test** → *Create new event* → pega el contenido de
   [`events/direct/create-order-macbook.json`](../events/direct/create-order-macbook.json) → **Test**.

6. Repite con otro valor: `events/direct/create-order-keyboard.json`, y con
   `events/direct/create-order-invalid.json` (debe fallar: mira `errorMessage`).

7. Abre el log en CloudWatch (botón *View CloudWatch logs*) y fíjate en la línea `REPORT` (duración, memoria).

**Investigación (para el README):** el runtime de Lambda para Java deserializa el JSON al tipo del primer
parámetro genérico de `RequestHandler<I, O>` (aquí `CreateOrderRequest`) usando getters/setters y
constructor sin argumentos, y serializa el objeto devuelto a JSON. Verifica con tus pruebas: si envías un campo
desconocido o un tipo incorrecto (`"quantity": "abc"`), ¿qué error da? Anota lo que observes.

---

## Parte III–IV — Arquitectura A: API Gateway con mapping templates (no-proxy)

### A.1 Crear las otras dos funciones Lambda

Repite el procedimiento anterior (mismo JAR, Java 17, 512 MB, 15 s):

| Función | Handler |
|---------|---------|
| `orders-get` | `com.example.orders.lambda.mapping.GetOrderHandler::handleRequest` |
| `orders-find` | `com.example.orders.lambda.mapping.FindOrdersHandler::handleRequest` |

Pruébalas con `events/direct/get-order-existing.json`, `get-order-missing.json`, `find-orders-cust-42.json`,
`find-orders-cust-100.json`.

> Nota: cada función es un entorno aislado; una orden creada en `orders-create` **no** existe en `orders-get`.
> Por eso hay órdenes de ejemplo precargadas (`ORD-001`, `ORD-002` de `CUST-42` y `ORD-003` de `CUST-7`).

### A.2 Crear la API

1. **API Gateway** → **Create API** → **REST API** (la normal, *Build*) → Name `orders-api-mapping`,
   Endpoint type *Regional*.
2. Crea recursos: **Create resource** `/orders`; luego dentro de `/orders` otro con path `{id}`
   (queda `/orders/{id}`).

### A.3 `GET /orders/{id}` → `orders-get`

1. En `/orders/{id}` → **Create method** → `GET`, Integration type **Lambda function**,
   **Lambda proxy integration: DESACTIVADO**, selecciona `orders-get`. (Acepta el permiso que pide.)
2. **Integration request → Edit**: *Request body passthrough* = **Never** ; *Mapping templates* → **Add**
   `application/json` con el contenido de
   [`aws/mapping-templates/get-order.request.vtl`](../aws/mapping-templates/get-order.request.vtl):
   ```
   { "id": "$input.params('id')" }
   ```

3. **Method response**: añade los códigos **404**, **400** y **500** (el **200** ya existe).
4. **Integration response**: la fila por defecto (200) se queda con regex vacío. **Create response** para cada error:

   | HTTP status | Lambda error regex | Mapping template (`application/json`) |
   |-------------|--------------------|----------------------------------------|
   | 404 | `.*ORDER_NOT_FOUND.*` | `$input.path('$.errorMessage')` |
   | 400 | `.*INVALID_ORDER.*` | `$input.path('$.errorMessage')` |
   | 500 | `.*INTERNAL_ERROR.*` | `$input.path('$.errorMessage')` |

   (contenido en [`aws/mapping-templates/error.response.vtl`](../aws/mapping-templates/error.response.vtl))

5. Pestaña **Test** de la consola: Path `{id}` = `ORD-001` → 200; `ORD-999` → 404.

### A.4 `GET /orders?customerId=` → `orders-find`

1. En `/orders` → método `GET`, Lambda `orders-find`, proxy **desactivado**.
2. **Method request → Edit**: *URL query string parameters* → añade `customerId` (marca *Required* si quieres que API Gateway
   rechace la petición sin parámetro; si no, llega vacío y Java responde 400 `INVALID_ORDER`).
3. Integration request, mapping template `application/json`:
   [`aws/mapping-templates/find-orders.request.vtl`](../aws/mapping-templates/find-orders.request.vtl)
   ```
   { "customerId": "$input.params('customerId')" }
   ```
4. Method responses 400 y 500, e integration responses igual que en A.3 (404 no aplica).

### A.5 `POST /orders` → `orders-create`

1. En `/orders` → método `POST`, Lambda `orders-create`, proxy **desactivado**.
2. Integration request → passthrough **Never**, mapping template `application/json`:
   [`aws/mapping-templates/create-order.request.vtl`](../aws/mapping-templates/create-order.request.vtl)
   ```
   $input.json('$')
   ```
3. **Method response**: elimina el 200 y crea **201**, además de 400 y 500.
   **Integration response**: edita la fila por defecto para que su *Method response status* sea **201**;
   añade 400 (`.*INVALID_ORDER.*`) y 500 (`.*INTERNAL_ERROR.*`) con la plantilla de error.

### A.6 Desplegar y probar

**Deploy API** → *New stage* `dev`. Copia la *Invoke URL*.

```bash
URL=https://u51q1hfu7e.execute-api.us-east-1.amazonaws.com/dev

curl -i "$URL/orders/ORD-001"                      # 200
curl -i "$URL/orders/ORD-999"                      # 404 ORDER_NOT_FOUND
curl -i "$URL/orders?customerId=CUST-42"           # 200, 2 órdenes
curl -i "$URL/orders?customerId=CUST-100"          # 200, []
curl -i "$URL/orders?customerId="                  # 400 INVALID_ORDER
curl -i -X POST "$URL/orders" -H "Content-Type: application/json" \
     -d '{"customerId":"CUST-42","product":"MacBook Air","quantity":1}'   # 201
curl -i -X POST "$URL/orders" -H "Content-Type: application/json" \
     -d '{"customerId":"CUST-42","product":"MacBook Air","quantity":0}'   # 400
```

Si algo sale `500 Internal server error` sin cuerpo útil: revisa que *Request body passthrough* no impida el template,
y revisa los logs de la función en CloudWatch.

**Checkpoint 3**: con esto funcionando, escribe en el README la lista de responsabilidades de API Gateway
(ya hay un borrador en el README; ajústalo a lo que viste).

---

## Parte V — Arquitectura B: Lambda proxy integration

### B.1 Función Lambda única

* Name `orders-api-proxy`, Java 17, 512 MB, 15 s, mismo JAR.
* Handler: `com.example.orders.lambda.proxy.OrdersApiHandler::handleRequest`
* **Configuration → Environment variables**: `LOG_EVENT` = `true` (solo para capturar el evento real; luego puedes quitarla).

### B.2 Crear una API REST nueva

1. **Create API** → **REST API** → `orders-api-proxy` (separada de la A para poder comparar).
2. Recursos: `/orders` y `/orders/{id}`.
3. Métodos, todos con **Lambda proxy integration ACTIVADO** y función `orders-api-proxy`:
   * `/orders` → `POST`, `GET`
   * `/orders/{id}` → `GET` (el `DELETE` se añade en el paso final)
4. No configures mapping templates ni method/integration responses: aquí no hacen falta.
5. **Deploy API** → stage `dev2`.

### B.3 Probar y capturar el evento real

```bash
URL=https://pz27uxbj89.execute-api.us-east-1.amazonaws.com/dev2

curl -i "$URL/orders/ORD-001"                      # 200
curl -i "$URL/orders/ORD-999"                      # 404
curl -i "$URL/orders?customerId=CUST-42"           # 200
curl -i -X POST "$URL/orders" -H "Content-Type: application/json" \
     -d '{"customerId":"CUST-42","product":"MacBook Air","quantity":1}'   # 201 + Location
curl -i -X POST "$URL/orders" -H "Content-Type: application/json" -d '{"customerId":"CUST-42"}'  # 400
curl -i -X POST "$URL/orders" -H "Content-Type: application/json" -d '{no es json'              # 400 INVALID_JSON
```

Ve a **CloudWatch → Log groups → /aws/lambda/orders-api-proxy** y busca la línea `EVENT: {...}`.
Copia ese JSON real, formatéalo y **reemplaza** el contenido de `events/proxy/*.json` si quieres (los de este repo son un ejemplo
escrito a mano siguiendo el esquema documentado).

En el README identifica dónde está: método (`httpMethod`), ruta (`path`/`resource`), `pathParameters`,
`queryStringParameters`, `headers` y `body`.

---

## Reto final — `DELETE /orders/{id}`

1. En local: `mvn package` (el código del DELETE ya está implementado).
2. En Lambda `orders-api-proxy` → **Upload from** `.jar` y sube el nuevo JAR.
3. En la API `orders-api-proxy` → recurso `/orders/{id}` → **Create method** → `DELETE`, **Lambda proxy ON**,
   función `orders-api-proxy`.
4. **Deploy API** al stage `dev2` (¡sin redeploy el método nuevo no existe!).
5. Probar (ojo: el estado es en memoria; si el entorno se reinicia, `ORD-001` vuelve a existir):

```bash
curl -i -X DELETE "$URL/orders/ORD-003"      # 204 sin cuerpo
curl -i -X DELETE "$URL/orders/ORD-003"      # 404 ORDER_NOT_FOUND
curl -i "$URL/orders/ORD-003"                # 404 (confirma el borrado)
```

Compara lo que necesitaste cambiar con lo que predijiste en [delete-prediction.md](delete-prediction.md).

---

## Error inesperado (500)

El 500 no es fácil de provocar a propósito en AWS sin romper el código. Lo demostramos con el test unitario
`unexpectedFailureReturns500WithoutLeakingDetails` (sale en `mvn test`): el adaptador devuelve
`500 INTERNAL_ERROR` con mensaje genérico y deja el detalle solo en CloudWatch. Explícalo en el video al mostrar los tests.

---

## Limpieza al terminar

Elimina las APIs (`orders-api-mapping`, `orders-api-proxy`), las funciones Lambda (`orders-*`) y los log groups de CloudWatch
para no dejar recursos. Los costos de este laboratorio son mínimos, pero no dejes APIs públicas abiertas.
