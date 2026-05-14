package com.zxl.hazel.trace.exporter;

import com.zxl.hazel.ContextManager;
import com.zxl.hazel.apm.APMContextProcessorRegistry;
import com.zxl.hazel.apm.APMType;
import com.zxl.hazel.trace.Span;
import com.zxl.hazel.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.*;

/**
 * Trace导出器注册中心：管理所有导出器实例（异步批量导出）
 *
 * <p>设计原则：
 * <ul>
 *   <li><b>延迟导出</b>：endSpan只加入缓冲区，不立即导出</li>
 *   <li><b>批量触发</b>：缓冲区达到阈值或clear时触发导出</li>
 *   <li><b>溢出保护</b>：缓冲区满时丢弃新Span，防止内存溢出</li>
 *   <li><b>异步批量</b>：使用单线程池异步导出，不阻塞主流程</li>
 *   <li><b>导出器无关</b>：导出器直接从Span对象获取数据，不依赖线程上下文</li>
 *   <li><b>线程安全</b>：使用CopyOnWriteArrayList管理导出器</li>
 * </ul>
 *
 * @author hazel
 */
public class TraceExporterRegistry {

    private static final Logger log = LoggerFactory.getLogger(TraceExporterRegistry.class);

    /**
     * 导出器列表（线程安全）
     */
    private static final List<TraceExporter> exporters = new CopyOnWriteArrayList<>();

    /**
     * 是否已初始化SPI
     */
    private static volatile boolean initialized = false;

    /**
     * Span缓冲区（批量导出）
     */
    private static final List<Span> spanBuffer = new ArrayList<>(64);

    /**
     * 批量导出阈值
     */
    private static final int BATCH_SIZE = 64;

    /**
     * 缓冲区最大容量（溢出保护）
     */
    private static final int MAX_BUFFER_SIZE = 10000;

    /**
     * 异步导出线程池（单线程，保证顺序）
     */
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        private int counter = 0;

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "hazel-trace-exporter-" + counter++);
            thread.setDaemon(true);
            return thread;
        }
    });

    static {
        initSpiExporters();
        Runtime.getRuntime().addShutdownHook(new Thread(TraceExporterRegistry::shutdown, "hazel-trace-exporter-shutdown"));
    }

    /**
     * 注册导出器
     */
    public static void register(TraceExporter exporter) {
        if (exporter == null) {
            throw new IllegalArgumentException("Exporter cannot be null");
        }
        exporters.add(exporter);
        log.info("TraceExporter registered: {}", exporter.name());
    }

    /**
     * 注销导出器
     */
    public static void unregister(TraceExporter exporter) {
        exporters.remove(exporter);
        log.info("TraceExporter unregistered: {}", exporter.name());
    }

    /**
     * 导出Span（加入缓冲区，达到阈值或clear时统一导出）
     *
     * @param span 要导出的Span
     */
    public static void export(Span span) {
        if (span == null) {
            return;
        }

        synchronized (spanBuffer) {
            // 溢出保护：缓冲区满时丢弃
            if (spanBuffer.size() >= MAX_BUFFER_SIZE) {
                log.debug("Span buffer full ({}), dropping span: traceId={}", MAX_BUFFER_SIZE, span.getTraceId());
                return;
            }

            spanBuffer.add(span);

            // 批量触发：达到阈值时立即导出
            if (spanBuffer.size() >= BATCH_SIZE) {
                flush();
            }
        }
    }

    /**
     * 强制刷新缓冲区（立即导出所有待导出的Span）
     */
    public static void flush() {
        synchronized (spanBuffer) {
            try {
                Tracer.startSpan(Span.OPERATION_NAME_EXPORT);
                if (!spanBuffer.isEmpty()) {
                    flushBuffer();
                }
            } finally {
                Tracer.endSpan();
            }
        }
    }

    /**
     * 批量导出缓冲区中的Span（内部方法，调用前必须持有锁）
     */
    private static void flushBuffer() {
        if (spanBuffer.isEmpty()) {
            return;
        }

        List<Span> spansToExport = new ArrayList<>(spanBuffer);
        spanBuffer.clear();

        log.debug("Triggering async batch export: {} spans", spansToExport.size());

        Runnable task = ContextManager.wrap(() -> {
            log.debug("Async export thread started, exporting {} spans", spansToExport.size());
            for (TraceExporter exporter : exporters) {
                if (!exporter.isEnabled()) {
                    continue;
                }
                exportBatch(exporter, spansToExport);
            }
        });

        executor.submit(task);
    }

    /**
     * 批量导出Span到指定导出器
     */
    private static void exportBatch(TraceExporter exporter, List<Span> spans) {
        for (Span span : spans) {
            try {
                exporter.export(span);
            } catch (Exception e) {
                log.debug("Exporter {} failed to export span {}: {}", exporter.name(), span.getSpanId(), e.getMessage());
            }
        }
    }

    /**
     * 获取所有导出器
     */
    public static List<TraceExporter> getExporters() {
        return Collections.unmodifiableList(new ArrayList<>(exporters));
    }

    /**
     * 清空所有导出器
     */
    public static void clear() {
        for (TraceExporter exporter : exporters) {
            try {
                exporter.shutdown();
            } catch (Exception e) {
                log.warn("Failed to shutdown exporter {}: {}", exporter.name(), e.getMessage());
            }
        }
        exporters.clear();
        log.info("All trace exporters cleared");
    }

    /**
     * 初始化SPI导出器
     */
    private static void initSpiExporters() {
        if (initialized) {
            return;
        }

        synchronized (TraceExporterRegistry.class) {
            if (initialized) {
                return;
            }

            if (APMContextProcessorRegistry.getDetectedAPM() != APMType.NONE) {
                initialized = true;
                log.info("External APM detected, Hazel TraceExporter disabled");
                return;
            }

            ServiceLoader<TraceExporter> loader = ServiceLoader.load(TraceExporter.class);
            int count = 0;
            for (TraceExporter exporter : loader) {
                if (exporter.isEnabled()) {
                    exporters.add(exporter);
                    log.debug("SPI TraceExporter loaded: {}", exporter.name());
                    count++;
                }
            }

            initialized = true;
            log.debug("Loaded {} SPI TraceExporters", count);
        }
    }

    /**
     * 关闭导出器
     */
    public static void shutdown() {
        flush();

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                log.warn("Force shutdown trace exporter executor");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        clear();
    }
}