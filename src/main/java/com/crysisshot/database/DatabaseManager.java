package com.crysisshot.database;

import com.crysisshot.CrysisShot;
import com.crysisshot.models.PlayerStats;
import com.crysisshot.ranking.Rank;
import com.crysisshot.utils.Logger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Database manager for CrysisShot plugin
 * Handles all database operations with connection pooling using HikariCP
 */
public class DatabaseManager {
    
    private final CrysisShot plugin;
    private HikariDataSource dataSource;
    private boolean initialized = false;
      // SQL Queries
    private static final String CREATE_PLAYERS_TABLE = """
        CREATE TABLE IF NOT EXISTS crysis_players (
            player_id TEXT PRIMARY KEY,
            player_name TEXT NOT NULL,
            total_kills INTEGER DEFAULT 0,
            total_deaths INTEGER DEFAULT 0,
            bow_kills INTEGER DEFAULT 0,
            melee_kills INTEGER DEFAULT 0,
            current_rank TEXT DEFAULT 'NOVATO',
            games_played INTEGER DEFAULT 0,
            games_won INTEGER DEFAULT 0,
            longest_kill_streak INTEGER DEFAULT 0,
            total_arrows_fired INTEGER DEFAULT 0,
            total_arrows_hit INTEGER DEFAULT 0,
            total_damage_dealt REAL DEFAULT 0.0,
            powerups_collected INTEGER DEFAULT 0,
            total_playtime INTEGER DEFAULT 0,
            first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            is_active BOOLEAN DEFAULT 1
        )
    """;
    
