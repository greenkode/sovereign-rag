package ai.sovereignrag.accounting;/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2024 jPOS Software SRL
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import ai.sovereignrag.accounting.entity.GLAccountEntity;
import ai.sovereignrag.accounting.entity.GLEntryEntity;
import ai.sovereignrag.accounting.entity.JournalEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * ai.sovereignrag.minigl.AccountEntity Detail bulk response object.
 *
 * @author <a href="mailto:apr@jpos.org">Alejandro Revilla</a>
 * @see GLSession#getAccountDetail
 */
public class AccountDetail {
    JournalEntity journal;
    GLAccountEntity account;
    Instant end;
    Instant start;
    BigDecimal initialBalance;
    BigDecimal finalBalance;
    BigDecimal debits;
    BigDecimal credits;
    List<GLEntryEntity> entries;
    short[] layers;

    /**
     * Constructs an ai.sovereignrag.minigl.AccountDetail.
     * @param journal the ai.sovereignrag.minigl.JournalEntity.
     * @param account the account.
     * @param balance balance (reporting currency), could be initial if naturalOrder or final if not.
     * @param start start date (inclusive).
     * @param end end date (inclusive).
     * @param entries list of GLEntries.
     * @param layers the layers involved in this detail
     * @param ascendingOrder if we should compute balance normally or inverted
     */
    public AccountDetail(
            JournalEntity journal, GLAccountEntity account,
            BigDecimal balance,
            Instant start, Instant end, List<GLEntryEntity> entries, short[] layers, boolean ascendingOrder)
    {
        super();
        this.journal               = journal;
        this.account               = account;
        this.start                 = start;
        this.end                   = end;
        this.entries               = entries;
        this.layers                = layers;
        if (ascendingOrder) {
            this.initialBalance = balance;
            computeBalances();
        } else {
            this.finalBalance = balance;
            computeReverseBalances(balance);
        }         
    }

    /**
     * Constructs an ai.sovereignrag.minigl.AccountDetail.
     * @param journal the ai.sovereignrag.minigl.JournalEntity.
     * @param account the account.
     * @param balance balance (reporting currency), could be initial if naturalOrder or final if not.
     * @param start start date (inclusive).
     * @param end end date (inclusive).
     * @param entries list of GLEntries.
     * @param layers the layers involved in this detail
     */
    public AccountDetail(
            JournalEntity journal, GLAccountEntity account,
            BigDecimal balance,
            Instant start, Instant end, List<GLEntryEntity> entries, short[] layers)
    {
        this(journal, account, balance, start, end, entries, layers, true);
    }

    /**
     * Constructs an ai.sovereignrag.minigl.AccountDetail (reverse order, mini statement)
     * @param journal the ai.sovereignrag.minigl.JournalEntity.
     * @param account the account.
     * @param balance final balance (reporting currency).
     * @param entries list of GLEntries.
     * @param layers the layers involved in this detail
     */
    public AccountDetail(
            JournalEntity journal, GLAccountEntity account,
            BigDecimal balance,
            List<GLEntryEntity> entries, short[] layers)
    {
        this(journal, account, balance, null, null, entries, layers, false);
    }

    public JournalEntity getJournal() {
        return journal;
    }
    public GLAccountEntity getAccount() {
        return account;
    }
    public BigDecimal getInitialBalance() {
        return initialBalance;
    }
    public BigDecimal getFinalBalance() {
        return finalBalance;
    }
    public BigDecimal getDebits () {
        return debits;
    }
    public BigDecimal getCredits () {
        return credits;
    }
    public Instant getStart() {
        return start;
    }
    public Instant getEnd() {
        return end;
    }
    public List<GLEntryEntity> getEntries() {
        return entries;
    }
    public short[] getLayers() {
        return layers;
    }
    public int size() {
        return entries.size();
    }
    private void computeBalances() {
        BigDecimal balance = initialBalance;
        debits = credits = BigDecimal.ZERO;
        for (GLEntryEntity entry : entries) {
            balance = balance.add (entry.getImpact());
            entry.setBalance (balance);
            if (entry.isCredit())
                credits = credits.add (entry.getAmount());
            else
                debits = debits.add (entry.getAmount());
        }
        finalBalance = balance;
    }
    private void computeReverseBalances(BigDecimal balance) {
        debits = credits = BigDecimal.ZERO;
        for (GLEntryEntity entry : entries) {
            if (end == null)
                end = entry.getTransaction().getTimestamp();
            start = entry.getTransaction().getTimestamp();
            entry.setBalance(balance);
            balance = balance.subtract (entry.getImpact());
            if (entry.isCredit())
                credits = credits.add (entry.getAmount());
            else
                debits = debits.add (entry.getAmount());
        }
        this.initialBalance = balance;

    }
}

