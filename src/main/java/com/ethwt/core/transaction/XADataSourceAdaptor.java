/**
 * 
 */
package com.ethwt.core.transaction;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

/**
 * @author Neil Lin
 *
 */
public interface XADataSourceAdaptor {
	
	/**
	 * Adapt the specific {@link XADataSource} and enroll it with a JTA {@link TransactionManager}
	 * 
	 * @param xaDataSource the XA data source to adapt
	 * @param poolConfig configure for connection pooling
	 * @return the adapted data source
	 * @throws Exception if data source cannot be adapted
	 */
	DataSource adapt(XADataSource xaDataSource, PoolConfig poolConfig) throws Exception;
}
