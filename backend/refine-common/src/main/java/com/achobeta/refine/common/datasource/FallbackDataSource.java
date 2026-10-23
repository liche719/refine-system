package com.achobeta.refine.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

/**
 * 从库读失败时回退主库。
 *
 * <p>带熔断：从库连续不可用时，不再让每个读请求都先等一次连接超时，
 * 而是在熔断窗口内直接走主库，窗口结束后放一个探测请求。
 */
final class FallbackDataSource implements DataSource {
    private static final Logger log = LoggerFactory.getLogger(FallbackDataSource.class);
    private static final long DEFAULT_CIRCUIT_OPEN_MILLIS = 10_000L;

    private final DataSource replica;
    private final DataSource primary;
    private final ReplicaCircuitBreaker breaker;

    FallbackDataSource(DataSource replica, DataSource primary) {
        this(replica, primary, new ReplicaCircuitBreaker(DEFAULT_CIRCUIT_OPEN_MILLIS));
    }

    FallbackDataSource(DataSource replica, DataSource primary, ReplicaCircuitBreaker breaker) {
        this.replica = replica;
        this.primary = primary;
        this.breaker = breaker;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return openConnection(() -> replica.getConnection(), primary::getConnection, "read");
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return openConnection(() -> replica.getConnection(username, password),
                () -> primary.getConnection(username, password), "authenticated read");
    }

    private Connection openConnection(ConnectionSupplier replicaConnection,
                                      ConnectionSupplier primaryConnection,
                                      String mode) throws SQLException {
        if (breaker.isOpen()) {
            log.debug("Replica circuit is open; routing {} to primary without probing replica", mode);
            return primaryConnection.get();
        }
        try {
            Connection connection = replicaConnection.get();
            breaker.recordSuccess();
            return connection;
        } catch (SQLException exception) {
            breaker.recordFailure();
            log.warn("Replica unavailable; routing {} to primary for {}ms: {}",
                    mode, breaker.openDurationMillis(), exception.getMessage());
            return primaryConnection.get();
        }
    }

    @FunctionalInterface
    private interface ConnectionSupplier {
        Connection get() throws SQLException;
    }

    @Override public PrintWriter getLogWriter() throws SQLException { return primary.getLogWriter(); }
    @Override public void setLogWriter(PrintWriter out) throws SQLException { primary.setLogWriter(out); }
    @Override public void setLoginTimeout(int seconds) throws SQLException { primary.setLoginTimeout(seconds); }
    @Override public int getLoginTimeout() throws SQLException { return primary.getLoginTimeout(); }
    @Override public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException { return primary.getParentLogger(); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { return primary.unwrap(iface); }
    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return primary.isWrapperFor(iface); }
}
