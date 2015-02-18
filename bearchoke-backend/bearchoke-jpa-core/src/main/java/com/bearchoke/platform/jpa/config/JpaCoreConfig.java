/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bearchoke.platform.jpa.config;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.transaction.SystemException;
import java.util.Properties;

/**
 * Created by Bjorn Harvold
 * Date: 1/9/14
 * Time: 11:55 PM
 * Responsibility:
 */
@Configuration
@Profile("jpa")
@EnableJpaRepositories(basePackages = {"org.axonframework.eventstore.jpa", "com.bearchoke.platform.jpa"})
@EnableTransactionManagement
public class JpaCoreConfig {

    @Inject
    private Environment environment;

    @Inject
    private DataSource dataSource;

    @Bean
    public EntityManagerFactory entityManagerFactory(JpaVendorAdapter jpaVendorAdapter) {

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(jpaVendorAdapter);

        String[] packages = environment.getProperty("jpa.packages.to.scan").split(",");
        factory.setPackagesToScan(packages);

        factory.setJpaProperties(dataSourceConfig());
        factory.afterPropertiesSet();

        return factory.getObject();
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        hibernateJpaVendorAdapter.setShowSql(false);
        hibernateJpaVendorAdapter.setGenerateDdl(true);
        hibernateJpaVendorAdapter.setDatabasePlatform(environment.getProperty("jpa.dialect"));

        return hibernateJpaVendorAdapter;
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    public UserTransactionManager userTransactionManager() {
        UserTransactionManager utm = new UserTransactionManager();
        utm.setForceShutdown(true);

        return utm;
    }

    @Bean
    public UserTransactionImp userTransactionImp() throws SystemException {
        UserTransactionImp uti = new UserTransactionImp();
        uti.setTransactionTimeout(300);

        return uti;
    }

    @Bean
    public JtaTransactionManager jtaTransactionManager() throws SystemException {
        JtaTransactionManager jtam = new JtaTransactionManager(userTransactionImp(), userTransactionManager());
        return jtam;
    }

    private Properties dataSourceConfig() {
      Properties props = new Properties();

      props.put("hibernate.connection.charSet", "UTF-8");

      props.put("hibernate.show_sql", "true");
      props.put("hibernate.use_sql_comments", "true");
      props.put("hibernate.format_sql", "true");

      props.put("hibernate.ejb.naming_strategy", "org.hibernate.cfg.ImprovedNamingStrategy");

      props.put("hibernate.current_session_context_class", "jta");
      props.put("hibernate.archive.autodetection", "class");

      //props.put("hibernate.transaction.manager_lookup_class", "com.atomikos.icatch.jta.hibernate3.TransactionManagerLookup");
      //props.put("hibernate.dialect", environment.getProperty("jpa.dialect"));
      //props.put("hibernate.hbm2ddl.auto", environment.getProperty("jpa.hibernate.create.strategy"));

      props.put("hibernate.dialect", "com.bearchoke.platform.jpa.config.PostgreSQL9DialectCustom");
      props.put("hibernate.hbm2ddl.auto", "");
      props.put("hibernate.generateDdl", "false");

      return props;
    }

}
