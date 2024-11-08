package com.atm.database;

/** Centralized location for all query */
public final class PostgreSQLQueries {
  private PostgreSQLQueries() {}

  public static final class Tables {
    /*
     * Users table
     */
    static final String CREATE_USERS_TABLE =
        "CREATE TABLE IF NOT EXISTS users ("
            + "id BIGSERIAL PRIMARY KEY,"
            + "username VARCHAR(50) UNIQUE NOT NULL,"
            + "password_hash VARCHAR(255) NOT NULL,"
            + "created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,"
            + "last_login TIMESTAMPTZ"
            + ")";

    /*
     * Balances table
     */
    static final String CREATE_BALANCES_TABLE =
        "CREATE TABLE IF NOT EXISTS balances ("
            + "user_id BIGINT PRIMARY KEY,"
            + "balance NUMERIC(19,4) NOT NULL DEFAULT 0.0000,"
            + "last_updated TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,"
            + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
            + ")";

    /*
     * Transactions table
     */
    static final String CREATE_TRANSACTIONS_TABLE =
        "CREATE TABLE IF NOT EXISTS transactions ("
            + "id BIGSERIAL PRIMARY KEY,"
            + "from_user_id BIGINT,"
            + "to_user_id BIGINT,"
            + "amount NUMERIC(19,4) NOT NULL,"
            + "type VARCHAR(50) NOT NULL,"
            + "created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,"
            + "FOREIGN KEY (from_user_id) REFERENCES users(id),"
            + "FOREIGN KEY (to_user_id) REFERENCES users(id)"
            + ")";

    /*
     * Sessions table with automatic expiration
     */
    static final String CREATE_SESSIONS_TABLE =
        "CREATE TABLE IF NOT EXISTS sessions ("
            + "id BIGSERIAL PRIMARY KEY,"
            + "user_id BIGINT NOT NULL,"
            + "created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,"
            + "last_activity_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,"
            + "expires_at TIMESTAMPTZ DEFAULT (CURRENT_TIMESTAMP + INTERVAL '1 minutes'),"
            + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
            + ")";

    static String[] getAllCreateTableStatements() {
      return new String[] {
        CREATE_USERS_TABLE, CREATE_BALANCES_TABLE, CREATE_TRANSACTIONS_TABLE, CREATE_SESSIONS_TABLE
      };
    }
  }

  public static final class Indexes {
    static final String CREATE_USERNAME_IDX =
        "CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)";

    static final String CREATE_SESSION_USER_IDX =
        "CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id)";

    static final String CREATE_SESSION_EXPIRY_IDX =
        "CREATE INDEX IF NOT EXISTS idx_sessions_expires_at ON sessions(expires_at)";

    static final String CREATE_TRANSACTION_USERS_IDX =
        "CREATE INDEX IF NOT EXISTS idx_transactions_users ON transactions(from_user_id, to_user_id)";

    static final String CREATE_TRANSACTION_DATE_IDX =
        "CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at DESC)";

    static String[] getAllCreateIndexStatements() {
      return new String[] {
        CREATE_USERNAME_IDX,
        CREATE_SESSION_USER_IDX,
        CREATE_SESSION_EXPIRY_IDX,
        CREATE_TRANSACTION_USERS_IDX,
        CREATE_TRANSACTION_DATE_IDX
      };
    }
  }

  public static final class Users {
    // Create
    static final String INSERT =
        "INSERT INTO users (username, password_hash) VALUES (?, ?) RETURNING id";

    // Read
    static final String GET_BY_USERNAME = "SELECT * FROM users WHERE username = ?";
    static final String GET_BY_ID = "SELECT * FROM users WHERE id = ?";

    // Update
    static final String UPDATE_LAST_LOGIN =
        "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";

    // Delete
    static final String DELETE = "DELETE FROM users WHERE id = ?";
  }

  public static final class Balances {
    // Create
    static final String INITIALIZE = "INSERT INTO balances (user_id) VALUES (?)";

    // Read with lock
    static final String GET = "SELECT balance FROM balances WHERE user_id = ? FOR UPDATE";

    static final String LOCK_FOR_UPDATE =
        "SELECT user_id, balance FROM balances WHERE user_id IN (?, ?) FOR UPDATE";

    // Update with optimistic locking
    static final String UPDATE_WITH_LOCK =
        "UPDATE balances SET balance = ?, last_updated = CURRENT_TIMESTAMP " + "WHERE user_id = ?";

    static final String UPDATE_BALANCE_BATCH =
        "UPDATE balances SET "
            + "balance = CASE "
            + "  WHEN user_id = ? THEN balance - ? "
            + "  WHEN user_id = ? THEN balance + ? "
            + "END, "
            + "last_updated = CURRENT_TIMESTAMP "
            + "WHERE user_id IN (?, ?)";

    static final String CHECK_SUFFICIENT_BALANCE =
        "SELECT balance >= ? as sufficient " + "FROM balances WHERE user_id = ? FOR UPDATE";
  }

  public static final class Sessions {
    // Create
    static final String CREATE =
        "INSERT INTO sessions (user_id, last_activity_at, expires_at) "
            + "VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 minutes') "
            + "RETURNING id";

    // Read
    static final String GET_BY_ID =
        "SELECT * FROM sessions WHERE id = ? AND expires_at > CURRENT_TIMESTAMP";

    static final String GET_ACTIVE =
        "SELECT * FROM sessions "
            + "WHERE user_id = ? AND expires_at > CURRENT_TIMESTAMP "
            + "ORDER BY created_at DESC LIMIT 1";

    // Update
    static final String UPDATE_ACTIVITY =
        "UPDATE sessions SET last_activity_at = CURRENT_TIMESTAMP, "
            + "expires_at = CURRENT_TIMESTAMP + INTERVAL '1 minutes' "
            + "WHERE id = ? AND expires_at > CURRENT_TIMESTAMP";

    // Delete
    static final String DELETE = "DELETE FROM sessions WHERE id = ?";

    static final String CLEANUP_USER_SESSIONS = "DELETE FROM sessions WHERE user_id = ?";

    static final String CLEANUP_STALE =
        "DELETE FROM sessions WHERE expires_at <= CURRENT_TIMESTAMP";
  }

  public static final class Transactions {
    // Create with balance check
    static final String CREATE =
        "INSERT INTO transactions (from_user_id, to_user_id, amount, type) "
            + "VALUES (?, ?, ?, ?) "
            + "RETURNING id";

    // Read
    static final String GET_BY_ID = "SELECT * FROM transactions WHERE id = ?";

    static final String GET_BY_USER =
        "SELECT * FROM transactions "
            + "WHERE from_user_id = ? OR to_user_id = ? "
            + "ORDER BY created_at DESC";

    static final String GET_RECENT =
        "SELECT * FROM transactions "
            + "WHERE (from_user_id = ? OR to_user_id = ?) "
            + "ORDER BY created_at DESC LIMIT ?";
  }
}
