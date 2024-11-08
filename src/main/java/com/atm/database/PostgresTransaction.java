package com.atm.database;

import com.atm.exception.DatabaseException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PostgresTransaction {
  private final PostgresConnection postgresConnection;

  public PostgresTransaction(PostgresConnection postgresConnection) {
    this.postgresConnection = postgresConnection;
  }

  public <T> T executeInTransaction(Function<Connection, T> operation) {
    try (Connection conn = postgresConnection.getConnection()) {
      try {
        T result = operation.apply(conn);
        conn.commit();
        return result;
      } catch (Exception e) {
        try {
          conn.rollback();
        } catch (SQLException rollbackEx) {
          log.error("Failed to rollback transaction", rollbackEx);
        }
        if (e instanceof SQLException) {
          handlePostgresException((SQLException) e);
        }
        throw new DatabaseException("Transaction failed", e);
      }
    } catch (SQLException e) {
      handlePostgresException(e);
      throw new DatabaseException("Failed to execute transaction", e);
    }
  }

  private void handlePostgresException(SQLException e) {
    String sqlState = e.getSQLState();
    switch (sqlState) {
      case "23505":
        throw new DatabaseException("Duplicate entry: " + e.getMessage());
      case "23503":
        throw new DatabaseException("Referenced record not found: " + e.getMessage());
      case "40001":
        throw new DatabaseException("Transaction conflict, please retry: " + e.getMessage());
      case "40P01":
        throw new DatabaseException("Deadlock detected, please retry: " + e.getMessage());
      default:
        log.error("PostgreSQL error: {} - {}", sqlState, e.getMessage());
    }
  }
}
