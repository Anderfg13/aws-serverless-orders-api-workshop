package com.example.orders.lambda.mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.orders.application.OrderService;
import com.example.orders.domain.CreateOrderRequest;
import com.example.orders.domain.Order;
import com.example.orders.lambda.OrdersApplication;

/**
 * Lambda handler: {@code com.example.orders.lambda.mapping.CreateOrderHandler::handleRequest}.
 * The Lambda runtime turns the JSON event into a {@link CreateOrderRequest} and the returned
 * {@link Order} back into JSON.
 */
public class CreateOrderHandler implements RequestHandler<CreateOrderRequest, Order> {

    private final OrderService service;

    public CreateOrderHandler() {
        this(OrdersApplication.service());
    }

    public CreateOrderHandler(OrderService service) {
        this.service = service;
    }

    @Override
    public Order handleRequest(CreateOrderRequest input, Context context) {
        try {
            return service.createOrder(input);
        } catch (Exception e) {
            throw MappedApiException.from(e);
        }
    }
}
