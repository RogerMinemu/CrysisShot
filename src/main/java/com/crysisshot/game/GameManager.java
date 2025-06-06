package com.crysisshot.game;

import com.crysisshot.CrysisShot;
import com.crysisshot.database.DatabaseManager;
import com.crysisshot.localization.MessageManager;
import com.crysisshot.models.GamePlayer;
import com.crysisshot.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all game sessions and player interactions
 * Handles player join/leave, session creation, and global game state
 */
public class GameManager {
    
    private final CrysisShot plugin;
    private final DatabaseManager databaseManager;
    private final MessageManager messageManager;
    
    // Active game sessions
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    
    // Player management
    private final Map<UUID, GamePlayer> activePlayers = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerSessions = new ConcurrentHashMap<>();
    
    // Player restoration data (for when they leave games)
    private final Map<UUID, PlayerRestoreData> restoreData = new ConcurrentHashMap<>();
    
    // Cleanup task
    private BukkitTask cleanupTask;
    
    public GameManager(CrysisShot plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.messageManager = plugin.getMessageManager();
        
        startCleanupTask();
        Logger.info("GameManager initialized successfully");
    }
    
    /**
     * Shutdown the game manager and cleanup all sessions
     */
    public void shutdown() {
        Logger.info("Shutting down GameManager...");
        
        // Stop cleanup task
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        
        // End all active sessions
        for (GameSession session : sessions.values()) {
            session.endGame("Plugin shutting down");
        }
        sessions.clear();
        
        // Restore all active players
        for (UUID playerId : activePlayers.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                removePlayerFromGame(player, false);
            }
        }
        
        activePlayers.clear();
        playerSessions.clear();
        restoreData.clear();
        
