package com.example.orders.lambda.proxy;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.example.orders.application.OrderRepository;
import com.example.orders.application.OrderService;
import com.example.orders.domain.Order;
import com.example.orders.infrastructure.InMemoryOrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The HTTP adapter tested locally with hand-built API Gateway events (no AWS needed). */
class OrdersApiHandlerTest {

    private OrdersApiHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrdersApiHandler(new OrderService(InMemoryOrderRepository.withSampleData()));
    }

    @Test
    void postCreatesOrderAndReturns201WithLocation() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("POST", "/orders", "/orders")
                        .withBody("{\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}"),
                null);

        assertEquals(201, response.getStatusCode());
        JsonNode body = json(response);
        assertEquals("CUST-42", body.get("customerId").asText());
        assertEquals("/orders/" + body.get("id").asText(), response.getHeaders().get("Location"));
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    void postWithInvalidQuantityReturns400() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("POST", "/orders", "/orders")
                        .withBody("{\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":0}"),
                null);

        assertEquals(400, response.getStatusCode());
        assertEquals("INVALID_ORDER", json(response).get("error").asText());
    }

    @Test
    void postWithoutBodyReturns400() throws Exception {
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("POST", "/orders", "/orders"), null);

        assertEquals(400, response.getStatusCode());
        assertEquals("INVALID_JSON", json(response).get("error").asText());
    }

    @Test
    void postWithMalformedJsonReturns400() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("POST", "/orders", "/orders").withBody("{not json"), null);

        assertEquals(400, response.getStatusCode());
        assertEquals("INVALID_JSON", json(response).get("error").asText());
    }

    @Test
    void getReturnsExistingOrderWith200() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("GET", "/orders/{id}", "/orders/ORD-001").withPathParameters(Map.of("id", "ORD-001")),
                null);

        assertEquals(200, response.getStatusCode());
        assertEquals("ORD-001", json(response).get("id").asText());
    }

    @Test
    void getMissingOrderReturns404() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("GET", "/orders/{id}", "/orders/ORD-999").withPathParameters(Map.of("id", "ORD-999")),
                null);

        assertEquals(404, response.getStatusCode());
        JsonNode body = json(response);
        assertEquals("ORDER_NOT_FOUND", body.get("error").asText());
        assertEquals("Order ORD-999 does not exist", body.get("message").asText());
    }

    @Test
    void deleteExistingOrderReturns204WithEmptyBody() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("DELETE", "/orders/{id}", "/orders/ORD-001").withPathParameters(Map.of("id", "ORD-001")),
                null);

        assertEquals(204, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void deleteMissingOrderReturns404() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("DELETE", "/orders/{id}", "/orders/ORD-999").withPathParameters(Map.of("id", "ORD-999")),
                null);

        assertEquals(404, response.getStatusCode());
        assertEquals("ORDER_NOT_FOUND", json(response).get("error").asText());
    }

    @Test
    void getAfterDeleteReturns404() {
        Map<String, String> id = Map.of("id", "ORD-001");
        handler.handleRequest(request("DELETE", "/orders/{id}", "/orders/ORD-001").withPathParameters(id), null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("GET", "/orders/{id}", "/orders/ORD-001").withPathParameters(id), null);

        assertEquals(404, response.getStatusCode());
    }

    @Test
    void searchReturnsCustomerOrders() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("GET", "/orders", "/orders").withQueryStringParameters(Map.of("customerId", "CUST-42")),
                null);

        assertEquals(200, response.getStatusCode());
        assertEquals(2, json(response).size());
    }

    @Test
    void searchForCustomerWithoutOrdersReturnsEmptyArray() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("GET", "/orders", "/orders").withQueryStringParameters(Map.of("customerId", "CUST-100")),
                null);

        assertEquals(200, response.getStatusCode());
        assertEquals(0, json(response).size());
    }

    @Test
    void searchWithoutCustomerIdReturns400() throws Exception {
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("GET", "/orders", "/orders"), null);

        assertEquals(400, response.getStatusCode());
        assertEquals("INVALID_ORDER", json(response).get("error").asText());
    }

    @Test
    void unknownRouteReturns404() throws Exception {
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("GET", "/customers", "/customers"), null);

        assertEquals(404, response.getStatusCode());
        assertEquals("ROUTE_NOT_FOUND", json(response).get("error").asText());
    }

    @Test
    void unsupportedMethodReturns405() {
        APIGatewayProxyResponseEvent response =
                handler.handleRequest(request("PUT", "/orders", "/orders"), null);

        assertEquals(405, response.getStatusCode());
    }

    @Test
    void unexpectedFailureReturns500WithoutLeakingDetails() throws Exception {
        OrderRepository broken = new OrderRepository() {
            public Order save(Order order) {
                throw new IllegalStateException("secret internal detail");
            }

            public Optional<Order> findById(String id) {
                throw new IllegalStateException("secret internal detail");
            }

            public List<Order> findByCustomerId(String customerId) {
                throw new IllegalStateException("secret internal detail");
            }

            public boolean deleteById(String id) {
                throw new IllegalStateException("secret internal detail");
            }

            public String nextId() {
                return "ORD-X";
            }
        };

        APIGatewayProxyResponseEvent response = new OrdersApiHandler(new OrderService(broken)).handleRequest(
                request("GET", "/orders/{id}", "/orders/ORD-001").withPathParameters(Map.of("id", "ORD-001")),
                null);

        assertEquals(500, response.getStatusCode());
        JsonNode body = json(response);
        assertEquals("INTERNAL_ERROR", body.get("error").asText());
        assertEquals("Unexpected error", body.get("message").asText());
    }

    private static APIGatewayProxyRequestEvent request(String method, String resource, String path) {
        return new APIGatewayProxyRequestEvent()
                .withHttpMethod(method)
                .withResource(resource)
                .withPath(path);
    }

    private static JsonNode json(APIGatewayProxyResponseEvent response) throws Exception {
        return ApiResponses.JSON.readTree(response.getBody());
    }
}
