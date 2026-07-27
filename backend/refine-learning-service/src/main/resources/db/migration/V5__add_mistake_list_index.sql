-- Supports the default review-list path: user scope, unresolved status, newest first.
CREATE INDEX idx_mistake_user_status_updated
    ON MistakeQuestion (user_id, question_status, update_time DESC);
