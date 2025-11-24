CREATE OR REPLACE FUNCTION trigger_refresh_support_ticket_statistics()
RETURNS trigger AS $$
DECLARE
    affected_date DATE;
BEGIN
    IF TG_OP = 'DELETE' THEN
        affected_date := DATE(OLD.created_date);
    ELSE
        affected_date := DATE(NEW.created_date);
    END IF;

    IF affected_date >= CURRENT_DATE - INTERVAL '90 days' THEN
        PERFORM pg_notify('refresh_support_ticket_stats', affected_date::text);
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_refresh_support_ticket_statistics ON support_ticket;
CREATE TRIGGER tr_refresh_support_ticket_statistics
    AFTER INSERT OR UPDATE OR DELETE ON support_ticket
    FOR EACH ROW
    EXECUTE FUNCTION trigger_refresh_support_ticket_statistics();
