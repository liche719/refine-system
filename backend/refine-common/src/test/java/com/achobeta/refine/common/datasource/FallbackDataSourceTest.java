package com.achobeta.refine.common.datasource;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
}
