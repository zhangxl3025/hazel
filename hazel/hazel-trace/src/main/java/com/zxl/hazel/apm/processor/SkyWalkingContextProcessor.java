package com.zxl.hazel.apm.processor;

import com.zxl.hazel.apm.APMContextProcessor;
import com.zxl.hazel.apm.APMType;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SkyWalking APM 上下文处理器
 *
 * <p>使用 SkyWalking 官方 API 实现跨线程上下文传递：
 * <ul>
 *   <li>ContextManager.capture() - 捕获当前上下文</li>
 *   <li>ContextManager.continued() - 恢复上下文到当前线程</li>
 *   <li>ContextManager.remove() - 清理当前线程上下文</li>
 * </ul>
 *
 * <p><b>依赖要求：</b>项目必须已挂载 skywalking-agent.jar
 *
 * @author hazel
 */
public class SkyWalkingContextProcessor implements APMContextProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SkyWalkingContextProcessor.class);

    @Override
    public APMType supportedAPM() {
        return APMType.SKYWALKING;
    }

    @Override
    public Object capture() {
        // 直接调用官方 API
        return ContextManager.capture();
    }

    @Override
    public void restore(Object snapshot) {
        if (snapshot == null) {
            return;
        }
        // 直接调用官方 API
        ContextManager.continued((ContextSnapshot) snapshot);
    }

    @Override
    public void cleanup() {
        // 清理当前线程上下文
    }
}