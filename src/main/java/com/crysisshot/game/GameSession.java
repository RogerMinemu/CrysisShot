package com.crysisshot.game;

import com.crysisshot.CrysisShot;
import com.crysisshot.config.ConfigManager;
import com.crysisshot.localization.MessageManager;
import com.crysisshot.models.GamePlayer;
import com.crysisshot.models.GamePlayer.GamePlayerState;
import com.crysisshot.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single game session with state management
 * Handles game lifecycle, player management, and state transitions
 */
public class GameSession {
    
    private final String sessionId;
    private final CrysisShot plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    
    // Game state
    private GameState currentState;
    private final Map<UUID, GamePlayer> players;
    private final List<Location> spawnPoints;
    private long gameStartTime;
    private long gameEndTime;
    private long createdTime;
    private int targetScore;
    private int maxPlayers;
    private int minPlayers;
    private long maxDuration; // in milliseconds
    
    // Game mechanics
    private GamePlayer winner;
    private final Map<UUID, Integer> playerScores;
    private BukkitTask gameTask;
    private BukkitTask countdownTask;
    private int countdownSeconds;
    
    // Arena information
    private String arenaName;
    private Location lobbyLocation;
    private List<Location> powerupSpawnLocations;
    
    /**
     * Game states for state machine management
     */
    public enum GameState {
        WAITING,    // Waiting for players to join
        STARTING,   // Countdown before game starts
        ACTIVE,     // Game is actively running
        ENDING,     // Game has ended, showing results
        RESETTING   // Cleaning up and preparing for next game
    }
    
    public GameSession(String sessionId, CrysisShot plugin, String arenaName) {
        this.sessionId = sessionId;
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.arenaName = arenaName;
        
        // Initialize collections
        this.players = new ConcurrentHashMap<>();
        this.playerScores = new ConcurrentHashMap<>();
        this.spawnPoints = new ArrayList<>();
        this.powerupSpawnLocations = new ArrayList<>();
          // Load configuration
        loadGameConfiguration();
        
        // Set initial state and creation time
        this.currentState = GameState.WAITING;
        this.createdTime = System.currentTimeMillis();
        
        Logger.info("Created new game session: " + sessionId + " for arena: " + arenaName);
    }
      /**
     * Load game configuration from config manager
     */
    private void loadGameConfiguration() {
        this.targetScore = configManager.getTargetScore();
        this.maxPlayers = configManager.getMaxPlayers();
        this.minPlayers = configManager.getMinPlayers();
        // TODO: Add these methods to ConfigManager in Step 2.2
        this.maxDuration = 15 * 60 * 1000; // Default 15 minutes
        this.countdownSeconds = 10; // Default 10 seconds countdown
    }
    
    /**
     * Add a player to the game session
     */
    public boolean addPlayer(Player bukkitPlayer) {
        if (currentState != GameState.WAITING) {
            return false; // Can only join during waiting state
        }
        
        if (players.size() >= maxPlayers) {
            return false; // Game is full
        }
        
        UUID playerId = bukkitPlayer.getUniqueId();
        if (players.containsKey(playerId)) {
            return false; // Player already in game
        }
        
        // Create GamePlayer instance
        GamePlayer gamePlayer = new GamePlayer(bukkitPlayer);
        players.put(playerId, gamePlayer);
        playerScores.put(playerId, 0);
        
        // Notify all players
        broadcastMessage("game.player-joined", 
            "player", bukkitPlayer.getName(),
            "current", String.valueOf(players.size()),
            "max", String.valueOf(maxPlayers));
        
        Logger.info("Player " + bukkitPlayer.getName() + " joined game session " + sessionId);
        
        // Check if we can start the game
        checkStartConditions();
        
        return true;
    }
    
    /**
     * Remove a player from the game session
     */
    public boolean removePlayer(UUID playerId) {
        GamePlayer gamePlayer = players.remove(playerId);
        if (gamePlayer == null) {
            return false;
        }
        
        // Restore player's original state
        gamePlayer.restoreOriginalState();
        
        // Remove from scores
        playerScores.remove(playerId);
        
        // Notify remaining players
        broadcastMessage("game.player-left",
            "player", gamePlayer.getPlayerName(),
            "current", String.valueOf(players.size()),
            "max", String.valueOf(maxPlayers));
        
        Logger.info("Player " + gamePlayer.getPlayerName() + " left game session " + sessionId);
        
        // Check if game should end due to insufficient players
        if (currentState == GameState.ACTIVE && players.size() < 2) {
            endGame("Insufficient players");
        }
        
        return true;
    }
    
    /**
     * Check if conditions are met to start the game
     */
    private void checkStartConditions() {
        if (currentState == GameState.WAITING && players.size() >= minPlayers) {
            startCountdown();
        }
    }
    
