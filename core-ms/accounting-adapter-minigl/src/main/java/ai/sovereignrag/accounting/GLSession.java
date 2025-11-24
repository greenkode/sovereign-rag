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

// Replaced ai.sovereignrag.accounting.entity.* with local entity classes

import ai.sovereignrag.accounting.entity.AccountLockEntity;
import ai.sovereignrag.accounting.entity.BalanceCacheEntity;
import ai.sovereignrag.accounting.entity.CheckpointEntity;
import ai.sovereignrag.accounting.entity.CompositeAccountEntity;
import ai.sovereignrag.accounting.entity.CurrencyEntity;
import ai.sovereignrag.accounting.entity.FinalAccountEntity;
import ai.sovereignrag.accounting.entity.GLAccountEntity;
import ai.sovereignrag.accounting.entity.GLEntryEntity;
import ai.sovereignrag.accounting.entity.GLTransactionEntity;
import ai.sovereignrag.accounting.entity.GLTransactionGroupEntity;
import ai.sovereignrag.accounting.entity.JournalEntity;
import ai.sovereignrag.accounting.entity.RuleInfoEntity;
import ai.sovereignrag.accounting.repository.GLAccountRepository;
import ai.sovereignrag.commons.enumeration.ResponseCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import static java.math.BigDecimal.ZERO;

/**
 * MiniGL facility entry point.
 *
 * @author <a href="mailto:apr@jpos.org">Alejandro Revilla</a>
 */
public class GLSession {
    private static final Map<String, Object> ruleCache = new HashMap<>();
    private final EntityManager entityManager;
    private final GLAccountRepository accountRepository;
    public static final short[] LAYER_ZERO = new short[]{0};

    private long SAFE_WINDOW = 1000L;
    private boolean ignoreBalanceCache = false;
    private boolean strictAccountCodes = true;
    private NativeDialect nativeDialect = NativeDialect.ORM;

    private static String POSTGRESQL_GET_BALANCES =
            "SELECT SUM(CASE WHEN entry.credit='N' THEN entry.amount ELSE -entry.amount end) AS balance,\n" +
                    " COUNT(entry.id) AS count\n" +
                    " FROM accounting_transentry AS entry\n";

    private static String MYSQL_GET_BALANCES =
            "SELECT SUM(if(entry.credit='N',entry.amount,-entry.amount)) AS balance,\n" +
                    "  COUNT(entry.id) AS count\n" +
                    "  from transentry as entry\n";

    public GLSession(EntityManager entityManager, GLAccountRepository accountRepository) {
        this.entityManager = entityManager;
        this.accountRepository = accountRepository;
    }

    public GLAccountEntity getChart(String code)
            throws HibernateException, GLException {
        Query q = entityManager.createQuery(
                "from CompositeAccountEntity acct where code=:code and parent is null"
        );
        q.setParameter("code", code);
        Iterator iter = q.getResultList().iterator();
        return (GLAccountEntity) (iter.hasNext() ? iter.next() : null);
    }

    /**
     * @return List of charts of accounts.
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global read permission.
     */
    public List<GLAccountEntity> getCharts()
            throws HibernateException, GLException {
        Query q = entityManager.createQuery(
                "from CompositeAccountEntity acct where parent is null"
        );
        return q.getResultList();
    }

    /**
     * @param chart chart of accounts.
     * @param code  GLAccountEntity's code.
     * @return GLAccountEntity with given code in given chart, or null.
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global READ permission.
     */
    public GLAccountEntity getAccount(GLAccountEntity chart, String code)
            throws HibernateException, GLException {
        Query q = entityManager.createQuery(
                "from GLAccountEntity acct where root=:chart and code=:code"
        );
        q.setParameter("chart", chart.getId());
        q.setParameter("code", code);
        Iterator iter = q.getResultList().iterator();
        return (GLAccountEntity) (iter.hasNext() ? iter.next() : null);
    }

    /**
     * Add GLAccountEntity to parent.
     * Check permissions, parent's type and optional CurrencyEntity.
     *
     * @param parent parent GLAccountEntity
     * @param acct   GLAccountEntity to add
     * @throws HibernateException on error
     * @throws GLException        if user doesn't have permissions, or type mismatch
     */
    public void addAccount(CompositeAccountEntity parent, GLAccountEntity acct)
            throws HibernateException, GLException {
        addAccount(parent, acct, true);
    }

    /**
     * Add GLAccountEntity to parent.
     * Check permissions, parent's type and optional CurrencyEntity.
     *
     * @param parent parent GLAccountEntity
     * @param acct   GLAccountEntity to add
     * @param fast   true if we want a fast add that do not eagerly load all children
     * @throws HibernateException on error
     * @throws GLException        if user doesn't have permissions, type mismatch or Duplicate Code
     */
    @SuppressWarnings("unchecked")
    public void addAccount(CompositeAccountEntity parent, GLAccountEntity acct, boolean fast)
            throws HibernateException, GLException {
        if (strictAccountCodes)
            validateAccountCode(parent, acct);

        if (!parent.isChart() && !parent.equalsType(acct)) {
            StringBuffer sb = new StringBuffer("Type mismatch ");
            sb.append(parent.getTypeAsString());
            sb.append('/');
            sb.append(acct.getTypeAsString());
            throw new GLException(sb.toString(), ResponseCode.INVALID_REQUEST);
        }
        String currencyCode = parent.getCurrencyCode();
        if (currencyCode != null
                && !currencyCode.equals(acct.getCurrencyCode())) {
            StringBuffer sb = new StringBuffer("ai.sovereignrag.accounting.entity.CurrencyEntity mismatch ");
            sb.append(currencyCode);
            sb.append('/');
            sb.append(acct.getCurrencyCode());
            throw new GLException(sb.toString(), ResponseCode.INVALID_REQUEST);
        }
        acct.setRoot(parent.getRoot());
        acct.setParent(parent);
        try {
            accountRepository.save(acct);
        } catch (ConstraintViolationException e) {
            e.fillInStackTrace();
            throw new GLException("Duplicate code", e, ResponseCode.INVALID_REQUEST);
        }
        if (!fast)
            parent.getChildren().add(acct);
    }

    /**
     * Add a chart of accounts.
     * Check permissions.
     *
     * @param acct chart to add
     * @throws HibernateException on error
     * @throws GLException        if user doesn't have write permission
     */
    public void addChart(GLAccountEntity acct)
            throws HibernateException, GLException {
        entityManager.persist(acct);
    }

    /**
     * Add a ai.sovereignrag.accounting.entity.JournalEntity
     * Check permissions.
     *
     * @param j The new ai.sovereignrag.accounting.entity.JournalEntity
     * @throws HibernateException on error
     * @throws GLException        if user doesn't have write permission
     */
    public void addJournal(JournalEntity j) throws HibernateException, GLException {
        entityManager.persist(j);
    }

    /**
     * @param chart chart of accounts.
     * @param code  GLAccountEntity's code.
     * @return final GLAccountEntity with given code in given chart, or null.
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global READ permission.
     */
    public FinalAccountEntity getFinalAccount(GLAccountEntity chart, String code)
            throws HibernateException, GLException {
        Query q = entityManager.createQuery(
                "from FinalAccountEntity acct where root=:chart and code=:code"
        );
        q.setParameter("chart", chart.getId());
        q.setParameter("code", code);
        Iterator iter = q.getResultList().iterator();
        return (FinalAccountEntity) (iter.hasNext() ? iter.next() : null);
    }

    /**
     * @param chart chart of accounts.
     * @return list of final accounts
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global READ permission.
     */
    public List<FinalAccountEntity> getFinalAccounts(GLAccountEntity chart)
            throws HibernateException, GLException {
        Query q = entityManager.createQuery(
                "from FinalAccountEntity acct where root=:chart"
        );
        q.setParameter("chart", chart.getId());
        return (List<FinalAccountEntity>) q.getResultList();
    }

    /**
     * @param parent parent GLAccountEntity.
     * @return list of composite accounts children of the parent GLAccountEntity
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global READ permission.
     */
    public List<CompositeAccountEntity> getCompositeChildren(GLAccountEntity parent) throws HibernateException, GLException {
        Query q = entityManager.createQuery(
                "from CompositeAccountEntity acct where parent=:parent"
        );
        q.setParameter("parent", parent);
        return (List<CompositeAccountEntity>) q.getResultList();
    }

    /**
     * @param parent parent GLAccountEntity.
     * @return list of final accounts children of the parent GLAccountEntity
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global READ permission.
     */
    public List<FinalAccountEntity> getFinalChildren(GLAccountEntity parent) throws HibernateException, GLException {

        Query q = entityManager.createQuery(
                "from FinalAccountEntity acct where parent=:parent"
        );
        q.setParameter("parent", parent);
        return (List<FinalAccountEntity>) q.getResultList();
    }

