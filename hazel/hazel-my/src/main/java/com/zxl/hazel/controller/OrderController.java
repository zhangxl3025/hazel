package com.zxl.hazel.controller;

import com.zxl.hazel.annotation.Controller;
import com.zxl.hazel.annotation.PathVariable;
import com.zxl.hazel.annotation.RequestMapping;
import com.zxl.hazel.dao.InventoryRepository;
import com.zxl.hazel.dao.OrderRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zxl.hazel.entity.Order;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Map;
@Controller
@RequestMapping("/api/orders")  // 注意：这里是类级别的注解
public class OrderController {
    private final OrderRepository orderRepository = new OrderRepository();
    private final InventoryRepository inventoryRepository = new InventoryRepository();
    private final ObjectMapper objectMapper;

    public OrderController() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @RequestMapping(method = "GET")  // 空value表示使用类路径
    public void getAllOrders(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        String json = objectMapper.writeValueAsString(orderRepository.findAllOrders());
        out.write(json);
    }

    @RequestMapping(value = "/{id}", method = "GET")
    public void getOrderById(HttpServletRequest req, HttpServletResponse resp,
                             @PathVariable("id") Long id) throws Exception {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        Order order = orderRepository.findOrderById(id);
        if (order != null) {
            String json = objectMapper.writeValueAsString(order);
            out.write(json);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.write("{\"error\":\"Order not found\"}");
        }
    }

    @RequestMapping(method = "POST")
    public void createOrder(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader reader = req.getReader();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = objectMapper.readValue(sb.toString(), Map.class);
        String productName = (String) requestMap.get("productName");
        BigDecimal amount = new BigDecimal(requestMap.get("amount").toString());

        if (inventoryRepository.findByProductName(productName) == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"Product not found\"}");
            return;
        }

        Order order = new Order(null, productName, amount, "CREATED");
        Order savedOrder = orderRepository.createOrder(order);

        String json = objectMapper.writeValueAsString(savedOrder);
        resp.setStatus(HttpServletResponse.SC_CREATED);
        out.write(json);
    }

    @RequestMapping(value = "/{id}", method = "PUT")
    public void updateOrderStatus(HttpServletRequest req, HttpServletResponse resp,
                                  @PathVariable("id") Long id) throws Exception {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader reader = req.getReader();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = objectMapper.readValue(sb.toString(), Map.class);
        String status = (String) requestMap.get("status");

        boolean updated = orderRepository.updateOrderStatus(id, status);
        if (updated) {
            out.write("{\"message\":\"Order status updated successfully\"}");
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.write("{\"error\":\"Order not found\"}");
        }
    }
}