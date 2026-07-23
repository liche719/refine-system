CREATE TABLE IF NOT EXISTS user_learning_vectors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    question_id VARCHAR(64) NULL,
    action_type VARCHAR(32) NOT NULL,
    question_content TEXT NULL,
    subject VARCHAR(64) NULL,
    knowledge_point_id INT NULL,
    embedding_text MEDIUMTEXT NULL,
    metadata_text TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_event (event_id),
    KEY idx_user_created (user_id, created_at),
    KEY idx_user_action (user_id, action_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS learning_insights (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    insight_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    confidence_score DECIMAL(5,4) NULL,
    metadata TEXT NULL,
    is_active TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_type_active (user_id, insight_type, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS consumed_events (
    event_id CHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
