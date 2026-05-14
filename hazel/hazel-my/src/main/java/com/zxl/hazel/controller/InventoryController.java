package com.zxl.hazel.controller;

import com.zxl.hazel.annotation.Controller;
import com.zxl.hazel.annotation.PathVariable;
import com.zxl.hazel.annotation.RequestMapping;
import com.zxl.hazel.dao.InventoryRepository;
import com.zxl.hazel.entity.Inventory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/api/inventory")  // 类级别注解
public class InventoryController {
    private final InventoryRepository inventoryRepository = new InventoryRepository();
    private final ObjectMapper objectMapper;

    public InventoryController() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @RequestMapping(method = "GET")  // 空value，匹配 /api/inventory
    public void getAllInventory(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        String json = objectMapper.writeValueAsString(inventoryRepository.findAllInventory());
        out.write(json);
    }

    @RequestMapping(value = "/{productName}", method = "GET")
    public void getInventoryByProductName(HttpServletRequest req, HttpServletResponse resp,
                                          @PathVariable("productName") String productName) throws Exception {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        Inventory inventory = inventoryRepository.findByProductName(productName);
        if (inventory != null) {
            String json = objectMapper.writeValueAsString(inventory);
            out.write(json);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.write("{\"error\":\"Product not found\"}");
        }
    }

    @RequestMapping(value = "/deduct", method = "POST")
    public void deductInventory(HttpServletRequest req, HttpServletResponse resp) throws Exception {
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
        Integer quantity = (Integer) requestMap.get("quantity");

        boolean success = inventoryRepository.deductInventory(productName, quantity);

        if (success) {
            out.write("{\"message\":\"Inventory deducted successfully\"}");
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"Insufficient inventory or product not found\"}");
        }
    }

    @RequestMapping(value = "/add", method = "POST")
    public void addInventory(HttpServletRequest req, HttpServletResponse resp) throws Exception {
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
        Integer quantity = (Integer) requestMap.get("quantity");

        boolean success = inventoryRepository.addInventory(productName, quantity);

        if (success) {
            out.write("{\"message\":\"Inventory added successfully\"}");
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"Product not found\"}");
        }
    }
}