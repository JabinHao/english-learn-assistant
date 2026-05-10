CREATE TABLE llm_pipeline_cache (
    id BIGSERIAL PRIMARY KEY,
    cache_key VARCHAR(128) NOT NULL UNIQUE,
    operation VARCHAR(64) NOT NULL,
    prompt_hash VARCHAR(64) NOT NULL,
    result_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_llm_pipeline_cache_operation ON llm_pipeline_cache(operation, created_at);
