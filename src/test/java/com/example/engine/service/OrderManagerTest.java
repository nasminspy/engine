package com.example.engine.service;

import com.example.engine.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

class OrderManagerTest {

    private OrderManager orderManager;

    @BeforeEach
    void setUp() {
        orderManager = new OrderManager();
    }

    @Test
    void testAddBuyOrderAndRetrieve() {
        Order order = new Order("AAPL", 150.0, 10, Order.Type.BUY);
        orderManager.addOrder(order);

        PriorityQueue<Order> buyOrders = orderManager.getBuyOrders("AAPL");
        assertEquals(1, buyOrders.size());
        assertEquals(order, buyOrders.peek());

        PriorityQueue<Order> sellOrders = orderManager.getSellOrders("AAPL");
        assertTrue(sellOrders.isEmpty());
    }

    @Test
    void testAddSellOrderAndRetrieve() {
        Order order = new Order("AAPL", 155.0, 5, Order.Type.SELL);
        orderManager.addOrder(order);

        PriorityQueue<Order> sellOrders = orderManager.getSellOrders("AAPL");
        assertEquals(1, sellOrders.size());
        assertEquals(order, sellOrders.peek());

        PriorityQueue<Order> buyOrders = orderManager.getBuyOrders("AAPL");
        assertTrue(buyOrders.isEmpty());
    }

    @Test
    void testMatchOrdersExactPriceAndQuantity() {
        Order buy = new Order("GOOG", 100.0, 10, Order.Type.BUY);
        Order sell = new Order("GOOG", 100.0, 10, Order.Type.SELL);

        orderManager.addOrder(buy);
        orderManager.addOrder(sell);

        orderManager.matchOrders("GOOG");

        assertEquals(0, orderManager.getBuyOrders("GOOG").size());
        assertEquals(0, orderManager.getSellOrders("GOOG").size());
    }

    @Test
    void testMatchOrdersPartialMatch() {
        Order buy = new Order("TSLA", 200.0, 15, Order.Type.BUY);
        Order sell = new Order("TSLA", 195.0, 10, Order.Type.SELL);

        orderManager.addOrder(buy);
        orderManager.addOrder(sell);

        orderManager.matchOrders("TSLA");

        assertEquals(1, orderManager.getBuyOrders("TSLA").size());
        assertEquals(0, orderManager.getSellOrders("TSLA").size());

        Order remainingBuy = orderManager.getBuyOrders("TSLA").peek();
        assertNotNull(remainingBuy);
        assertEquals(5, remainingBuy.getQuantity());
    }

    @Test
    void testMatchOrdersNoMatchDueToPrice() {
        Order buy = new Order("NFLX", 90.0, 10, Order.Type.BUY);
        Order sell = new Order("NFLX", 100.0, 10, Order.Type.SELL);

        orderManager.addOrder(buy);
        orderManager.addOrder(sell);

        orderManager.matchOrders("NFLX");

        assertEquals(1, orderManager.getBuyOrders("NFLX").size());
        assertEquals(1, orderManager.getSellOrders("NFLX").size());
    }

    @Test
    void testMatchOrdersWithMultipleBuysAndSells() {
        Order buy1 = new Order("MSFT", 300.0, 5, Order.Type.BUY);
        Order buy2 = new Order("MSFT", 310.0, 5, Order.Type.BUY);
        Order sell1 = new Order("MSFT", 295.0, 4, Order.Type.SELL);
        Order sell2 = new Order("MSFT", 305.0, 6, Order.Type.SELL);

        orderManager.addOrder(buy1);
        orderManager.addOrder(buy2);
        orderManager.addOrder(sell1);
        orderManager.addOrder(sell2);

        orderManager.matchOrders("MSFT");

        PriorityQueue<Order> buys = orderManager.getBuyOrders("MSFT");
        PriorityQueue<Order> sells = orderManager.getSellOrders("MSFT");

        int remainingBuyQty = buys.stream().mapToInt(Order::getQuantity).sum();
        int remainingSellQty = sells.stream().mapToInt(Order::getQuantity).sum();

        assertTrue(remainingBuyQty > 0 || remainingSellQty > 0);
    }

    @Test
    void testGetOrdersWhenNoneExist() {
        PriorityQueue<Order> buys = orderManager.getBuyOrders("UNKNOWN");
        PriorityQueue<Order> sells = orderManager.getSellOrders("UNKNOWN");

        assertNotNull(buys);
        assertNotNull(sells);
        assertTrue(buys.isEmpty());
        assertTrue(sells.isEmpty());
    }
}
