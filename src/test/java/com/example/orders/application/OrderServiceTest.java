package com.example.orders.application;

import com.example.orders.domain.CreateOrderRequest;
import com.example.orders.domain.InvalidOrderException;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderNotFoundException;
import com.example.orders.infrastructure.InMemoryOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure JUnit: no AWS, no HTTP, no web server. */
class OrderServiceTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(InMemoryOrderRepository.withSampleData());
    }

    @Test
    void createsValidOrder() {
        Order created = service.createOrder(new CreateOrderRequest("CUST-100", "Monitor", 3));

        assertNotNull(created.getId());
        assertEquals("CUST-100", created.getCustomerId());
        assertEquals("Monitor", created.getProduct());
        assertEquals(3, created.getQuantity());
    }

    @Test
    void createdOrderCanBeRetrieved() {
        Order created = service.createOrder(new CreateOrderRequest("CUST-100", "Monitor", 3));

        assertEquals(created, service.getOrder(created.getId()));
    }

    @Test
    void generatesDifferentIdsForEachOrder() {
        Order first = service.createOrder(new CreateOrderRequest("CUST-1", "A", 1));
        Order second = service.createOrder(new CreateOrderRequest("CUST-1", "B", 1));

        assertTrue(!first.getId().equals(second.getId()));
    }

    @Test
    void retrievesExistingOrder() {
        Order order = service.getOrder("ORD-001");

        assertEquals("CUST-42", order.getCustomerId());
        assertEquals("MacBook Air", order.getProduct());
        assertEquals(1, order.getQuantity());
    }

    @Test
    void failsWhenOrderDoesNotExist() {
        OrderNotFoundException error =
                assertThrows(OrderNotFoundException.class, () -> service.getOrder("ORD-999"));

        assertEquals("Order ORD-999 does not exist", error.getMessage());
        assertEquals("ORD-999", error.getOrderId());
    }

    @Test
    void findsOrdersByCustomer() {
        List<Order> orders = service.getOrdersByCustomer("CUST-42");

        assertEquals(2, orders.size());
        assertTrue(orders.stream().allMatch(o -> o.getCustomerId().equals("CUST-42")));
    }

    @Test
    void returnsEmptyListForCustomerWithoutOrders() {
        assertTrue(service.getOrdersByCustomer("CUST-100").isEmpty());
    }

    @Test
    void includesNewOrderInCustomerSearch() {
        service.createOrder(new CreateOrderRequest("CUST-100", "Monitor", 3));

        assertEquals(1, service.getOrdersByCustomer("CUST-100").size());
    }

    @Test
    void rejectsNullRequest() {
        assertThrows(InvalidOrderException.class, () -> service.createOrder(null));
    }

    @Test
    void rejectsMissingCustomer() {
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest(null, "Monitor", 1)));
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("  ", "Monitor", 1)));
    }

    @Test
    void rejectsMissingProduct() {
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-1", "", 1)));
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-1", "Monitor", 0)));
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-1", "Monitor", -5)));
    }

    @Test
    void rejectsBlankIdOnLookup() {
        assertThrows(InvalidOrderException.class, () -> service.getOrder(" "));
    }

    @Test
    void rejectsBlankCustomerOnSearch() {
        assertThrows(InvalidOrderException.class, () -> service.getOrdersByCustomer(null));
    }
}
