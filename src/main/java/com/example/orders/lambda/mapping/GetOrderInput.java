package com.example.orders.lambda.mapping;

/** Event shape built by the API Gateway mapping template: {@code {"id": "ORD-42"}}. */
public class GetOrderInput {

    private String id;

    public GetOrderInput() {
    }

    public GetOrderInput(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
