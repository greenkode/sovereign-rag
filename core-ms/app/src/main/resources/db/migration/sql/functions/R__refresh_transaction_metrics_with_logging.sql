CREATE OR REPLACE FUNCTION refresh_transaction_metrics_with_logging()
RETURNS TABLE (
    view_name TEXT,
    refresh_duration_seconds NUMERIC,
    status TEXT
) AS $$
DECLARE
    v_start_time TIMESTAMP;
    v_end_time TIMESTAMP;
    v_log_id BIGINT;
    v_row_count BIGINT;
BEGIN
    v_start_time := CURRENT_TIMESTAMP;
    INSERT INTO materialized_view_refresh_log (view_name, refresh_started_at)
    VALUES ('mv_transaction_metrics_daily', v_start_time)
    RETURNING id INTO v_log_id;

    BEGIN
        BEGIN
            REFRESH MATERIALIZED VIEW CONCURRENTLY mv_transaction_metrics_daily;
        EXCEPTION WHEN OTHERS THEN
            REFRESH MATERIALIZED VIEW mv_transaction_metrics_daily;
        END;
        GET DIAGNOSTICS v_row_count = ROW_COUNT;
        v_end_time := CURRENT_TIMESTAMP;

        UPDATE materialized_view_refresh_log
        SET refresh_completed_at = v_end_time,
            refresh_duration_seconds = EXTRACT(EPOCH FROM (v_end_time - v_start_time)),
            status = 'SUCCESS',
            row_count = v_row_count
        WHERE id = v_log_id;

        view_name := 'mv_transaction_metrics_daily';
        refresh_duration_seconds := EXTRACT(EPOCH FROM (v_end_time - v_start_time));
        status := 'SUCCESS';
        RETURN NEXT;
    EXCEPTION WHEN OTHERS THEN
        UPDATE materialized_view_refresh_log
        SET refresh_completed_at = CURRENT_TIMESTAMP,
            status = 'FAILED',
            error_message = SQLERRM
        WHERE id = v_log_id;

        view_name := 'mv_transaction_metrics_daily';
        refresh_duration_seconds := NULL;
        status := 'FAILED';
        RETURN NEXT;
    END;

    RETURN;
END;
$$ LANGUAGE plpgsql;
