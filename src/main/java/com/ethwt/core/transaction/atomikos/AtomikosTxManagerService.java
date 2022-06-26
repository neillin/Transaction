/**
 * 
 */
package com.ethwt.core.transaction.atomikos;

import java.io.File;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.apache.commons.lang3.StringUtils;

import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.TransactionManagerImp;
import com.atomikos.icatch.jta.UserTransactionImp;
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
 * @author Neil Lin
 *
 */
public class AtomikosTxManagerService implements TransactionManagerService {

	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private final AtomikosConfig config = 
			Optional.ofNullable((AtomikosConfig)Config.getInstance().getJsonObjectConfig(AtomikosConfig.CONFIG_NAME, AtomikosConfig.class))
			.orElse(new AtomikosConfig());
	private XADataSourceAdaptor xaDataSourceAdaptor;
	private XAConnectionFactoryAdaptor xaConnectionFactoryAdaptor;
	private UserTransactionServiceImp service;

	@Override
	public TransactionManager getTransactionManager() {
		this.makeSureInitialized();
		return atomikosTxManager();
	}

	@Override
	public UserTransaction getUserTransaction() {
		this.makeSureInitialized();
		return new UserTransactionImp();
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
			Properties properties = new Properties();
			properties.putAll(this.config.asProperties());
			this.service = new UserTransactionServiceImp(properties);
			this.service.init();
			this.xaDataSourceAdaptor = new XADataSourceAdaptor() {
				
				@Override
				public DataSource adapt(XADataSource xaDataSource, PoolConfig poolConfig) throws Exception {
			        TransactionSynchronizationRegistry txSyncRegistry = new TransactionSynchronizationRegistryImple();

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
	
	TransactionManager atomikosTxManager() {
		return TransactionManagerImp.getTransactionManager();
	}
	
	void initLogDir() {
		if(StringUtils.isEmpty(this.config.getLogBaseDir())) {
			File home = new File(System.getProperty("user.home"));
			this.config.setLogBaseDir(new File(home,".narayana/txLogs").getAbsolutePath());
		}
	}
	
}