    /**
     * @param chart chart of accounts.
     * @return list of all accounts
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global READ permission.
     */
    public List<GLAccountEntity> getAllAccounts(GLAccountEntity chart)
            throws HibernateException, GLException {
        Query q = entityManager.createQuery(
                "from GLAccountEntity acct where root=:chart"
        );
        q.setParameter("chart", chart.getId());
        return (List<GLAccountEntity>) q.getResultList();
    }

    /**
     * @param chart chart of accounts.
     * @param code  GLAccountEntity's code.
     * @return composite GLAccountEntity with given code in given chart, or null.
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global READ permission.
     */
    public CompositeAccountEntity getCompositeAccount(GLAccountEntity chart, String code)
            throws HibernateException, GLException {
        Query q = entityManager.createQuery(
                "from CompositeAccountEntity acct where root=:chart and code=:code"
        );
        q.setParameter("chart", chart.getId());
        q.setParameter("code", code);
        Iterator iter = q.getResultList().iterator();
        return (CompositeAccountEntity) (iter.hasNext() ? iter.next() : null);
    }

    /**
     * @param chartName chart of GLAccountEntity's code.
     * @param code      GLAccountEntity's code.
     * @return GLAccountEntity with given code in given chart, or null.
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global READ permission.
     */
    public GLAccountEntity getAccount(String chartName, String code)
            throws HibernateException, GLException {
        GLAccountEntity chart = getChart(chartName);
        if (chart == null)
            throw new GLException("Chart '" + chartName + "' does not exist", ResponseCode.INVALID_ACCOUNT);
        return getAccount(chart, code);
    }

    /**
     * @param chartName chart of GLAccountEntity's code.
     * @param code      GLAccountEntity's code.
     * @return final GLAccountEntity with given code in given chart, or null.
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global READ permission.
     */
    public FinalAccountEntity getFinalAccount(String chartName, String code)
            throws HibernateException, GLException {
        GLAccountEntity chart = getChart(chartName);
        if (chart == null)
            throw new GLException("Chart '" + chartName + "' does not exist", ResponseCode.INVALID_ACCOUNT);
        return getFinalAccount(chart, code);
    }

    /**
     * @param chartName chart of GLAccountEntity's code.
     * @param code      GLAccountEntity's code.
     * @return composite GLAccountEntity with given code in given chart, or null.
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global READ permission.
     */
    public CompositeAccountEntity getCompositeAccount(String chartName, String code)
            throws HibernateException, GLException {
        GLAccountEntity chart = getChart(chartName);
        if (chart == null)
            throw new GLException("Chart '" + chartName + "' does not exist", ResponseCode.INVALID_ACCOUNT);
        return getCompositeAccount(chart, code);
    }

    /**
     * @param name JournalEntity's name.
     * @return JournalEntity or null.
     * @throws GLException        if users doesn't have global READ permission.
     * @throws HibernateException on database errors.
     */
    public JournalEntity getJournal(String name)
            throws HibernateException, GLException {
        Query q = entityManager.createQuery(
                "from JournalEntity j where name=:name"
        );
        q.setParameter("name", name);
        Iterator iter = q.getResultList().iterator();
        JournalEntity j = iter.hasNext() ? (JournalEntity) iter.next() : null;
        if (j == null)
            throw new GLException("ai.sovereignrag.accounting.JournalEntity '" + name + "' does not exist", ResponseCode.INVALID_ACCOUNT);
        return j;
    }

    /**
     * @return list of all journals
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global READ permission.
     */
    public List<JournalEntity> getAllJournals()
            throws HibernateException, GLException {
        Query q = entityManager.createQuery(
                "from JournalEntity j order by chart"
        );
        return (List<JournalEntity>) q.getResultList();
    }

    /**
     * @return list of all CurrencyEntity ids
     * @see CurrencyEntity
     */
    public List<String> getCurrencyCodes() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<CurrencyEntity> root = query.from(CurrencyEntity.class);
        query.select(root.get("id"));
        TypedQuery<String> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    /**
     * Post transaction in a given JournalEntity.
     *
     * @param JournalEntity the JournalEntity.
     * @param txn           the transaction.
     * @throws GLException        if user doesn't have POST permission or any ai.sovereignrag.accounting.rule associated with this JournalEntity and/or GLAccountEntity raises a ai.sovereignrag.accounting.GLException.
     * @throws HibernateException on database errors.
     * @see JournalRule
     */
    public void post(JournalEntity JournalEntity, GLTransactionEntity txn)
            throws HibernateException, GLException {
        txn.setJournal(JournalEntity);
        txn.setTimestamp(Instant.now());
        if (txn.getPostDate() == null)
            txn.setPostDate(Util.floor(txn.getTimestamp()));
        else
            invalidateCheckpoints(txn);
        Collection rules = getRules(txn);
        // dumpRules (rules);
        applyRules(txn, rules);
        entityManager.persist(txn);
    }

    /**
     * Moves a transaction to a new JournalEntity
     *
     * @param txn           the Transaction
     * @param JournalEntity the New ai.sovereignrag.accounting.entity.JournalEntity
     * @throws GLException        if user doesn't have POST permission on the old and new journals.
     * @throws HibernateException on database errors.
     */
    public void move(GLTransactionEntity txn, JournalEntity JournalEntity)
            throws GLException, HibernateException {
        invalidateCheckpoints(txn);    // invalidate in old JournalEntity
        txn.setJournal(JournalEntity);
        invalidateCheckpoints(txn);    // invalidate in new JournalEntity
        applyRules(txn, getRules(txn));
        entityManager.merge(txn);
    }

    /**
     * Summarize transactions in a JournalEntity.
     *
     * @param JournalEntity the JournalEntity.
     * @param start         date (inclusive).
     * @param end           date (inclusive).
     * @param description   summary transaction's description
     * @return ai.sovereignrag.accounting.entity.GLTransactionEntity a summary transaction
     * @throws GLException        if user doesn't have read permission on this JournalEntity.
     * @throws HibernateException on database/mapping errors
     */
    public GLTransactionEntity summarize
    (JournalEntity JournalEntity, Instant start, Instant end, String description, short[] layers)
            throws HibernateException, GLException {
        Instant startFloored = Util.floor(start);
        Instant endCeiled = Util.ceil(end);

        if (endCeiled.compareTo(startFloored) < 0) {
            throw new GLException("Invalid date range "
                    + Util.dateToString(startFloored) + ":" + Util.dateToString(endCeiled), ResponseCode.TRANSACTION_NOT_PERMITTED);
        }
        var lockDate = JournalEntity.getLockDate();
        if (lockDate != null && !LocalDate.ofInstant(startFloored, ZoneId.systemDefault()).isAfter(lockDate)) {
            throw new GLException
                    ("ai.sovereignrag.accounting.JournalEntity is locked at " + lockDate, ResponseCode.TRANSACTION_NOT_PERMITTED);
        }
        setLockDate(JournalEntity, LocalDate.ofInstant(endCeiled, ZoneId.systemDefault()));

        GLTransactionEntity txn = new GLTransactionEntity(description);
        for (int i = 0; i < layers.length; i++) {
            Iterator debits = findSummarizedGLEntries(JournalEntity, startFloored, endCeiled, false, layers[i]);
            Iterator credits = findSummarizedGLEntries(JournalEntity, startFloored, endCeiled, true, layers[i]);
            while (debits.hasNext()) {
                Object[] obj = (Object[]) debits.next();
                txn.createDebit(
                        (FinalAccountEntity) obj[0],
                        (BigDecimal) obj[1],
                        null, layers[i]
                );
            }
            while (credits.hasNext()) {
                Object[] obj = (Object[]) credits.next();
                txn.createCredit(
                        (FinalAccountEntity) obj[0],
                        (BigDecimal) obj[1],
                        null, layers[i]
                );
            }
        }
        txn.setJournal(JournalEntity);
        txn.setTimestamp(Instant.now());
        txn.setPostDate(endCeiled);
        deleteGLTransactions(JournalEntity, startFloored, endCeiled);
        entityManager.persist(txn); // force post - no ai.sovereignrag.accounting.rule validations
        JournalEntity.setLockDate(null);
        return txn;
    }

