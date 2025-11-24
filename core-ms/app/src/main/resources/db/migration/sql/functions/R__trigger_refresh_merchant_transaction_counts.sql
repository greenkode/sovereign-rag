CREATE OR REPLACE FUNCTION trigger_refresh_merchant_transaction_counts()
RETURNS trigger AS $$
BEGIN
    IF (TG_OP = 'INSERT' AND NEW.transaction_group = 'BILL_PAYMENT') OR
       (TG_OP = 'UPDATE' AND (OLD.transaction_group = 'BILL_PAYMENT' OR NEW.transaction_group = 'BILL_PAYMENT')) OR
       (TG_OP = 'DELETE' AND OLD.transaction_group = 'BILL_PAYMENT') THEN

        PERFORM pg_notify('refresh_merchant_counts', '');
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_refresh_merchant_transaction_counts ON transaction;
CREATE TRIGGER tr_refresh_merchant_transaction_counts
    AFTER INSERT OR UPDATE OR DELETE ON transaction
    FOR EACH ROW
    EXECUTE FUNCTION trigger_refresh_merchant_transaction_counts();
