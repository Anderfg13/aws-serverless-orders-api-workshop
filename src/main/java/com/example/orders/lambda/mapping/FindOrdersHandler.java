package com.example.orders.lambda.mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.orders.application.OrderService;
import com.example.orders.domain.Order;
import com.example.orders.lambda.OrdersApplication;

import java.util.List;

/** Lambda handler: {@code com.example.orders.lambda.mapping.FindOrdersHandler::handleRequest}. */
public class FindOrdersHandler implements RequestHandler<FindOrdersInput, List<Order>> {

    private final OrderService service;

    public FindOrdersHandler() {
        this(OrdersApplication.service());
    }

    public FindOrdersHandler(OrderService service) {
        this.service = service;
    }

    @Override
    public List<Order> handleRequest(FindOrdersInput input, Context context) {
        try {
            return service.getOrdersByCustomer(input == null ? null : input.getCustomerId());
        } catch (Exception e) {
            throw MappedApiException.from(e);
        }
    }
}
