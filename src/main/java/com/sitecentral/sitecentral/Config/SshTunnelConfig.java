package com.sitecentral.sitecentral.Config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;
import java.util.Properties;

@Component
public class SshTunnelConfig {

    @Value("${ssh.host}") private String sshHost;
    @Value("${ssh.user}") private String sshUser;
    @Value("${ssh.privateKeyPath}") private String privateKeyPath;
    @Value("${ssh.remote.mysql.port}") private int remotePort;
    @Value("${ssh.local.mysql.port}") private int localPort;

    private Session session;

    // Inject del DataSource MySQL per evictare connessioni stale
    @Autowired(required = false)
    @Qualifier("mysqlDataSource")
    @Lazy
    private DataSource mysqlDataSource;

    @PostConstruct
    public void init() {
        connect();
    }

    public synchronized void connect() {
        try {
            // OK SOLO se sessione connessa *e* forward realmente utilizzabile
            if (session != null && session.isConnected() && isLocalForwardAlive()) {
                return;
            }

            // teardown hard
            try { if (session != null) session.disconnect(); } catch (Exception ignored) {}
            session = null;

            JSch jsch = new JSch();
            jsch.addIdentity(privateKeyPath);
            session = jsch.getSession(sshUser, sshHost, 22);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("TCPKeepAlive", "yes");
            session.setConfig(config);

            session.setServerAliveInterval(15000);
            session.setServerAliveCountMax(3);

            session.connect(10000);
            session.setPortForwardingL(localPort, "127.0.0.1", remotePort);

            // aspetta che la porta sia davvero “up”
            if (!waitForwardReady(10)) {
                throw new IllegalStateException("Port forwarding non pronto");
            }

            System.out.println("--- [OK] TUNNEL SSH CONNESSO E FORWARD PRONTO ---");
            evictStaleConnections();

        } catch (Exception e) {
            System.err.println("--- [ERRORE] SSH: " + e.getMessage());
            try { if (session != null) session.disconnect(); } catch (Exception ignored) {}
            session = null;
        }
    }

    private boolean waitForwardReady(int maxSeconds) {
        for (int i = 0; i < maxSeconds; i++) {
            if (isLocalForwardAlive()) return true;
            try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return false; }
        }
        return false;
    }

    private boolean isLocalForwardAlive() {
        try (var socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", localPort), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void checkTunnelStatus() {
        boolean ok = (session != null && session.isConnected() && isLocalForwardAlive());
        if (!ok) {
            System.err.println("--- [CRITICO] TUNNEL DOWN/ZOMBIE (session=" + (session != null && session.isConnected()) +
                    ", forward=" + isLocalForwardAlive() + ") -> riconnessione ---");
            connect();
        } else {
            System.out.println("--- [HEALTH] Tunnel SSH reale attivo ---");
        }
    }

    @Autowired
    private org.springframework.context.ApplicationContext context;

    private void evictStaleConnections() {
        try {
            // Recuperiamo il bean solo nel momento del bisogno per evitare il loop
            DataSource ds = context.getBean("mysqlDataSource", DataSource.class);
            if (ds instanceof HikariDataSource hikariDS) {
                System.out.println("--- [INFO] Evicting connessioni MySQL stale... ---");
                hikariDS.getHikariPoolMXBean().softEvictConnections();
                System.out.println("--- [OK] Connessioni stale eted ---");
            }
        } catch (Exception e) {
            System.err.println("--- [WARN] Impossibile evictare connessioni (forse il bean non è ancora pronto): " + e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (session != null && session. isConnected()) {
            session.disconnect();
            System.out.println("--- [INFO] TUNNEL SSH CHIUSO ---");
        }
    }
}