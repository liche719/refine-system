package com.achobeta.refine.common.datasource;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * 从库可用性的极简熔断器。
 *
 * <p>目标不是做通用熔断框架，而是解决一个具体放大效应：
 * 从库挂掉时，每个读请求都要先等一次连接超时才回退主库；持续流量下这些等待会占满
 * 请求线程与主库连接池，把「读慢」升级成「整站不可写」。熔断打开后，读请求直接走主库，
 * 到期再放一个探测请求。
 */
final class ReplicaCircuitBreaker {

    private static final long CLOSED = Long.MIN_VALUE;

    private final long openDurationMillis;
    private final LongSupplier clock;
    private final AtomicLong openUntilMillis = new AtomicLong(CLOSED);

    ReplicaCircuitBreaker(long openDurationMillis) {
        this(openDurationMillis, System::currentTimeMillis);
    }

    ReplicaCircuitBreaker(long openDurationMillis, LongSupplier clock) {
        this.openDurationMillis = Math.max(0L, openDurationMillis);
        this.clock = clock;
    }

    /** true 表示当前处于熔断期，调用方不要再尝试从库。 */
    boolean isOpen() {
        return clock.getAsLong() < openUntilMillis.get();
    }

    void recordFailure() {
        openUntilMillis.set(clock.getAsLong() + openDurationMillis);
    }

    void recordSuccess() {
        openUntilMillis.set(CLOSED);
    }

    long openDurationMillis() {
        return openDurationMillis;
    }
}
