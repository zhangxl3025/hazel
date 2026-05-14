package com.zxl.hazel.demo.ds.proxy;

import com.zxl.hazel.trace.Span;
import com.zxl.hazel.trace.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class TracePreparedStatement implements PreparedStatement {

    // 线程本地缓存，避免重复创建DateTimeFormatter
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // 预分配初始容量，减少List扩容
    private static final int DEFAULT_PARAM_COUNT = 8;

    @Delegate
    private final PreparedStatement target;
    private final String sql;

    // 使用数组代替List，减少对象开销和装箱开销
    private Object[] parameters;
    private int paramCount;

    // 懒加载初始化数组
    private void ensureCapacity(int index) {
        if (parameters == null) {
            parameters = new Object[DEFAULT_PARAM_COUNT];
            paramCount = 0;
        }
        if (index >= parameters.length) {
            // 扩容策略：1.5倍增长，减少扩容次数
            int newSize = parameters.length + (parameters.length >> 1);
            parameters = Arrays.copyOf(parameters, newSize);
        }
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        target.setNull(parameterIndex, sqlType);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = null;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        target.setBoolean(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        target.setByte(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        target.setShort(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        target.setInt(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        target.setLong(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        target.setFloat(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        target.setDouble(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setBigDecimal(int parameterIndex, java.math.BigDecimal x) throws SQLException {
        target.setBigDecimal(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        target.setString(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        target.setBytes(parameterIndex, x);
        ensureCapacity(parameterIndex);
        // 不对byte[]进行转换，延迟到格式化时处理
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        target.setDate(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        target.setTime(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        target.setTimestamp(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        target.setObject(parameterIndex, x);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        target.setObject(parameterIndex, x, targetSqlType);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        target.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
        ensureCapacity(parameterIndex);
        parameters[parameterIndex - 1] = x;
        paramCount = Math.max(paramCount, parameterIndex);
    }

    // 使用StringBuilder复用，减少内存分配
    private final ThreadLocal<StringBuilder> stringBuilderCache = ThreadLocal.withInitial(() -> new StringBuilder(256));
    private final ThreadLocal<String> sqlCache = new ThreadLocal<>();

    // 构建完整的SQL语句（优化版本）
    private String buildSqlWithParameters() {
        if (paramCount == 0) {
            return sql;
        }

        // 尝试从缓存获取
        String cached = sqlCache.get();
        if (cached != null && cached.length() == sql.length() + estimateAddedLength()) {
            return cached;
        }

        StringBuilder sb = stringBuilderCache.get();
        sb.setLength(0); // 重用StringBuilder

        int paramIdx = 0;
        int lastIdx = 0;
        int nextQmIdx;

        // 使用直接索引遍历，避免频繁的方法调用
        while ((nextQmIdx = sql.indexOf('?', lastIdx)) != -1) {
            sb.append(sql, lastIdx, nextQmIdx);

            if (paramIdx < paramCount && parameters[paramIdx] != null) {
                appendFormattedValue(sb, parameters[paramIdx]);
            } else if (paramIdx < paramCount) {
                sb.append("NULL");
            } else {
                sb.append('?');
            }

            lastIdx = nextQmIdx + 1;
            paramIdx++;
        }

        sb.append(sql, lastIdx, sql.length());

        String result = sb.toString();
        sqlCache.set(result); // 缓存结果
        return result;
    }

    // 估算额外字符长度，用于缓存key判断
    private int estimateAddedLength() {
        if (paramCount == 0) return 0;
        int estimate = 0;
        for (int i = 0; i < paramCount; i++) {
            Object param = parameters[i];
            if (param != null) {
                if (param instanceof String) {
                    estimate += ((String) param).length() + 2; // 加2个单引号
                } else if (param instanceof byte[]) {
                    estimate += ((byte[]) param).length * 2 + 4; // hex格式需要2倍长度
                } else {
                    estimate += 10; // 估计数字、日期等的平均长度
                }
            } else {
                estimate += 4; // "NULL"长度
            }
        }
        return estimate;
    }

    // 高效格式化值
    private void appendFormattedValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("NULL");
            return;
        }

        // 按类型处理，使用switch表达式（Java 14+）
        switch (value.getClass().getName()) {
            case "java.lang.String":
                sb.append('\'');
                appendEscapedString(sb, (String) value);
                sb.append('\'');
                break;

            case "java.sql.Timestamp":
                Timestamp ts = (Timestamp) value;
                sb.append('\'');
                sb.append(ts.toLocalDateTime().format(TIMESTAMP_FORMATTER));
                sb.append('\'');
                break;

            case "java.sql.Date":
                Date date = (Date) value;
                sb.append('\'');
                sb.append(date.toLocalDate().format(DATE_FORMATTER));
                sb.append('\'');
                break;

            case "java.sql.Time":
                Time time = (Time) value;
                sb.append('\'');
                sb.append(time.toLocalTime().format(TIME_FORMATTER));
                sb.append('\'');
                break;

            case "[B": // byte数组的Class名
                appendHexBytes(sb, (byte[]) value);
                break;

            case "java.lang.Boolean":
                sb.append(((Boolean) value) ? '1' : '0');
                break;

            default:
                // 处理数字、BigDecimal等
                sb.append(value);
                break;
        }
    }

    // 高效转义字符串，避免创建新字符串对象
    private void appendEscapedString(StringBuilder sb, String str) {
        int last = 0;
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == '\'') {
                if (last < i) {
                    sb.append(str, last, i);
                }
                sb.append("''"); // 单引号转义为两个单引号
                last = i + 1;
            }
        }
        if (last < len) {
            sb.append(str, last, len);
        }
    }

    // 高效转换byte[]为hex，避免中间字符串
    private void appendHexBytes(StringBuilder sb, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            sb.append("NULL");
            return;
        }
        sb.append("0x");
        for (byte b : bytes) {
            // 使用位运算提高效率
            int high = (b >> 4) & 0x0F;
            int low = b & 0x0F;
            sb.append((char) (high < 10 ? '0' + high : 'a' + high - 10));
            sb.append((char) (low < 10 ? '0' + low : 'a' + low - 10));
        }
    }

    @Override
    public boolean execute() throws SQLException {
        beforeExecute();
        Throwable exception = null;
        try {
            return target.execute();
        } catch (SQLException e) {
            exception = e;
            throw e;
        } finally {
            afterExecute(exception);
        }
    }

    @Override
    public int executeUpdate() throws SQLException {
        beforeExecute();
        Throwable exception = null;
        try {
            return target.executeUpdate();
        } catch (SQLException e) {
            exception = e;
            throw e;
        } finally {
            afterExecute(exception);
        }
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        beforeExecute();
        Throwable exception = null;
        try {
            return target.executeQuery();
        } catch (SQLException e) {
            exception = e;
            throw e;
        } finally {
            afterExecute(exception);
        }
    }

    @Override
    public void clearParameters() throws SQLException {
        target.clearParameters();
        if (parameters != null) {
            Arrays.fill(parameters, 0, paramCount, null);
        }
        paramCount = 0;
        sqlCache.remove(); // 清除缓存
    }

    private void beforeExecute() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            String txId = "SQL-" + UUID.randomUUID().toString().substring(0, 8);
            Tracer.setCurrentTransactionId(txId);
        }

        // 创建 SQL 子 Span
        Span span = Tracer.startSpan("SQL-" + extractSqlOperation());
        span.addTag("db.type", "SQL");
        span.addTag("db.statement", buildSqlWithParameters());
        span.addTag("span.layer", "Database");
    }

    private void afterExecute(Throwable exception) {
        if (Tracer.hasActive()) {
            Span span = Tracer.currentSpan();
            if (exception != null) {
                span.addTag("db.status", "ERROR");
                span.addTag("db.error", exception.getMessage());
            } else {
                span.addTag("db.status", "OK");
            }
            Tracer.endSpan();
        }
    }

    private String extractSqlOperation() {
        String trimmed = sql.trim().toUpperCase();
        if (trimmed.startsWith("SELECT")) return "SELECT";
        if (trimmed.startsWith("INSERT")) return "INSERT";
        if (trimmed.startsWith("UPDATE")) return "UPDATE";
        if (trimmed.startsWith("DELETE")) return "DELETE";
        return "EXEC";
    }

    @Override
    public void close() throws SQLException {
        try {
            target.close();
        } finally {
            // 清理资源，防止内存泄漏
            if (parameters != null) {
                Arrays.fill(parameters, 0, paramCount, null);
            }
            stringBuilderCache.remove();
            sqlCache.remove();
        }
    }
}