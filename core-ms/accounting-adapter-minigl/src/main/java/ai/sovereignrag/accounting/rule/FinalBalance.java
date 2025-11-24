/*
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

package ai.sovereignrag.accounting.rule;

import ai.sovereignrag.accounting.GLException;
import ai.sovereignrag.accounting.GLSession;
import ai.sovereignrag.accounting.JournalRule;
import ai.sovereignrag.accounting.entity.GLAccountEntity;
import ai.sovereignrag.accounting.entity.GLEntryEntity;
import ai.sovereignrag.accounting.entity.GLTransactionEntity;
import ai.sovereignrag.accounting.entity.JournalEntity;
import ai.sovereignrag.commons.enumeration.ResponseCode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;

import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ZERO;

public abstract class FinalBalance implements JournalRule {
    private static final Log log = 
        LogFactory.getLog (FinalBalance.class);

    protected abstract String getRuleName();
    protected abstract boolean isError
        (BigDecimal balance, BigDecimal paramBalance);

    public void check (GLSession session, GLTransactionEntity txn,
                       String param, GLAccountEntity account, int[] offsets, short[] layers)
        throws GLException, HibernateException
    {
        List entries = txn.getEntries();
        if (entries.size() == 0 || offsets.length ==0)
            return;
        for (int i=0; i<offsets.length; i++) {
            GLEntryEntity entry = ((GLEntryEntity) entries.get (offsets[i]));
            if (match (entry.getLayer(), layers))
                checkBalance (session, txn, entry, param, offsets, layers);
        }
    }
    private void checkBalance 
        (GLSession session, GLTransactionEntity txn, GLEntryEntity entry, 
         String param, int[] offsets, short[] layers)
        throws GLException, HibernateException
    {
        JournalEntity journal = txn.getJournal();
        GLAccountEntity account = entry.getAccount();
        session.lock (journal, account);

        BigDecimal balance = session.getBalance (journal, account, layers);
        BigDecimal minBalance = 
            (param != null) ?  new BigDecimal (param) : ZERO;
        BigDecimal impact = getImpact (txn, account, offsets, layers);

        if (isError (balance.add (impact), minBalance)) {
            String sb = getRuleName() + " ai.sovereignrag.minigl.rule for account " +
                    account.getCode() +
                    " failed. balance=" +
                    balance +
                    ", impact=" +
                    impact +
                    ", minimum=" +
                    param;
            throw new GLException (sb, ResponseCode.TRANSACTION_FAILED);
        }
    }
    private BigDecimal getImpact 
        (GLTransactionEntity txn, GLAccountEntity account, int[] offsets, short[] layers)
    {
        BigDecimal impact = ZERO;
        List entries = txn.getEntries();
        
        for (int i=0; i<offsets.length; i++) {
            GLEntryEntity entry = ((GLEntryEntity) entries.get (offsets[i]));
            if (account.equals (entry.getAccount()) && match (entry.getLayer(), layers)) 
                impact = impact.add (entry.getImpact());
        }
        return impact;
    }
    private boolean match (short layer, short[] layers) {
        for (int i=0; i<layers.length; i++)
            if (layers[i] == layer)
                return true;
        return false;
    }
}

