/**
 * 
 */
package com.ethwt.core.transaction;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.transaction.TransactionManager;

/**
 * @author neillin
 *
 */
public interface XAConnectionFactoryAdaptor {
	
	/**
	 * Adapt the specific {@link XAConnectionFactory} and enroll with a JTA {@link TransactionManager}
	 * 
	 * @param factory the XA connection factory to adapt
	 * @param poolConfig configure for connection pooling
	 * @return the adapted connection factory
	 * @throws Exception if the factory cannot be adapted
	 */
	ConnectionFactory adapt(XAConnectionFactory factory, PoolConfig poolConfig) throws Exception;
}
