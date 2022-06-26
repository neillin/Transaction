/**
 * 
 */
package com.ethwt.core.transaction.narayana;

import java.io.File;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.apache.commons.lang3.StringUtils;

import com.arjuna.ats.arjuna.coordinator.ActionStatus;
import com.ethwt.core.transaction.PoolConfig;
import com.ethwt.core.transaction.TransactionManagerService;
import com.ethwt.core.transaction.XAConnectionFactoryAdaptor;
import com.ethwt.core.transaction.XADataSourceAdaptor;
import com.ethwt.core.transaction.jms.ConnectionFactoryProxy;
import com.ethwt.core.transaction.jms.TransactionHelperImpl;
import com.networknt.config.Config;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.narayana.NarayanaTransactionIntegration;

/**
 * @author neillin
 *
 */
public class JBossTxManagerService implements TransactionManagerService {
	
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private final NarayanaConfig config = 
			Optional.ofNullable((NarayanaConfig)Config.getInstance().getJsonObjectConfig(NarayanaConfig.CONFIG_NAME, NarayanaConfig.class))
			.orElse(new NarayanaConfig());
	private XADataSourceAdaptor xaDataSourceAdaptor;
	private XAConnectionFactoryAdaptor xaConnectionFactoryAdaptor;

	@Override
	public TransactionManager getTransactionManager() {
		this.makeSureInitialized();
		return com.arjuna.ats.jta.TransactionManager.transactionManager();
	}

	@Override
	public UserTransaction getUserTransaction() {
		this.makeSureInitialized();
		return com.arjuna.ats.jta.UserTransaction.userTransaction();
	}

	@Override
	public XAConnectionFactoryAdaptor getConnectionAdaptor() {
		this.makeSureInitialized();
		return this.xaConnectionFactoryAdaptor;
	}

	@Override
	public XADataSourceAdaptor getDataSourceAdaptor() {
		this.makeSureInitialized();
		return this.xaDataSourceAdaptor;
	}


	void makeSureInitialized() {
		if (this.initialized.compareAndSet(false, true)) {
			initLogDir();
			initTransactionManagerId();
			NarayanaPropertiesInitializer initializer = new NarayanaPropertiesInitializer(this.config);
			initializer.setup();
			this.xaDataSourceAdaptor = new XADataSourceAdaptor() {
				
				@Override
				public DataSource adapt(XADataSource xaDataSource, PoolConfig poolConfig) throws Exception {
			        TransactionSynchronizationRegistry txSyncRegistry = new com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple();

			        AgroalDataSourceConfigurationSupplier configurationSupplier = new AgroalDataSourceConfigurationSupplier()
			                .connectionPoolConfiguration( cp -> cp
			                        .maxSize( 10 )
			                        .transactionIntegration( new NarayanaTransactionIntegration( getTransactionManager(), txSyncRegistry ) )
			                        .connectionFactoryConfiguration( cf -> cf
			                                .autoCommit( true ) )
			                );
					return AgroalDataSource.from(configurationSupplier);
				}
			};
			
			this.xaConnectionFactoryAdaptor = new XAConnectionFactoryAdaptor() {
				
				@Override
				public ConnectionFactory adapt(XAConnectionFactory factory, PoolConfig poolConfig) throws Exception {
					return new ConnectionFactoryProxy(factory, new TransactionHelperImpl(getTransactionManager()));
				}
			};
			
		}
	}
	
	void initLogDir() {
		if(StringUtils.isEmpty(this.config.getLogDir())) {
			File home = new File(System.getProperty("user.home"));
			this.config.setLogDir(new File(home,".narayana/txLogs").getAbsolutePath());
		}
	}
	
	void initTransactionManagerId() {
		if(StringUtils.isEmpty(this.config.getTransactionManagerId())) {
			this.config.setTransactionManagerId(UUID.randomUUID().toString());
		}
	}
}
