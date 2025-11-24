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
import ai.sovereignrag.accounting.entity.GLTransactionEntity;
import org.hibernate.HibernateException;

public interface JournalRule {
    /**
     * @param session current ai.sovereignrag.minigl.GLSession
     * @param txn ai.sovereignrag.minigl.GLTransaction
     * @param param ai.sovereignrag.minigl.rule handback parameter
     * @param account account that triggers this ai.sovereignrag.minigl.rule (may be an ancestor)
     * @param entryOffsets entries that matches this ai.sovereignrag.minigl.rule
     * @throws GLException if ai.sovereignrag.minigl.rule denies transaction
     */
    void check(GLSession session, GLTransactionEntity txn,
               String param, GLAccountEntity account, int[] entryOffsets, short[] layers)
        throws GLException, HibernateException;
}