    /**
     * Start the pre-game countdown
     */
    private void startCountdown() {
        if (currentState != GameState.WAITING) {
            return;
        }
        
        setState(GameState.STARTING);
        
        // Start countdown task
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdownSeconds > 0) {
                broadcastMessage("game.countdown", "seconds", String.valueOf(countdownSeconds));
                countdownSeconds--;
            } else {
                countdownTask.cancel();
                startGame();
            }
        }, 0L, 20L); // Run every second (20 ticks)
        
        Logger.info("Started countdown for game session " + sessionId);
    }
    
    /**
     * Start the actual game
     */
    private void startGame() {
        if (currentState != GameState.STARTING) {
            return;
        }
        
        setState(GameState.ACTIVE);
        gameStartTime = System.currentTimeMillis();
        
        // Set all players to playing state
        for (GamePlayer gamePlayer : players.values()) {
            gamePlayer.setState(GamePlayerState.PLAYING);
            gamePlayer.resetSessionStats();
            
            // Teleport to random spawn point
            if (!spawnPoints.isEmpty()) {
                Location spawnPoint = spawnPoints.get(new Random().nextInt(spawnPoints.size()));
                gamePlayer.getBukkitPlayer().teleport(spawnPoint);
            }
            
            // Give starting equipment (will be handled by inventory management)
            setupPlayerInventory(gamePlayer);
        }
        
        // Start game management task
        startGameTask();
        
        // Broadcast game start
        broadcastMessage("game.started");
        
        Logger.info("Started game session " + sessionId + " with " + players.size() + " players");
    }
    
    /**
     * Setup player inventory for game
     */
    private void setupPlayerInventory(GamePlayer gamePlayer) {
        Player bukkitPlayer = gamePlayer.getBukkitPlayer();
        
        // Clear inventory
        bukkitPlayer.getInventory().clear();
        
        // TODO: Add proper item management in later steps
        // For now, just give basic items
        bukkitPlayer.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW));
        bukkitPlayer.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 1));
        bukkitPlayer.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.WOODEN_SWORD));
        
        // Set health and hunger
        bukkitPlayer.setHealth(bukkitPlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
        bukkitPlayer.setFoodLevel(20);
        bukkitPlayer.setSaturation(20);
    }
    
    /**
     * Start the main game management task
     */
    private void startGameTask() {
        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Update power-up effects for all players
            for (GamePlayer gamePlayer : players.values()) {
                gamePlayer.updatePowerupEffects();
            }
            
            // Check win conditions
            checkWinConditions();
            
            // Check time limit
            if (maxDuration > 0 && System.currentTimeMillis() - gameStartTime >= maxDuration) {
                endGame("Time limit reached");
            }
            
        }, 0L, 20L); // Run every second
    }
    
    /**
     * Check if any player has won the game
     */
    private void checkWinConditions() {
        for (GamePlayer gamePlayer : players.values()) {
            if (gamePlayer.getCurrentScore() >= targetScore) {
                winner = gamePlayer;
                endGame("Score limit reached");
                return;
            }
        }
    }
    
    /**
     * End the game
     */
    public void endGame(String reason) {
        if (currentState != GameState.ACTIVE) {
            return;
        }
        
        setState(GameState.ENDING);
        gameEndTime = System.currentTimeMillis();
        
        // Cancel game task
        if (gameTask != null) {
            gameTask.cancel();
        }
        
        // Determine winner if not already set
        if (winner == null) {
            winner = getPlayerWithHighestScore();
        }
        
        // Broadcast game end
        if (winner != null) {
            broadcastMessage("game.won",
                "winner", winner.getPlayerName(),
                "score", String.valueOf(winner.getCurrentScore()));
        } else {
            broadcastMessage("game.ended", "reason", reason);
        }
        
        // Show final statistics
        showFinalStatistics();
        
        // Schedule cleanup
        Bukkit.getScheduler().runTaskLater(plugin, this::resetGame, 100L); // 5 seconds delay
        
        Logger.info("Ended game session " + sessionId + ". Reason: " + reason);
    }
    
    /**
     * Get the player with the highest score
     */
    private GamePlayer getPlayerWithHighestScore() {
        return players.values().stream()
            .max(Comparator.comparingInt(GamePlayer::getCurrentScore))
            .orElse(null);
    }
    
    /**
     * Show final game statistics to all players
     */
    private void showFinalStatistics() {
        for (GamePlayer gamePlayer : players.values()) {
            Player bukkitPlayer = gamePlayer.getBukkitPlayer();
            
            messageManager.sendMessage(bukkitPlayer, "game.stats.header");
            messageManager.sendMessage(bukkitPlayer, "game.stats.score", 
                "score", String.valueOf(gamePlayer.getCurrentScore()));
            messageManager.sendMessage(bukkitPlayer, "game.stats.kills", 
                "kills", String.valueOf(gamePlayer.getSessionKills()));
            messageManager.sendMessage(bukkitPlayer, "game.stats.deaths", 
                "deaths", String.valueOf(gamePlayer.getSessionDeaths()));
            messageManager.sendMessage(bukkitPlayer, "game.stats.kdr", 
                "kdr", String.format("%.2f", gamePlayer.getKDRatio()));
            messageManager.sendMessage(bukkitPlayer, "game.stats.accuracy", 
                "accuracy", String.format("%.1f%%", gamePlayer.getArrowAccuracy()));
        }
    }
    
    /**
     * Reset the game for a new round
     */
    private void resetGame() {
        setState(GameState.RESETTING);
        
        // Restore all players' original states
        for (GamePlayer gamePlayer : players.values()) {
            gamePlayer.restoreOriginalState();
        }
        
        // Clear collections
        players.clear();
        playerScores.clear();
        
        // Reset game state
        winner = null;
        gameStartTime = 0;
        gameEndTime = 0;
        countdownSeconds = 10; // Default countdown duration
        
        // Return to waiting state
        setState(GameState.WAITING);
        
        Logger.info("Reset game session " + sessionId + " for new round");
    }
    
    /**
     * Handle player kill event
     */
    public void handlePlayerKill(GamePlayer killer, GamePlayer victim, boolean wasArrowKill) {
        if (currentState != GameState.ACTIVE) {
            return;
        }
        
        // Record statistics
        killer.recordKill(victim, wasArrowKill);
        victim.recordDeath(killer);
        
        // Update scores map
        playerScores.put(killer.getPlayerId(), killer.getCurrentScore());
        
        // Broadcast kill message
        String killType = wasArrowKill ? "bow" : "sword";
        broadcastMessage("game.kill-" + killType,
            "killer", killer.getPlayerName(),
            "victim", victim.getPlayerName());
        
        // Check for special kill messages
        if (killer.getKillStreak() >= 3) {
            broadcastMessage("game.combo-achieved",
                "player", killer.getPlayerName(),
                "streak", String.valueOf(killer.getKillStreak()));
        }
        
        Logger.info("Kill recorded: " + killer.getPlayerName() + " -> " + victim.getPlayerName() + 
                   " (Arrow: " + wasArrowKill + ")");
    }
    
    /**
     * Broadcast a message to all players in the game
     */
    private void broadcastMessage(String messageKey, String... placeholders) {
        for (GamePlayer gamePlayer : players.values()) {
            Player bukkitPlayer = gamePlayer.getBukkitPlayer();
            if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
                messageManager.sendMessage(bukkitPlayer, messageKey, placeholders);
            }
        }
    }
    
    /**
     * Set the game state and handle state transitions
     */
    private void setState(GameState newState) {
        GameState oldState = this.currentState;
        this.currentState = newState;
        
        Logger.debug("Game session " + sessionId + " state changed: " + oldState + " -> " + newState);
    }
    
    // Getters and state checkers
    public String getSessionId() { return sessionId; }
    public GameState getCurrentState() { return currentState; }
    public Map<UUID, GamePlayer> getPlayers() { return new HashMap<>(players); }
    public int getPlayerCount() { return players.size(); }
    public int getMaxPlayers() { return maxPlayers; }
    public int getMinPlayers() { return minPlayers; }
    public GamePlayer getWinner() { return winner; }
    public long getGameStartTime() { return gameStartTime; }
    public long getGameEndTime() { return gameEndTime; }
    public String getArenaName() { return arenaName; }
    
    public boolean isWaiting() { return currentState == GameState.WAITING; }
    public boolean isStarting() { return currentState == GameState.STARTING; }
    public boolean isActive() { return currentState == GameState.ACTIVE; }
    public boolean isEnding() { return currentState == GameState.ENDING; }
    public boolean isResetting() { return currentState == GameState.RESETTING; }
    
    public boolean canJoin() { 
        return currentState == GameState.WAITING && players.size() < maxPlayers; 
    }
    
    public boolean hasPlayer(UUID playerId) {
        return players.containsKey(playerId);
    }
    
    public GamePlayer getPlayer(UUID playerId) {
        return players.get(playerId);
    }
    
    /**
     * Additional getter methods for GameManager integration
     */
    public GameState getState() { return currentState; }
    public long getCreatedTime() { return createdTime; }
    
    /**
     * Get game duration in milliseconds
     */
    public long getGameDuration() {
        if (gameStartTime == 0) {
            return 0;
        }
        
        long endTime = gameEndTime > 0 ? gameEndTime : System.currentTimeMillis();
        return endTime - gameStartTime;
    }
    
    /**
     * Add spawn point for the arena
     */
    public void addSpawnPoint(Location location) {
        spawnPoints.add(location.clone());
    }
    
    /**
     * Set the lobby location
     */
    public void setLobbyLocation(Location location) {
        this.lobbyLocation = location.clone();
    }
    
    /**
     * Add power-up spawn location
     */
    public void addPowerupSpawnLocation(Location location) {
        powerupSpawnLocations.add(location.clone());
    }
    
    /**
     * Cleanup method for proper resource management
     */
    public void cleanup() {
        // Cancel any running tasks
        if (gameTask != null) {
            gameTask.cancel();
        }
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        
        // Restore all players
        for (GamePlayer gamePlayer : players.values()) {
            gamePlayer.restoreOriginalState();
        }
        
        Logger.info("Cleaned up game session " + sessionId);
    }
}
