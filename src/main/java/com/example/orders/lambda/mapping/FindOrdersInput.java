package com.example.orders.lambda.mapping;

/** Event shape built by the API Gateway mapping template: {@code {"customerId": "CUST-42"}}. */
public class FindOrdersInput {

    private String customerId;

    public FindOrdersInput() {
    }

    public FindOrdersInput(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
}