    /**
     * @param JournalEntity the JournalEntity.
     * @param id            txn id
     * @return ai.sovereignrag.accounting.GLTransactionEntity or null
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public GLTransactionEntity getTransaction(JournalEntity JournalEntity, long id)
            throws HibernateException, GLException {
        GLTransactionEntity txn = null;
        try {
            txn = entityManager.getReference(GLTransactionEntity.class, Long.valueOf(id));
            if (!txn.getJournal().equals(JournalEntity))
                throw new GLException(
                        "The transaction does not belong to the specified JournalEntity", ResponseCode.TRANSACTION_NOT_FOUND
                );
        } catch (ObjectNotFoundException e) {
            // okay to happen
        }
        return txn;
    }

    /**
     * @param JournalEntity  the JournalEntity.
     * @param start          date (inclusive).
     * @param end            date (inclusive).
     * @param searchString   optional search string
     * @param findByPostDate true to find by postDate, false to find by timestamp
     * @param pageNumber     the page number
     * @param pageSize       the page size
     * @return list of transactions
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public TypedQuery<GLTransactionEntity> createFindTransactionsCriteria
    (JournalEntity JournalEntity, Instant start, Instant end, String searchString,
     boolean findByPostDate, int pageNumber, int pageSize)
            throws HibernateException, GLException {
        int firstResult = 0;
        if (pageSize > 0 && pageNumber > 0)
            firstResult = pageSize * (pageNumber - 1);

        return createFindTransactionsCriteriaByRange(
                JournalEntity, start, end, searchString, findByPostDate, firstResult, pageSize
        );
    }

    /**
     * @param JournalEntity  the JournalEntity.
     * @param start          date (inclusive).
     * @param end            date (inclusive).
     * @param searchString   optional search string
     * @param findByPostDate true to find by postDate, false to find by timestamp
     * @param firstResult    the first result
     * @param pageSize       the page size
     * @return list of transactions
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public TypedQuery<GLTransactionEntity> createFindTransactionsCriteriaByRange
    (JournalEntity JournalEntity, Instant start, Instant end, String searchString,
     boolean findByPostDate, int firstResult, int pageSize)
            throws HibernateException, GLException {
        String dateField = findByPostDate ? "postDate" : "timestamp";
        Instant startAdjusted = start;
        Instant endAdjusted = end;
        if (findByPostDate) {
            if (start != null)
                startAdjusted = Util.floor(start);
            if (end != null)
                endAdjusted = Util.ceil(end);
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<GLTransactionEntity> query = cb.createQuery(GLTransactionEntity.class);
        Root<GLTransactionEntity> root = query.from(GLTransactionEntity.class);

        Predicate journalPred = cb.equal(root.get("journal"), JournalEntity);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(journalPred);

        if (startAdjusted != null && startAdjusted.equals(endAdjusted)) {
            predicates.add(cb.equal(root.get(dateField), startAdjusted));
        } else {
            if (startAdjusted != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get(dateField), startAdjusted));
            if (endAdjusted != null)
                predicates.add(cb.lessThanOrEqualTo(root.get(dateField), endAdjusted));
        }
        if (searchString != null) {
            predicates.add(cb.like(root.get("detail"), "%" + searchString + "%"));
        }

        query.where(predicates.toArray(new Predicate[0]));
        TypedQuery<GLTransactionEntity> typedQuery = entityManager.createQuery(query);

        if (pageSize > 0 && firstResult > 0) {
            typedQuery.setMaxResults(pageSize);
            typedQuery.setFirstResult(firstResult);
        }

        return typedQuery;
    }

    /**
     * @param JournalEntity  the JournalEntity.
     * @param start          date (inclusive).
     * @param end            date (inclusive).
     * @param searchString   optional search string
     * @param findByPostDate true to find by postDate, false to find by timestamp
     * @param pageNumber     the page number
     * @param pageSize       the page size
     * @return list of transactions
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public List findTransactions
    (JournalEntity JournalEntity, Instant start, Instant end, String searchString,
     boolean findByPostDate, int pageNumber, int pageSize)
            throws HibernateException, GLException {
        return createFindTransactionsCriteria
                (JournalEntity, start, end, searchString, findByPostDate, pageNumber, pageSize).getResultList();
    }

    /**
     * @param JournalEntity  the JournalEntity.
     * @param start          date (inclusive).
     * @param end            date (inclusive).
     * @param searchString   optional search string
     * @param findByPostDate true to find by postDate, false to find by timestamp
     * @return list of transactions
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public List findTransactions
    (JournalEntity JournalEntity, Instant start, Instant end, String searchString, boolean findByPostDate)
            throws HibernateException, GLException {
        return findTransactions(JournalEntity, start, end, searchString, findByPostDate, 0, 0);
    }

    /**
     * @param JournalEntity  the JournalEntity.
     * @param start          date (inclusive).
     * @param end            date (inclusive).
     * @param searchString   optional search string
     * @param findByPostDate true to find by postDate, false to find by timestamp
     * @return list of transactions' ids
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public List findTransactionsIds
    (JournalEntity JournalEntity, Instant start, Instant end, String searchString,
     boolean findByPostDate, int pageNumber, int pageSize)
            throws HibernateException, GLException {
        String dateField = findByPostDate ? "postDate" : "timestamp";
        Instant startAdjusted = start;
        Instant endAdjusted = end;
        if (findByPostDate) {
            if (start != null)
                startAdjusted = Util.floor(start);
            if (end != null)
                endAdjusted = Util.ceil(end);
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<GLTransactionEntity> root = query.from(GLTransactionEntity.class);

        query.select(root.get("id"));

        Predicate journalPred = cb.equal(root.get("journal"), JournalEntity);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(journalPred);

        if (startAdjusted != null && startAdjusted.equals(endAdjusted)) {
            predicates.add(cb.equal(root.get(dateField), startAdjusted));
        } else {
            if (startAdjusted != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get(dateField), startAdjusted));
            if (endAdjusted != null)
                predicates.add(cb.lessThanOrEqualTo(root.get(dateField), endAdjusted));
        }
        if (searchString != null) {
            predicates.add(cb.like(root.get("detail"), "%" + searchString + "%"));
        }

        query.where(predicates.toArray(new Predicate[0]));
        TypedQuery<Long> typedQuery = entityManager.createQuery(query);

        if (pageSize > 0 && pageNumber > 0) {
            typedQuery.setMaxResults(pageSize);
            typedQuery.setFirstResult(pageSize * (pageNumber - 1));
        }
        return typedQuery.getResultList();
    }

    /**
     * @param JournalEntity  the JournalEntity.
     * @param start          date (inclusive).
     * @param end            date (inclusive).
     * @param searchString   optional search string
     * @param findByPostDate true to find by postDate, false to find by timestamp
     * @return number of transactions
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public Long findTransactionsRowCount
    (JournalEntity JournalEntity, Instant start, Instant end, String searchString, boolean findByPostDate)
            throws HibernateException, GLException {
        String dateField = findByPostDate ? "postDate" : "timestamp";
        Instant startAdjusted = start;
        Instant endAdjusted = end;
        if (findByPostDate) {
            if (start != null)
                startAdjusted = Util.floor(start);
            if (end != null)
                endAdjusted = Util.ceil(end);
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<GLTransactionEntity> root = query.from(GLTransactionEntity.class);

        query.select(cb.count(root));

        Predicate journalPred = cb.equal(root.get("journal"), JournalEntity);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(journalPred);

        if (startAdjusted != null && startAdjusted.equals(endAdjusted)) {
            predicates.add(cb.equal(root.get(dateField), startAdjusted));
        } else {
            if (startAdjusted != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get(dateField), startAdjusted));
            if (endAdjusted != null)
                predicates.add(cb.lessThanOrEqualTo(root.get(dateField), endAdjusted));
        }
        if (searchString != null) {
            predicates.add(cb.like(root.get("detail"), "%" + searchString + "%"));
        }

        query.where(predicates.toArray(new Predicate[0]));
        TypedQuery<Long> typedQuery = entityManager.createQuery(query);

        return typedQuery.getSingleResult();
    }

    /**
     * Current Balance for GLAccountEntity in a given JournalEntity.
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @return current balance.
     * @throws GLException if user doesn't have READ permission on this jounral.
     */
    public BigDecimal getBalance(JournalEntity JournalEntity, GLAccountEntity acct)
            throws HibernateException, GLException {
        return getBalances(JournalEntity, acct, null, true)[0];
    }