        Logger.info("GameManager shutdown complete");
    }
      /**
     * Create a new game session
     */
    public GameSession createSession(String sessionId, String arenaName) {
        if (sessions.containsKey(sessionId)) {
            throw new IllegalArgumentException("Session with ID '" + sessionId + "' already exists");
        }
        
        GameSession session = new GameSession(sessionId, plugin, arenaName);
        sessions.put(sessionId, session);
        
        Logger.info("Created new game session: " + sessionId + " (Arena: " + arenaName + ")");
        return session;
    }
    
    /**
     * Remove a game session
     */
    public void removeSession(String sessionId) {
        GameSession session = sessions.remove(sessionId);
        if (session != null) {
            Logger.info("Removed game session: " + sessionId);
        }
    }
    
    /**
     * Get a game session by ID
     */
    public GameSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * Get all active sessions
     */
    public Map<String, GameSession> getAllSessions() {
        return new HashMap<>(sessions);
    }
      /**
     * Add a player to a game session
     */
    public boolean addPlayerToGame(Player player, String sessionId) {
        GameSession session = sessions.get(sessionId);
        if (session == null) {
            messageManager.sendMessage(player, "error.session-not-found");
            return false;
        }
        
        // Check if player is already in a game
        if (isPlayerInGame(player)) {
            messageManager.sendMessage(player, "error.already-in-game");
            return false;
        }
        
        // Store player's current state for restoration
        storePlayerState(player);
        
        // Try to add player to session (GameSession will create GamePlayer internally)
        if (session.addPlayer(player)) {
            activePlayers.put(player.getUniqueId(), session.getPlayers().get(player.getUniqueId()));
            playerSessions.put(player.getUniqueId(), sessionId);
            
            Logger.info("Player " + player.getName() + " joined session: " + sessionId);
            return true;
        } else {
            // Failed to add - restore player state
            restorePlayerState(player);
            return false;
        }
    }
      /**
     * Remove a player from their current game
     */
    public boolean removePlayerFromGame(Player player, boolean voluntary) {
        UUID playerId = player.getUniqueId();
        
        if (!isPlayerInGame(player)) {
            return false;
        }
        
        String sessionId = playerSessions.get(playerId);
        GameSession session = sessions.get(sessionId);
        GamePlayer gamePlayer = activePlayers.get(playerId);
        
        if (session != null && gamePlayer != null) {
            // Remove from session using the player's UUID
            session.removePlayer(playerId);
            
            // Update statistics if game was in progress
            if (session.getCurrentState() == GameSession.GameState.ACTIVE) {
                updatePlayerStats(gamePlayer, false); // Not a win
            }
        }
        
        // Clean up local references
        activePlayers.remove(playerId);
        playerSessions.remove(playerId);
        
        // Restore player state
        restorePlayerState(player);
        
        Logger.info("Player " + player.getName() + " left session: " + sessionId);
        return true;
    }
    
    /**
     * Check if a player is currently in a game
     */
    public boolean isPlayerInGame(Player player) {
        return activePlayers.containsKey(player.getUniqueId());
    }
    
    /**
     * Get the GamePlayer instance for a player
     */
    public GamePlayer getGamePlayer(Player player) {
        return activePlayers.get(player.getUniqueId());
    }
    
    /**
     * Get the session ID for a player
     */
    public String getPlayerSession(Player player) {
        return playerSessions.get(player.getUniqueId());
    }
    
    /**
     * Store player's current state before joining a game
     */
    private void storePlayerState(Player player) {
        PlayerRestoreData data = new PlayerRestoreData();
        data.gameMode = player.getGameMode();
        data.health = player.getHealth();
        data.foodLevel = player.getFoodLevel();
        data.experience = player.getExp();
        data.level = player.getLevel();
        data.location = player.getLocation().clone();
        data.inventory = player.getInventory().getContents().clone();
        data.armor = player.getInventory().getArmorContents().clone();
        data.potionEffects = player.getActivePotionEffects().toArray(new PotionEffect[0]);
        
        restoreData.put(player.getUniqueId(), data);
    }
    
    /**
     * Restore player's state after leaving a game
     */
    private void restorePlayerState(Player player) {
        PlayerRestoreData data = restoreData.remove(player.getUniqueId());
        if (data == null) return;
        
        // Clear current state
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getActivePotionEffects().forEach(effect -> 
            player.removePotionEffect(effect.getType()));
        
        // Restore saved state
        player.setGameMode(data.gameMode);
        player.setHealth(data.health);
        player.setFoodLevel(data.foodLevel);
        player.setExp(data.experience);
        player.setLevel(data.level);
        player.teleport(data.location);
        player.getInventory().setContents(data.inventory);
        player.getInventory().setArmorContents(data.armor);
          for (PotionEffect effect : data.potionEffects) {
            player.addPotionEffect(effect);
        }
    }
    
    /**
     * Update player statistics after a game
     */
    public void updatePlayerStats(GamePlayer gamePlayer, boolean won) {
        try {
            // TODO: Implement full stats update in Step 2.2 when database methods are available
            // For now, just log the game completion
            Logger.info("Game completed for " + gamePlayer.getPlayerName() + 
                       " - Won: " + won + 
                       " - Kills: " + gamePlayer.getSessionKills() + 
                       " - Deaths: " + gamePlayer.getSessionDeaths());
            
            // Basic stats tracking will be added when database interface is expanded
            
        } catch (Exception e) {
            Logger.severe("Failed to update player stats for " + gamePlayer.getPlayerName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Start the cleanup task for inactive sessions
     */
    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupInactiveSessions();
            }
        }.runTaskTimer(plugin, 20 * 60, 20 * 60); // Run every minute
    }    /**
     * Clean up inactive sessions
     */
    private void cleanupInactiveSessions() {
        // Clean up empty waiting sessions
        sessions.entrySet().removeIf(entry -> {
            GameSession session = entry.getValue();
            // TODO: Add session creation time tracking in Step 2.2
            // For now, clean up sessions based on state and activity
            if (session.getCurrentState() == GameSession.GameState.WAITING && 
                session.getPlayerCount() == 0) {
                
                Logger.info("Cleaning up empty session: " + entry.getKey());
                session.endGame("Session cleanup");
                return true;
            }
            return false;
        });
    }
    
    /**
     * Get plugin instance
     */
    public CrysisShot getPlugin() {
        return plugin;
    }
    
    /**
     * Get database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * Get message manager
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    /**
     * Data class for storing player state
     */
    private static class PlayerRestoreData {
        GameMode gameMode;
        double health;
        int foodLevel;
        float experience;
        int level;
        Location location;
        ItemStack[] inventory;
        ItemStack[] armor;
        PotionEffect[] potionEffects;
    }
}
