package com.atm.database;

import com.atm.exception.DatabaseException;
import com.atm.exception.InsufficientFundsException;
import com.atm.model.Session;
import com.atm.model.User;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Database implements AutoCloseable {
  private final PostgresConnection connection;
  private final PostgresTransaction transaction;

  public Database() {
    this.connection = new PostgresConnection();
    this.transaction = new PostgresTransaction(connection);
    initializeDatabase();
  }

  private void initializeDatabase() {
    transaction.executeInTransaction(
        connection -> {
          try (Statement stmt = connection.createStatement()) {
            for (String createTable : PostgreSQLQueries.Tables.getAllCreateTableStatements()) {
              stmt.execute(createTable);
            }
            for (String createIndex : PostgreSQLQueries.Indexes.getAllCreateIndexStatements()) {
              stmt.execute(createIndex);
            }
          } catch (SQLException e) {
            throw new DatabaseException("Failed to initialize database", e);
          }
          log.info("Database initialized successfully");
          return null;
        });
  }

  /*
   * User operations
   */
  public Long createUser(String username, String passwordHash) {
    return transaction.executeInTransaction(
        connection -> {
          try (PreparedStatement pstmt =
              connection.prepareStatement(
                  PostgreSQLQueries.Users.INSERT, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
              if (rs.next()) {
                Long userId = rs.getLong(1);
                initializeBalance(connection, userId);
                log.info("Successfully created user: {}", username);
                return userId;
              }
            }
            throw new DatabaseException("Failed to create user: no ID returned");
          } catch (SQLException e) {
            throw new DatabaseException("Failed to create user", e);
          }
        });
  }

  public Optional<User> getUserByUsername(String username) {
    return transaction.executeInTransaction(
        connection -> {
          try (PreparedStatement pstmt =
              connection.prepareStatement(PostgreSQLQueries.Users.GET_BY_USERNAME)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
              if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
              }
            }
            return Optional.empty();
          } catch (SQLException e) {
            log.error("Failed to get user by username", e);
            return Optional.empty();
          }
        });
  }

  public void updateLastLogin(Long userId) {
    transaction.executeInTransaction(
        connection -> {
          try (PreparedStatement pstmt =
              connection.prepareStatement(PostgreSQLQueries.Users.UPDATE_LAST_LOGIN)) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
            return null;
          } catch (SQLException e) {
            log.error("Failed to update last login", e);
            return null;
          }
        });
  }

  private void initializeBalance(Connection connection, Long userId) throws SQLException {
    try (PreparedStatement pstmt =
        connection.prepareStatement(PostgreSQLQueries.Balances.INITIALIZE)) {
      pstmt.setLong(1, userId);
      pstmt.executeUpdate();
      log.debug("Initialized balance for user: {}", userId);
    } catch (SQLException e) {
      log.error("Failed to initialize balance for user: {}", userId, e);
      throw e;
    }
  }

  /*
   * Session operations
   */
  public Long createSession(Long userId) {
    return transaction.executeInTransaction(
        connection -> {
          try (PreparedStatement cleanup =
              connection.prepareStatement(PostgreSQLQueries.Sessions.CLEANUP_USER_SESSIONS)) {
            cleanup.setLong(1, userId);
            cleanup.executeUpdate();
          } catch (SQLException e) {
            log.error("Failed to cleanup user sessions", e);
          }

          try (PreparedStatement pstmt =
              connection.prepareStatement(
                  PostgreSQLQueries.Sessions.CREATE, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, userId);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
              if (rs.next()) {
                log.info("Created session for user: {}", userId);
                return rs.getLong(1);
              }
              throw new DatabaseException("Failed to create session: no ID returned");
            }
          } catch (SQLException e) {
            throw new DatabaseException("Failed to create session", e);
          }
        });
  }

  public Optional<Session> getSessionById(Long sessionId) {
    return transaction.executeInTransaction(
        connection -> {
          try (PreparedStatement pstmt =
              connection.prepareStatement(PostgreSQLQueries.Sessions.GET_BY_ID)) {
            pstmt.setLong(1, sessionId);
            try (ResultSet rs = pstmt.executeQuery()) {
              if (rs.next() && !isSessionExpired(rs)) {
                return Optional.of(mapResultSetToSession(rs));
              }
            }
            return Optional.empty();
          } catch (SQLException e) {
            log.error("Failed to get session by ID", e);
            return Optional.empty();
          }
        });
  }

  public void updateSessionActivity(Long sessionId) {
    transaction.executeInTransaction(
        connection -> {
          try (PreparedStatement pstmt =
              connection.prepareStatement(PostgreSQLQueries.Sessions.UPDATE_ACTIVITY)) {
            pstmt.setLong(1, sessionId);
            int updated = pstmt.executeUpdate();
            if (updated == 0) {
              throw new DatabaseException("Session not found or expired: " + sessionId);
            }
            return null;
          } catch (SQLException e) {
            log.error("Failed to update session activity", e);
            return null;
          }
        });
  }

  public Optional<Session> getActiveSession(Long userId) {
    return transaction.executeInTransaction(
        connection -> {
          try (PreparedStatement pstmt =
              connection.prepareStatement(PostgreSQLQueries.Sessions.GET_ACTIVE)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
              if (rs.next() && !isSessionExpired(rs)) {
                return Optional.of(mapResultSetToSession(rs));
              }
            }
            return Optional.empty();
          } catch (SQLException e) {
            log.error("Failed to get active session", e);
            return Optional.empty();
          }
        });
  }

  public void deleteSession(Long sessionId) {
    transaction.executeInTransaction(
        connection -> {
          try (PreparedStatement pstmt =
              connection.prepareStatement(PostgreSQLQueries.Sessions.DELETE)) {
            pstmt.setLong(1, sessionId);
            int deleted = pstmt.executeUpdate();
            if (deleted == 0) {
              throw new DatabaseException("Session not found: " + sessionId);
            }
            return null;
          } catch (SQLException e) {
            log.error("Failed to delete session", e);
            return null;
          }
        });
  }

  /*
   * Balance operations
   */
  public BigDecimal getBalance(Long userId) {
    return transaction.executeInTransaction(
        connection -> {
          try (PreparedStatement pstmt =
              connection.prepareStatement(PostgreSQLQueries.Balances.GET)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
              if (rs.next()) {
                return rs.getBigDecimal("balance");
              }
              throw new DatabaseException("No balance record found for user: " + userId);
            }
          } catch (SQLException e) {
            throw new DatabaseException("Failed to get balance", e);
          }
        });
  }

  /*
   * Transaction operations
   */

  public void createTransaction(Long userId, BigDecimal amount, String type) {
    transaction.executeInTransaction(
        connection -> {
          try {

            // Get current balance
            BigDecimal currentBalance;
            try (PreparedStatement balanceStmt =
                connection.prepareStatement(PostgreSQLQueries.Balances.GET)) {
              balanceStmt.setLong(1, userId);
              try (ResultSet rs = balanceStmt.executeQuery()) {
                if (!rs.next()) {
                  throw new DatabaseException("No balance record found for user: " + userId);
                }
                currentBalance = rs.getBigDecimal("balance");
              }
            }

            // Update balance
            BigDecimal newBalance;
            if (type.equals("DEPOSIT")) {
              newBalance = currentBalance.add(amount);
            } else {
              newBalance = currentBalance.subtract(amount);
            }

            try (PreparedStatement updateStmt =
                connection.prepareStatement(PostgreSQLQueries.Balances.UPDATE_WITH_LOCK)) {
              updateStmt.setBigDecimal(1, newBalance);
              updateStmt.setLong(2, userId);
              updateStmt.executeUpdate();
            }

            // Create transaction record
            Long transactionId;
            try (PreparedStatement transStmt =
                connection.prepareStatement(
                    PostgreSQLQueries.Transactions.CREATE, Statement.RETURN_GENERATED_KEYS)) {

              transStmt.setLong(1, userId);
              transStmt.setLong(2, userId);
              transStmt.setBigDecimal(3, amount);
              transStmt.setString(4, type);

              transStmt.executeUpdate();

              try (ResultSet rs = transStmt.getGeneratedKeys()) {
                if (!rs.next()) {
                  throw new DatabaseException("Failed to create transaction record");
                }
                transactionId = rs.getLong(1);
              }
            }

            log.info(
                "Successfully performed transaction of {} for user {}. Transaction ID: {}",
                amount,
                userId,
                transactionId);
            return transactionId;
          } catch (SQLException e) {
            log.error("Failed to perform transaction operation", e);
            throw new DatabaseException("Failed to perform transaction operation", e);
          }
        });
  }

  /**
   * Atomically performs a transfer between two users including balance updates and transaction
   * record
   */
  public void performTransfer(Long fromUserId, Long toUserId, BigDecimal amount) {
    transaction.executeInTransaction(
        connection -> {
          try {
            /**
             * Lock both balances in a fixed order to prevent deadlocks. This is a simple way to
             * avoid deadlocks but it's not the most efficient way. A more efficient way is to
             * dynamically order the locks based on the user IDs to minimize the chance of
             * deadlocks. This is a common pattern in database systems and is known as "Ordered
             * Locking".
             */
            Long firstLock = Math.min(fromUserId, toUserId);
            Long secondLock = Math.max(fromUserId, toUserId);

            try (PreparedStatement lockStmt =
                connection.prepareStatement(PostgreSQLQueries.Balances.LOCK_FOR_UPDATE)) {
              lockStmt.setLong(1, firstLock);
              lockStmt.setLong(2, secondLock);
              lockStmt.executeQuery();
            }

            /*
             * Get source balance and verify sufficient funds
             * Might be redundant if the balance is already checked in the service layer
             * but it's a good practice to re-verify in the database layer
             */
            BigDecimal sourceBalance = null;
            BigDecimal destinationBalance = null;
            try (PreparedStatement balanceStmt =
                connection.prepareStatement(PostgreSQLQueries.Balances.LOCK_FOR_UPDATE)) {
              balanceStmt.setLong(1, firstLock);
              balanceStmt.setLong(2, secondLock);

              try (ResultSet rs = balanceStmt.executeQuery()) {
                while (rs.next()) {
                  long userId = rs.getLong("user_id");
                  BigDecimal balance = rs.getBigDecimal("balance");

                  if (userId == fromUserId) {
                    sourceBalance = balance;
                  } else if (userId == toUserId) {
                    destinationBalance = balance;
                  }
                }
              }
            }

            // Verify source user and balance
            if (sourceBalance == null) {
              throw new DatabaseException("No balance record found for source user: " + fromUserId);
            }
            if (sourceBalance.compareTo(amount) < 0) {
              throw new InsufficientFundsException(
                  "Insufficient funds. Available: " + sourceBalance + ", Required: " + amount);
            }

            // Verify target user
            if (destinationBalance == null) {
              throw new DatabaseException("No balance record found for target user: " + toUserId);
            }

            /*
             * Update balances for both user atomically in a single transaction
             */
            try (PreparedStatement updateStmt =
                connection.prepareStatement(PostgreSQLQueries.Balances.UPDATE_BALANCE_BATCH)) {
              updateStmt.setLong(1, fromUserId);
              updateStmt.setBigDecimal(2, amount);
              updateStmt.setLong(3, toUserId);
              updateStmt.setBigDecimal(4, amount);
              updateStmt.setLong(5, fromUserId);
              updateStmt.setLong(6, toUserId);

              int updateCounts = updateStmt.executeUpdate();

              log.info("updateStmt: {}", updateStmt);

              log.info("updateCounts: {}", updateCounts);

              if (updateCounts != 2) {
                throw new DatabaseException("Failed to update both balances");
              }
            }

            // Create transaction record
            Long transactionId;
            try (PreparedStatement transStmt =
                connection.prepareStatement(
                    PostgreSQLQueries.Transactions.CREATE, Statement.RETURN_GENERATED_KEYS)) {

              transStmt.setLong(1, fromUserId);
              transStmt.setLong(2, toUserId);
              transStmt.setBigDecimal(3, amount);
              transStmt.setString(4, "TRANSFER");

              transStmt.executeUpdate();

              try (ResultSet rs = transStmt.getGeneratedKeys()) {
                if (!rs.next()) {
                  throw new DatabaseException("Failed to create transaction record");
                }
                transactionId = rs.getLong(1);
              }
            }

            log.info(
                "Successfully transferred {} from user {} to user {}. Transaction ID: {}",
                amount,
                fromUserId,
                toUserId,
                transactionId);
            return transactionId;
          } catch (SQLException e) {
            log.error("Failed to perform transfer operation", e);
            throw new DatabaseException("Failed to perform transfer operation", e);
          }
        });
  }

  /*
   * Utility methods
   */
  private boolean isSessionExpired(ResultSet rs) throws SQLException {
    Timestamp expiresAt = rs.getTimestamp("expires_at");
    return expiresAt != null && expiresAt.before(new Timestamp(System.currentTimeMillis()));
  }

  private User mapResultSetToUser(ResultSet rs) throws SQLException {
    return User.builder()
        .id(rs.getLong("id"))
        .username(rs.getString("username"))
        .passwordHash(rs.getString("password_hash"))
        .createdAt(getLocalDateTime(rs, "created_at"))
        .lastLogin(getLocalDateTime(rs, "last_login"))
        .build();
  }

  private Session mapResultSetToSession(ResultSet rs) throws SQLException {
    return Session.builder()
        .id(rs.getLong("id"))
        .userId(rs.getLong("user_id"))
        .createdAt(getLocalDateTime(rs, "created_at"))
        .lastActivityAt(getLocalDateTime(rs, "last_activity_at"))
        .expiresAt(getLocalDateTime(rs, "expires_at"))
        .build();
  }

  private LocalDateTime getLocalDateTime(ResultSet rs, String columnName) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnName);
    return timestamp != null ? timestamp.toLocalDateTime() : null;
  }

  @Override
  public void close() {
    connection.close();
  }
}
