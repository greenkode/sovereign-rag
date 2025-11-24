package ai.sovereignrag.accounting.rule;

import ai.sovereignrag.accounting.GLException;
import ai.sovereignrag.accounting.GLSession;
import ai.sovereignrag.accounting.JournalRule;
import ai.sovereignrag.accounting.entity.GLAccountEntity;
import ai.sovereignrag.accounting.entity.GLEntryEntity;
import ai.sovereignrag.accounting.entity.GLTransactionEntity;
import ai.sovereignrag.accounting.entity.JournalEntity;
import ai.sovereignrag.commons.enumeration.ResponseCode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;


public class CanPost implements JournalRule {

    public void check (GLSession session, GLTransactionEntity txn,
                       String param, GLAccountEntity account, int[] entryOffsets, short[] layers)
        throws GLException
    {
        JournalEntity journal = txn.getJournal();
        Instant postDate   = txn.getPostDate();
        LocalDate end        = journal.getEnd();
        LocalDate start      = journal.getStart();
        LocalDate lockDate   = journal.getLockDate();

        if (journal.isClosed()) {
            throw new GLException (
                "Journal '" + journal.getName() + "' is closed", ResponseCode.TRANSACTION_NOT_PERMITTED);
        }
        if (postDate == null)
            throw new GLException ("Invalid transaction. Posting date is null", ResponseCode.TRANSACTION_FAILED);

        if (start != null && LocalDate.ofInstant(postDate, ZoneId.systemDefault()).isBefore (start)) {
            throw new GLException (
                "Journal '" + journal.getName() + 
                "' cannot accept transactions with a posting date less than " +
                        start, ResponseCode.TRANSACTION_NOT_PERMITTED
            );
        }
        if (end != null && LocalDate.ofInstant(postDate, ZoneId.systemDefault()).isAfter (end)) {
            throw new GLException (
                "Journal '" + journal.getName() + 
                "' cannot accept transactions with a post date greater than " +
                        end, ResponseCode.TRANSACTION_NOT_PERMITTED
            );
        }
        if (lockDate != null && (LocalDate.ofInstant(postDate, ZoneId.systemDefault()).isBefore(lockDate) ||
        LocalDate.ofInstant(postDate, ZoneId.systemDefault()).isEqual(lockDate))) {
            throw new GLException (
                "Journal '" + journal.getName() + 
                "' has a temporary lockDate at " + lockDate, ResponseCode.TRANSACTION_NOT_PERMITTED
            );
        }
        checkEntries (txn, postDate);
    }
    private void checkEntries (GLTransactionEntity txn, Instant postDate)
        throws GLException
    {
        var list = txn.getEntries();
        for (GLEntryEntity entry : list) {
            GLAccountEntity acct = entry.getAccount();
            Instant end = acct.getExpiration();
            if (end != null && postDate.isAfter(end)) {
                throw new GLException(
                        "Account '" + acct +
                                "' cannot accept transactions with a post date greater than " +
                                end, ResponseCode.INVALID_TRANSACTION
                );
            }
        }
    }
}
