CREATE OR REPLACE FUNCTION set_support_ticket_closed_date()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'CLOSED' AND (OLD.status IS NULL OR OLD.status != 'CLOSED') THEN
        NEW.closed_date = CURRENT_TIMESTAMP;
    ELSIF NEW.status != 'CLOSED' AND OLD.status = 'CLOSED' THEN
        NEW.closed_date = NULL;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_set_support_ticket_closed_date ON support_ticket;
CREATE TRIGGER tr_set_support_ticket_closed_date
    BEFORE UPDATE ON support_ticket
    FOR EACH ROW
    EXECUTE FUNCTION set_support_ticket_closed_date();