    /**
     * Current Balance for GLAccountEntity in a given JournalEntity.
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param layer         the layers.
     * @return current balance.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public BigDecimal getBalance(JournalEntity JournalEntity, GLAccountEntity acct, short layer)
            throws HibernateException, GLException {
        return getBalances(JournalEntity, acct, null, true, new short[]{layer}, 0L)[0];
    }

    /**
     * Current Balance for GLAccountEntity in a given JournalEntity for a given set of layers.
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param layers        the layers.
     * @return current balance.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public BigDecimal getBalance(JournalEntity JournalEntity, GLAccountEntity acct, short[] layers)
            throws HibernateException, GLException {
        return getBalances(JournalEntity, acct, null, true, layers, 0L)[0];
    }

    /**
     * Minimum Balance for GLAccountEntity in a given JournalEntity for a given set of layers
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param layers        set of layers
     * @return minimum balance among given layer sets
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public BigDecimal getMinBalance(JournalEntity JournalEntity, GLAccountEntity acct, short[]... layers)
            throws HibernateException, GLException {
        BigDecimal minBalance = null;
        for (short[] layer : layers) {
            BigDecimal bd = getBalance(JournalEntity, acct, layer);
            if (minBalance == null || bd.compareTo(minBalance) < 0)
                minBalance = bd;
        }
        return minBalance == null ? ZERO : minBalance;
    }

    /**
     * Maximum Balance for GLAccountEntity in a given JournalEntity for a given set of layers
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param layers        set of layers
     * @return maximum balance among given layer sets
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public BigDecimal getMaxBalance(JournalEntity JournalEntity, GLAccountEntity acct, short[]... layers)
            throws HibernateException, GLException {
        BigDecimal maxBalance = null;
        for (short[] layer : layers) {
            BigDecimal bd = getBalance(JournalEntity, acct, layer);
            if (maxBalance == null || bd.compareTo(maxBalance) > 0)
                maxBalance = bd;
        }
        return maxBalance == null ? ZERO : maxBalance;
    }

    /**
     * Current Balance for GLAccountEntity in a given JournalEntity.
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param layers        comma separated list of layers
     * @return current balance.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public BigDecimal getBalance(JournalEntity JournalEntity, GLAccountEntity acct, String layers)
            throws HibernateException, GLException {
        return getBalances(JournalEntity, acct, null, true, toLayers(layers), 0L)[0];
    }

    /**
     * Balance for GLAccountEntity in a given JournalEntity in a given date.
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param date          date (inclusive).
     * @return balance at given date.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public BigDecimal getBalance(JournalEntity JournalEntity, GLAccountEntity acct, Instant date)
            throws HibernateException, GLException {
        return getBalances(JournalEntity, acct, date, true)[0];
    }

    /**
     * Balance for GLAccountEntity in a given JournalEntity in a given date.
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param date          date (inclusive).
     * @param layer         layer
     * @return balance at given date.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public BigDecimal getBalance(JournalEntity JournalEntity, GLAccountEntity acct, Instant date, short layer)
            throws HibernateException, GLException {
        return getBalances(JournalEntity, acct, date, true, new short[]{layer}, 0L)[0];
    }

    /**
     * Balance for GLAccountEntity in a given JournalEntity in a given date.
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param date          date (inclusive).
     * @param layers        layers
     * @return balance at given date.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public BigDecimal getBalance(JournalEntity JournalEntity, GLAccountEntity acct, Instant date, short[] layers)
            throws HibernateException, GLException {
        return getBalances(JournalEntity, acct, date, true, layers, 0L)[0];
    }

    /**
     * Balance for GLAccountEntity in a given JournalEntity in a given date.
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param date          date (inclusive).
     * @param layers        comma separated list of layers
     * @return balance at given date.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public BigDecimal getBalance(JournalEntity JournalEntity, GLAccountEntity acct, Instant date, String layers)
            throws HibernateException, GLException {
        return getBalances(JournalEntity, acct, date, true, toLayers(layers), 0L)[0];
    }

    /**
     * Get Both Balances at given date
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param date          date (inclusive).
     * @param inclusive     either true or false
     * @return array of 2 BigDecimals with balance and entry count.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public BigDecimal[] getBalances
    (JournalEntity JournalEntity, GLAccountEntity acct, Instant date, boolean inclusive)
            throws HibernateException, GLException {
        return getBalances(JournalEntity, acct, date, inclusive, LAYER_ZERO, 0L);
    }


    /**
     * Get Both Balances at given date
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param date          date (inclusive).
     * @param inclusive     either true or false
     * @param layers        the layers
     * @param maxId         maximum ai.sovereignrag.accounting.GLEntryEntity ID to be considered in the query (if greater than zero)
     * @return array of 2 BigDecimals with balance and entry count.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public BigDecimal[] getBalancesORM
    (JournalEntity JournalEntity, GLAccountEntity acct, Instant date, boolean inclusive, short[] layers, long maxId)
            throws HibernateException, GLException {
        BigDecimal balance[] = {ZERO, ZERO};
        short[] layersCopy = Arrays.copyOf(layers, layers.length);
        if (acct.getChildren() != null) {
            if (acct.isChart()) {
                return getChartBalances
                        (JournalEntity, (CompositeAccountEntity) acct, date, inclusive, layersCopy, maxId);
            }
            Iterator iter = acct.getChildren().iterator();
            while (iter.hasNext()) {
                GLAccountEntity a = (GLAccountEntity) iter.next();
                BigDecimal[] b = getBalancesORM(JournalEntity, a, date, inclusive, layersCopy, maxId);
                balance[0] = balance[0].add(b[0]);
                // session.evict (a); FIXME this conflicts with r251 (cascade=evict generating a failed to lazily initialize a collection
            }
        } else if (acct.isFinalAccount()) {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<GLEntryEntity> query = cb.createQuery(GLEntryEntity.class);
            Root<GLEntryEntity> entryRoot = query.from(GLEntryEntity.class);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(entryRoot.get("account"), acct));
            predicates.add(entryRoot.get("layer").in((Object[]) toShortArray(layersCopy)));

            if (maxId > 0L) {
                predicates.add(cb.lessThanOrEqualTo(entryRoot.get("id"), maxId));
            }

            // Join with transaction
            predicates.add(cb.equal(entryRoot.get("transaction").get("journal"), JournalEntity));

            if (date != null) {
                if (inclusive) {
                    predicates.add(cb.lessThan(entryRoot.get("transaction").get("postDate"), Util.tomorrow(date)));
                } else {
                    Instant dateFloored = Util.floor(date);
                    predicates.add(cb.lessThan(entryRoot.get("transaction").get("postDate"), dateFloored));
                }
                CheckpointEntity chkp = getRecentCheckpoint(JournalEntity, acct, date, inclusive, layersCopy);
                if (chkp != null) {
                    balance[0] = chkp.getBalance();
                    predicates.add(cb.greaterThanOrEqualTo(entryRoot.get("transaction").get("postDate"), chkp.getDate()));
                }
            } else if (!ignoreBalanceCache) {
                BalanceCacheEntity bcache = getBalanceCache(JournalEntity, acct, layersCopy);
                if (bcache != null && (maxId == 0 || bcache.getRef() <= maxId)) {
                    balance[0] = bcache.getBalance();
                    predicates.add(cb.greaterThan(entryRoot.get("id"), bcache.getRef()));
                }
            }

            query.where(predicates.toArray(new Predicate[0]));
            TypedQuery<GLEntryEntity> typedQuery = entityManager.createQuery(query);
            List<GLEntryEntity> l = typedQuery.getResultList();
            balance[0] = applyEntries(balance[0], l);
            balance[1] = new BigDecimal(l.size()); // hint for CheckpointEntity
        }
        return balance;
    }

    /**
     * Get Both Balances at given date.
     * <p>
     * IMPORTANT NOTE: This function uses different implementations depending on the
     * dialect of the SQL server in use.  By default, native queries are generated for
     * the MySQL and PostgreSQL dialect, with other dialects using getBalancesORM instead.
     * <p>
     * Regarding balances of composite accounts - getBalancesORM knows that a given GLAccountEntity
     * is a child of a given parent correctly using the acct.parent reference, whereas the
     * native queries in getBalances cut some corners in order to take advantage of the
     * database index, it assumes that the parent shares the acct.code prefix.
     * <p>
     * In such cases, if MySQL or PostgreSQL native queries are used for balance calculations,
     * accounts not following this convention are excluded from the results resulting in the
     * wrong balance.  This was the reason for adding stricter checks of GLAccountEntity codes to
     * Import.createCharts and ai.sovereignrag.accounting.GLSession.addAccount.
     * <p>
     * It's possible to force the use of getBalancesORM by calling `ai.sovereignrag.accounting.GLSession.forceDialect`
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param date          date (inclusive).
     * @param inclusive     either true or false
     * @param layers        the layers
     * @param maxId         maximum ai.sovereignrag.accounting.GLEntryEntity ID to be considered in the query (if greater than zero)
     * @return array of 2 BigDecimals with balance and entry count.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public BigDecimal[] getBalances
    (JournalEntity JournalEntity, GLAccountEntity acct, Instant date, boolean inclusive, short[] layers, long maxId)
            throws HibernateException, GLException {
        return getBalances0(JournalEntity, acct, date, inclusive, layers, maxId, null);
    }

    private BigDecimal[] getBalances0
            (JournalEntity JournalEntity, GLAccountEntity acct, Instant date, boolean inclusive, short[] layers, long maxId, BalanceCacheEntity bcache)
            throws HibernateException, GLException {
        StringBuilder select;
        switch (nativeDialect) {
            case MYSQL:
                select = new StringBuilder(MYSQL_GET_BALANCES);
                break;
            case POSTGRESQL:
                select = new StringBuilder(POSTGRESQL_GET_BALANCES);
                break;
            default:
                return getBalancesORM(JournalEntity, acct, date, inclusive, layers, maxId);
        }

        BigDecimal balance[] = {ZERO, ZERO};
        select.append(", accounting_transacc as txn\n");

        if (date == null && !ignoreBalanceCache) {
            short[] layersCopy = Arrays.copyOf(layers, layers.length);
            if (bcache == null)
                bcache = getBalanceCache(JournalEntity, acct, layersCopy);
            if (maxId > 0 && bcache != null && bcache.getRef() > maxId)
                bcache = null; // ignore bcache 'in the future'
        }
        if (!acct.isFinalAccount()) {
            select.append(", accounting_acct as acct");
        }
        StringBuffer qs = new StringBuffer();
        if (maxId > 0L) {
            where(qs, "entry.id <= :maxId");
        }
        if (bcache != null) {
            where(qs, "entry.id > :bcache_ref");
        }
        if (acct.isFinalAccount()) {
            where(qs, "entry.account = :acctId");
        } else if (acct.isChart()) {
            where(qs, "entry.account = acct.id");
            where(qs, "acct.root = :acctId");
        } else {
            where(qs, "entry.account = acct.id");
            where(qs, "acct.code like :code");
        }
        where(qs, "(entry.transaction = txn.id and txn.journal = :journal)\n");
        if (date != null) {
            where(qs, "txn.postDate < :date");
        }
        where(qs, "entry.layer in");
        qs.append("  (");
        qs.append(layersToString(layers, ','));
        qs.append(')');
        select.append(qs);

        Query q = entityManager.createNativeQuery(select.toString());
        // TODO: Replace with modern result mapping
        // q.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        if (acct.isFinalAccount() || acct.isChart())
            q.setParameter("acctId", acct.getId());
        else {
            q.setParameter("code", acct.getCode() + "%");
        }
        q.setParameter("journal", JournalEntity.getId());
        if (date != null) {
            q.setParameter("date", inclusive ? Util.tomorrow(date) : date);
        }
        if (maxId > 0L)
            q.setParameter("maxId", maxId);
        if (bcache != null)
            q.setParameter("bcache_ref", bcache.getRef());

        List<Map<String, Object>> result = q.getResultList();
        if (result.size() == 1) {
            Map m = result.get(0);
            BigDecimal bd = (BigDecimal) m.get("balance");
            if (bd != null) {
                balance[0] = bd;
                balance[1] = new BigDecimal((BigInteger) m.get("count"));
                if (acct.isCredit())
                    balance[0] = balance[0].negate();
            }
        }
        if (bcache != null) {
            balance[0] = balance[0].add(bcache.getBalance());
        }
        return balance;
    }

    public List<FinalAccountEntity> getDeepFinalChildren(GLAccountEntity acct) throws HibernateException, GLException {
        return getDeepFinalChildren0(acct);
    }


    private StringBuffer where(StringBuffer sb, String clausse) {
        sb.append(sb.length() == 0 ? " WHERE " : " AND ");
        sb.append(clausse);
        sb.append('\n');
        return sb;
    }

    private String layersToString(short[] layers, char sep) {
        StringBuffer sb = new StringBuffer();
        Arrays.sort(layers);
        for (int i = 0; i < layers.length; i++) {
            if (i > 0)
                sb.append(sep);
            sb.append(layers[i]);
        }
        return sb.toString();
    }

    private String layersToString(short[] layers) {
        return layersToString(layers, '.');
    }

    /**
     * ai.sovereignrag.accounting.AccountDetail for date range
     *
     * @param JournalEntity  the JournalEntity.
     * @param acct           the GLAccountEntity.
     * @param start          date (inclusive).
     * @param end            date (inclusive).
     * @param layers         array of the layers included.
     * @param ascendingOrder boolean.
     * @param maxResults     int.
     * @return ai.sovereignrag.accounting.GLAccountEntity detail for given period.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public AccountDetail getAccountDetail
    (JournalEntity JournalEntity, GLAccountEntity acct, Instant start, Instant end, short[] layers, boolean ascendingOrder, int maxResults)
            throws HibernateException, GLException {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<GLEntryEntity> query = cb.createQuery(GLEntryEntity.class);
        Root<GLEntryEntity> entryRoot = query.from(GLEntryEntity.class);

        List<Predicate> predicates = new ArrayList<>();

        boolean hasChildren = false;
        if (acct.isCompositeAccount()) {
            List<Long> childIds = getChildren(acct);
            if (!childIds.isEmpty()) {
                hasChildren = true;
                predicates.add(entryRoot.get("account").get("id").in(childIds));
            }
        }
        if (!hasChildren) {
            predicates.add(cb.equal(entryRoot.get("account"), acct));
        }

        predicates.add(entryRoot.get("layer").in((Object[]) toShortArray(layers)));
        predicates.add(cb.equal(entryRoot.get("transaction").get("journal"), JournalEntity));

        Instant startAdjusted = start;
        Instant endAdjusted = end;
        if (start != null || (start == null && ascendingOrder)) {
            startAdjusted = Util.floor(start);
            predicates.add(cb.greaterThanOrEqualTo(entryRoot.get("transaction").get("postDate"), startAdjusted));
        }
        if (end != null || (end == null && ascendingOrder)) {
            endAdjusted = Util.ceil(end);
            predicates.add(cb.lessThanOrEqualTo(entryRoot.get("transaction").get("postDate"), endAdjusted));
        }

        query.where(predicates.toArray(new Predicate[0]));

        long maxEntry = 0L;
        List<GLEntryEntity> entries;
        BigDecimal initialBalance[];
        if (ascendingOrder) {
            query.orderBy(
                    cb.asc(entryRoot.get("transaction").get("postDate")),
                    cb.asc(entryRoot.get("transaction").get("timestamp")),
                    cb.asc(entryRoot.get("id"))
            );
            TypedQuery<GLEntryEntity> typedQuery = entityManager.createQuery(query);
            if (maxResults > 0)
                typedQuery.setMaxResults(maxResults);
            entries = typedQuery.getResultList();
            initialBalance = getBalances(JournalEntity, acct, startAdjusted, false, layers, maxEntry);
        } else {
            query.orderBy(
                    cb.desc(entryRoot.get("transaction").get("postDate")),
                    cb.desc(entryRoot.get("transaction").get("timestamp")),
                    cb.desc(entryRoot.get("id"))
            );
            TypedQuery<GLEntryEntity> typedQuery = entityManager.createQuery(query);
            if (maxResults > 0)
                typedQuery.setMaxResults(maxResults);
            entries = typedQuery.getResultList();
            if (entries.size() > 0) {
                maxEntry = entries.get(0).getId();
            }
            initialBalance = getBalances(JournalEntity, acct, endAdjusted, true, layers, maxEntry);
        }
        return new AccountDetail(JournalEntity, acct, initialBalance[0], startAdjusted, endAdjusted, entries, layers, ascendingOrder);
    }


    /**
     * ai.sovereignrag.accounting.AccountDetail for date range
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param start         date (inclusive).
     * @param end           date (inclusive).
     * @return ai.sovereignrag.accounting.GLAccountEntity detail for given period.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public AccountDetail getAccountDetail
    (JournalEntity JournalEntity, GLAccountEntity acct, Instant start, Instant end, short[] layers)
            throws HibernateException, GLException {
        return getAccountDetail(JournalEntity, acct, start, end, layers, true, 0);
    }

    /**
     * ai.sovereignrag.accounting.AccountDetail for date range
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param layers        layer set
     * @param maxResults    number of entries in mini statement
     * @return ai.sovereignrag.accounting.GLAccountEntity detail for given period.
     * @throws GLException if user doesn't have READ permission on this JournalEntity.
     */
    public AccountDetail getMiniStatement
    (JournalEntity JournalEntity, GLAccountEntity acct, short[] layers, int maxResults)
            throws HibernateException, GLException {
        return getAccountDetail(JournalEntity, acct, null, null, layers, false, maxResults);
    }


