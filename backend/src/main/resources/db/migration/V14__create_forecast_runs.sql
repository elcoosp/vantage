-- V14__create_forecast_runs.sql
-- Stores demand-forecast model runs for backtesting, accuracy tracking, and versioning.
-- Replaces the ad-hoc / no-persistence approach of the current Holt-Winters calculator.

CREATE TABLE IF NOT EXISTS forecast_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(100),
    trained_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    horizon_days INTEGER NOT NULL DEFAULT 7,
    mape DOUBLE PRECISION,
    smape DOUBLE PRECISION,
    mase DOUBLE PRECISION,
    pinball_loss DOUBLE PRECISION,
    coverage DOUBLE PRECISION,
    mse DOUBLE PRECISION,
    forecast_payload JSONB NOT NULL,
    feature_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_forecast_runs_tenant_product ON forecast_runs(tenant_id, product_id);
CREATE INDEX IF NOT EXISTS idx_forecast_runs_model ON forecast_runs(model_name, model_version);
CREATE INDEX IF NOT EXISTS idx_forecast_runs_trained_at ON forecast_runs(trained_at DESC);
