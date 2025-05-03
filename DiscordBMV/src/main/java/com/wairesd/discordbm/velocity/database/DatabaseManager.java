package com.wairesd.discordbm.velocity.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load SQLite JDBC driver", e);
        }
    }
    private static final String IP_BLOCKS_TABLE = "ip_blocks";
    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS %s (
                ip TEXT PRIMARY KEY,
                attempts INTEGER DEFAULT 0,
                block_until TIMESTAMP,
                current_block_time INTEGER DEFAULT 0
            )""".formatted(IP_BLOCKS_TABLE);
    private static final String CREATE_PLAYERS_TABLE_SQL = """
    CREATE TABLE IF NOT EXISTS players (
        uuid TEXT PRIMARY KEY,
        username TEXT NOT NULL UNIQUE,
        last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )""";
    private final String dbUrl;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DatabaseManager(String dbUrl) {
        this.dbUrl = dbUrl;
        initDatabase();
    }

    public void executeSQL(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
        }
    }

    public <T> T querySQL(String sql, SQLFunction<PreparedStatement, T> handler, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return handler.apply(stmt);
        }
    }

    @FunctionalInterface
    public interface SQLFunction<P, R> {
        R apply(P p) throws SQLException;
    }

    private void initDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL); // Для ip_blocks
            stmt.execute(CREATE_PLAYERS_TABLE_SQL);
            logger.info("Database tables initialized");
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    public CompletableFuture<Boolean> isBlocked(String ip) {
        return CompletableFuture.supplyAsync(() -> checkIfBlocked(ip), executor);
    }

    private boolean checkIfBlocked(String ip) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = prepareSelectBlockUntilStatement(conn, ip)) {
            ResultSet rs = stmt.executeQuery();
            return isBlockActive(rs);
        } catch (SQLException e) {
            logError("Error checking blocked IP {}: {}", ip, e);
            return false;
        }
    }

    private PreparedStatement prepareSelectBlockUntilStatement(Connection conn, String ip) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT block_until FROM " + IP_BLOCKS_TABLE + " WHERE ip = ?");
        stmt.setString(1, ip);
        return stmt;
    }

    private boolean isBlockActive(ResultSet rs) throws SQLException {
        if (rs.next()) {
            Timestamp blockUntil = rs.getTimestamp("block_until");
            return blockUntil != null && blockUntil.after(Timestamp.from(Instant.now()));
        }
        return false;
    }

    public CompletableFuture<Void> incrementFailedAttempt(String ip) {
        return CompletableFuture.runAsync(() -> updateFailedAttempt(ip), executor);
    }

    private void updateFailedAttempt(String ip) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            updateAttemptsAndBlockTime(conn, ip);
            conn.commit();
        } catch (SQLException e) {
            logError("Error incrementing failed attempt for IP {}: {}", ip, e);
        }
    }

    private void updateAttemptsAndBlockTime(Connection conn, String ip) throws SQLException {
        try (PreparedStatement insertOrUpdateStmt = prepareInsertOrUpdateStatement(conn, ip)) {
            insertOrUpdateStmt.executeUpdate();
        }

        try (PreparedStatement selectStmt = prepareSelectAttemptsStatement(conn, ip)) {
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                int attempts = rs.getInt("attempts");
                long currentBlockTime = rs.getLong("current_block_time");
                if (attempts >= 10) {
                    updateBlockUntil(conn, ip, currentBlockTime);
                }
            }
        }
    }

    public void savePlayer(UUID uuid, String username) throws SQLException {
        executeSQL("""
        INSERT INTO players (uuid, username) 
        VALUES (?, ?)
        ON CONFLICT(uuid) DO UPDATE SET 
            username = excluded.username,
            last_seen = CURRENT_TIMESTAMP""",
                uuid.toString(),
                username
        );
    }

    private PreparedStatement prepareInsertOrUpdateStatement(Connection conn, String ip) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO %s (ip, attempts, current_block_time) VALUES (?, 1, ?) 
                ON CONFLICT(ip) DO UPDATE SET attempts = attempts + 1""".formatted(IP_BLOCKS_TABLE));
        stmt.setString(1, ip);
        stmt.setLong(2, 5 * 60 * 1000);
        return stmt;
    }

    private PreparedStatement prepareSelectAttemptsStatement(Connection conn, String ip) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT attempts, current_block_time FROM " + IP_BLOCKS_TABLE + " WHERE ip = ?");
        stmt.setString(1, ip);
        return stmt;
    }

    private void updateBlockUntil(Connection conn, String ip, long currentBlockTime) throws SQLException {
        long blockUntil = System.currentTimeMillis() + currentBlockTime;
        long newBlockTime = Math.min(currentBlockTime * 2, 60 * 60 * 1000);

        try (PreparedStatement updateStmt = prepareUpdateBlockUntilStatement(conn, ip, blockUntil, newBlockTime)) {
            updateStmt.executeUpdate();
        }
    }

    private PreparedStatement prepareUpdateBlockUntilStatement(Connection conn, String ip, long blockUntil, long newBlockTime) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("""
                UPDATE %s SET block_until = ?, attempts = 0, current_block_time = ? WHERE ip = ?""".formatted(IP_BLOCKS_TABLE));
        stmt.setTimestamp(1, new Timestamp(blockUntil));
        stmt.setLong(2, newBlockTime);
        stmt.setString(3, ip);
        return stmt;
    }

    public CompletableFuture<Void> resetAttempts(String ip) {
        return CompletableFuture.runAsync(() -> deleteIpEntry(ip), executor);
    }

    private void deleteIpEntry(String ip) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = prepareDeleteStatement(conn, ip)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            logError("Error resetting attempts for IP {}: {}", ip, e);
        }
    }

    private PreparedStatement prepareDeleteStatement(Connection conn, String ip) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + IP_BLOCKS_TABLE + " WHERE ip = ?");
        stmt.setString(1, ip);
        return stmt;
    }

    private void logError(String message, String ip, Throwable e) {
        logger.error(message.formatted(ip), e);
    }
}
