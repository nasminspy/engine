package com.example.engine.controller;

import com.example.engine.dto.OrderRequest;
import com.example.engine.dto.OrderResponse;
import com.example.engine.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> addOrder(@Valid @RequestBody OrderRequest request) {
        logger.debug("Received new order request: {}", request);
        OrderResponse response = orderService.addOrder(request);
        logger.info("Order created with ID: {}", response.getId());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable int id) {
        logger.info("Fetching order by ID: {}", id);
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    logger.warn("Order not found for ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<OrderResponse>> getOrdersBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.debug("Fetching orders for symbol '{}', page {}, size {}", symbol, page, size);
        List<OrderResponse> orders = orderService.getOrdersBySymbol(symbol, PageRequest.of(page, size));
        logger.info("Returning {} orders for symbol '{}'", orders.size(), symbol);
        return ResponseEntity.ok(orders);
    }
}
