package com.achobeta.refine.common.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReadReplicaAspectTest {

    private final ReadReplicaAspect aspect = new ReadReplicaAspect();

    @AfterEach
    void clearRoute() {
        ReadWriteContext.clear();
    }

    @Test
    void routesNonTransactionalReadToReplica() throws Throwable {
        Method method = Sample.class.getDeclaredMethod("plainRead");
        AtomicBoolean sawReplicaRoute = new AtomicBoolean(false);

        aspect.routeRead(joinPoint(method, () -> {
            sawReplicaRoute.set(ReadWriteContext.isReplica());
            return null;
        }));

        assertThat(sawReplicaRoute).isTrue();
        assertThat(ReadWriteContext.isReplica()).isFalse();
    }

    @Test
    void refusesToRouteWhenMethodIsAlsoTransactional() throws Throwable {
        Method method = Sample.class.getDeclaredMethod("transactionalRead");
        AtomicBoolean sawPrimaryRoute = new AtomicBoolean(false);

        aspect.routeRead(joinPoint(method, () -> {
            sawPrimaryRoute.set(!ReadWriteContext.isReplica());
            return null;
        }));

        // 事务会绑定连接，若此刻切从库就可能把写事务连到只读库，因此必须留在主库
        assertThat(sawPrimaryRoute).isTrue();
    }

    @Test
    void nestedReadKeepsReplicaRouteForOuterCall() throws Throwable {
        Method method = Sample.class.getDeclaredMethod("plainRead");

        aspect.routeRead(joinPoint(method, () -> {
            // 模拟外层方法内部又调用了一个 @ReadReplica 方法
            aspect.routeRead(joinPoint(method, () -> null));
            assertThat(ReadWriteContext.isReplica()).isTrue();
            return null;
        }));

        assertThat(ReadWriteContext.isReplica()).isFalse();
    }

    @Test
    void nestedReadRestoresExistingReplicaRoute() throws Throwable {
        Method method = Sample.class.getDeclaredMethod("plainRead");
        ReadWriteContext.useReplica();

        aspect.routeRead(joinPoint(method, () -> {
            aspect.routeRead(joinPoint(method, () -> null));
            return null;
        }));

        // 外层仍然处于从库路由，直到最外层结束由调用者清理
        assertThat(ReadWriteContext.isReplica()).isTrue();
    }

    @Test
    void allowsRoutingForPlainMethods() throws NoSuchMethodException {
        assertThat(ReadReplicaAspect.canRouteToReplica(Sample.class.getDeclaredMethod("plainRead"))).isTrue();
        assertThat(ReadReplicaAspect.canRouteToReplica(Sample.class.getDeclaredMethod("transactionalRead"))).isFalse();
    }

    private static ProceedingJoinPoint joinPoint(Method method, ThrowingBody body) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        try {
            when(joinPoint.proceed()).thenAnswer(invocation -> body.run());
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
        return joinPoint;
    }

    interface ThrowingBody {
        Object run() throws Throwable;
    }

    static class Sample {
        void plainRead() {
        }

        @Transactional
        void transactionalRead() {
        }
    }
}
