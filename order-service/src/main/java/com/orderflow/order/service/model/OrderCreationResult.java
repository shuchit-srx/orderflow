package com.orderflow.order.service.model;

import com.orderflow.order.dto.OrderResponse;

public record OrderCreationResult(
        OrderResponse order,
        boolean createdNew
) {

    public static OrderCreationResult created(
            OrderResponse order
    ) {

        return new OrderCreationResult(
                order,
                true
        );
    }

    public static OrderCreationResult replay(
            OrderResponse order
    ) {

        return new OrderCreationResult(
                order,
                false
        );
    }
}