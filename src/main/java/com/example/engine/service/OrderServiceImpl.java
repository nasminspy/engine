package com.example.engine.service;

import com.example.engine.dto.OrderRequest;
import com.example.engine.dto.OrderResponse;
import com.example.engine.model.Order;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final Map<Integer, Order> allOrders = new ConcurrentHashMap<>();
    private final BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Order> deadLetterQueue = new LinkedBlockingQueue<>();

    private final ExecutorService executorService;
    private final OrderManager orderManager;
    private final int maxRetryAttempts;

    public OrderServiceImpl(
            OrderManager orderManager,
            @Value("${order.processor.pool-size:4}") int poolSize,
            @Value("${order.processor.retry-count:3}") int maxRetryAttempts
    ) {
        this.orderManager = orderManager;
        this.maxRetryAttempts = maxRetryAttempts;
        this.executorService = Executors.newFixedThreadPool(poolSize);
        for (int i = 0; i < poolSize; i++) {
            executorService.submit(this::processOrders);
        }
        logger.info("Order processor started with pool size: {}, max retries: {}", poolSize, maxRetryAttempts);
    }

    @Override
    public OrderResponse addOrder(OrderRequest request) {
        Order order = new Order(request.getSymbol(), request.getPrice(), request.getQuantity(), request.getType());
        allOrders.put(order.getId(), order);
        enqueueOrder(order);
        return OrderResponse.fromOrder(order);
    }

    private void enqueueOrder(Order order) {
        try {
            orderQueue.put(order);
            logger.info("Order queued: {}", order);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while enqueuing order", e);
            throw new RuntimeException("Order queuing failed", e);
        }
    }

    @Override
    public Optional<OrderResponse> getOrderById(int id) {
        return Optional.ofNullable(allOrders.get(id))
                .map(OrderResponse::fromOrder);
    }

    @Override
    public List<OrderResponse> getOrdersBySymbol(String symbol, Pageable pageable) {
        Stream<Order> orderStream = Stream.concat(
                orderManager.getBuyOrders(symbol).stream(),
                orderManager.getSellOrders(symbol).stream()
        );
        return orderStream
                .sorted()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(OrderResponse::fromOrder)
                .collect(Collectors.toList());
    }

    private void processOrders() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Order order = orderQueue.take();
                if (!attemptProcessing(order)) {
                    deadLetterQueue.offer(order);
                    logger.error("Moved to dead-letter queue: {}", order);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Processor thread interrupted, shutting down");
            } catch (Exception e) {
                logger.error("Unexpected processing error", e);
            }
        }
    }

    private boolean attemptProcessing(Order order) {
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                orderManager.addOrder(order);
                orderManager.matchOrders(order.getSymbol());
                logger.info("Order processed (attempt {}): {}", attempt, order);
                return true;
            } catch (Exception e) {
                logger.warn("Attempt {} failed for order {}: {}", attempt, order.getId(), e.getMessage());
                backoff(attempt);
            }
        }
        return false;
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(100L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        logger.info("Shutting down order processor...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Forced shutdown of order processor...");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Shutdown interrupted", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
