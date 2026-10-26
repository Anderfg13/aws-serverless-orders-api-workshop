package com.example.orders.infrastructure;

import com.example.orders.application.OrderRepository;
import com.example.orders.domain.Order;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory storage. NOT persistent: a Lambda execution environment is ephemeral, and
 * different Lambda functions (or concurrent environments of the same function) each have
 * their own copy. Predefined orders are seeded so that reads work in every environment.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    public InMemoryOrderRepository() {
    }

    /** Repository pre-loaded with the sample data used across the workshop. */
    public static InMemoryOrderRepository withSampleData() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        repository.save(new Order(repository.nextId(), "CUST-42", "MacBook Air", 1));
        repository.save(new Order(repository.nextId(), "CUST-42", "Keyboard", 2));
        repository.save(new Order(repository.nextId(), "CUST-7", "Mouse", 1));
        return repository;
    }

    @Override
    public Order save(Order order) {
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        return orders.values().stream()
                .filter(order -> order.getCustomerId().equals(customerId))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
    }

    @Override
    public String nextId() {
        return String.format("ORD-%03d", sequence.incrementAndGet());
    }
}
