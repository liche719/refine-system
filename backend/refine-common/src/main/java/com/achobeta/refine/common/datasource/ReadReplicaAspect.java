package com.achobeta.refine.common.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * 把 {@link ReadReplica} 标注的方法路由到从库。
 *
 * <p>两个必须处理好的边界：
 * <ol>
 *   <li><b>嵌套调用</b>：内层方法返回时不能直接把标记清掉，否则外层后续查询会悄悄走主库
 *       （读放大失效，且行为随调用链变化）。这里保存并恢复之前的值。</li>
 *   <li><b>与事务共存</b>：切面优先级高于事务拦截器，事务是在切面之后才开启的，
 *       因此这里无法通过 {@code isActualTransactionActive()} 判断。若目标方法或类上还有
 *       {@code @Transactional}，说明会绑定连接后再路由，可能把写事务连到只读从库——
 *       这种情况直接放弃路由、留在主库并告警。</li>
 * </ol>
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReadReplicaAspect {
    private static final Logger log = LoggerFactory.getLogger(ReadReplicaAspect.class);

    @Around("@annotation(com.achobeta.refine.common.datasource.ReadReplica) || " +
            "@within(com.achobeta.refine.common.datasource.ReadReplica)")
    public Object routeRead(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        if (!canRouteToReplica(method)) {
            log.warn("@ReadReplica is combined with @Transactional, routing stays on primary: {}",
                    method.getDeclaringClass().getSimpleName() + "#" + method.getName());
            return joinPoint.proceed();
        }
        boolean previous = ReadWriteContext.isReplica();
        ReadWriteContext.useReplica();
        try {
            return joinPoint.proceed();
        } finally {
            if (previous) {
                ReadWriteContext.useReplica();
            } else {
                ReadWriteContext.clear();
            }
        }
    }

    static boolean canRouteToReplica(Method method) {
        return !AnnotatedElementUtils.hasAnnotation(method, Transactional.class)
                && !AnnotatedElementUtils.hasAnnotation(method.getDeclaringClass(), Transactional.class);
    }
}
