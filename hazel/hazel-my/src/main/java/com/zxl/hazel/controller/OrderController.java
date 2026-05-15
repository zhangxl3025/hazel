package com.zxl.hazel.controller;

import com.zxl.hazel.annotation.ioc.Controller;
import com.zxl.hazel.annotation.ioc.Inject;
import com.zxl.hazel.annotation.PathVariable;
import com.zxl.hazel.annotation.RequestBody;
import com.zxl.hazel.annotation.RequestMapping;
import com.zxl.hazel.dao.InventoryRepository;
import com.zxl.hazel.dao.OrderRepository;
import com.zxl.hazel.entity.Order;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
@Controller
@RequestMapping("/api/orders")  // 注意：这里是类级别的注解
public class OrderController {
    @Inject
    private OrderRepository orderRepository;
    @Inject
    private InventoryRepository inventoryRepository;

    @RequestMapping(method = "GET")  // 空value表示使用类路径
    public List<Order> getAllOrders() throws SQLException {
        return orderRepository.findAllOrders();
    }

    @RequestMapping(value = "/{id}", method = "GET")
    public Order getOrderById(@PathVariable("id") Long id) throws SQLException {
        return orderRepository.findOrderById(id);
    }

    @RequestMapping(method = "POST")
    public Order createOrder(@RequestBody Map<String, Object> request) throws SQLException {
        String productName = (String) request.get("productName");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());

        if (inventoryRepository.findByProductName(productName) == null) {
            throw new RuntimeException("Product not found");
        }

        Order order = new Order(null, productName, amount, "CREATED");
        return orderRepository.createOrder(order);
    }

    @RequestMapping(value = "/{id}", method = "PUT")
    public Map<String, String> updateOrderStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> request) throws SQLException {
        String status = (String) request.get("status");

        boolean updated = orderRepository.updateOrderStatus(id, status);
        if (updated) {
            return Map.of("message", "Order status updated successfully");
        } else {
            throw new RuntimeException("Order not found");
        }
    }
}