package com.example.orders.application;

import com.example.orders.domain.CreateOrderRequest;
import com.example.orders.domain.InvalidOrderException;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderNotFoundException;

import java.util.List;

/**
 * Application logic. Deals with orders, customers, products and quantities; it has
 * no idea that HTTP, API Gateway or Lambda exist. Business rules (validation) live here.
 */
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order createOrder(CreateOrderRequest request) {
        validate(request);
        Order order = new Order(
                repository.nextId(),
                request.getCustomerId().trim(),
                request.getProduct().trim(),
                request.getQuantity());
        return repository.save(order);
    }

    public Order getOrder(String id) {
        requireNotBlank(id, "id");
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> getOrdersByCustomer(String customerId) {
        requireNotBlank(customerId, "customerId");
        return repository.findByCustomerId(customerId);
    }

    private static void validate(CreateOrderRequest request) {
        if (request == null) {
            throw new InvalidOrderException("Order data is required");
        }
        requireNotBlank(request.getCustomerId(), "customerId");
        requireNotBlank(request.getProduct(), "product");
        if (request.getQuantity() <= 0) {
            throw new InvalidOrderException("quantity must be greater than zero");
        }
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidOrderException(field + " is required");
        }
    }
}
