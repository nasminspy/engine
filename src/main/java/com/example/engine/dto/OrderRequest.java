package com.example.engine.dto;

import com.example.engine.model.Order;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotBlank
    private String symbol;

    @Min(0)
    private double price;

    @Min(1)
    private int quantity;

    @NotNull
    private Order.Type type;
}
