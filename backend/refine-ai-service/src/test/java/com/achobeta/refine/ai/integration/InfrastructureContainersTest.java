package com.achobeta.refine.ai.integration;

import com.achobeta.refine.ai.analytics.infrastructure.AnalyticsMapper;
import com.achobeta.refine.ai.analytics.infrastructure.MyBatisAnalyticsRepository;
import com.achobeta.refine.ai.rag.RagDocumentRepository;
import com.achobeta.refine.ai.rag.application.query.RagChunkDraft;
import com.achobeta.refine.ai.rag.application.query.RagDocumentMetadata;
import com.achobeta.refine.ai.rag.application.query.RagSearchQuery;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.DriverManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class InfrastructureContainersTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("ai_db")
            .withUsername("ai_app")
            .withPassword("ai_app");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4"))
            .withExposedPorts(6379);

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management");

    @Container
    static final PostgreSQLContainer<?> PGVECTOR = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("vector_db")
            .withUsername("refine")
            .withPassword("refine");

    @Test
    void requiredInfrastructureSupportsTheExpectedProtocols() throws Exception {
        try (var mysql = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var postgres = DriverManager.getConnection(PGVECTOR.getJdbcUrl(), PGVECTOR.getUsername(), PGVECTOR.getPassword());
             var socket = new Socket()) {
            assertThat(mysql.createStatement().executeQuery("SELECT 1").next()).isTrue();
            postgres.createStatement().execute("CREATE EXTENSION IF NOT EXISTS vector");
            assertThat(postgres.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM pg_extension WHERE extname='vector'").next()).isTrue();
            socket.connect(new InetSocketAddress(REDIS.getHost(), REDIS.getMappedPort(6379)), 2_000);
            assertThat(socket.isConnected()).isTrue();
            assertThat(RABBIT.getAmqpPort()).isPositive();
        }
    }

    @Test
    void pgVectorStoresChunkedKnowledgeAndSearchesIt() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(PGVECTOR.getJdbcUrl());
        config.setUsername(PGVECTOR.getUsername());
        config.setPassword(PGVECTOR.getPassword());
        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            RagDocumentRepository repository = new RagDocumentRepository(new JdbcTemplate(dataSource));
            repository.initializeSchema(2);
            RagDocumentMetadata document = new RagDocumentMetadata("math/linear-equations.md", "a".repeat(64),
                    "Linear equations", "Mathematics", "Grade 7", "PEP", "Chapter 3", "3.1", "82", true);
            repository.replaceDocument(document, List.of(new RagChunkDraft(0, "An equation has an unknown value.",
                    "b".repeat(64), "[0.1,0.2]")), "embedding-v1", 2);

            var result = repository.semanticSearch(new RagSearchQuery("equation", "[0.1,0.2]", "embedding-v1", 2, 3));

            assertThat(result).singleElement().satisfies(chunk -> {
                assertThat(chunk.document().citation()).contains("Chapter 3");
                assertThat(chunk.content()).contains("unknown value");
            });
        }
    }

    @Test
    void analyticsIdempotencySuppressesDuplicatesButNotTruncation() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(MYSQL.getJdbcUrl());
        config.setUsername(MYSQL.getUsername());
        config.setPassword(MYSQL.getPassword());
        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            new JdbcTemplate(dataSource).execute("""
                    CREATE TABLE consumed_events (
                        event_id CHAR(36) NOT NULL,
                        event_type VARCHAR(100) NOT NULL,
                        consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (event_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            Configuration myBatisConfiguration = new Configuration();
            myBatisConfiguration.addMapper(AnalyticsMapper.class);
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            factory.setConfiguration(myBatisConfiguration);
            AnalyticsMapper mapper = new SqlSessionTemplate(factory.getObject()).getMapper(AnalyticsMapper.class);
            MyBatisAnalyticsRepository repository = new MyBatisAnalyticsRepository(mapper);

            assertThat(repository.markConsumed("00000000-0000-0000-0000-000000000001",
                    "learning.activity.recorded.v1")).isTrue();
            assertThat(repository.markConsumed("00000000-0000-0000-0000-000000000001",
                    "learning.activity.recorded.v1")).isFalse();
            assertThatThrownBy(() -> repository.markConsumed("00000000-0000-0000-0000-000000000002",
                    "x".repeat(101)))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(DuplicateKeyException.class);
        }
    }
}
