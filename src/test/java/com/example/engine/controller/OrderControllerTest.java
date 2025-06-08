package com.example.engine.controller;

import com.example.engine.dto.OrderRequest;
import com.example.engine.dto.OrderResponse;
import com.example.engine.model.Order.Type;
import com.example.engine.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addOrder_ShouldReturnCreatedOrder() throws Exception {
        OrderRequest request = new OrderRequest("AAPL", 150.0, 10, Type.BUY);
        OrderResponse response = new OrderResponse(1, "AAPL", 150.0, 10, Type.BUY);

        when(orderService.addOrder(Mockito.any(OrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.symbol", is("AAPL")))
                .andExpect(jsonPath("$.type", is("BUY")));
    }

    @Test
    void getOrderById_ShouldReturnOrderIfFound() throws Exception {
        int orderId = 42;
        OrderResponse response = new OrderResponse(orderId, "GOOG", 2000.0, 5, Type.SELL);

        when(orderService.getOrderById(orderId)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId)))
                .andExpect(jsonPath("$.symbol", is("GOOG")))
                .andExpect(jsonPath("$.type", is("SELL")));
    }

    @Test
    void getOrderById_ShouldReturnNotFoundIfMissing() throws Exception {
        when(orderService.getOrderById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrdersBySymbol_ShouldReturnPagedOrders() throws Exception {
        String symbol = "TSLA";
        OrderResponse o1 = new OrderResponse(1, symbol, 300.0, 20, Type.BUY);
        OrderResponse o2 = new OrderResponse(2, symbol, 310.0, 15, Type.SELL);
        List<OrderResponse> mockList = List.of(o1, o2);

        when(orderService.getOrdersBySymbol(symbol, PageRequest.of(0, 10))).thenReturn(mockList);

        mockMvc.perform(get("/api/orders/symbol/{symbol}", symbol)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].symbol", is("TSLA")))
                .andExpect(jsonPath("$[1].symbol", is("TSLA")));
    }
}
