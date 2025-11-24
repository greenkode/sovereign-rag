CREATE OR REPLACE FUNCTION update_transaction_reference_sequences_last_updated()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_transaction_reference_sequences_last_updated_trigger ON transaction_reference_sequences;
CREATE TRIGGER update_transaction_reference_sequences_last_updated_trigger
    BEFORE UPDATE
    ON transaction_reference_sequences
    FOR EACH ROW
EXECUTE FUNCTION update_transaction_reference_sequences_last_updated();
