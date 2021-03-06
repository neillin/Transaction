/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ethwt.core.transaction.jms;


import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;
import javax.transaction.xa.XAException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class ConnectionManager {
	private static Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    private final XAConnectionFactory xaConnectionFactory;

    private final String user;

    private final String pass;

    private XAConnection connection;

    private XASession session;

    public ConnectionManager(XAConnectionFactory xaConnectionFactory, String user, String pass) {
        this.xaConnectionFactory = xaConnectionFactory;
        this.user = user;
        this.pass = pass;
    }

    /**
     * Invoke {@link XAResourceConsumer} accept method before making sure that JMS connection is available. Current
     * connection is used if one is available. If connection is not available, new connection is created before the
     * accept call and closed after it.
     *
     * @param consumer {@link XAResourceConsumer} to be executed.
     * @throws XAException if JMS connection cannot be created.
     */
    public void connectAndAccept(XAResourceConsumer consumer) throws XAException {
        if (isConnected()) {
            consumer.accept(session.getXAResource());
            return;
        }

        connect();
        try {
            consumer.accept(session.getXAResource());
        } finally {
            disconnect();
        }
    }

    /**
     * Invoke {@link XAResourceFunction} apply method before making sure that JMS connection is available. Current
     * connection is used if one is available. If connection is not available, new connection is created before the
     * apply call and closed after it.
     *
     * @param function {@link XAResourceFunction} to be executed.
     * @param <T> Return type of the {@link XAResourceFunction}.
     * @return The result of {@link XAResourceFunction}.
     * @throws XAException if JMS connection cannot be created.
     */
    public <T> T connectAndApply(XAResourceFunction<T> function) throws XAException {
        if (isConnected()) {
            return function.apply(session.getXAResource());
        }

        connect();
        try {
            return function.apply(session.getXAResource());
        } finally {
            disconnect();
        }
    }

    /**
     * Create JMS connection.
     *
     * @throws XAException if JMS connection cannot be created.
     */
    public void connect() throws XAException {
        if (isConnected()) {
            return;
        }

        try {
            connection = createXAConnection();
            session = connection.createXASession();
        } catch (JMSException e) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException ignore) {
                }
            }
           log.warn("Failed to create JMS connection",e);
            throw new XAException(XAException.XAER_RMFAIL);
        }
    }

    /**
     * Close current JMS connection.
     */
    public void disconnect() {
        if (!isConnected()) {
            return;
        }

        try {
            connection.close();
        } catch (JMSException e) {
            log.warn("Failed to close JMS connection {}", connection.toString(), e);
        } finally {
            connection = null;
            session = null;
        }
    }

    /**
     * Check if JMS connection is active.
     *
     * @return {@code true} if JMS connection is active.
     */
    public boolean isConnected() {
        return connection != null && session != null;
    }

    private XAConnection createXAConnection() throws JMSException {
        if (user == null && pass == null) {
            return xaConnectionFactory.createXAConnection();
        }

        return xaConnectionFactory.createXAConnection(user, pass);
    }

}
