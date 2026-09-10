package com.emprestimos.config;

import java.io.File;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

/**
 * Configura a origem de dados SQLite ativando o modo WAL (Write-Ahead Logging)
 * e definindo um timeout de ocupacao para evitar travamentos de arquivo em
 * acessos concorrentes de multiplos usuarios.
 */
@Configuration
public class SqliteDataSourceConfig {

    private final String url;
    private final boolean walEnabled;

    public SqliteDataSourceConfig(
            @Value("${spring.datasource.url}") String url,
            @Value("${app.sqlite.wal:true}") boolean walEnabled) {
        this.url = url;
        this.walEnabled = walEnabled;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        String filePath = url.replace("jdbc:sqlite:", "");
        File dbFile = new File(filePath);
        File parent = dbFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        SQLiteConfig config = new SQLiteConfig();
        if (walEnabled) {
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);
            config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        }
        config.setBusyTimeout(5000);
        config.enforceForeignKeys(true);

        SQLiteDataSource dataSource = new SQLiteDataSource(config);
        dataSource.setUrl(url);
        return dataSource;
    }
}