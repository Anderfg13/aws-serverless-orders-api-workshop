package com.example.orders.lambda.proxy;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the HTTP responses of the adapter: status code, headers and JSON serialization. */
final class ApiResponses {

    static final ObjectMapper JSON = new ObjectMapper();

    private ApiResponses() {
    }

    static APIGatewayProxyResponseEvent json(int status, Object body) {
        return json(status, body, Map.of());
    }

    static APIGatewayProxyResponseEvent json(int status, Object body, Map<String, String> extraHeaders) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.putAll(extraHeaders);
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(status)
                    .withHeaders(headers)
                    .withBody(JSON.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize response", e);
        }
    }

    static APIGatewayProxyResponseEvent noContent() {
        return new APIGatewayProxyResponseEvent().withStatusCode(204);
    }

    static APIGatewayProxyResponseEvent error(int status, String code, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        return json(status, body);
    }
}
