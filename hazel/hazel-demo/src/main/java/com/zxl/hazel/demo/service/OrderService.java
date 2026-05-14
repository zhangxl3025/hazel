package com.zxl.hazel.demo.service;

import com.zxl.hazel.ContextManager;
import com.zxl.hazel.sync.SyncContext;
import com.zxl.hazel.sync.SyncContextHolder;
import com.zxl.hazel.trace.Traceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final ExecutorService executorService;
    private final AsyncOrderService asyncOrderService;

    public OrderService(JdbcTemplate jdbcTemplate, DataSource dataSource, AsyncOrderService asyncOrderService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.executorService = Executors.newFixedThreadPool(20);
        this.asyncOrderService = asyncOrderService;
    }

    // ==================== 基础 CRUD ====================

    public List<Map<String, Object>> queryAllOrders() {
        log.info("查询所有订单");
        return jdbcTemplate.queryForList("SELECT * FROM orders ORDER BY created_at DESC");
    }

    public List<Map<String, Object>> queryInventory() {
        log.info("查询库存");
        return jdbcTemplate.queryForList("SELECT * FROM inventory");
    }

    public void clearOrders() {
        jdbcTemplate.update("DELETE FROM orders");
        log.info("订单表已清空");
    }

    public void resetInventory(String productName, int quantity) {
        jdbcTemplate.update(
                "UPDATE inventory SET quantity = ? WHERE product_name = ?",
                quantity, productName
        );
        log.info("库存重置: product={}, quantity={}", productName, quantity);
    }

    // ==================== 测试1：纯 Spring 事务 ====================



    @Transactional
    @Traceable(
            value = "createOrderWithSpringTx",
            tags = {
                    "order.type", "{order.type}",
                    "user.level", "{user.level}",
                    "payment.method", "{payment.method}"
            },
            metrics = @Traceable.MetricsConfig(
                    enabled = true,
                    name = "order.create",
                    type = Traceable.MetricsType.COUNTER_AND_TIMER,
                    dimensions = {"order.type", "user.level"}  // 只按这两个维度聚合
            )
    )
    public void createOrderWithSpringTx(String productName, double amount) {
        log.info("=== Spring 事务开始 ===");

        jdbcTemplate.update(
                "UPDATE inventory SET quantity = quantity - 1 WHERE product_name = ?",
                productName
        );

        String orderNo = "ORD-SPRING-" + System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO orders (order_no, product_name, amount, status) VALUES (?, ?, ?, 'CREATED')",
                orderNo, productName, amount
        );

        log.info("Spring 事务完成: orderNo={}", orderNo);
    }

    // ==================== 测试2：异步任务 + 事务计数 ====================

    @Transactional
    @Traceable(
            value = "createOrderWithAsyncTask",
            tags = {
                    "order.type", "{order.type}",
                    "user.level", "{user.level}",
                    "payment.method", "{payment.method}"
            },
            metrics = @Traceable.MetricsConfig(
                    enabled = true,
                    name = "order.create",
                    type = Traceable.MetricsType.COUNTER_AND_TIMER,
                    dimensions = {"order.type", "user.level"}  // 只按这两个维度聚合
            )
    )
    public void createOrderWithAsyncTask(String productName, double amount) throws Exception {
        log.info("=== 主事务开始 ===");

        SyncContext txContext = SyncContextHolder.getInstance().capture();
        Thread mainThread = Thread.currentThread();
        jdbcTemplate.update(
                "UPDATE inventory SET quantity = quantity - 1 WHERE product_name = ?",
                productName
        );

        String mainOrderNo = "ORD-MAIN-" + System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO orders (order_no, product_name, amount, status) VALUES (?, ?, ?, 'CREATED')",
                mainOrderNo, productName, amount
        );
        log.info("主事务订单创建: orderNo={}", mainOrderNo);

        Connection conn = DataSourceUtils.getConnection(dataSource);

        // 异步执行同步方法，并接收返回值
        executorService.submit(ContextManager.wrapWithCount(() -> {
            String orderNo = asyncOrderService.createOrderSync(conn, productName, amount);
            txContext.recordResult(mainThread, "orderNo", orderNo);
        }));

        executorService.submit(ContextManager.wrapWithCount(() -> {
            Integer stock = asyncOrderService.queryStockSync(conn, productName);
            txContext.recordResult(mainThread, "stock", stock);
        }));

        executorService.submit(ContextManager.wrapWithCount(() -> {
            asyncOrderService.logSync();
        }));

        txContext.awaitRefCountZero(mainThread);

        // 获取所有异步结果
        Map<String, Object> results = txContext.awaitWithResults(mainThread);
        log.info("异步任务结果: {}", results);

        log.info("=== 主事务完成 ===");
    }

    // ==================== 测试3：批量异步任务 ====================

    @Transactional
    @Traceable(
            value = "batchCreateOrdersWithCount",
            tags = {
                    "order.type", "{order.type}",
                    "user.level", "{user.level}",
                    "payment.method", "{payment.method}"
            },
            metrics = @Traceable.MetricsConfig(
                    enabled = true,
                    name = "order.create",
                    type = Traceable.MetricsType.COUNTER_AND_TIMER,
                    dimensions = {"order.type", "user.level"}  // 只按这两个维度聚合
            )
    )
    public void batchCreateOrdersWithCount(String productName, double amount, int count) throws Exception {
        log.info("=== 批量事务开始，数量={} ===", count);

        SyncContext txContext = SyncContextHolder.getInstance().capture();
        Thread mainThread = Thread.currentThread();

        jdbcTemplate.update(
                "UPDATE inventory SET quantity = quantity - ? WHERE product_name = ?",
                count, productName
        );

        Connection conn = DataSourceUtils.getConnection(dataSource);

        for (int i = 0; i < count; i++) {
            final int index = i;
            executorService.submit(ContextManager.wrapWithCount(() -> {
                String orderNo = asyncOrderService.createBatchOrderSync(conn, productName, amount, index);
                txContext.recordResult(mainThread, "order_" + index, orderNo);
            }));
        }


        Map<String, Object> results = txContext.awaitWithResults(mainThread);
        log.info("批量订单结果: {}", results);

        log.info("=== 批量事务完成，共创建 {} 个订单 ===", count);
    }

    // ==================== 测试4：异步任务失败，主事务回滚 ====================

    @Transactional
    @Traceable(
            value = "createOrderWithFailingAsyncTask",
            tags = {
                    "order.type", "{order.type}",
                    "user.level", "{user.level}",
                    "payment.method", "{payment.method}"
            },
            metrics = @Traceable.MetricsConfig(
                    enabled = true,
                    name = "order.create",
                    type = Traceable.MetricsType.COUNTER_AND_TIMER,
                    dimensions = {"order.type", "user.level"}  // 只按这两个维度聚合
            )
    )
    public void createOrderWithFailingAsyncTask(String productName, double amount) throws Exception {
        log.info("=== 主事务开始 ===");

        SyncContext syncContext = SyncContextHolder.getInstance().capture();
        Thread mainThread = Thread.currentThread();

        jdbcTemplate.update(
                "UPDATE inventory SET quantity = quantity - 1 WHERE product_name = ?",
                productName
        );

        Connection conn = DataSourceUtils.getConnection(dataSource);

        executorService.submit(ContextManager.wrapWithCount(() -> {
            asyncOrderService.createFailingOrderSync(conn, productName, amount);
        }));
        syncContext.awaitRefCountZero(mainThread);

        log.info("=== 主事务完成 ===");
    }
}