# Checklist de capturas para el README

Guarda cada captura en `docs/images/` con **exactamente** el nombre indicado (formato `.png`).
El README ya tiene las referencias a estos archivos: en cuanto los copies a la carpeta, se verán
sin tocar nada. Marca la casilla cuando la tengas.

| ✔ | Archivo | Qué debe verse | Sección del README |
|---|---------|----------------|--------------------|
| ☐ | `01-mvn-test.png` | Terminal con `mvn test` → `Tests run: 41, Failures: 0` y `BUILD SUCCESS` | Part I |
| ☐ | `02-mvn-package.png` | Terminal con `mvn package` → `BUILD SUCCESS` y el listado del JAR en `target/` | Part I |
| ☐ | `03-jar-contents.png` | Contenido del JAR (`jar tf target/orders-api-1.0.0.jar \| head -40`) mostrando `com/example/orders/...` | Checkpoint 1 |
| ☐ | `05-lambda-create-macbook.png` | Consola Lambda `orders-create`, pestaña Test: evento de entrada y resultado `Succeeded` con el JSON de la orden | Part II |
| ☐ | `06-lambda-create-keyboard.png` | Lo mismo con el segundo valor de entrada (`CUST-100` / `Keyboard`) | Part II |
| ☐ | `07-lambda-create-invalid.png` | Resultado con `quantity: 0`: `errorMessage` con `INVALID_ORDER` | Part II |
| ☐ | `08-lambda-get-existing.png` | `orders-get` con `{"id":"ORD-001"}` → orden | Part II |
| ☐ | `09-lambda-get-missing.png` | `orders-get` con `{"id":"ORD-999"}` → error `ORDER_NOT_FOUND` | Part II |
| ☐ | `10-apigw-get-integration-request.png` | API Gateway (API mapping) → `GET /orders/{id}` → Integration request con el mapping template | Part IV |
| ☐ | `11-apigw-get-integration-response.png` | Integration response con las filas 200/404/400/500 y sus regex | Part IV |
| ☐ | `12-curl-a-get-200.png` | `curl -i .../dev/orders/ORD-001` → `HTTP 200` + JSON | Part IV |
| ☐ | `13-curl-a-get-404.png` | `curl -i .../dev/orders/ORD-999` → `HTTP 404` + JSON de error | Part IV |
| ☐ | `14-curl-a-find.png` | `curl -i ".../dev/orders?customerId=CUST-42"` y `CUST-100` (puede ser una sola captura con ambos) | Part IV |
| ☐ | `15-curl-a-post-201.png` | `curl -i -X POST ...` → `HTTP 201` + orden creada | Part IV |
| ☐ | `16-curl-a-post-400.png` | POST con `quantity: 0` → `HTTP 400` + JSON de error | Part IV |
| ☐ | `17-apigw-a-resources.png` | Árbol de recursos/métodos de `orders-api-mapping` (`/orders` GET/POST, `/orders/{id}` GET) | Part IV |
| ☐ | `18-apigw-b-resources.png` | Árbol de recursos/métodos de `orders-api-proxy` mostrando integración proxy | Part V |
| ☐ | `19-curl-b-get-200.png` | `curl -i .../prod/orders/ORD-001` → 200 | Part V |
| ☐ | `20-curl-b-get-404.png` | `curl -i .../prod/orders/ORD-999` → 404 | Part V |
| ☐ | `21-curl-b-post-201-location.png` | POST → `201` y **el header `Location`** visible | Part V |
| ☐ | `22-curl-b-post-400.png` | POST inválido → `400` | Part V |
| ☐ | `23-cloudwatch-proxy-event.png` | CloudWatch Logs de `orders-api-proxy` con la línea `EVENT: {...}` (evento real) | Part V |
| ☐ | `24-apigw-b-delete-method.png` | `orders-api-proxy` con el método `DELETE` creado en `/orders/{id}` | Final challenge |
| ☐ | `25-curl-b-delete-204.png` | `curl -i -X DELETE .../orders/ORD-003` → `HTTP 204` sin cuerpo | Final challenge |
| ☐ | `26-curl-b-delete-404.png` | Segundo DELETE (o inexistente) → `HTTP 404` | Final challenge |

Notas:

* `04` está libre a propósito (se reservaba para el diagrama de la Parte II, que ya está hecho con Mermaid en el README).
* Tapa o recorta tus datos sensibles: el *Account ID* y, si quieres, la URL de la API. Nunca captures claves de acceso.
* Si repites un curl para que se vea mejor `-i` (headers), agranda la terminal para que quepa todo en una captura.
* También necesitas el **video** (ver la lista al final de `AWS_STEP_BY_STEP.md`); no va dentro de `docs/images/`.
