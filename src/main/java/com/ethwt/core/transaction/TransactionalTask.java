/**
 * 
 */
package com.ethwt.core.transaction;

/**
 * @author neillin
 *
 */
@FunctionalInterface
public interface TransactionalTask<T> extends TransactionalTag, ExceptionProducer<T> {
	
}
