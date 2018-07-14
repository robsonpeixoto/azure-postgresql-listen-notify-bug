package com.robsonpeixoto;


import java.io.InputStream;
import java.util.logging.LogManager;

public class LoggingConfig {
    public LoggingConfig() {
        try {
            final LogManager logManager = LogManager.getLogManager();
            try (final InputStream is = getClass().getResourceAsStream("/logging.properties")) {
                logManager.readConfiguration(is);
            }
        } catch (Exception e) {
            // The runtime won't show stack traces if the exception is thrown
            e.printStackTrace();
        }
    }
}
