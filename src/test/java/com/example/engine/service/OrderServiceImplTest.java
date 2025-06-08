package com.example.engine.service;

import com.example.engine.dto.OrderRequest;
import com.example.engine.dto.OrderResponse;
import com.example.engine.model.Order;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

class OrderServiceImplTest {

    @Mock
    private OrderManager orderManager;

    private OrderServiceImpl orderService;

    private AutoCloseable mocks;

    @BeforeEach
    void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        orderService = new OrderServiceImpl(orderManager, 1, 2);  // poolSize=1, retry=2
    }

    @AfterEach
    void tearDown() throws Exception {
        orderService.shutdownExecutor();
        mocks.close();
    }

    @Test
    void testAddOrderSuccess() {
        OrderRequest request = new OrderRequest("AAPL", 100.0, 10, Order.Type.BUY);
        OrderResponse response = orderService.addOrder(request);

        assertNotNull(response);
        assertEquals("AAPL", response.getSymbol());
        assertEquals(100.0, response.getPrice());
        assertEquals(10, response.getQuantity());
        assertEquals(Order.Type.BUY, response.getType());

        Optional<OrderResponse> fetched = orderService.getOrderById(response.getId());
        assertTrue(fetched.isPresent());
        assertEquals(response.getId(), fetched.get().getId());
    }

    @Test
    void testGetOrderByIdFound() {
        OrderRequest request = new OrderRequest("GOOG", 1500.0, 5, Order.Type.SELL);
        OrderResponse added = orderService.addOrder(request);

        Optional<OrderResponse> fetched = orderService.getOrderById(added.getId());
        assertTrue(fetched.isPresent());
        assertEquals(added.getId(), fetched.get().getId());
    }

    @Test
    void testGetOrderByIdNotFound() {
        Optional<OrderResponse> response = orderService.getOrderById(999);
        assertTrue(response.isEmpty());
    }

    @Test
    void testGetOrdersBySymbol() {
        String symbol = "TSLA";

        Order buyOrder = new Order(symbol, 800, 2, Order.Type.BUY);
        Order sellOrder = new Order(symbol, 805, 2, Order.Type.SELL);

        PriorityQueue<Order> buyQueue = new PriorityQueue<>();
        buyQueue.add(buyOrder);
        when(orderManager.getBuyOrders(symbol)).thenReturn(buyQueue);

        PriorityQueue<Order> sellQueue = new PriorityQueue<>();
        sellQueue.add(sellOrder);
        when(orderManager.getSellOrders(symbol)).thenReturn(sellQueue);

        Pageable pageable = PageRequest.of(0, 10);
        List<OrderResponse> responses = orderService.getOrdersBySymbol(symbol, pageable);

        assertEquals(2, responses.size());
        assertTrue(responses.stream().anyMatch(o -> o.getType() == Order.Type.BUY));
        assertTrue(responses.stream().anyMatch(o -> o.getType() == Order.Type.SELL));
    }

    @Test
    void testOrderProcessingSuccess() {
        OrderRequest request = new OrderRequest("MSFT", 250.0, 5, Order.Type.BUY);

        doNothing().when(orderManager).addOrder(any(Order.class));
        doNothing().when(orderManager).matchOrders(anyString());

        orderService.addOrder(request);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(orderManager, atLeastOnce()).addOrder(any(Order.class));
            verify(orderManager, atLeastOnce()).matchOrders(eq("MSFT"));
        });
    }

    @Test
    void testOrderProcessingRetriesAndDeadLetterQueue() {
        OrderRequest request = new OrderRequest("NFLX", 600.0, 1, Order.Type.SELL);

        doThrow(new RuntimeException("Simulated failure")).when(orderManager).addOrder(any(Order.class));

        orderService.addOrder(request);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(orderManager, times(2)).addOrder(any(Order.class));
            verify(orderManager, never()).matchOrders(anyString());
        });
    }

    @Test
    void testShutdownExecutor() {
        assertDoesNotThrow(() -> orderService.shutdownExecutor());
    }
}