    /**
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @param date          date (null for last CheckpointEntity)
     * @param inclusive     either true or false
     * @return Most recent check point for given date.
     * @throws GLException if user doesn't have CheckpointEntity permission on this JournalEntity.
     */
    public CheckpointEntity getRecentCheckpoint
    (JournalEntity JournalEntity, GLAccountEntity acct, Instant date, boolean inclusive, short[] layers)
            throws HibernateException, GLException {

        String qryString = "from CheckpointEntity where journal = :journal and account = :account "
                + ((layers == null) ? "" : "and layers = :layers ")
                + ((date == null) ? "" :
                inclusive ? " and date <= :date " : " and date < :date ")
                + "order by date desc";

        jakarta.persistence.TypedQuery<CheckpointEntity> q = entityManager.createQuery(qryString, CheckpointEntity.class);
        q.setParameter("journal", JournalEntity);
        q.setParameter("account", acct);
        if (layers != null)
            q.setParameter("layers", layersToString(layers));
        if (date != null)
            q.setParameter("date", date);

        q.setMaxResults(1);
        //q.setReadOnly(true);

        List<CheckpointEntity> results = q.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }


    public BalanceCacheEntity getBalanceCache
            (JournalEntity JournalEntity, GLAccountEntity acct, short[] layers)
            throws HibernateException, GLException {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BalanceCacheEntity> query = cb.createQuery(BalanceCacheEntity.class);
        Root<BalanceCacheEntity> root = query.from(BalanceCacheEntity.class);

        Predicate journalPred = cb.equal(root.get("journal"), JournalEntity);
        Predicate accountPred = cb.equal(root.get("account"), acct);
        query.where(journalPred, accountPred);

        if (layers != null) {
            Predicate layersPred = cb.equal(root.get("layers"), layersToString(layers));
            query.where(journalPred, accountPred, layersPred);
        }

        query.orderBy(cb.desc(root.get("ref")));
        TypedQuery<BalanceCacheEntity> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(1);
        List<BalanceCacheEntity> results = typedQuery.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }


