/**
 * 
 */
package com.ethwt.core.transaction;

/**
 * @author neillin
 *
 */
@FunctionalInterface
public interface ExceptionRunnable {
	
	void run() throws Exception;
	
}
