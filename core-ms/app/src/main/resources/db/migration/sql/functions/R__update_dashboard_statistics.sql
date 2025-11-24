CREATE OR REPLACE FUNCTION update_dashboard_statistics()
    RETURNS void AS
$$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'mv_daily_core_metrics') THEN
        ANALYZE mv_daily_core_metrics;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'mv_hourly_reconciliation_metrics') THEN
        ANALYZE mv_hourly_reconciliation_metrics;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'mv_daily_transaction_metrics') THEN
        ANALYZE mv_daily_transaction_metrics;
    END IF;
END;
$$ LANGUAGE plpgsql;
