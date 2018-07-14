package com.robsonpeixoto;

import org.postgresql.PGNotification;
import org.postgresql.ssl.LibPQFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class NotificationTest {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");

        final Map<String, String> env = System.getenv();

        final String url = String.format(
                "jdbc:postgresql://%s:%s/%s",
                env.get("DATABASE_HOST"),
                env.getOrDefault("DATABASE_PORT", "5432"),
                env.get("DATABASE_NAME")
        );

        final Properties props = new Properties();
        props.setProperty("user", env.get("DATABASE_USER"));
        props.setProperty("password", env.get("DATABASE_PASSWORD"));

        if (Objects.equals(env.get("DATABASE_USE_SSL"), "true")) {
            props.setProperty("ssl", "true");
            props.setProperty("sslmode", "verify-ca");
            props.setProperty("sslrootcert", env.get("DATABASE_SSL_CA_CERTIFICATE_FILE"));
            props.setProperty("sslcert", env.get("DATABASE_SSL_CERTIFICATE_FILE"));
            props.setProperty("sslkey", env.get("DATABASE_SSL_KEY_FILE"));
        }

        final Connection lConn = DriverManager.getConnection(url, props);
        final Listener listener = new Listener(lConn);
        listener.run();
    }
}

class Listener extends Thread {
    private final org.postgresql.PGConnection pgconn;
    private static final Logger LOGGER = Logger.getLogger(Listener.class.getName());

    Listener(final Connection conn) throws SQLException {
        this.pgconn = conn.unwrap(org.postgresql.PGConnection.class);
        final Statement stmt = conn.createStatement();
        stmt.execute("LISTEN watchers");
        stmt.close();
    }

    public void run() {
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                final org.postgresql.PGNotification[] notifications = pgconn.getNotifications();
                if (notifications != null) {
                    for (final PGNotification notification : notifications) {
                        LOGGER.info(String.format("Got notification: %s payload: %s pid: %s",
                                notification.getName(), notification.getParameter(), notification.getPID())
                        );
                    }
                } else {
                    LOGGER.info("No notification");
                }
                TimeUnit.SECONDS.sleep(5);
            }
        } catch (SQLException | InterruptedException sqle) {
            sqle.printStackTrace();
        }
    }
}

