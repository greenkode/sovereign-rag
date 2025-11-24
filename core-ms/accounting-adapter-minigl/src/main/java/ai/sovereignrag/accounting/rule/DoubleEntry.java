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
import ai.sovereignrag.accounting.entity.RuleInfoEntity;
import ai.sovereignrag.commons.enumeration.ResponseCode;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

/**
 * Verify that debits equals credits.
 *
 * This ai.sovereignrag.minigl.rule check the following conditions:
 * <ul>
 *  <li>This {@link GLTransactionEntity} has two or more {@link GLEntryEntity}s.</li>
 *  <li>Debits equals Credits (in the reporting currency).</li>
 * </ul>
 * @author <a href="mailto:apr@jpos.org">Alejandro Revilla</a>
 * @see JournalRule
 * @see RuleInfoEntity
 */
public class DoubleEntry implements JournalRule {
    private static final BigDecimal ZERO = new BigDecimal ("0.00");

    public void check (GLSession session, GLTransactionEntity txn,
                       String param, GLAccountEntity account, int[] entryOffsets, short[] layers)
        throws GLException
    {
        for (int i=0; i<layers.length; i++) 
            checkEntries (txn, layers[i]);
    }

    private void checkEntries (GLTransactionEntity txn, short layer) 
        throws GLException
    {
        List list = txn.getEntries();
        // if (list.size() < 2) 
        //     throw new ai.sovereignrag.minigl.GLException ("too few entries (" + list.size() + ")");
        BigDecimal debits  = ZERO;
        BigDecimal credits = ZERO;
        Iterator iter = list.iterator();

        while (iter.hasNext()) {
            GLEntryEntity entry = (GLEntryEntity) iter.next();
            if (entry.getLayer() == layer) {
                if (entry.isDebit ())
                    debits = debits.add (entry.getAmount());
                else
                    credits = credits.add (entry.getAmount());
            }
        }
        if (!debits.equals (credits)) {
            throw new GLException (
                "Transaction (" + txn.getDetail() + ") does not balance. debits="+debits.toString() +
                ", credits=" + credits.toString() + " (layer=" + layer + ")", ResponseCode.TRANSACTION_FAILED
            );
        }
    }
}
