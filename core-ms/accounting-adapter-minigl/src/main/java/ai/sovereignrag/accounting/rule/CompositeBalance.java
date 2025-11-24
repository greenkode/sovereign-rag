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

import ai.sovereignrag.accounting.entity.GLAccountEntity;
import ai.sovereignrag.accounting.entity.GLEntryEntity;
import ai.sovereignrag.accounting.entity.GLTransactionEntity;
import ai.sovereignrag.accounting.entity.JournalEntity;
import ai.sovereignrag.accounting.GLException;
import ai.sovereignrag.accounting.GLSession;
import ai.sovereignrag.accounting.JournalRule;
import ai.sovereignrag.commons.enumeration.ResponseCode;
import org.hibernate.HibernateException;

import java.math.BigDecimal;
import java.util.List;

abstract class CompositeBalance implements JournalRule {
    private static final BigDecimal ZERO = new BigDecimal ("0.00");

    protected abstract String getRuleName();
    protected abstract boolean isError
        (BigDecimal balance, BigDecimal paramBalance);

    public void check (GLSession session, GLTransactionEntity txn,
                       String param, GLAccountEntity account, int[] offsets, short[] layers)
         
        throws GLException, HibernateException
    {
        JournalEntity journal = txn.getJournal();
        session.lock (journal, account);
        BigDecimal balance = session.getBalance (journal, account, layers);

        BigDecimal paramBalance =
            (param != null) ?  new BigDecimal (param) : ZERO;
        BigDecimal impact = getImpact (txn, layers, offsets);
        if (isError (balance.add(impact), paramBalance)) {
            StringBuffer sb = new StringBuffer (getRuleName());
            sb.append (" ai.sovereignrag.minigl.rule for account ");
            sb.append (account.getCode());
            sb.append (" failed. balance=");
            sb.append (balance);
            sb.append (", impact=");
            sb.append (impact);
            sb.append (", minimum=");
            sb.append (param);
            throw new GLException (sb.toString(), ResponseCode.TRANSACTION_FAILED);
        }
    }
    private BigDecimal getImpact (GLTransactionEntity txn, short[] layers, int[] offsets) {
        BigDecimal impact = ZERO;
        List entries = txn.getEntries();
        
        for (int i=0; i<offsets.length; i++) {
            GLEntryEntity e = ((GLEntryEntity) entries.get (offsets[i]));
            if (isInLayer(e.getLayer(), layers)) {
                impact = impact.add (
                  e.getImpact ()
                );
            }
        }
        return impact;
    }

    private boolean isInLayer (short layer, short[] layers) {
        for (short l : layers)
            if (layer == l)
                return true;
        return false;
    }
}

