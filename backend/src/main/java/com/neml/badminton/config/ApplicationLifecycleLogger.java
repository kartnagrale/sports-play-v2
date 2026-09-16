package com.neml.badminton.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

@Component
public class ApplicationLifecycleLogger {
    private static final Logger log = LoggerFactory.getLogger(ApplicationLifecycleLogger.class);

    private final Environment environment;
    private final DataSource dataSource;
    private final String logPath;

    public ApplicationLifecycleLogger(Environment environment, DataSource dataSource,
            @Value("${logging.file.path}") String logPath) {
        this.environment = environment;
        this.dataSource = dataSource;
        this.logPath = logPath;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        String[] profiles = environment.getActiveProfiles();
        log.info("Sports-Play ready profiles={} port={} logFile={}/sports-play.log",
                profiles.length == 0 ? "default" : Arrays.toString(profiles),
                environment.getProperty("server.port", "8001"), logPath);
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL().replaceFirst("\\?.*$", "");
            log.info("Database ready product={} version={} url={} schema={}",
                    connection.getMetaData().getDatabaseProductName(),
                    connection.getMetaData().getDatabaseProductVersion(), url, connection.getSchema());
        } catch (SQLException exception) {
            log.warn("Application started but database identity could not be logged", exception);
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void stopping() {
        log.info("Sports-Play shutdown requested");
    }
}
