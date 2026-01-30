package com.sitecentral.sitecentral.Config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.sitecentral. repository. mysql",
        entityManagerFactoryRef = "mysqlEntityManagerFactory",
        transactionManagerRef = "mysqlTransactionManager"
)
public class MysqlConfig {

    @Value("${spring.datasource.mysql.url}") String url;
    @Value("${spring.datasource.mysql.username}") String user;
    @Value("${spring.datasource.mysql.password}") String pass;

    @Bean(name = "mysqlDataSource")
    @DependsOn("sshTunnelConfig")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setPoolName("HikariPool-MySQL-SSH");

        // --- CONFIGURAZIONE DI RECUPERO FORZATO ---
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);

        // Se la connessione fallisce, Hikari riproverà all'infinito ogni 5 secondi
        config.setInitializationFailTimeout(0);
        config.setConnectionTimeout(5000);

        // Vita brevissima: se il tunnel traballa, cambiamo connessione ogni 2 minuti
        config.setMaxLifetime(120000);
        config.setKeepaliveTime(30000);
        config.setValidationTimeout(3000);

        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    @Bean(name = "mysqlEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("mysqlDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.sitecentral.entity. mysql")
                .persistenceUnit("mysql")
                .build();
    }

    @Bean(name = "mysqlTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("mysqlEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}