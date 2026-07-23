CREATE TABLE IF NOT EXISTS UserInformation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    user_picture_resource VARCHAR(255) NULL,
    user_account VARCHAR(255) NOT NULL,
    user_phone_num VARCHAR(20) NULL,
    user_email VARCHAR(255) NULL,
    user_password VARCHAR(100) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_status TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    UNIQUE KEY uk_user_account (user_account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
