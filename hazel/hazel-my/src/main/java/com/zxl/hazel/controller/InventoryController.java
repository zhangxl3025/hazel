package com.zxl.hazel.controller;

import com.zxl.hazel.annotation.ioc.Controller;
import com.zxl.hazel.annotation.ioc.Inject;
import com.zxl.hazel.annotation.PathVariable;
import com.zxl.hazel.annotation.RequestBody;
import com.zxl.hazel.annotation.RequestMapping;
import com.zxl.hazel.dao.InventoryRepository;
import com.zxl.hazel.entity.Inventory;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/api/inventory")  // 类级别注解
public class InventoryController {
    @Inject
    private InventoryRepository inventoryRepository;

    @RequestMapping(method = "GET")  // 空value，匹配 /api/inventory
    public List<Inventory> getAllInventory() throws SQLException {
        return inventoryRepository.findAllInventory();
    }

    @RequestMapping(value = "/{productName}", method = "GET")
    public Inventory getInventoryByProductName(@PathVariable("productName") String productName) throws SQLException {
        return inventoryRepository.findByProductName(productName);
    }

    @RequestMapping(value = "/deduct", method = "POST")
    public Map<String, String> deductInventory(@RequestBody Map<String, Object> request) throws SQLException {
        String productName = (String) request.get("productName");
        Integer quantity = (Integer) request.get("quantity");

        boolean success = inventoryRepository.deductInventory(productName, quantity);

        if (success) {
            return Map.of("message", "Inventory deducted successfully");
        } else {
            throw new RuntimeException("Insufficient inventory or product not found");
        }
    }

    @RequestMapping(value = "/add", method = "POST")
    public Map<String, String> addInventory(@RequestBody Map<String, Object> request) throws SQLException {
        String productName = (String) request.get("productName");
        Integer quantity = (Integer) request.get("quantity");

        boolean success = inventoryRepository.addInventory(productName, quantity);

        if (success) {
            return Map.of("message", "Inventory added successfully");
        } else {
            throw new RuntimeException("Product not found");
        }
    }
}