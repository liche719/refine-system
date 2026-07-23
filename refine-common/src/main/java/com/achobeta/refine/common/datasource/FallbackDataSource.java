package com.achobeta.refine.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

final class FallbackDataSource implements DataSource {
    private static final Logger log = LoggerFactory.getLogger(FallbackDataSource.class);
    private final DataSource replica;
    private final DataSource primary;

    FallbackDataSource(DataSource replica, DataSource primary) {
        this.replica = replica;
        this.primary = primary;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return replica.getConnection();
        } catch (SQLException exception) {
            log.warn("Replica unavailable; routing read to primary: {}", exception.getMessage());
            return primary.getConnection();
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        try {
            return replica.getConnection(username, password);
        } catch (SQLException exception) {
            log.warn("Replica unavailable; routing authenticated read to primary: {}", exception.getMessage());
            return primary.getConnection(username, password);
        }
    }

    @Override public PrintWriter getLogWriter() throws SQLException { return primary.getLogWriter(); }
    @Override public void setLogWriter(PrintWriter out) throws SQLException { primary.setLogWriter(out); }
    @Override public void setLoginTimeout(int seconds) throws SQLException { primary.setLoginTimeout(seconds); }
    @Override public int getLoginTimeout() throws SQLException { return primary.getLoginTimeout(); }
    @Override public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException { return primary.getParentLogger(); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { return primary.unwrap(iface); }
    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return primary.isWrapperFor(iface); }
}
