package com.emprestimos.config;

import java.net.URI;
import java.net.URISyntaxException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Origem de dados de producao (PostgreSQL).
 *
 * <p>Aceita a connection string tanto no formato JDBC (<code>jdbc:postgresql://...</code>)
 * quanto no formato libpq/psql (<code>postgresql://...</code>, como exibido no Neon),
 * normalizando o prefixo <code>jdbc:</code> quando ausente.
 *
 * <p>O driver JDBC do PostgreSQL nao entende credenciais embutidas na URL
 * (<code>usuario:senha@host</code> e formato libpq/psql). Por isso o usuario/senha
 * sao extraidos do "userinfo" e repassados ao Hikari como propriedades, com a URL
 * limpa sendo entregue ao driver.
 */
@Configuration
@Profile("prod")
public class PostgresDataSourceConfig {

    private static final String PREFIXO_JDBC = "jdbc:";

    @Bean
    @Primary
    public DataSource dataSource(@Value("${spring.datasource.url}") String url) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(extrairCredenciais(url, config));
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }

    private String extrairCredenciais(String url, HikariConfig config) {
        String entrada = url == null ? "" : url.trim();
        if (entrada.isEmpty()) {
            return entrada;
        }

        boolean temPrefixoJdbc = entrada.startsWith(PREFIXO_JDBC);
        String soUrl = entrada;
        if (temPrefixoJdbc) {
            soUrl = entrada.substring(PREFIXO_JDBC.length());
        }

        try {
            URI uri = new URI(soUrl);
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                int separador = userInfo.indexOf(':');
                if (separador != -1) {
                    config.setUsername(userInfo.substring(0, separador));
                    config.setPassword(userInfo.substring(separador + 1));
                } else {
                    config.setUsername(userInfo);
                }
                URI limpo = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                        uri.getPath(), uri.getQuery(), uri.getFragment());
                soUrl = limpo.toString();
            }
        } catch (URISyntaxException ignore) {
            // URL nao parseavel como URI (ex.: prefixo jdbc nao convencional):
            // entrega como esta embutida na conexao original.
        }

        return PREFIXO_JDBC + soUrl;
    }
}