package com.example.orders.lambda.proxy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.example.orders.application.OrderService;
import com.example.orders.domain.CreateOrderRequest;
import com.example.orders.domain.InvalidOrderException;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderNotFoundException;
import com.example.orders.lambda.OrdersApplication;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;

/**
 * Architecture B (Lambda proxy integration): the HTTP adapter.
 *
 * <p>This is the only class that knows about API Gateway events, routes, path/query
 * parameters, headers and HTTP status codes. It translates the HTTP request into calls to
 * {@link OrderService} and translates results and domain exceptions back into HTTP.
 *
 * <p>Lambda handler: {@code com.example.orders.lambda.proxy.OrdersApiHandler::handleRequest}.
 */
public class OrdersApiHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String ORDERS = "/orders";
    private static final String ORDER_BY_ID = "/orders/{id}";

    private final OrderService service;

    public OrdersApiHandler() {
        this(OrdersApplication.service());
    }

    public OrdersApiHandler(OrderService service) {
        this.service = service;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        logRawEventIfRequested(event, context);
        try {
            return route(event);
        } catch (OrderNotFoundException e) {
            return ApiResponses.error(404, "ORDER_NOT_FOUND", e.getMessage());
        } catch (InvalidOrderException e) {
            return ApiResponses.error(400, "INVALID_ORDER", e.getMessage());
        } catch (InvalidJsonException e) {
            return ApiResponses.error(400, "INVALID_JSON", e.getMessage());
        } catch (Exception e) {
            log(context, e);
            return ApiResponses.error(500, "INTERNAL_ERROR", "Unexpected error");
        }
    }

    private APIGatewayProxyResponseEvent route(APIGatewayProxyRequestEvent event) {
        String method = event.getHttpMethod();
        String resource = event.getResource();

        if (ORDERS.equals(resource)) {
            return switch (method) {
                case "POST" -> createOrder(event);
                case "GET" -> findOrders(event);
                default -> methodNotAllowed(method, resource);
            };
        }
        if (ORDER_BY_ID.equals(resource)) {
            return switch (method) {
                case "GET" -> getOrder(event);
                case "DELETE" -> deleteOrder(event);
                default -> methodNotAllowed(method, resource);
            };
        }
        return ApiResponses.error(404, "ROUTE_NOT_FOUND", "No route for " + method + " " + event.getPath());
    }

    private APIGatewayProxyResponseEvent createOrder(APIGatewayProxyRequestEvent event) {
        Order created = service.createOrder(parseBody(event.getBody()));
        return ApiResponses.json(201, created, Map.of("Location", ORDERS + "/" + created.getId()));
    }

    private APIGatewayProxyResponseEvent getOrder(APIGatewayProxyRequestEvent event) {
        return ApiResponses.json(200, service.getOrder(pathParameter(event, "id")));
    }

    private APIGatewayProxyResponseEvent deleteOrder(APIGatewayProxyRequestEvent event) {
        service.deleteOrder(pathParameter(event, "id"));
        return ApiResponses.noContent();
    }

    private APIGatewayProxyResponseEvent findOrders(APIGatewayProxyRequestEvent event) {
        Map<String, String> query = event.getQueryStringParameters();
        String customerId = query == null ? null : query.get("customerId");
        return ApiResponses.json(200, service.getOrdersByCustomer(customerId));
    }

    private static String pathParameter(APIGatewayProxyRequestEvent event, String name) {
        Map<String, String> parameters = event.getPathParameters();
        return parameters == null ? null : parameters.get(name);
    }

    private static CreateOrderRequest parseBody(String body) {
        if (body == null || body.isBlank()) {
            throw new InvalidJsonException("Request body is required");
        }
        try {
            return ApiResponses.JSON.readValue(body, CreateOrderRequest.class);
        } catch (JsonProcessingException e) {
            throw new InvalidJsonException("Request body is not valid JSON for an order");
        }
    }

    private static APIGatewayProxyResponseEvent methodNotAllowed(String method, String resource) {
        return ApiResponses.error(405, "METHOD_NOT_ALLOWED", method + " is not supported on " + resource);
    }

    /** Set the environment variable LOG_EVENT=true to inspect the real event in CloudWatch. */
    private static void logRawEventIfRequested(APIGatewayProxyRequestEvent event, Context context) {
        if (!"true".equals(System.getenv("LOG_EVENT")) || context == null) {
            return;
        }
        try {
            context.getLogger().log("EVENT: " + ApiResponses.JSON.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            context.getLogger().log("EVENT: <not serializable> " + e.getMessage());
        }
    }

    private static void log(Context context, Exception e) {
        if (context != null && context.getLogger() != null) {
            context.getLogger().log("Unexpected error: " + e);
        }
        e.printStackTrace();
    }

    /** Malformed HTTP payload: an adapter concern, never reaches the application logic. */
    private static class InvalidJsonException extends RuntimeException {
        InvalidJsonException(String message) {
            super(message);
        }
    }
}
