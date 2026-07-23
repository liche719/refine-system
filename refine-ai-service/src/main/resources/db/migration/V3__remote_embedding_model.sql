ALTER TABLE user_learning_vectors
    ADD COLUMN embedding_model VARCHAR(128) NULL AFTER embedding_text;

