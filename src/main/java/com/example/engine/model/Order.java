package com.example.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Comparable<Order> {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private int id;
    private String symbol;
    private double price;
    private int quantity;
    private Type type;
    private long timestamp;

    public Order(String symbol, double price, int quantity, Type type) {
        this.id = COUNTER.incrementAndGet();
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.type = type;
        // Use System.currentTimeMillis() for more readable timestamps or System.nanoTime() for precision
        this.timestamp = System.nanoTime();
    }

    @Override
    public int compareTo(Order other) {
        // Buy orders: higher price prioritized; Sell orders: lower price prioritized
        int priceComparison = (this.type == Type.BUY)
                ? Double.compare(other.price, this.price)
                : Double.compare(this.price, other.price);

        if (priceComparison != 0) {
            return priceComparison;
        }
        // If prices equal, earlier timestamp has higher priority (FIFO)
        return Long.compare(this.timestamp, other.timestamp);
    }

    @Override
    public String toString() {
        return String.format(
                "Order{id=%d, symbol='%s', price=%.2f, quantity=%d, type=%s}",
                id, symbol, price, quantity, type
        );
    }

    public enum Type {
        BUY, SELL
    }
}
