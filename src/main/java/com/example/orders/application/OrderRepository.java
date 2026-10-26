package com.example.orders.application;

import com.example.orders.domain.Order;

import java.util.List;
import java.util.Optional;

/** Storage port. Today in-memory; could be DynamoDB later without touching OrderService. */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> findByCustomerId(String customerId);

    /** @return true if an order was removed, false if it did not exist */
    boolean deleteById(String id);

    /** Next identifier, e.g. ORD-004. */
    String nextId();
}
