package com.achobeta.refine.common.datasource;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FallbackDataSourceTest {

    @Test
    void usesPrimaryWhenReplicaConnectionFails() throws SQLException {
        DataSource replica = mock(DataSource.class);
        DataSource primary = mock(DataSource.class);
        Connection primaryConnection = mock(Connection.class);
        when(replica.getConnection()).thenThrow(new SQLException("replica stopped"));
        when(primary.getConnection()).thenReturn(primaryConnection);

        Connection actual = new FallbackDataSource(replica, primary).getConnection();

        assertThat(actual).isSameAs(primaryConnection);
        verify(primary).getConnection();
    }

    @Test
    void stopsProbingReplicaWhileCircuitIsOpen() throws SQLException {
        DataSource replica = mock(DataSource.class);
        DataSource primary = mock(DataSource.class);
        when(replica.getConnection()).thenThrow(new SQLException("replica stopped"));
        when(primary.getConnection()).thenReturn(mock(Connection.class));
        AtomicLong now = new AtomicLong(1_000L);
        ReplicaCircuitBreaker breaker = new ReplicaCircuitBreaker(10_000L, now::get);
        FallbackDataSource dataSource = new FallbackDataSource(replica, primary, breaker);

        // 第一次：探测失败 → 熔断打开
        dataSource.getConnection();
        // 熔断期内再读 5 次，不应该再碰从库（否则每个请求都要等一次连接超时）
        for (int i = 0; i < 5; i++) {
            dataSource.getConnection();
        }

        verify(replica, times(1)).getConnection();
        verify(primary, times(6)).getConnection();
    }

    @Test
    void probesReplicaAgainAfterCircuitWindow() throws SQLException {
        DataSource replica = mock(DataSource.class);
        DataSource primary = mock(DataSource.class);
        Connection replicaConnection = mock(Connection.class);
        when(primary.getConnection()).thenReturn(mock(Connection.class));
        when(replica.getConnection())
                .thenThrow(new SQLException("replica stopped"))
                .thenReturn(replicaConnection);
        AtomicLong now = new AtomicLong(1_000L);
        ReplicaCircuitBreaker breaker = new ReplicaCircuitBreaker(10_000L, now::get);
        FallbackDataSource dataSource = new FallbackDataSource(replica, primary, breaker);

        dataSource.getConnection();
        now.addAndGet(10_001L);
        Connection recovered = dataSource.getConnection();

        assertThat(recovered).isSameAs(replicaConnection);
        verify(replica, times(2)).getConnection();
    }
}