    private static final String INSERT_PLAYER = """
        INSERT OR REPLACE INTO crysis_players 
        (player_id, player_name, total_kills, total_deaths, bow_kills, melee_kills, current_rank,
         games_played, games_won, longest_kill_streak, total_arrows_fired, total_arrows_hit, 
         total_damage_dealt, powerups_collected, total_playtime, first_join, last_seen, is_active)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;
    
    private static final String SELECT_PLAYER = """
        SELECT * FROM crysis_players WHERE player_id = ?
    """;
    
    private static final String UPDATE_LAST_SEEN = """
        UPDATE crysis_players SET last_seen = CURRENT_TIMESTAMP WHERE player_id = ?
    """;
    
    private static final String SELECT_TOP_PLAYERS = """
        SELECT * FROM crysis_players WHERE is_active = 1 
        ORDER BY %s DESC LIMIT ?
    """;
    
    public DatabaseManager(CrysisShot plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize the database connection and create tables
     */
    public boolean initialize() {
        try {
            setupDataSource();
            createTables();
            initialized = true;
            Logger.info("Database initialized successfully");
            return true;
        } catch (Exception e) {
            Logger.severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Setup HikariCP connection pool
     */
    private void setupDataSource() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File databaseFile = new File(dataFolder, "crysisshot.db");
        
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("CrysisShot-Pool");
        
        // SQLite specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        dataSource = new HikariDataSource(config);
    }
    
    /**
     * Create database tables if they don't exist
     */
    private void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_PLAYERS_TABLE);
            Logger.info("Database tables created/verified");
        }
    }
    
    /**
     * Get a connection from the pool
     */
    private Connection getConnection() throws SQLException {
        if (!initialized || dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database not initialized or connection pool closed");
        }
        return dataSource.getConnection();
    }
    
    /**     * Save player statistics to database
     */
    public CompletableFuture<Boolean> savePlayerStats(PlayerStats stats) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(INSERT_PLAYER)) {
                
                stmt.setString(1, stats.getPlayerId().toString());
                stmt.setString(2, stats.getPlayerName());
                stmt.setInt(3, stats.getTotalKills());
                stmt.setInt(4, stats.getTotalDeaths());
                stmt.setInt(5, stats.getBowKills());
                stmt.setInt(6, stats.getMeleeKills());
                stmt.setString(7, stats.getCurrentRank().name());
                stmt.setInt(8, stats.getGamesPlayed());
                stmt.setInt(9, stats.getGamesWon());
                stmt.setInt(10, stats.getLongestKillStreak());
                stmt.setInt(11, stats.getTotalArrowsFired());
                stmt.setInt(12, stats.getTotalArrowsHit());
                stmt.setDouble(13, stats.getTotalDamageDealt());
                stmt.setInt(14, stats.getPowerupsCollected());
                stmt.setLong(15, stats.getTotalPlaytime());
                stmt.setTimestamp(16, stats.getFirstJoin());
                stmt.setTimestamp(17, stats.getLastSeen());
                stmt.setBoolean(18, stats.isActive());
                
                return stmt.executeUpdate() > 0;
                
            } catch (SQLException e) {
                Logger.severe("Failed to save player stats for " + stats.getPlayerName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Load player statistics from database
     */
    public CompletableFuture<PlayerStats> loadPlayerStats(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SELECT_PLAYER)) {
                
                stmt.setString(1, playerId.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToPlayerStats(rs);
                    }
                }
                
                return null; // Player not found
                
            } catch (SQLException e) {
                Logger.severe("Failed to load player stats for " + playerId + ": " + e.getMessage());
                return null;
            }
        });
    }
    
    /**
     * Update player's last seen timestamp
     */
    public CompletableFuture<Void> updateLastSeen(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(UPDATE_LAST_SEEN)) {
                
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                Logger.severe("Failed to update last seen for " + playerId + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Get top players by specified criteria
     */
    public CompletableFuture<List<PlayerStats>> getTopPlayers(String orderBy, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerStats> topPlayers = new ArrayList<>();
              // Validate orderBy parameter to prevent SQL injection
            String validOrderBy = validateOrderByColumn(orderBy);
            if (validOrderBy == null) {
                Logger.severe("Invalid orderBy parameter: " + orderBy);
                return topPlayers;
            }
            
            String query = String.format(SELECT_TOP_PLAYERS, validOrderBy);
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, limit);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        topPlayers.add(mapResultSetToPlayerStats(rs));
                    }
                }                } catch (SQLException e) {
                Logger.severe("Failed to get top players: " + e.getMessage());
            }
            
            return topPlayers;
        });
    }
    
    /**
     * Validate and return safe column name for ORDER BY
     */
    private String validateOrderByColumn(String orderBy) {
        Set<String> validColumns = Set.of(
            "total_kills", "total_deaths", "games_played", "games_won",
            "longest_kill_streak", "total_arrows_fired", "total_arrows_hit",
            "total_damage_dealt", "powerups_collected", "total_playtime"
        );
        
        return validColumns.contains(orderBy.toLowerCase()) ? orderBy : null;
    }
      /**
     * Map ResultSet to PlayerStats object
     */
    private PlayerStats mapResultSetToPlayerStats(ResultSet rs) throws SQLException {
        PlayerStats stats = new PlayerStats();
        
        stats.setPlayerId(UUID.fromString(rs.getString("player_id")));
        stats.setPlayerName(rs.getString("player_name"));
        stats.setTotalKills(rs.getInt("total_kills"));
        stats.setTotalDeaths(rs.getInt("total_deaths"));
        stats.setBowKills(rs.getInt("bow_kills"));
        stats.setMeleeKills(rs.getInt("melee_kills"));
        
        // Parse rank from string
        try {
            String rankStr = rs.getString("current_rank");
            stats.setCurrentRank(Rank.valueOf(rankStr));
        } catch (IllegalArgumentException e) {
            // Default to NOVATO if rank is invalid
            stats.setCurrentRank(Rank.NOVATO);
        }
        
        stats.setGamesPlayed(rs.getInt("games_played"));
        stats.setGamesWon(rs.getInt("games_won"));
        stats.setLongestKillStreak(rs.getInt("longest_kill_streak"));
        stats.setTotalArrowsFired(rs.getInt("total_arrows_fired"));
        stats.setTotalArrowsHit(rs.getInt("total_arrows_hit"));
        stats.setTotalDamageDealt(rs.getDouble("total_damage_dealt"));
        stats.setPowerupsCollected(rs.getInt("powerups_collected"));
        stats.setTotalPlaytime(rs.getLong("total_playtime"));
        stats.setFirstJoin(rs.getTimestamp("first_join"));
        stats.setLastSeen(rs.getTimestamp("last_seen"));
        stats.setActive(rs.getBoolean("is_active"));
        
        return stats;
    }
    
    /**
     * Get database connection pool statistics
     */
    public String getPoolStats() {
        if (dataSource == null) {
            return "DataSource not initialized";
        }
        
        return String.format("Pool Stats - Active: %d, Idle: %d, Total: %d",
                           dataSource.getHikariPoolMXBean().getActiveConnections(),
                           dataSource.getHikariPoolMXBean().getIdleConnections(),
                           dataSource.getHikariPoolMXBean().getTotalConnections());
    }
    
    /**
     * Close the database connection pool
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Logger.info("Database connection pool closed");
        }
        initialized = false;
    }
    
    /**
     * Check if database is initialized and ready
     */
    public boolean isInitialized() {
        return initialized && dataSource != null && !dataSource.isClosed();
    }
}
