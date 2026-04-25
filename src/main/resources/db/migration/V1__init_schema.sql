CREATE TABLE candidate_batch (
    id BIGSERIAL PRIMARY KEY,
    run_date DATE NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    source_count INTEGER NOT NULL DEFAULT 0,
    candidate_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE candidate_article (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES candidate_batch(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    source VARCHAR(200) NOT NULL,
    published_at TIMESTAMP,
    summary TEXT,
    coarse_filter_reason TEXT,
    llm_score DOUBLE PRECISION,
    llm_reason TEXT,
    rank_order INTEGER,
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_candidate_article_batch_url ON candidate_article(batch_id, url);
CREATE INDEX idx_candidate_article_run_order ON candidate_article(batch_id, rank_order);
CREATE INDEX idx_candidate_article_selected ON candidate_article(selected);
