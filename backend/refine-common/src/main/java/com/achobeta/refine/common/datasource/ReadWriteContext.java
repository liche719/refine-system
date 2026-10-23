package com.achobeta.refine.common.datasource;

final class ReadWriteContext {
    /**
     * 用普通 ThreadLocal 而不是 withInitial：否则每次读连接都会在线程池线程上留下一个
     * false 的残留条目，只能等线程复用后偶然被覆盖。
     */
    private static final ThreadLocal<Boolean> REPLICA = new ThreadLocal<>();

    private ReadWriteContext() {
    }

    static void useReplica() {
        REPLICA.set(Boolean.TRUE);
    }

    static boolean isReplica() {
        return Boolean.TRUE.equals(REPLICA.get());
    }

    static void clear() {
        REPLICA.remove();
    }
}
