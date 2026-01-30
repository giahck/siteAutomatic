package com.sitecentral.sitecentral.Entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class test implements CommandLineRunner {

    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbc;

    @Autowired
    @Qualifier("postgresJdbcTemplate")
    private JdbcTemplate postgresJdbc;

    @Override
    public void run(String... args) {
        System.out.println("\n--- TEST JDBC ESPLICITO ---");

        // TEST MYSQL (via SSH)
        try {
            // Sostituisci 'users' con una tabella che sai esistere su MySQL
            Integer count = mysqlJdbc.queryForObject("SELECT COUNT(*) FROM asset", Integer.class);
            System.out.println("✅ MYSQL (SSH) OK! Numero record: " + count);
        } catch (Exception e) {
            System.err.println("❌ MYSQL (SSH) FALLITO: " + e.getMessage());
        }

        // TEST POSTGRESQL (Locale)
        try {
            // Query semplice per vedere se Postgres risponde
            String version = postgresJdbc.queryForObject("SELECT version()", String.class);
            System.out.println("✅ POSTGRESQL OK! Versione: " + version.substring(0, 30) + "...");

            // Verifica se l'estensione pgvector è attiva
            Integer vectorExt = postgresJdbc.queryForObject(
                    "SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class);
            System.out.println(vectorExt > 0 ? "✅ PGVECTOR: Installato" : "⚠️ PGVECTOR: Non trovato");

        } catch (Exception e) {
            System.err.println("❌ POSTGRESQL FALLITO: " + e.getMessage());
        }

        System.out.println("--- FINE TEST ---\n");
    }
}