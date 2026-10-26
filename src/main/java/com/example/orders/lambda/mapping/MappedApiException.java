package com.example.orders.lambda.mapping;

import com.example.orders.domain.InvalidOrderException;
import com.example.orders.domain.OrderNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Architecture A (API Gateway mapping templates): the Lambda cannot choose an HTTP status,
 * so it fails with an exception whose message is already the JSON error body. API Gateway
 * matches the message against its integration-response selection patterns
 * (ORDER_NOT_FOUND -> 404, INVALID_ORDER -> 400, INTERNAL_ERROR -> 500) and returns the
 * message as the response body.
 */
public class MappedApiException extends RuntimeException {

    private static final ObjectMapper JSON = new ObjectMapper();

    private MappedApiException(String json) {
        super(json);
    }

    /** Translates any failure of the application into the error contract. */
    public static MappedApiException from(Throwable failure) {
        if (failure instanceof OrderNotFoundException) {
            return of("ORDER_NOT_FOUND", failure.getMessage());
        }
        if (failure instanceof InvalidOrderException) {
            return of("INVALID_ORDER", failure.getMessage());
        }
        return of("INTERNAL_ERROR", "Unexpected error");
    }

    private static MappedApiException of(String code, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        try {
            return new MappedApiException(JSON.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            return new MappedApiException("{\"error\":\"INTERNAL_ERROR\",\"message\":\"Unexpected error\"}");
        }
    }
}