    /**
     * @param JournalEntity the ai.sovereignrag.accounting.JournalEntity
     * @param acct          the GLAccountEntity
     * @param date          CheckpointEntity date (inclusive)
     * @param threshold     minimum number of  GLEntries required to create a CheckpointEntity
     * @throws GLException if user doesn't have CheckpointEntity permission on this JournalEntity.
     */
    public void createCheckpoint
    (JournalEntity JournalEntity, GLAccountEntity acct, Instant date, int threshold)
            throws HibernateException, GLException {
        createCheckpoint(JournalEntity, acct, date, threshold, LAYER_ZERO);
    }

    /**
     * @param JournalEntity the ai.sovereignrag.accounting.JournalEntity
     * @param acct          the GLAccountEntity
     * @param date          CheckpointEntity date (inclusive)
     * @param layers        taken into GLAccountEntity in this CheckpointEntity
     * @param threshold     minimum number of  GLEntries required to create a CheckpointEntity
     * @throws GLException if user doesn't have CheckpointEntity permission on this JournalEntity.
     */
    public void createCheckpoint
    (JournalEntity JournalEntity, GLAccountEntity acct, Instant date, int threshold, short[] layers)
            throws HibernateException, GLException {
        if (date == null)
            throw new GLException("Invalid CheckpointEntity date", ResponseCode.TRANSACTION_FAILED);
        // Transaction tx = session.beginTransaction();
        entityManager.lock(JournalEntity, LockModeType.PESSIMISTIC_WRITE);
        createCheckpoint0(JournalEntity, acct, date, threshold, layers);
        // tx.commit();
    }

    public BigDecimal createBalanceCache
            (JournalEntity JournalEntity, GLAccountEntity acct, short[] layers)
            throws HibernateException, GLException {
        return createBalanceCache(JournalEntity, acct, layers, getSafeMaxGLEntryId());
    }

    private BigDecimal createBalanceCache
            (JournalEntity JournalEntity, GLAccountEntity acct, short[] layers, long maxId)
            throws HibernateException, GLException {
        BigDecimal balance;
        BalanceCacheEntity bc = null;
        if (acct.isCompositeAccount()) {
            balance = ZERO;
            Iterator iter = ((CompositeAccountEntity) acct).getChildren().iterator();
            while (iter.hasNext()) {
                GLAccountEntity a = (GLAccountEntity) iter.next();
                balance = balance.add(createBalanceCache(JournalEntity, a, layers, maxId));
            }
        } else if (acct.isFinalAccount())
            bc = createFinalBalanceCache(JournalEntity, (FinalAccountEntity) acct, layers, maxId);

        return getBalances0(JournalEntity, acct, null, true, layers, 0L, bc)[0];
    }

    public BalanceCacheEntity createFinalBalanceCache(JournalEntity JournalEntity, FinalAccountEntity acct, short[] layers) throws GLException {
        return createFinalBalanceCache(JournalEntity, acct, layers, getSafeMaxGLEntryId());
    }

    private BalanceCacheEntity createFinalBalanceCache(JournalEntity JournalEntity, FinalAccountEntity acct, short[] layers, long maxId) throws GLException {
        lock(JournalEntity, acct);
        BalanceCacheEntity c = getBalanceCache(JournalEntity, acct, layers);
        BigDecimal balance = getBalances0(JournalEntity, acct, null, true, layers, maxId, c)[0];
        if (c == null) {
            c = new BalanceCacheEntity();
            c.setJournal(JournalEntity);
            c.setAccount(acct);
            c.setLayers(layersToString(layers));
            c.setBalance(ZERO);     //Ensure we load a balance in the cache
            //if maxId > 0 then the cache includes some entries, and
            // we set the balance in the next if, before saving it.
        }
        if (maxId != c.getRef()) {
            c.setRef(maxId);
            c.setBalance(balance);
            entityManager.merge(c);
        }
        return c;
    }

