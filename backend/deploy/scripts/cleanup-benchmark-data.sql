-- Removes only the local benchmark fixture created by prepare-benchmark-data.sql.
DELETE FROM learning_db.MistakeQuestion
WHERE user_id = 'demo-user'
  AND question_id LIKE 'benchmark-%';
