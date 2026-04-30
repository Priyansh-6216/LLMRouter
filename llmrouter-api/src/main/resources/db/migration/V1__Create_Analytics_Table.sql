CREATE TABLE IF NOT EXISTS request_analytics (
    request_id VARCHAR(50) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    latency_ms BIGINT,
    input_tokens INTEGER,
    output_tokens INTEGER,
    estimated_cost_usd DOUBLE PRECISION,
    cache_hit BOOLEAN,
    fallback_used BOOLEAN,
    task_type VARCHAR(50),
    routing_policy VARCHAR(50),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    error TEXT
);

CREATE INDEX idx_tenant_id ON request_analytics(tenant_id);
CREATE INDEX idx_timestamp ON request_analytics(timestamp);
