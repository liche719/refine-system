-- Local benchmark fixture only. It is never executed by Flyway.
-- Re-running this script is safe: it removes only rows with the benchmark prefix.
SET SESSION cte_max_recursion_depth = 20000;

START TRANSACTION;

DELETE FROM learning_db.MistakeQuestion
WHERE user_id = 'demo-user'
  AND question_id LIKE 'benchmark-%';

INSERT INTO learning_db.MistakeQuestion (
    user_id,
    question_id,
    question_content,
    subject,
    knowledge_point_id,
    question_status,
    source,
    create_time,
    update_time
)
WITH RECURSIVE sequence AS (
    SELECT 1 AS number
    UNION ALL
    SELECT number + 1
    FROM sequence
    WHERE number < 20000
)
SELECT
    'demo-user',
    CONCAT('benchmark-', LPAD(number, 5, '0')),
    CONCAT('Local benchmark question ', number),
    CASE MOD(number, 4)
        WHEN 0 THEN 'math'
        WHEN 1 THEN 'physics'
        WHEN 2 THEN 'chemistry'
        ELSE 'english'
    END,
    MOD(number, 20) + 1,
    CASE WHEN MOD(number, 5) = 0 THEN 1 ELSE 0 END,
    'benchmark',
    DATE_SUB(NOW(), INTERVAL number SECOND),
    DATE_SUB(NOW(), INTERVAL number SECOND)
FROM sequence;

COMMIT;

SELECT COUNT(*) AS benchmark_rows
FROM learning_db.MistakeQuestion
WHERE user_id = 'demo-user'
  AND question_id LIKE 'benchmark-%';
