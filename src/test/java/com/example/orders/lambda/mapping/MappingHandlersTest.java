package com.example.orders.lambda.mapping;

import com.example.orders.application.OrderService;
import com.example.orders.domain.CreateOrderRequest;
import com.example.orders.domain.Order;
import com.example.orders.infrastructure.InMemoryOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Architecture A handlers invoked directly, as the Lambda runtime would (context is unused). */
class MappingHandlersTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(InMemoryOrderRepository.withSampleData());
    }

    @Test
    void createHandlerReturnsOrder() {
        Order order = new CreateOrderHandler(service)
                .handleRequest(new CreateOrderRequest("CUST-42", "MacBook Air", 1), null);

        assertEquals("CUST-42", order.getCustomerId());
    }

    @Test
    void createHandlerReportsInvalidOrder() {
        MappedApiException error = assertThrows(MappedApiException.class,
                () -> new CreateOrderHandler(service)
                        .handleRequest(new CreateOrderRequest("CUST-42", "MacBook Air", 0), null));

        assertTrue(error.getMessage().contains("\"error\":\"INVALID_ORDER\""));
    }

    @Test
    void getHandlerReturnsExistingOrder() {
        Order order = new GetOrderHandler(service).handleRequest(new GetOrderInput("ORD-001"), null);

        assertEquals("ORD-001", order.getId());
    }

    @Test
    void getHandlerReportsMissingOrder() {
        MappedApiException error = assertThrows(MappedApiException.class,
                () -> new GetOrderHandler(service).handleRequest(new GetOrderInput("ORD-999"), null));

        assertEquals("{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}",
                error.getMessage());
    }

    @Test
    void getHandlerReportsMissingInputAsInvalid() {
        MappedApiException error = assertThrows(MappedApiException.class,
                () -> new GetOrderHandler(service).handleRequest(null, null));

        assertTrue(error.getMessage().contains("INVALID_ORDER"));
    }

    @Test
    void findHandlerReturnsCustomerOrders() {
        List<Order> orders = new FindOrdersHandler(service)
                .handleRequest(new FindOrdersInput("CUST-42"), null);

        assertEquals(2, orders.size());
    }

    @Test
    void findHandlerReturnsEmptyListForUnknownCustomer() {
        assertTrue(new FindOrdersHandler(service)
                .handleRequest(new FindOrdersInput("CUST-100"), null).isEmpty());
    }

    @Test
    void unexpectedFailuresAreReportedAsInternalError() {
        MappedApiException error = MappedApiException.from(new IllegalStateException("db down"));

        assertEquals("{\"error\":\"INTERNAL_ERROR\",\"message\":\"Unexpected error\"}", error.getMessage());
    }
}
