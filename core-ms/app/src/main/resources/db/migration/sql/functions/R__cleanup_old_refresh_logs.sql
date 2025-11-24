CREATE OR REPLACE FUNCTION cleanup_old_refresh_logs()
RETURNS void AS $$
BEGIN
    DELETE FROM materialized_view_refresh_log
    WHERE refresh_started_at < CURRENT_TIMESTAMP - INTERVAL '7 days';
END;
$$ LANGUAGE plpgsql;
