/**
 * 
 */
package com.ethwt.core.transaction;

/**
 * @author neillin
 *
 */
@FunctionalInterface
public interface ExceptionProducer<T> {
	
	T execute() throws Exception;
	
}
