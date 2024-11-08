package com.atm.database;

import com.atm.exception.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PostgresConnection implements AutoCloseable {
  private final HikariDataSource dataSource;

  public PostgresConnection() {

    Properties props = loadProperties();

    String jdbcUrl = System.getenv().getOrDefault("DB_URL", props.getProperty("db.url"));
    String username = System.getenv().getOrDefault("DB_USERNAME", props.getProperty("db.username"));
    String password = System.getenv().getOrDefault("DB_PASSWORD", props.getProperty("db.password"));

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(username);
    config.setPassword(password);

    // Connection pool settings
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(5);
    config.setIdleTimeout(300000);
    config.setConnectionTimeout(10000);
    config.setAutoCommit(false);

    /**
     * Commonly used properties: 1. cachePrepStmts: Enable prepared statement caching 2.
     * prepStmtCacheSize: The number of prepared statements to cache 3. prepStmtCacheSqlLimit: The
     * SQL query length to cache
     */
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

    this.dataSource = new HikariDataSource(config);
    initialize();
  }

  private Properties loadProperties() {
    Properties props = new Properties();
    try (InputStream input =
        PostgresConnection.class.getClassLoader().getResourceAsStream("database.properties")) {
      if (input == null) {
        throw new RuntimeException("Unable to find database.properties");
      }
      props.load(input);
      return props;
    } catch (IOException e) {
      throw new RuntimeException("Failed to load database properties", e);
    }
  }

  private void initialize() {
    try (Connection conn = getConnection()) {
      log.info("Successfully connected to PostgreSQL database");
    } catch (SQLException e) {
      log.error("Failed to initialize database connection", e);
      throw new DatabaseException("Failed to initialize database connection", e);
    }
  }

  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  @Override
  public void close() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
  }
}
