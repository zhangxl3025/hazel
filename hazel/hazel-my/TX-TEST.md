# Hazel Demo 事务测试指南

## 📋 日志格式说明

日志已配置支持以下 MDC 字段：

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] [%X{spanId}] [%X{segmentId}] [%X{operation}] [tx:%X{globalTxId}] [local-tx:%X{transactionId}] %-5level %logger{36} - %msg%n
```

### MDC 字段含义

| 字段 | MDC Key | 说明 |
|------|---------|------|
| traceId | `traceId` | 链路追踪ID（全局唯一） |
| spanId | `spanId` | 当前Span ID |
| segmentId | `segmentId` | 片段ID |
| operation | `operation` | 操作名称 |
| **globalTxId** | `globalTxId` | **全局事务ID**（跨服务/跨模块） |
| **transactionId** | `transactionId` | **当前事务ID**（本地事务） |

## 🚀 测试接口

### 1. 手动管理事务
```bash
curl http://localhost:8080/tx/manual/1
```

**日志输出示例：**
```
2024-01-01 12:00:00.001 [http-nio-8080-exec-1] [trace-001] [span-001] [seg-001] [GET-/tx/manual/1] [tx:global-tx-001] [local-tx:manual-tx-1] INFO  c.z.h.d.c.UserController - [UserController.testManualTransaction] 开始测试手动事务
2024-01-01 12:00:00.002 [http-nio-8080-exec-1] [trace-001] [span-001] [seg-001] [GET-/tx/manual/1] [tx:global-tx-001] [local-tx:manual-tx-1] INFO  c.z.h.d.c.UserController - [UserController.testManualTransaction] 事务开始: transactionId=manual-tx-1, globalTxId=global-tx-001
2024-01-01 12:00:00.050 [http-nio-8080-exec-1] [trace-001] [span-002] [seg-002] [getUser] [tx:global-tx-001] [local-tx:manual-tx-1] INFO  c.z.h.d.c.UserController - [UserController.getUser] 开始查询用户, userId=1
2024-01-01 12:00:00.100 [http-nio-8080-exec-1] [trace-001] [span-001] [seg-001] [GET-/tx/manual/1] [tx:global-tx-001] [local-tx:manual-tx-1] INFO  c.z.h.d.c.UserController - [UserController.testManualTransaction] 事务提交成功
```

### 2. 自动管理事务（推荐）
```bash
curl http://localhost:8080/tx/auto/1
```

**日志输出示例：**
```
2024-01-01 12:00:00.001 [http-nio-8080-exec-1] [trace-001] [span-001] [seg-001] [GET-/tx/auto/1] [tx:global-tx-002] [local-tx:auto-tx-1] INFO  c.z.h.d.c.UserController - [UserController.testAutoTransaction] 开始测试自动事务
2024-01-01 12:00:00.002 [http-nio-8080-exec-1] [trace-001] [span-001] [seg-001] [GET-/tx/auto/1] [tx:global-tx-002] [local-tx:auto-tx-1] INFO  c.z.h.d.c.UserController - [UserController.testAutoTransaction] 在事务中执行
2024-01-01 12:00:00.100 [http-nio-8080-exec-1] [trace-001] [span-001] [seg-001] [GET-/tx/auto/1] [tx:global-tx-002] [local-tx:auto-tx-1] INFO  c.z.h.d.c.UserController - [UserController.testAutoTransaction] 事务执行成功
```

### 3. 事务回滚测试
```bash
# 正常情况
curl http://localhost:8080/tx/rollback/1

# 触发回滚
curl http://localhost:8080/tx/rollback/999
```

**回滚日志示例：**
```
2024-01-01 12:00:00.001 [http-nio-8080-exec-1] [trace-001] [span-001] [seg-001] [GET-/tx/rollback/999] [tx:global-tx-003] [local-tx:rollback-tx-999] INFO  c.z.h.d.c.UserController - [UserController.testTransactionRollback] 开始测试事务回滚
2024-01-01 12:00:00.002 [http-nio-8080-exec-1] [trace-001] [span-001] [seg-001] [GET-/tx/rollback/999] [tx:global-tx-003] [local-tx:rollback-tx-999] INFO  c.z.h.d.c.UserController - [UserController.testTransactionRollback] 执行中...
2024-01-01 12:00:00.050 [http-nio-8080-exec-1] [trace-001] [span-001] [seg-001] [GET-/tx/rollback/999] [tx:global-tx-003] [local-tx:rollback-tx-999] ERROR c.z.h.d.c.UserController - [UserController.testTransactionRollback] 事务已回滚: 模拟业务异常，触发回滚
```

### 4. 事务保存点测试
```bash
curl http://localhost:8080/tx/savepoint/1
```

## 🔍 MDC 自动注入机制

hazel-trace 通过 `MdcHook` 自动管理 MDC：

```java
// MdcHook.java
class MdcHook implements SpanContextHook {
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    private static final String MDC_OPERATION = "operation";
    private static final String MDC_SEGMENT_ID = "segmentId";
    private static final String MDC_GLOBAL_TX_ID = "globalTxId";      // 全局事务ID
    private static final String MDC_TRANSACTION_ID = "transactionId";  // 当前事务ID
    
    @Override
    public void onSpanChanged(Span span) {
        // 当 Span 变化时自动更新 MDC
        MDC.put(MDC_TRACE_ID, span.getTraceId());
        MDC.put(MDC_SPAN_ID, span.getSpanId());
        MDC.put(MDC_GLOBAL_TX_ID, span.getGlobalTxId());
        MDC.put(MDC_TRANSACTION_ID, span.getTransactionId());
        // ...
    }
}
```

## 📊 日志字段对比

| 场景 | traceId | spanId | globalTxId | transactionId |
|------|---------|--------|------------|---------------|
| 普通HTTP请求 | ✅ | ✅ | ❌ | ❌ |
| 使用 @Traceable | ✅ | ✅ | ❌ | ❌ |
| 使用 TransactionManager | ✅ | ✅ | ✅ | ✅ |
| 跨线程传递 | ✅ | ✅ | ✅ | ✅ |

## ⚠️ 注意事项

1. **空值显示**：MDC 值为空时显示为 `[]`，例如 `[tx:]` 表示没有全局事务
2. **自动清理**：请求结束后 `MdcHook.onClear()` 会自动清理 MDC
3. **跨线程传递**：使用 `ContextManager.wrap()` 可以传递 MDC 到子线程
