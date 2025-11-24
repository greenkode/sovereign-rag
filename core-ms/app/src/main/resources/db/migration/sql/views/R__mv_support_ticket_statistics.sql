DROP MATERIALIZED VIEW IF EXISTS mv_support_ticket_statistics_daily CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_support_ticket_statistics_weekly CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_support_ticket_statistics_monthly CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_support_ticket_summary_current CASCADE;

CREATE MATERIALIZED VIEW mv_support_ticket_statistics_daily AS
WITH daily_stats AS (
    SELECT
        DATE(created_date) as stat_date,
        category,
        status,
        COUNT(*) as ticket_count
    FROM support_ticket
    GROUP BY DATE(created_date), category, status
),
pivoted_stats AS (
    SELECT
        stat_date,
        category,
        COALESCE(SUM(CASE WHEN status = 'OPEN' THEN ticket_count END), 0) as open_count,
        COALESCE(SUM(CASE WHEN status = 'IN_PROGRESS' THEN ticket_count END), 0) as in_progress_count,
        COALESCE(SUM(CASE WHEN status = 'CLOSED' THEN ticket_count END), 0) as closed_count,
        SUM(ticket_count) as total_count
    FROM daily_stats
    GROUP BY stat_date, category
)
SELECT
    stat_date,
    category,
    open_count,
    in_progress_count,
    closed_count,
    total_count,
    stat_date as date_range_start,
    stat_date + INTERVAL '1 day' - INTERVAL '1 second' as date_range_end,
    NOW() as last_updated
FROM pivoted_stats
ORDER BY stat_date DESC, category;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_support_ticket_stats_daily_pk
ON mv_support_ticket_statistics_daily (stat_date, category);

CREATE INDEX IF NOT EXISTS idx_mv_support_ticket_stats_daily_date_range
ON mv_support_ticket_statistics_daily (date_range_start, date_range_end);

CREATE MATERIALIZED VIEW mv_support_ticket_statistics_weekly AS
WITH weekly_stats AS (
    SELECT
        DATE_TRUNC('week', created_date) as week_start,
        category,
        status,
        COUNT(*) as ticket_count
    FROM support_ticket
    GROUP BY DATE_TRUNC('week', created_date), category, status
),
pivoted_weekly AS (
    SELECT
        week_start,
        category,
        COALESCE(SUM(CASE WHEN status = 'OPEN' THEN ticket_count END), 0) as open_count,
        COALESCE(SUM(CASE WHEN status = 'IN_PROGRESS' THEN ticket_count END), 0) as in_progress_count,
        COALESCE(SUM(CASE WHEN status = 'CLOSED' THEN ticket_count END), 0) as closed_count,
        SUM(ticket_count) as total_count
    FROM weekly_stats
    GROUP BY week_start, category
)
SELECT
    week_start,
    category,
    open_count,
    in_progress_count,
    closed_count,
    total_count,
    week_start as date_range_start,
    week_start + INTERVAL '7 days' - INTERVAL '1 second' as date_range_end,
    NOW() as last_updated
FROM pivoted_weekly
ORDER BY week_start DESC, category;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_support_ticket_stats_weekly_pk
ON mv_support_ticket_statistics_weekly (week_start, category);

CREATE MATERIALIZED VIEW mv_support_ticket_statistics_monthly AS
WITH monthly_stats AS (
    SELECT
        DATE_TRUNC('month', created_date) as month_start,
        category,
        status,
        COUNT(*) as ticket_count
    FROM support_ticket
    GROUP BY DATE_TRUNC('month', created_date), category, status
),
pivoted_monthly AS (
    SELECT
        month_start,
        category,
        COALESCE(SUM(CASE WHEN status = 'OPEN' THEN ticket_count END), 0) as open_count,
        COALESCE(SUM(CASE WHEN status = 'IN_PROGRESS' THEN ticket_count END), 0) as in_progress_count,
        COALESCE(SUM(CASE WHEN status = 'CLOSED' THEN ticket_count END), 0) as closed_count,
        SUM(ticket_count) as total_count
    FROM monthly_stats
    GROUP BY month_start, category
)
SELECT
    month_start,
    category,
    open_count,
    in_progress_count,
    closed_count,
    total_count,
    month_start as date_range_start,
    month_start + INTERVAL '1 month' - INTERVAL '1 second' as date_range_end,
    NOW() as last_updated
FROM pivoted_monthly
ORDER BY month_start DESC, category;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_support_ticket_stats_monthly_pk
ON mv_support_ticket_statistics_monthly (month_start, category);

CREATE MATERIALIZED VIEW mv_support_ticket_summary_current AS
SELECT
    category,
    status,
    COUNT(*) as current_count,
    MIN(created_date) as oldest_ticket,
    MAX(created_date) as newest_ticket,
    AVG(EXTRACT(EPOCH FROM (COALESCE(closed_date, CURRENT_TIMESTAMP) - created_date))/3600) as avg_age_hours,
    NOW() as last_updated
FROM support_ticket
GROUP BY category, status
ORDER BY category, status;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_support_ticket_summary_current_pk
ON mv_support_ticket_summary_current (category, status);
