package com.zxl.hazel.demo.aop;

import com.zxl.hazel.trace.Span;
import com.zxl.hazel.trace.Tracer;
import com.zxl.hazel.sync.SyncContext;
import com.zxl.hazel.sync.SyncContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@Order//保证足够大,开启事务以后才走此代理
public class SpringTransactionalAspect {

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object injectTransactionId(ProceedingJoinPoint pjp) throws Throwable {
        Span span = Tracer.currentSpan();
        String oldTransactionId = null;

        if (span != null) {
            oldTransactionId = span.getTransactionId();

            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                String transactionId = TransactionSynchronizationManager.getCurrentTransactionName();
                Tracer.setCurrentTransactionId(transactionId);
            }
        }

        try {
            return pjp.proceed();
        } catch (Exception e) {
            // 异常时也要等待异步任务，但要确保异常继续抛出
            waitForAsyncTasks();
            throw e;
        } finally {
            // 正常完成时等待异步任务
            if (!TransactionSynchronizationManager.isActualTransactionActive() ||
                    !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                // 只在有事务且不是只读时等待
                waitForAsyncTasks();
            }

            if (span != null) {
                span.setTransactionId(oldTransactionId);
            }
        }
    }

    private void waitForAsyncTasks() {
        // 只有当前有事务时才需要等待
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }

        SyncContext txContext = SyncContextHolder.getInstance().capture();
        if (txContext == null) {
            return;
        }

        Thread currentThread = Thread.currentThread();

        try {
            // 等待计数归零，如果有异常会自动抛出
            txContext.awaitRefCountZero(currentThread);
        } catch (Exception e) {
            // 异步任务异常，标记事务回滚
            throw new RuntimeException("异步任务执行失败，事务回滚", e);
        } finally {
            txContext.cleanupThread(currentThread);
        }
    }
}