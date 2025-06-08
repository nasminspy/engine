package com.example.engine.dto;

import com.example.engine.model.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private int id;
    private String symbol;
    private double price;
    private int quantity;
    private Order.Type type;

    public static OrderResponse fromOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order must not be null");
        }
        return new OrderResponse(
                order.getId(),
                order.getSymbol(),
                order.getPrice(),
                order.getQuantity(),
                order.getType()
        );
    }
}
