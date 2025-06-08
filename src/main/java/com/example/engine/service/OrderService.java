package com.example.engine.service;

import com.example.engine.dto.OrderRequest;
import com.example.engine.dto.OrderResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    OrderResponse addOrder(OrderRequest request);

    Optional<OrderResponse> getOrderById(int id);

    List<OrderResponse> getOrdersBySymbol(String symbol, Pageable pageable);
}
