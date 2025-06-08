package com.example.engine.service;

import com.example.engine.model.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class OrderManager {

    private final Map<String, PriorityQueue<Order>> buyOrders = new ConcurrentHashMap<>();
    private final Map<String, PriorityQueue<Order>> sellOrders = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> symbolLocks = new ConcurrentHashMap<>();

    public void addOrder(Order order) {
        ReentrantLock lock = symbolLocks.computeIfAbsent(order.getSymbol(), s -> new ReentrantLock());
        lock.lock();
        try {
            PriorityQueue<Order> queue = getQueue(order.getSymbol(), order.getType());
            queue.offer(order);
        } finally {
            lock.unlock();
        }
    }

    public void matchOrders(String symbol) {
        ReentrantLock lock = symbolLocks.computeIfAbsent(symbol, s -> new ReentrantLock());
        lock.lock();
        try {
            PriorityQueue<Order> buys = buyOrders.get(symbol);
            PriorityQueue<Order> sells = sellOrders.get(symbol);
            if (buys == null || sells == null || buys.isEmpty() || sells.isEmpty()) return;

            // While there are buy and sell orders and top buy price >= top sell price
            while (!buys.isEmpty() && !sells.isEmpty() && buys.peek().getPrice() >= sells.peek().getPrice()) {
                Order buy = buys.peek();   // highest priority buy order (highest price, earliest time)
                Order sell = sells.peek(); // highest priority sell order (lowest price, earliest time)

                // Calculate matched quantity as min of buy and sell quantities
                int matchedQty = Math.min(buy.getQuantity(), sell.getQuantity());

                // Reduce quantities by matched amount to reflect partial or full matches
                buy.setQuantity(buy.getQuantity() - matchedQty);
                sell.setQuantity(sell.getQuantity() - matchedQty);

                // Remove fully matched buy orders from queue
                if (buy.getQuantity() == 0) buys.poll();

                // Remove fully matched sell orders from queue
                if (sell.getQuantity() == 0) sells.poll();
            }
        } finally {
            lock.unlock();
        }
    }

    public PriorityQueue<Order> getBuyOrders(String symbol) {
        return buyOrders.getOrDefault(symbol, emptyQueue());
    }

    public PriorityQueue<Order> getSellOrders(String symbol) {
        return sellOrders.getOrDefault(symbol, emptyQueue());
    }

    private PriorityQueue<Order> getQueue(String symbol, Order.Type type) {
        Map<String, PriorityQueue<Order>> map = (type == Order.Type.BUY) ? buyOrders : sellOrders;
        return map.computeIfAbsent(symbol, k -> new PriorityQueue<>());
    }

    private PriorityQueue<Order> emptyQueue() {
        return new PriorityQueue<>();
    }
}
