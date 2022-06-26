/**
 * 
 */
package com.ethwt.core.transaction;

/**
 * @author neillin
 *
 */
@FunctionalInterface
public interface TransactionalRunnable extends TransactionalTag, ExceptionRunnable {

}
