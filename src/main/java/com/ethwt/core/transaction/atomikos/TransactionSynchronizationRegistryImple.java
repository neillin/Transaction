/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2006,
 * @author JBoss Inc.
 *
 * $Id$
 */
package com.ethwt.core.transaction.atomikos;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionSynchronizationRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atomikos.icatch.CompositeTransaction;
import com.atomikos.icatch.config.Configuration;
import com.atomikos.icatch.jta.TransactionManagerImp;
import com.ethwt.core.transaction.jms.ConnectionFactoryProxy;

/**
 * Implementation of the TransactionSynchronizationRegistry interface, in line with the JTA 1.1 specification.
 *
 * @author jonathan.halliday@jboss.com
 */
public class TransactionSynchronizationRegistryImple implements TransactionSynchronizationRegistry
{
	private static Logger log = LoggerFactory.getLogger(ConnectionFactoryProxy.class);

    // This Imple is stateless and just delegates the work down to the transaction manager.

    /*
     * http://java.sun.com/javaee/5/docs/api/javax/transaction/TransactionSynchronizationRegistry.html
     * http://jcp.org/aboutJava/communityprocess/maintenance/jsr907/907ChangeLog.html
     */


    private transient javax.transaction.TransactionManager tm = TransactionManagerImp.getTransactionManager();

    private ConcurrentHashMap<Object, Map<Object,Object>> txResources = new ConcurrentHashMap<>();
    // Return an opaque object to represent the transaction bound to the current thread at the time this method is called.
    public Object getTransactionKey()
    {
        if (log.isTraceEnabled()) {
            log.trace("TransactionSynchronizationRegistryImple.getTransactionKey");
        }

        CompositeTransaction transactionImple = null;
        try
        {
            transactionImple = Configuration.getCompositeTransactionManager().getCompositeTransaction();
        }
        catch (Exception e)
        {
            throw new RuntimeException("The transaction implementation threw a SystemException", e);
        }

        if (transactionImple == null) {
            return null;
        } else {
            return transactionImple.getTid();
        }
    }

    // Add or replace an object in the Map of resources being managed for the transaction bound to the current thread at the time this method is called.
    public void putResource(Object key, Object value)
    {
        if (log.isTraceEnabled()) {
            log.trace("TransactionSynchronizationRegistryImple.putResource");
        }

        if(key ==  null)
        {
            throw new NullPointerException();
        }

        final Object txKey = this.getTransactionKey();
        
        if (txKey == null) {
        	throw new IllegalStateException("There is not active transaction associated with current thread.");
        }
        
        Map<Object,Object> map = this.txResources.computeIfAbsent(txKey, mkey -> {
        	try {
				this.tm.getTransaction().registerSynchronization(new Synchronization() {

					@Override
					public void beforeCompletion() {
					}

					@Override
					public void afterCompletion(int status) {
						txResources.remove(txKey);
					}
					
				});
			} catch (IllegalStateException e) {
				throw e;
			} catch (Exception e) {
				throw new IllegalStateException("Failed to add Tx resource ", e);
			}
        	return new HashMap<>();
        });
        map.put(key, value);
    }

    // Get an object from the Map of resources being managed for the transaction bound to the current thread at the time this method is called.
    public Object getResource(Object key)
    {
        if (log.isTraceEnabled()) {
            log.trace("TransactionSynchronizationRegistryImple.getResource");
        }

        if(key ==  null)
        {
            throw new NullPointerException();
        }

        final Object txKey = this.getTransactionKey();
        
        if (txKey == null) {
        	throw new IllegalStateException("There is not active transaction associated with current thread.");
        }

        Map<Object,Object> map = this.txResources.get(txKey);
        return map != null ? map.get(key) : null;
    }

    // Register a Synchronization instance with special ordering semantics.
    public void registerInterposedSynchronization(Synchronization synchronization)
    {
        if (log.isTraceEnabled()) {
            log.trace("TransactionSynchronizationRegistryImple.registerInterposedSynchronization - Class: " + synchronization.getClass() + " HashCode: " + synchronization.hashCode() + " toString: " + synchronization);
        }

        try
        {
            final Transaction tx = this.tm.getTransaction();
            if (tx == null) {
            	throw new IllegalStateException("There is not active transaction associated with current thread.");
            }
        	tx.registerSynchronization(synchronization);
        }
        catch (RollbackException e)
        {
            throw new com.arjuna.ats.jta.exceptions.RollbackException("The transaction implementation threw a RollbackException", e);
        }
        catch (SystemException e)
        {
            throw new RuntimeException("The transaction implementation threw a SystemException", e);
        }
    }

    // Return the status of the transaction bound to the current thread at the time this method is called.
    public int getTransactionStatus()
    {
        try
        {
            return tm.getStatus();
        }
        catch(SystemException e)
        {
            throw new RuntimeException("The transaction implementation threw a SystemException", e);
        }

    }

    // Set the rollbackOnly status of the transaction bound to the current thread at the time this method is called.
    public void setRollbackOnly()
    {
        if (log.isTraceEnabled()) {
            log.trace("TransactionSynchronizationRegistryImple.setRollbackOnly");
        }

        try
        {
            Transaction transaction = tm.getTransaction();

            if(transaction == null)
            {
                throw new IllegalStateException();
            }

            tm.setRollbackOnly();
        }
        catch (SystemException e)
        {
            throw new RuntimeException("The transaction implementation threw a SystemException", e);
        }
    }

    // Get the rollbackOnly status of the transaction bound to the current thread at the time this method is called.
    public boolean getRollbackOnly()
    {
        if (log.isTraceEnabled()) {
            log.trace("TransactionSynchronizationRegistryImple.getRollbackOnly");
        }

        try
        {
            final Transaction tx = this.tm.getTransaction();
            if (tx == null) {
            	throw new IllegalStateException("There is not active transaction associated with current thread.");
            }
            return (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK);
        }
        catch (SystemException e)
        {
            throw new RuntimeException("The transaction implementation threw a SystemException", e);
        }
    }
}
