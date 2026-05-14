package com.zxl.hazel.trace.support;

import com.zxl.hazel.trace.Span;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Span 工具类：提供 Span 相关的静态操作方法
 *
 * <p>职责：
 * <ul>
 *   <li>Span 工厂方法（创建根 Span、子 Span）</li>
 *   <li>Span 上下文管理（获取当前 Span、清理链路）</li>
 *   <li>Span 业务操作（设置事务ID等）</li>
 * </ul>
 *
 * @author hazel
 */
public final class SpanInitUtil {

    /**
     * Span ID全局序列号（用于生成唯一的spanId）
     */
    private static final AtomicLong SPAN_ID_SEQUENCE = new AtomicLong(0);

    private SpanInitUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 生成Trace ID（128位，32位hex）
     * 参考OpenTelemetry/Zipkin标准
     */
    public static String generateTraceId() {
        String uuid1 = UUID.randomUUID().toString().replace("-", "");
        String uuid2 = UUID.randomUUID().toString().replace("-", "");
        return uuid1.substring(0, 16) + uuid2.substring(0, 16);
    }

    /**
     * 生成全局唯一的Span ID（64位，16位hex）
     * 参考OpenTelemetry标准
     */
    public static String generateSpanId() {
        long sequence = SPAN_ID_SEQUENCE.incrementAndGet();
        long timestamp = System.currentTimeMillis();
        // 使用时间戳 + 序列号 + 随机数生成64位ID
        long high = timestamp ^ (sequence << 16);
        long low = UUID.randomUUID().getMostSignificantBits();
        String hex = Long.toHexString(high ^ low);
        // 补襦16位
        return String.format("%16s", hex).replace(' ', '0');
    }

    /**
     * 生成 Segment ID（128位，32位hex）
     * 参考 SkyWalking 标准
     * 同一线程内的 Span 共享同一个 segmentId
     */
    public static String generateSegmentId() {
        // 使用 UUID 生成唯一的 segmentId
        String uuid1 = UUID.randomUUID().toString().replace("-", "");
        String uuid2 = UUID.randomUUID().toString().replace("-", "");
        return uuid1.substring(0, 16) + uuid2.substring(0, 16);
    }


}
