package com.example.orders.lambda.mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.orders.application.OrderService;
import com.example.orders.domain.Order;
import com.example.orders.lambda.OrdersApplication;

/** Lambda handler: {@code com.example.orders.lambda.mapping.GetOrderHandler::handleRequest}. */
public class GetOrderHandler implements RequestHandler<GetOrderInput, Order> {

    private final OrderService service;

    public GetOrderHandler() {
        this(OrdersApplication.service());
    }

    public GetOrderHandler(OrderService service) {
        this.service = service;
    }

    @Override
    public Order handleRequest(GetOrderInput input, Context context) {
        try {
            return service.getOrder(input == null ? null : input.getId());
        } catch (Exception e) {
            throw MappedApiException.from(e);
        }
    }
}
