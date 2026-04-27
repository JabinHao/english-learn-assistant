CREATE TABLE chat_session (
    id BIGSERIAL PRIMARY KEY,
    learning_article_id BIGINT NOT NULL REFERENCES learning_article(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_message (
    id BIGSERIAL PRIMARY KEY,
    chat_session_id BIGINT NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_session_learning_article ON chat_session(learning_article_id, created_at);
CREATE INDEX idx_chat_message_session_created ON chat_message(chat_session_id, created_at);
