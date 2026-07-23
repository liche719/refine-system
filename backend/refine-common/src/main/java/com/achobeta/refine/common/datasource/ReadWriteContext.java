package com.achobeta.refine.common.datasource;

final class ReadWriteContext {
    private static final ThreadLocal<Boolean> REPLICA = ThreadLocal.withInitial(() -> false);

    private ReadWriteContext() {
    }

    static void useReplica() {
        REPLICA.set(true);
    }

    static boolean isReplica() {
        return REPLICA.get();
    }

    static void clear() {
        REPLICA.remove();
    }
}
