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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

/**
 * Bean that configures Narayana transaction manager.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class NarayanaPropertiesInitializer {

    private static final Logger logger = LoggerFactory.getLogger(NarayanaPropertiesInitializer.class);

    private final NarayanaConfig properties;

    public NarayanaPropertiesInitializer(NarayanaConfig narayanaProperties) {
        this.properties = narayanaProperties;
    }

    public void setup() {
        if (isPropertiesFileAvailable()) {
            logger.info("Non-empty Narayana properties file found, ignoring Narayana application properties");
            return;
        }
        setNodeIdentifier(this.properties.getTransactionManagerId());
        setXARecoveryNodes(this.properties.getXaRecoveryNodes());
        setObjectStoreDir(this.properties.getLogDir());
        setCommitOnePhase(this.properties.isOnePhaseCommit());
        setDefaultTimeout(this.properties.getDefaultTimeout());
        setPeriodicRecoveryPeriod(this.properties.getPeriodicRecoveryPeriod());
        setRecoveryBackoffPeriod(this.properties.getRecoveryBackoffPeriod());
        setXaResourceOrphanFilters(this.properties.getXaResourceOrphanFilters());
        setRecoveryModules(this.properties.getRecoveryModules());
        setExpiryScanners(this.properties.getExpiryScanners());
    }

    private boolean isPropertiesFileAvailable() {
        // If the Narayana default properties are equal to the System properties,
        // it means that either the Narayana properties file is missing or it is empty.
        return !PropertiesFactory.getDefaultProperties().stringPropertyNames()
                .equals(System.getProperties().stringPropertyNames());
    }

    private void setNodeIdentifier(String nodeIdentifier) {
        try {
            getPopulator(CoreEnvironmentBean.class).setNodeIdentifier(nodeIdentifier);
        } catch (CoreEnvironmentBeanException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void setXARecoveryNodes(List<String> xaRecoveryNodes) {
        getPopulator(JTAEnvironmentBean.class).setXaRecoveryNodes(xaRecoveryNodes);
    }

    private void setObjectStoreDir(String objectStoreDir) {
        if (objectStoreDir != null) {
            getPopulator(ObjectStoreEnvironmentBean.class).setObjectStoreDir(objectStoreDir);
            getPopulator(ObjectStoreEnvironmentBean.class, "communicationStore").setObjectStoreDir(objectStoreDir);
            getPopulator(ObjectStoreEnvironmentBean.class, "stateStore").setObjectStoreDir(objectStoreDir);
        }
    }

    private void setCommitOnePhase(boolean isCommitOnePhase) {
        getPopulator(CoordinatorEnvironmentBean.class).setCommitOnePhase(isCommitOnePhase);
    }

    private void setDefaultTimeout(int defaultTimeout) {
        getPopulator(CoordinatorEnvironmentBean.class).setDefaultTimeout(defaultTimeout);
    }

    private void setPeriodicRecoveryPeriod(int periodicRecoveryPeriod) {
        getPopulator(RecoveryEnvironmentBean.class).setPeriodicRecoveryPeriod(periodicRecoveryPeriod);
    }

    private void setRecoveryBackoffPeriod(int recoveryBackoffPeriod) {
        getPopulator(RecoveryEnvironmentBean.class).setRecoveryBackoffPeriod(recoveryBackoffPeriod);
    }

    private void setXaResourceOrphanFilters(List<String> xaResourceOrphanFilters) {
        getPopulator(JTAEnvironmentBean.class).setXaResourceOrphanFilterClassNames(xaResourceOrphanFilters);
    }

    private void setRecoveryModules(List<String> recoveryModules) {
        getPopulator(RecoveryEnvironmentBean.class).setRecoveryModuleClassNames(recoveryModules);
    }

    private void setExpiryScanners(List<String> expiryScanners) {
        getPopulator(RecoveryEnvironmentBean.class).setExpiryScannerClassNames(expiryScanners);
    }

    private <T> T getPopulator(Class<T> beanClass) {
        return BeanPopulator.getDefaultInstance(beanClass);
    }

    private <T> T getPopulator(Class<T> beanClass, String name) {
        return BeanPopulator.getNamedInstance(beanClass, name);
    }

}
