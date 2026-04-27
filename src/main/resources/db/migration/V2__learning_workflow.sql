CREATE TABLE learning_article (
    id BIGSERIAL PRIMARY KEY,
    candidate_article_id BIGINT NOT NULL UNIQUE REFERENCES candidate_article(id),
    status VARCHAR(32) NOT NULL,
    title VARCHAR(500) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    source VARCHAR(200) NOT NULL,
    published_at TIMESTAMP,
    article_content TEXT,
    summary TEXT,
    selected_at TIMESTAMP NOT NULL,
    translated_at TIMESTAMP,
    vocabulary_extracted_at TIMESTAMP,
    eudic_pushed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE article_paragraph (
    id BIGSERIAL PRIMARY KEY,
    learning_article_id BIGINT NOT NULL REFERENCES learning_article(id) ON DELETE CASCADE,
    paragraph_index INTEGER NOT NULL,
    english_text TEXT NOT NULL,
    chinese_text TEXT
);

CREATE TABLE vocabulary_item (
    id BIGSERIAL PRIMARY KEY,
    learning_article_id BIGINT NOT NULL REFERENCES learning_article(id) ON DELETE CASCADE,
    word VARCHAR(255) NOT NULL,
    lemma VARCHAR(255),
    type VARCHAR(32) NOT NULL,
    ipa VARCHAR(128),
    english_definition TEXT,
    chinese_definition TEXT,
    source_sentence TEXT,
    eudic_pushed BOOLEAN NOT NULL DEFAULT FALSE,
    eudic_pushed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_learning_article_status ON learning_article(status);
CREATE INDEX idx_article_paragraph_learning_article ON article_paragraph(learning_article_id, paragraph_index);
CREATE INDEX idx_vocabulary_item_learning_article ON vocabulary_item(learning_article_id, created_at);
