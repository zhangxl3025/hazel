package com.zxl.hazel.demo;

import com.zxl.hazel.demo.service.OrderService;
import com.zxl.hazel.trace.Span;
import com.zxl.hazel.trace.Tracer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("事务计数和链路追踪测试")
class TransactionCountTest {

    @Autowired
    private OrderService orderService;



    @BeforeAll
    static void beforeAll() {
        Span span = Tracer.startSpan("事务计数和链路追踪测试");
        span.setGlobalTxId("全局事务id");
    }
    @BeforeEach
    void setUp(TestInfo testInfo) {
        Tracer.startSpan(testInfo.getDisplayName());
        orderService.clearOrders();
        orderService.resetInventory("AirPods", 100);
        orderService.resetInventory("iPhone", 50);
    }

    @AfterEach
    void tearDown() {
        orderService.clearOrders();
        Tracer.endSpan();
    }
    @AfterAll
    static void afterAll() {
        Tracer.endSpan();
        Tracer.clearChain();
    }

    @Test
    @DisplayName("测试1：Spring 事务 + 日志打印")
    void test1_springTransactionWithMdc() {
        orderService.createOrderWithSpringTx("AirPods", 999.0);
        
        List<Map<String, Object>> orders = orderService.queryAllOrders();
        assertThat(orders).hasSize(1);
        
        List<Map<String, Object>> inventory = orderService.queryInventory();
        Map<String, Object> airPods = inventory.stream()
                .filter(i -> "AirPods".equals(i.get("product_name")))
                .findFirst()
                .orElse(null);
        assertThat(airPods).isNotNull();
        assertThat((Integer) airPods.get("quantity")).isEqualTo(99);
    }

    @Test
    @DisplayName("测试2：异步任务 + 事务计数")
    void test2_asyncTaskWithCount() throws Exception {
        int beforeStock = getStock("AirPods");
        
        orderService.createOrderWithAsyncTask("AirPods", 999.0);
        
        int afterStock = getStock("AirPods");
        assertThat(afterStock).isEqualTo(beforeStock - 1);
        
        List<Map<String, Object>> orders = orderService.queryAllOrders();
        // 主事务1个订单 + 异步任务1个订单 = 2个
        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("测试3：批量异步任务（5个并发）")
    void test3_batchAsyncTasks() throws Exception {
        int batchSize = 5;
        int beforeStock = getStock("AirPods");
        
        orderService.batchCreateOrdersWithCount("AirPods", 999.0, batchSize);
        
        int afterStock = getStock("AirPods");
        assertThat(afterStock).isEqualTo(beforeStock - batchSize);
        
        List<Map<String, Object>> orders = orderService.queryAllOrders();
        assertThat(orders).hasSize(batchSize);
    }

    @Test
    @DisplayName("测试4：异步任务失败，主事务回滚")
    void test4_asyncTaskFailRollback() {
        int beforeStock = getStock("AirPods");

        // 执行方法，应该抛出异常
        assertThatThrownBy(() -> orderService.createOrderWithFailingAsyncTask("AirPods", 999.0))
                .isInstanceOf(Exception.class);  // 只验证抛出了异常，不验证具体消息

        // 验证回滚结果
        int afterStock = getStock("AirPods");
        assertThat(afterStock).isEqualTo(beforeStock);

        List<Map<String, Object>> orders = orderService.queryAllOrders();
        assertThat(orders).isEmpty();
    }

    private int getStock(String productName) {
        List<Map<String, Object>> inventory = orderService.queryInventory();
        return inventory.stream()
                .filter(i -> productName.equals(i.get("product_name")))
                .map(i -> (Integer) i.get("quantity"))
                .findFirst()
                .orElse(0);
    }
}