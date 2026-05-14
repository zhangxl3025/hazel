package com.zxl.hazel.dao;


import com.zxl.hazel.bean.BeanContainer;
import com.zxl.hazel.entity.Inventory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryRepository {

    private DataSource getDataSource() {
        return BeanContainer.getBean(DataSource.class);
    }

    public List<Inventory> findAllInventory() throws SQLException {
        List<Inventory> inventories = new ArrayList<>();
        String sql = "SELECT * FROM inventory ORDER BY id";
        try (Connection conn = getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                inventories.add(mapRowToInventory(rs));
            }
        }
        return inventories;
    }
    
    public Inventory findByProductName(String productName) throws SQLException {
        String sql = "SELECT * FROM inventory WHERE product_name = ?";
        
        try (Connection conn = getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, productName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToInventory(rs);
                }
            }
        }
        return null;
    }
    
    public boolean deductInventory(String productName, int quantity) throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity - ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE product_name = ? AND quantity >= ?";
        
        try (Connection conn = getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, quantity);
            pstmt.setString(2, productName);
            pstmt.setInt(3, quantity);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public boolean addInventory(String productName, int quantity) throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity + ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE product_name = ?";
        
        try (Connection conn = getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, quantity);
            pstmt.setString(2, productName);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    private Inventory mapRowToInventory(ResultSet rs) throws SQLException {
        Inventory inventory = new Inventory();
        inventory.setId(rs.getLong("id"));
        inventory.setProductName(rs.getString("product_name"));
        inventory.setQuantity(rs.getInt("quantity"));
        inventory.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
            rs.getTimestamp("updated_at"): null);
        return inventory;
    }
}