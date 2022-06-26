package com.ethwt.core.transaction;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

public final class TransactionHandler {

    /**
     * For cases that the transaction should be marked for rollback
     * ie. when {@link RuntimeException} is thrown or when {@link Error} is thrown
     * or when the exception si marked in {@link Transactional#rollbackOn()}
     * then {@link Transaction#setRollbackOnly()} is invoked.
     */
    public static void handleExceptionNoThrow(TransactionalTask<?> transactional, Throwable t, Transaction tx)
            throws IllegalStateException, SystemException {

        for (Class<?> dontRollbackOnClass : transactional.dontRollbackOn()) {
            if (dontRollbackOnClass.isAssignableFrom(t.getClass())) {
                return;
            }
        }

        for (Class<?> rollbackOnClass : transactional.rollbackOn()) {
            if (rollbackOnClass.isAssignableFrom(t.getClass())) {
                tx.setRollbackOnly();
                return;
            }
        }

        // RuntimeException and Error are un-checked exceptions and rollback is expected
        if (t instanceof RuntimeException || t instanceof Error) {
            tx.setRollbackOnly();
            return;
        }
    }

    /**
     * <p>
     * It finished the transaction.
     * </p>
     * <p>
     * Call {@link TransactionManager#rollback()} when the transaction si marked for {@link Status#STATUS_MARKED_ROLLBACK}.
     * otherwise the transaction is committed.
     * Either way there is executed the {@link Runnable} 'afterEndTransaction' after the transaction is finished.
     * </p>
     */
    public static void endTransaction(TransactionManager tm, Transaction tx, ExceptionRunnable afterEndTransaction) throws Exception {
        try {
            if (tx != tm.getTransaction()) {
                throw new RuntimeException("Wrong transaction on thread");
            }

            if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                tm.rollback();
            } else {
                tm.commit();
            }
        } finally {
            afterEndTransaction.run();
        }
    }
}