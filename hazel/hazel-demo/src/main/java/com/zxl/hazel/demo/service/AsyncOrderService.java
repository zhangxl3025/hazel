package com.zxl.hazel.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class AsyncOrderService {

    private static final Logger log = LoggerFactory.getLogger(AsyncOrderService.class);

    /**
     * 同步方法：创建订单，返回订单号
     */
    public String createOrderSync(Connection conn, String productName, double amount) {
        log.info("异步任务：开始创建订单");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO orders (order_no, product_name, amount, status) VALUES (?, ?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            String orderNo = "ORD-ASYNC-" + System.currentTimeMillis();
            ps.setString(1, orderNo);
            ps.setString(2, productName);
            ps.setDouble(3, amount);
            ps.setString(4, "CREATED");
            ps.executeUpdate();

            // 获取生成的 ID
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    log.info("异步任务：订单创建完成, id={}, orderNo={}", id, orderNo);
                    return orderNo;
                }
            }
            log.info("异步任务：订单创建完成, orderNo={}", orderNo);
            return orderNo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 同步方法：查询库存
     */
    public Integer queryStockSync(Connection conn, String productName) {
        log.info("异步任务：开始查询库存");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT quantity FROM inventory WHERE product_name = ?")) {
            ps.setString(1, productName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int stock = rs.getInt("quantity");
                    log.info("异步任务：库存查询完成, product={}, stock={}", productName, stock);
                    return stock;
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 同步方法：记录日志
     */
    public void logSync() {
        log.info("异步任务：开始记录日志");
        log.info("异步任务：日志记录完成");
    }

    /**
     * 同步方法：批量创建订单
     */
    public String createBatchOrderSync(Connection conn, String productName, double amount, int index) {
        log.info("批量订单 {} 开始", index);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO orders (order_no, product_name, amount, status) VALUES (?, ?, ?, ?)")) {
            String orderNo = "ORD-BATCH-" + index + "-" + System.currentTimeMillis();
            ps.setString(1, orderNo);
            ps.setString(2, productName);
            ps.setDouble(3, amount);
            ps.setString(4, "CREATED");
            ps.executeUpdate();
            log.info("批量订单 {} 创建完成, orderNo={}", index, orderNo);
            return orderNo;
        } catch (SQLException e) {
            log.error("批量订单 {} 创建失败", index, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 同步方法：创建失败订单（用于测试回滚）
     */
    public void createFailingOrderSync(Connection conn, String productName, double amount) {
        log.info("异步任务开始");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO orders (order_no, product_name, amount, status) VALUES (?, ?, ?, ?)")) {
            String orderNo = "ORD-FAIL-" + System.currentTimeMillis();
            ps.setString(1, orderNo);
            ps.setString(2, productName);
            ps.setDouble(3, amount);
            ps.setString(4, "CREATED");
            ps.executeUpdate();
            log.info("异步任务订单创建成功，即将抛出异常");
            throw new RuntimeException("异步任务模拟失败");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}