/**
 * 
 */
package com.ethwt.core.transaction;

import static com.ethwt.core.transaction.TxUtils.withMandatory;
import static com.ethwt.core.transaction.TxUtils.withNever;
import static com.ethwt.core.transaction.TxUtils.withNotSupport;
import static com.ethwt.core.transaction.TxUtils.withRequired;
import static com.ethwt.core.transaction.TxUtils.withRequiresNew;
import static com.ethwt.core.transaction.TxUtils.withSupports;

import java.util.ServiceLoader;

import javax.transaction.TransactionManager;
import javax.transaction.Transactional.TxType;
import javax.transaction.UserTransaction;

/**
 * @author Neil Lin
 *
 */
public interface TransactionManagerService {
	
	public class Registry {
		private static TransactionManagerService instance;
		
		public static synchronized TransactionManagerService getInstance() {
			if (instance == null) {
				instance = ServiceLoader.load(TransactionManagerService.class).iterator().next();
			}
			return instance;
		}
		
		public static synchronized void setInstance(TransactionManagerService inst) {
			instance = inst;
		}
	}

	
	default void executeWithTx(TxType type, TransactionalRunnable task) throws Exception {
		
		this.executeWithTx(type, () -> {
			task.run();
			return null;
		});
		
	}
	
	
	default <T> T executeWithTx(TxType type, TransactionalTask<T> task) throws Exception {
		switch(type) {
		case MANDATORY:
			return withMandatory(getTransactionManager(), task);
		case NEVER:
			return withNever(getTransactionManager(), task);
		case NOT_SUPPORTED:
			return withNotSupport(getTransactionManager(), task);
		case REQUIRED:
			return withRequired(getTransactionManager(), task);
		case REQUIRES_NEW:
			return withRequiresNew(getTransactionManager(), task);
		case SUPPORTS:
			return withSupports(getTransactionManager(), task);
		default:
			throw new RuntimeException("Unsupported transaction type: "+ type.name());
		
		}
	}
	
	
	TransactionManager getTransactionManager();
	UserTransaction getUserTransaction();
	
	XAConnectionFactoryAdaptor getConnectionAdaptor();
	XADataSourceAdaptor getDataSourceAdaptor();

}