    /**
     * Lock a JournalEntity.
     *
     * @param JournalEntity the JournalEntity.
     * @throws HibernateException on database errors.
     * @throws GLException        if user doesn't have POST permission on this JournalEntity.
     */
    public void lock(JournalEntity JournalEntity)
            throws HibernateException, GLException {
        entityManager.lock(JournalEntity, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * Lock an GLAccountEntity in a given JournalEntity.
     *
     * @param JournalEntity the JournalEntity.
     * @param acct          the GLAccountEntity.
     * @throws GLException        if user doesn't have POST permission on this JournalEntity.
     * @throws HibernateException on database errors.
     */
    public void lock(JournalEntity JournalEntity, GLAccountEntity acct)
            throws HibernateException, GLException {
        AccountLockEntity lck = getLock(JournalEntity, acct);
    }

    /**
     * Open underlying Hibernate session.
     *
     * @throws HibernateException
     */
    public synchronized Session open() throws HibernateException {
        // TODO: Fix DB.open() to return Session
        // return db.open();
        return entityManager.unwrap(Session.class);
    }

    /**
     * Close underlying EntityManager.
     *
     * @throws HibernateException
     */
    public synchronized void close() throws HibernateException {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }

    /**
     * @return underlying Hibernate Session.
     */
    public Session session() {
        return entityManager.unwrap(Session.class);
    }

    /**
     * @return underlying EntityManager.
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Begin hibernate transaction.
     *
     * @return new Transaction
     */
    public EntityTransaction beginTransaction() throws HibernateException {
        entityManager.getTransaction().begin();
        return entityManager.getTransaction();
    }

    /**
     * Begin hibernate transaction.
     *
     * @param timeout timeout in seconds
     * @return new Transaction
     */
    public EntityTransaction beginTransaction(int timeout) throws HibernateException {
        entityManager.getTransaction().begin();
        EntityTransaction tx = entityManager.getTransaction();
        // Note: EntityTransaction doesn't support timeout like Hibernate Transaction
        // if (timeout > 0)
        //     tx.setTimeout (timeout);
        return tx;
    }

    /**
     * set a JournalEntity's lockDate
     *
     * @param JournalEntity the ai.sovereignrag.accounting.JournalEntity
     * @param lockDate      the lock date.
     * @throws HibernateException on database errors.
     * @throws GLException        if users doesn't have global READ permission.
     */
    public void setLockDate(JournalEntity JournalEntity, LocalDate lockDate)
            throws GLException, HibernateException {
        // Transaction tx = session.beginTransaction();
        entityManager.lock(JournalEntity, LockModeType.PESSIMISTIC_WRITE);
        JournalEntity.setLockDate(lockDate);
        // tx.commit();
    }

    public void deleteBalanceCache
            (JournalEntity JournalEntity, GLAccountEntity GLAccountEntity, short[] layers)
            throws HibernateException {
        StringBuilder sb = new StringBuilder("delete BalanceCacheEntity where journal = :journal");
        if (GLAccountEntity != null)
            sb.append(" and account = :account");
        if (layers != null)
            sb.append(" and layers = :layers");

        Query query = entityManager.createQuery(sb.toString())
                .setParameter("journal", JournalEntity);
        if (GLAccountEntity != null)
            query.setParameter("account", GLAccountEntity);
        if (layers != null)
            query.setParameter("layers", layersToString(layers));

        query.executeUpdate();
    }

    public GLTransactionGroupEntity createGroup(String name, List<GLTransactionEntity> transactions) {
        GLTransactionGroupEntity group = new GLTransactionGroupEntity(name);
        Set txns = new HashSet();
        for (GLTransactionEntity t : transactions) {
            // Ensure transaction is managed before adding to group
            GLTransactionEntity managedTxn = entityManager.contains(t) ? t : entityManager.merge(t);
            txns.add(managedTxn);
        }
        group.setTransactions(txns);
        entityManager.persist(group);
        return group;
    }

    public GLTransactionGroupEntity findTransactionGroup(String name) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<GLTransactionGroupEntity> query = cb.createQuery(GLTransactionGroupEntity.class);
        Root<GLTransactionGroupEntity> root = query.from(GLTransactionGroupEntity.class);

        query.where(cb.equal(root.get("name"), name));
        TypedQuery<GLTransactionGroupEntity> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(1);

        return typedQuery.getResultList().isEmpty() ? null : typedQuery.getResultList().getFirst();
    }

    public BigDecimal getBalance
            (JournalEntity JournalEntity, GLAccountEntity acct, GLTransactionGroupEntity group, short[] layers)
            throws HibernateException, GLException {
        BigDecimal balance = ZERO;
        for (GLTransactionEntity transaction : (Set<GLTransactionEntity>) group.getTransactions()) {
            if (transaction.getJournal().equals(JournalEntity)) {
                for (GLEntryEntity entry : (List<GLEntryEntity>) transaction.getEntries()) {
                    if (acct.equals(entry.getAccount()) && entry.hasLayers(layers)) {
                        if (entry.isIncrease()) {
                            balance = balance.add(entry.getAmount());
                        } else if (entry.isDecrease()) {
                            balance = balance.subtract(entry.getAmount());
                        }
                    }
                }
            }
        }
        return balance;
    }

    public boolean isIgnoreBalanceCache() {
        return ignoreBalanceCache;
    }

    public void setIgnoreBalanceCache(boolean ignoreBalanceCache) {
        this.ignoreBalanceCache = ignoreBalanceCache;
    }

    public boolean isEnforcingStrictAccountCodes() {
        return strictAccountCodes;
    }

    public void setEnforceStrictAccountCodes(boolean strictAccountCodes) {
        this.strictAccountCodes = strictAccountCodes;
    }

    // -----------------------------------------------------------------------
    // PUBLIC HELPERS
    // -----------------------------------------------------------------------
    public short[] toLayers(String layers) {
        StringTokenizer st = new StringTokenizer(layers, ", ");
        short[] sa = new short[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++)
            sa[i] = Short.parseShort(st.nextToken());
        return sa;
    }

    // -----------------------------------------------------------------------
    // PRIVATE METHODS
    // -----------------------------------------------------------------------
    private AccountLockEntity getLock(JournalEntity JournalEntity, GLAccountEntity acct)
            throws HibernateException {
        // Ensure we're working with managed entities
        JournalEntity managedJournal = entityManager.merge(JournalEntity);
        GLAccountEntity managedAccount = entityManager.merge(acct);
        
        AccountLockEntity key = new AccountLockEntity(managedJournal, managedAccount);
        AccountLockEntity.AccountLockId keyId = key.getId();
        AccountLockEntity lck = entityManager.find(AccountLockEntity.class, keyId, LockModeType.PESSIMISTIC_WRITE);
        if (lck == null) {
            // Find and lock the journal entity in the current persistence context
            entityManager.find(JournalEntity.class, managedJournal.getId(), LockModeType.PESSIMISTIC_WRITE);
        }
        lck = entityManager.find(AccountLockEntity.class, keyId, LockModeType.PESSIMISTIC_WRITE); // try again
        if (lck == null) {
            entityManager.persist(lck = key);
            entityManager.flush();
        }
        return lck;
    }

    private void createCheckpoint0
            (JournalEntity JournalEntity, GLAccountEntity acct, Instant date, int threshold, short[] layers)
            throws HibernateException, GLException {
        if (acct.isCompositeAccount()) {
            Iterator iter = ((CompositeAccountEntity) acct).getChildren().iterator();
            while (iter.hasNext()) {
                GLAccountEntity a = (GLAccountEntity) iter.next();
                createCheckpoint0(JournalEntity, a, date, threshold, layers);
            }
        } else if (acct.isFinalAccount()) {
            Instant sod = Util.floor(date);   // sod = start of day
            invalidateCheckpoints(JournalEntity, new GLAccountEntity[]{acct}, sod, sod, layers);
            BigDecimal b[] = getBalances(JournalEntity, acct, sod, false, layers, 0L);
            if (b[1].intValue() >= threshold) {
                CheckpointEntity c = new CheckpointEntity();
                c.setDate(sod);
                c.setBalance(b[0]);
                c.setJournal(JournalEntity);
                c.setAccount(acct);
                c.setLayers(layersToString(layers));
                entityManager.persist(c);
            }
        }
    }

    private GLAccountEntity[] getAccounts(GLTransactionEntity txn) {
        List list = txn.getEntries();
        GLAccountEntity[] accounts = new GLAccountEntity[list.size()];
        Iterator iter = list.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            GLEntryEntity entry = (GLEntryEntity) iter.next();
            accounts[i] = entry.getAccount();
        }
        return accounts;
    }

    private List getAccountHierarchyIds(GLAccountEntity acct)
            throws GLException {
        if (acct == null)
            throw new GLException("Invalid entry - GLAccountEntity is null", ResponseCode.INVALID_ACCOUNT);
        GLAccountEntity p = acct;
        List<Long> l = new ArrayList<Long>();
        while (p != null) {
            l.add(p.getId());
            p = p.getParent();
        }
        return l;
    }

    private void invalidateCheckpoints(GLTransactionEntity txn)
            throws HibernateException {
        GLAccountEntity[] accounts = getAccounts(txn);
        invalidateCheckpoints(
                txn.getJournal(), accounts, txn.getPostDate(), null, null
        );
    }

    private void invalidateCheckpoints
            (JournalEntity JournalEntity, GLAccountEntity[] accounts, Instant start, Instant end, short[] layers)
            throws HibernateException {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CheckpointEntity> query = cb.createQuery(CheckpointEntity.class);
        Root<CheckpointEntity> root = query.from(CheckpointEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("journal"), JournalEntity));

        if (accounts.length > 0) {
            predicates.add(root.get("account").in((Object[]) accounts));
        }

        if (layers != null) {
            predicates.add(cb.equal(root.get("layers"), layersToString(layers)));
        }

