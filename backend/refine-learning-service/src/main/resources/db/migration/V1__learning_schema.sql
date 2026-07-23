CREATE TABLE IF NOT EXISTS knowledgePoint (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    knowledge_point_id INT NOT NULL,
    parent_knowledge_point_id INT NULL,
    knowledge_desc VARCHAR(500) NULL,
    knowledge_level TINYINT NOT NULL DEFAULT 1,
    knowledge_point_name VARCHAR(100) NOT NULL,
    subject VARCHAR(50) NULL,
    status TINYINT NOT NULL DEFAULT 0,
    note TEXT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_knowledge (user_id, knowledge_point_id),
    KEY idx_parent (user_id, parent_knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS MistakeQuestion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    question_id VARCHAR(64) NOT NULL,
    question_content TEXT NOT NULL,
    subject VARCHAR(50) NULL,
    is_careless TINYINT NOT NULL DEFAULT 0,
    is_unfamiliar TINYINT NOT NULL DEFAULT 0,
    is_calculate_err TINYINT NOT NULL DEFAULT 0,
    is_time_shortage TINYINT NOT NULL DEFAULT 0,
    other_reason_flag TINYINT NOT NULL DEFAULT 0,
    other_reason VARCHAR(255) NULL,
    knowledge_point_id INT NULL,
    study_note TEXT NULL,
    question_status TINYINT NOT NULL DEFAULT 0,
    source VARCHAR(32) NOT NULL DEFAULT 'manual',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_question (user_id, question_id),
    KEY idx_user_created (user_id, create_time),
    KEY idx_user_knowledge (user_id, knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS UserData (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    questions_num INT NOT NULL DEFAULT 0,
    review_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    hard_questions INT NOT NULL DEFAULT 0,
    study_time INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_data (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS RenderBook (
    id BIGINT NOT NULL AUTO_INCREMENT,
    book_name VARCHAR(100) NOT NULL,
    book_content TEXT NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
