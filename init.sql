-- Create aggregated metrics table
CREATE TABLE IF NOT EXISTS metrics_aggregated (
    id BIGSERIAL,
    tenant_id VARCHAR(50) NOT NULL,
    window_type VARCHAR(10) NOT NULL, -- '1m', '5m', '1h', '24h'
    event_type VARCHAR(100) NOT NULL, -- e.g. 'cpu_utilization', 'request_count'
    value_sum DOUBLE PRECISION NOT NULL,
    value_count BIGINT NOT NULL,
    value_avg DOUBLE PRECISION NOT NULL,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    PRIMARY KEY (id, window_start)
);

-- Note: CockroachDB supports declarative partitioning, but for Serverless clusters, 
-- a single table with primary key index on (tenant_id, window_type, event_type, window_start) 
-- is extremely fast. We define appropriate indexes here.

-- Create indexes on metrics_aggregated for query performance
CREATE INDEX IF NOT EXISTS idx_metrics_tenant_window ON metrics_aggregated (tenant_id, window_type, event_type);
CREATE INDEX IF NOT EXISTS idx_metrics_window_start ON metrics_aggregated (window_start DESC);

-- Create alerts table
CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    alert_value DOUBLE PRECISION NOT NULL,
    threshold DOUBLE PRECISION NOT NULL,
    z_score DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'ACTIVE', 'ACKNOWLEDGED', 'RESOLVED'
    message VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alerts_tenant_created ON alerts (tenant_id, created_at DESC);

-- Create support tickets table
CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    user_query TEXT NOT NULL,
    matched_intent VARCHAR(100),
    confidence DOUBLE PRECISION,
    status VARCHAR(20) NOT NULL, -- 'AUTO_RESOLVED', 'PENDING_ESCALATION', 'ESCALATED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tickets_tenant_status ON support_tickets (tenant_id, status);
