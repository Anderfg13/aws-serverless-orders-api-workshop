package com.example.orders.lambda;

import com.example.orders.application.OrderService;
import com.example.orders.infrastructure.InMemoryOrderRepository;

/**
 * Composition root shared by the Lambda entry points. The service is created once per
 * execution environment (cold start) and reused while the environment stays warm.
 */
public final class OrdersApplication {

    private static final OrderService SERVICE =
            new OrderService(InMemoryOrderRepository.withSampleData());

    private OrdersApplication() {
    }

    public static OrderService service() {
        return SERVICE;
    }
}
