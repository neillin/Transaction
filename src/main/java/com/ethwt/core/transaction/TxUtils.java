/**
 * 
 */
package com.ethwt.core.transaction;

import static com.ethwt.core.transaction.TransactionHandler.*;

import java.util.Optional;

import javax.transaction.InvalidTransactionException;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionRequiredException;
import javax.transaction.TransactionalException;

/**
 * @author neillin
 *
 */
public abstract class TxUtils {

	public static final String[] TxStatusStrings =
	   {
	      "STATUS_ACTIVE",
	      "STATUS_MARKED_ROLLBACK",
	      "STATUS_PREPARED",
	      "STATUS_COMMITTED",
	      "STATUS_ROLLEDBACK",
	      "STATUS_UNKNOWN",
	      "STATUS_NO_TRANSACTION",
	      "STATUS_PREPARING",
	      "STATUS_COMMITTING",
	      "STATUS_ROLLING_BACK"
	   };

	public static String getTxStatus(int status) {
		if (status >= 0 && status < TxStatusStrings.length) {
			return TxStatusStrings[status];
		} else {
			return "STATUS_INVALID(" + status + ")";
		}
	}

	public static boolean isCompleted(int status) {
		return status == Status.STATUS_COMMITTED || status == Status.STATUS_ROLLEDBACK
				|| status == Status.STATUS_NO_TRANSACTION;
	}

	public static boolean isRollback(int status) {
		return status == Status.STATUS_MARKED_ROLLBACK || status == Status.STATUS_ROLLING_BACK
				|| status == Status.STATUS_ROLLEDBACK;
	}

	public static boolean isUncommitted(int status) {
		return status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK;
	}

	public static boolean isActive(int status) {
		return status == Status.STATUS_ACTIVE;
	}

	public static <T> T ignoreException(ExceptionProducer<T> producer) {
		try {
			return producer.execute();
		} catch (Exception e) {
			return null;
		}
	}

	public static TransactionManager getTransactionManager() {
		return Optional.ofNullable(TransactionManagerService.Registry.getInstance())
				.map(srv -> srv.getTransactionManager()).orElse(null);
	}

	public static Transaction currentTransaction() {
		return Optional.ofNullable(getTransactionManager()).map(mgr -> ignoreException(mgr::getTransaction))
				.orElse(null);
	}

	public static boolean hasTxContext() {
		return currentTransaction() != null;
	}

	static <T> T invokeInOurTx(TransactionManager tm, TransactionalTask<T> task, ExceptionRunnable callback)
			throws Exception {

		tm.begin();
		Transaction tx = tm.getTransaction();

		T ret = null;

		try {
			ret = task.execute();
		} catch (Throwable t) {
			handleException(task, t, tx);
		} finally {
			endTransaction(tm, tx, callback);
		}
		return ret;
	}

	static <T> T invokeInCallerTx(TransactionalTask<T> task, Transaction tx) throws Exception {
		T ret = null;

		try {
			ret = task.execute();
		} catch (Throwable t) {
			handleException(task, t, tx);
		}
		return ret;
	}

	static void handleException(TransactionalTask<?> task, Throwable t, Transaction tx) throws Exception {
		handleExceptionNoThrow(task, t, tx);
		sneakyThrow(t);
	}

	static <T> T invokeInNoTx(TransactionalTask<T> task) throws Exception {
		return task.execute();
	}

	@SuppressWarnings("unchecked")
	public static <E extends Throwable> void sneakyThrow(Throwable t) throws E {
		throw (E) t;
	}

	public static <T> T withMandatory(TransactionManager tm, TransactionalTask<T> task) throws Exception {
		Transaction tx = tm.getTransaction();
		if (tx == null) {
			throw new TransactionalException("Transaction is required for invocation",
					new TransactionRequiredException());
		}
		return task.execute();
	}

	public static <T> T withRequired(TransactionManager tm, TransactionalTask<T> task) throws Exception {
		Transaction tx = tm.getTransaction();
		if (tx == null) {
			return invokeInOurTx(tm, task, () -> {
			});
		} else {
			return invokeInCallerTx(task, tx);
		}
	}

	public static <T> T withNever(TransactionManager tm, TransactionalTask<T> task) throws Exception {
		Transaction tx = tm.getTransaction();
		if (tx != null) {
			throw new TransactionalException("Transaction is not allowed for invocation",
					new InvalidTransactionException());
		}
		return task.execute();
	}

	public static <T> T withNotSupport(TransactionManager tm, TransactionalTask<T> task) throws Exception {
		Transaction tx = tm.getTransaction();
		if (tx != null) {
			tm.suspend();
			try {
				return task.execute();
			} finally {
				tm.resume(tx);
			}
		} else {
			return task.execute();
		}
	}

	public static <T> T withSupports(TransactionManager tm, TransactionalTask<T> task) throws Exception {
		Transaction tx = tm.getTransaction();
		if (tx == null) {
			return invokeInNoTx(task);
		} else {
			return invokeInCallerTx(task, tx);
		}
	}

	public static <T> T withRequiresNew(TransactionManager tm, TransactionalTask<T> task) throws Exception {
		Transaction tx = tm.getTransaction();
		if (tx != null) {
			tm.suspend();
			return invokeInOurTx(tm, task, () -> tm.resume(tx));
		} else {
			return invokeInOurTx(tm, task, () -> {
			});
		}
	}
}
