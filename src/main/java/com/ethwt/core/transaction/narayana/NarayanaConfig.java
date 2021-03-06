/*
 * Copyright 2020 Red Hat, Inc, and individual contributors.
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

package com.ethwt.core.transaction.narayana;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Subset of Narayana properties which can be configured via Spring configuration. Use
 * jbossts-properties.xml for complete configuration.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class NarayanaConfig {
	
	public static final String CONFIG_NAME="narayana";

    /**
     * Prefix for Narayana specific properties.
     */
    static final String PROPERTIES_PREFIX = "narayana";

    /**
     * Transaction object store directory.
     */
    private String logDir;

    /**
     * Unique transaction manager id.
     */
    private String transactionManagerId = "1";

    /**
     * Enable one phase commit optimization.
     */
    private boolean onePhaseCommit = true;

    /**
     * Transaction timeout in seconds.
     */
    private int defaultTimeout = 60;

    /**
     * Interval in which periodic recovery scans are performed in seconds.
     */
    private int periodicRecoveryPeriod = 120;

    /**
     * Back off period between first and second phases of the recovery scan in seconds.
     */
    private int recoveryBackoffPeriod = 10;

    /**
     * Comma-separated list of orphan filters.
     */
    private List<String> xaResourceOrphanFilters = new ArrayList<>(Arrays.asList(
            "com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter",
            "com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter",
            "com.arjuna.ats.internal.jta.recovery.arjunacore.JTAActionStatusServiceXAResourceOrphanFilter"));

    /**
     * Comma-separated list of recovery modules.
     */
    private List<String> recoveryModules = new ArrayList<>(Arrays.asList(
            "com.arjuna.ats.internal.jta.recovery.arjunacore.CommitMarkableResourceRecordRecoveryModule",
            "com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule",
            "com.arjuna.ats.internal.txoj.recovery.TORecoveryModule",
            "com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule"));

    /**
     * Comma-separated list of expiry scanners.
     */
    private List<String> expiryScanners = new ArrayList<>(Collections.singletonList(
            "com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner"));


    /**
     * XA recovery nodes.
     */
    private List<String> xaRecoveryNodes = new ArrayList<>(Arrays.asList("1"));

	public String getLogDir() {
        return this.logDir;
    }

    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    public String getTransactionManagerId() {
        return this.transactionManagerId;
    }

    public void setTransactionManagerId(String transactionManagerId) {
        this.transactionManagerId = transactionManagerId;
    }

    public boolean isOnePhaseCommit() {
        return this.onePhaseCommit;
    }

    public void setOnePhaseCommit(boolean onePhaseCommit) {
        this.onePhaseCommit = onePhaseCommit;
    }

    public int getDefaultTimeout() {
        return this.defaultTimeout;
    }

    public int getPeriodicRecoveryPeriod() {
        return this.periodicRecoveryPeriod;
    }

    public void setPeriodicRecoveryPeriod(int periodicRecoveryPeriod) {
        this.periodicRecoveryPeriod = periodicRecoveryPeriod;
    }

    public int getRecoveryBackoffPeriod() {
        return this.recoveryBackoffPeriod;
    }

    public void setRecoveryBackoffPeriod(int recoveryBackoffPeriod) {
        this.recoveryBackoffPeriod = recoveryBackoffPeriod;
    }

    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public List<String> getXaResourceOrphanFilters() {
        return this.xaResourceOrphanFilters;
    }

    public void setXaResourceOrphanFilters(List<String> xaResourceOrphanFilters) {
        this.xaResourceOrphanFilters = xaResourceOrphanFilters;
    }

    public List<String> getRecoveryModules() {
        return this.recoveryModules;
    }

    public void setRecoveryModules(List<String> recoveryModules) {
        this.recoveryModules = recoveryModules;
    }

    public List<String> getExpiryScanners() {
        return this.expiryScanners;
    }

    public void setExpiryScanners(List<String> expiryScanners) {
        this.expiryScanners = expiryScanners;
    }
    
    public List<String> getXaRecoveryNodes() {
		return xaRecoveryNodes;
	}

	public void setXaRecoveryNodes(List<String> xaRecoveryNodes) {
		this.xaRecoveryNodes = xaRecoveryNodes;
	}

}
