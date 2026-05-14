package com.zxl.hazel.dao;


import com.zxl.hazel.bean.BeanContainer;
import com.zxl.hazel.entity.Order;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderRepository {

    private DataSource getDataSource() {
        return BeanContainer.getBean(DataSource.class);
    }
    
    public Order createOrder(Order order) throws SQLException {
        String sql = "INSERT INTO orders (order_no, product_name, amount, status) VALUES (?, ?, ?, ?)";
        
        if (order.getOrderNo() == null || order.getOrderNo().isEmpty()) {
            order.setOrderNo(generateOrderNo());
        }
        
        try (Connection conn = getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, order.getOrderNo());
            pstmt.setString(2, order.getProductName());
            pstmt.setBigDecimal(3, order.getAmount());
            pstmt.setString(4, order.getStatus() != null ? order.getStatus() : "CREATED");
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    order.setId(rs.getLong(1));
                }
            }
        }
        return order;
    }
    
    public List<Order> findAllOrders() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY id DESC";
        
        try (Connection conn = getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                orders.add(mapRowToOrder(rs));
            }
        }
        return orders;
    }
    
    public Order findOrderById(Long id) throws SQLException {
        String sql = "SELECT * FROM orders WHERE id = ?";
        
        try (Connection conn = getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToOrder(rs);
                }
            }
        }
        return null;
    }
    
    public boolean updateOrderStatus(Long id, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        
        try (Connection conn = getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setLong(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    private Order mapRowToOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setOrderNo(rs.getString("order_no"));
        order.setProductName(rs.getString("product_name"));
        order.setAmount(rs.getBigDecimal("amount"));
        order.setStatus(rs.getString("status"));
        order.setCreatedAt(rs.getTimestamp("created_at") != null ? 
            rs.getTimestamp("created_at") : null);
        return order;
    }
    
    private String generateOrderNo() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}