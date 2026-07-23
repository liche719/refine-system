package com.achobeta.refine.common.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReadReplicaAspect {
    @Around("@annotation(com.achobeta.refine.common.datasource.ReadReplica) || " +
            "@within(com.achobeta.refine.common.datasource.ReadReplica)")
    public Object routeRead(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            ReadWriteContext.useReplica();
        }
        try {
            return joinPoint.proceed();
        } finally {
            ReadWriteContext.clear();
        }
    }
}