        if (start.equals(end)) {
            predicates.add(cb.equal(root.get("date"), start));
        } else {
            predicates.add(cb.greaterThanOrEqualTo(root.get("date"), start));
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), end));
            }
        }

        query.where(predicates.toArray(new Predicate[0]));
        TypedQuery<CheckpointEntity> typedQuery = entityManager.createQuery(query);
        Iterator<CheckpointEntity> iter = typedQuery.getResultList().iterator();
        while (iter.hasNext()) {
            CheckpointEntity cp = (CheckpointEntity) iter.next();
            entityManager.remove(cp);
        }
    }

    private BigDecimal applyEntries(BigDecimal balance, List entries)
            throws GLException {
        Iterator iter = entries.iterator();
        while (iter.hasNext()) {
            GLEntryEntity entry = (GLEntryEntity) iter.next();
            if (entry.isIncrease()) {
                balance = balance.add(entry.getAmount());
            } else if (entry.isDecrease()) {
                balance = balance.subtract(entry.getAmount());
            } else {
                throw new GLException(
                        entry.toString() + " has invalid GLAccountEntity type", ResponseCode.TRANSACTION_FAILED
                );
            }
        }
        return balance;
    }

    private Object getRuleImpl(String clazz) throws GLException {
        Object impl = ruleCache.get(clazz);
        if (impl == null) {
            synchronized (ruleCache) {
                impl = ruleCache.get(clazz);
                if (impl == null) {
                    try {
                        Class cls = Class.forName(clazz);
                        impl = cls.newInstance();
                        ruleCache.put(clazz, impl);
                    } catch (Exception e) {
                        throw new GLException("Invalid ai.sovereignrag.accounting.rule " + clazz, e, ResponseCode.TRANSACTION_FAILED);
                    }
                }
            }
        }
        return impl;
    }

    private void addRules
            (Map<String, Object> ruleMap, JournalEntity JournalEntity, List acctHierarchy, int offset)
            throws HibernateException {
        Query q = entityManager.createQuery(
                "from RuleInfoEntity where journal=:journal and account.id in (:accts) order by id"
        );
        q.setParameter("journal", JournalEntity);
        q.setParameter("accts", acctHierarchy);
        // Note: Caching methods are Hibernate-specific, not available in Jakarta Persistence
        // q.setCacheable (true);
        // q.setCacheRegion ("rules");
        Iterator iter = q.getResultList().iterator();

        while (iter.hasNext()) {
            RuleInfoEntity ri = (RuleInfoEntity) iter.next();
            RuleEntry k = new RuleEntry(ri, ri.getAccount());
            RuleEntry re = (RuleEntry) ruleMap.get(k.getKey());
            if (re == null)
                ruleMap.put(k.getKey(), re = k);

            re.addOffset(offset);
        }
    }

    private void applyRules(GLTransactionEntity txn, Collection rules)
            throws HibernateException, GLException {
        Iterator iter = rules.iterator();
        while (iter.hasNext()) {
            RuleEntry re = (RuleEntry) iter.next();
            RuleInfoEntity ri = re.getRuleInfo();
            JournalRule rule = (JournalRule) getRuleImpl(ri.getClazz());
            rule.check(
                    this, txn, ri.getParam(), re.getAccount(),
                    re.getOffsets(), ri.getLayerArray()
            );
        }
    }

    private Collection getRules(GLTransactionEntity txn)
            throws HibernateException, GLException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        JournalEntity JournalEntity = txn.getJournal();

        Query q = entityManager.createQuery(
                "from RuleInfoEntity where journal=:journal and account is null order by id"
        );
        q.setParameter("journal", JournalEntity);
        Iterator iter = q.getResultList().iterator();
        while (iter.hasNext()) {
            RuleInfoEntity ri = (RuleInfoEntity) iter.next();
            RuleEntry re = new RuleEntry(ri);
            map.put(re.getKey(), re);
        }
        iter = txn.getEntries().iterator();
        for (int i = 0; iter.hasNext(); i++) {
            GLEntryEntity entry = (GLEntryEntity) iter.next();
            addRules(map, JournalEntity,
                    getAccountHierarchyIds(entry.getAccount()), i);
        }
        return map.values();
    }

    private BigDecimal[] getChartBalances
            (JournalEntity JournalEntity, CompositeAccountEntity acct, Instant date, boolean inclusive, short[] layers, long maxId)
            throws HibernateException, GLException {
        BigDecimal balance[] = {ZERO, ZERO};
        Iterator iter = ((CompositeAccountEntity) acct).getChildren().iterator();
        while (iter.hasNext()) {
            GLAccountEntity a = (GLAccountEntity) iter.next();
            BigDecimal[] b = getBalances(JournalEntity, a, date, inclusive, layers, maxId);
            if (a.isDebit()) {
                balance[0] = balance[0].add(b[0]);
                balance[1] = balance[1].add(b[1]);
            } else if (a.isCredit()) {
                balance[0] = balance[0].subtract(b[0]);
                balance[1] = balance[1].subtract(b[1]);
            } else {
                // We allow undefined type on composite accounts now (i.e. "order" accounts).
                // We don't add children balances.
                // throw new ai.sovereignrag.accounting.GLException ("ai.sovereignrag.accounting.GLAccountEntity " + a + " has wrong type");
            }
            // session.evict (a);  FIXME this conflicts with r251 (cascade=evict genearting a failed to lazily initialize a collection
        }
        return balance;
    }

    private Iterator findSummarizedGLEntries
            (JournalEntity JournalEntity, Instant start, Instant end, boolean credit, short layer)
            throws HibernateException, GLException {
        StringBuffer qs = new StringBuffer(
                "select entry.account.id, sum(entry.amount)" +
                        " from GLEntryEntity entry," +
                        " GLTransactionEntity txn" +
                        " where txn.id = entry.transaction" +
                        " and credit = :credit" +
                        " and txn.journal = :journal" +
                        " and entry.layer = :layer"
        );
        boolean equalDate = start.equals(end);
        if (equalDate) {
            qs.append(" and txn.postDate = :date");
        } else {
            qs.append(" and txn.postDate >= :start");
            qs.append(" and txn.postDate <= :end");
        }
        qs.append(" group by entry.account.id");
        Query q = entityManager.createQuery(qs.toString());
        q.setParameter("journal", JournalEntity.getId());
        q.setParameter("credit", credit ? "Y" : "N");
        q.setParameter("layer", layer);
        if (equalDate)
            q.setParameter("date", start);
        else {
            q.setParameter("start", start);
            q.setParameter("end", end);
        }
        return q.getResultList().iterator();
    }

    private void deleteGLTransactions(JournalEntity JournalEntity, Instant start, Instant end)
            throws HibernateException, GLException {
        boolean equalDate = start.equals(end);

        StringBuffer qs = new StringBuffer(
                "from GLTransactionEntity where journal = :journal"
        );
        if (equalDate) {
            qs.append(" and postDate = :date");
        } else {
            qs.append(" and postDate >= :start");
            qs.append(" and postDate <= :endDate");
        }
        Query q = entityManager.createQuery(qs.toString());
        q.setParameter("journal", JournalEntity.getId());
        if (equalDate)
            q.setParameter("date", start);
        else {
            q.setParameter("start", start);
            q.setParameter("endDate", end);
        }
        // Replaced ScrollableResults with regular list iteration as it's Hibernate-specific
        List results = q.getResultList();
        for (Object entity : results) {
            entityManager.remove(entity);
        }
    }

    private static Short[] toShortArray(short[] i) {
        if (i == null)
            return new Short[0];
        Short[] sa = new Short[i.length];
        for (int j = 0; j < i.length; j++)
            sa[j] = Short.valueOf(i[j]);
        return sa;
    }

    private long getMaxGLEntryId() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<GLEntryEntity> query = cb.createQuery(GLEntryEntity.class);
        Root<GLEntryEntity> root = query.from(GLEntryEntity.class);

        query.orderBy(cb.desc(root.get("id")));
        TypedQuery<GLEntryEntity> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(1);

        GLEntryEntity entry = typedQuery.getSingleResult();
        return entry != null ? entry.getId() : 0L;
    }

    private long getSafeMaxGLEntryId() {
        return Math.max(getMaxGLEntryId() - SAFE_WINDOW, 0L);
    }

    public void overrideSafeWindow(long l) {
        this.SAFE_WINDOW = l;
    }

    private void recurseChildren(GLAccountEntity acct, List<Long> list) {
        for (GLAccountEntity a : acct.getChildren()) {
            if (a.isFinalAccount())
                list.add(a.getId());
            else recurseChildren(a, list);
        }
    }

    private List<Long> getChildren(GLAccountEntity acct) {
        List<Long> list = new ArrayList<Long>();
        recurseChildren(acct, list);
        return list;
    }

    private List<FinalAccountEntity> getDeepFinalChildren0(GLAccountEntity acct) throws HibernateException, GLException {
        List<FinalAccountEntity> list = new ArrayList<>();
        if (acct.getChildren() != null) {
            for (GLAccountEntity a : acct.getChildren()) {
                list.addAll(getDeepFinalChildren0(a));
            }
        } else if (acct instanceof FinalAccountEntity) {
            list.add((FinalAccountEntity) acct);
        }
        return list;
    }

    private void validateAccountCode(GLAccountEntity parent, GLAccountEntity child)
            throws GLException {
        if (!parent.isChart() && !child.getCode().startsWith(parent.getCode())) {
            throw new GLException("Child GLAccountEntity code `" + child.getCode() + "` must start with parent GLAccountEntity code `" + parent.getCode() + "`", ResponseCode.INVALID_ACCOUNT);
        }
    }

    public String toString() {
        return super.toString() + "[EntityManager=" + entityManager.toString() + "]";
    }

    /*
    private void dumpRules (Collection rules) {
        log.warn ("--- rules ---");
        Iterator iter = rules.iterator();
        while (iter.hasNext()) {
            log.warn (iter.next());
        }
    }
    */

    private void setDialect() {
        // Try to determine dialect from EntityManager/Session
        try {
            if (entityManager != null) {
                String dialectName = entityManager.getEntityManagerFactory().getProperties()
                        .getOrDefault("hibernate.dialect", "").toString().toLowerCase();
                if (dialectName.contains("mysql")) {
                    nativeDialect = NativeDialect.MYSQL;
                } else if (dialectName.contains("postgres")) {
                    nativeDialect = NativeDialect.POSTGRESQL;
                } else {
                    nativeDialect = NativeDialect.ORM;
                }
            }
        } catch (Exception e) {
            // Default to ORM if we can't determine dialect
            nativeDialect = NativeDialect.ORM;
        }
    }

    public void forceDialect(NativeDialect dialect) {
        nativeDialect = dialect;
    }

    public enum NativeDialect {
        MYSQL, POSTGRESQL, ORM
    }
}
