package com.crysisshot.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a player within a CrysisShot game session
 * Tracks all game-specific player state and statistics
 */
public class GamePlayer {
    
    private final UUID playerId;
    private final Player bukkitPlayer;
    private String playerName;
    
    // Game state
    private GamePlayerState state;
    private int currentScore;
    private int killStreak;
    private int arrows;
    private boolean hasSword;
    private boolean hasShield;
    private long lastKillTime;
    
    // Game statistics (session-specific)
    private int sessionKills;
    private int sessionDeaths;
    private int sessionArrowsFired;
    private int sessionArrowsHit;
    private double sessionDamageDealt;
    private int sessionPowerupsCollected;
    
    // Power-up effects
    private boolean hasSpeedBoost;
    private boolean hasInvisibility;
    private boolean hasScoreMultiplier;
    private long speedBoostEndTime;
    private long invisibilityEndTime;
    private long scoreMultiplierEndTime;
    private double currentMultiplier = 1.0;
    
    // Inventory management
    private ItemStack[] originalInventory;
    private ItemStack[] originalArmor;
    private Location originalLocation;
    private int originalExp;
    private float originalExhaustion;
    private float originalSaturation;
    private int originalFoodLevel;
    
    // Respawn management
    private boolean isRespawning;
    private long respawnTime;
    private Location respawnLocation;
    
    /**
     * Player states within a game
     */
    public enum GamePlayerState {
        WAITING,      // In lobby/queue
        PLAYING,      // Active in game
        SPECTATING,   // Spectating after death/elimination
        RESPAWNING,   // Waiting to respawn
        ELIMINATED    // Permanently out of current game
    }
    
    public GamePlayer(Player bukkitPlayer) {
        this.playerId = bukkitPlayer.getUniqueId();
        this.bukkitPlayer = bukkitPlayer;
        this.playerName = bukkitPlayer.getName();
        this.state = GamePlayerState.WAITING;
        
        // Initialize game stats
        resetSessionStats();
        
        // Initialize inventory state
        this.arrows = 1; // Start with 1 arrow as per game rules
        this.hasSword = true; // Always have sword
        this.hasShield = false;
        
        // Store original player state
        storeOriginalState();
    }
    
    /**
     * Reset all session-specific statistics
     */
    public void resetSessionStats() {
        this.currentScore = 0;
        this.killStreak = 0;
        this.sessionKills = 0;
        this.sessionDeaths = 0;
        this.sessionArrowsFired = 0;
        this.sessionArrowsHit = 0;
        this.sessionDamageDealt = 0.0;
        this.sessionPowerupsCollected = 0;
        this.lastKillTime = 0;
        
        // Reset power-up effects
        clearAllPowerups();
    }
    
    /**
     * Store the player's original state before joining game
     */
    private void storeOriginalState() {
        this.originalInventory = bukkitPlayer.getInventory().getContents().clone();
        this.originalArmor = bukkitPlayer.getInventory().getArmorContents().clone();
        this.originalLocation = bukkitPlayer.getLocation().clone();
        this.originalExp = bukkitPlayer.getTotalExperience();
        this.originalExhaustion = bukkitPlayer.getExhaustion();
        this.originalSaturation = bukkitPlayer.getSaturation();
        this.originalFoodLevel = bukkitPlayer.getFoodLevel();
    }
    
    /**
     * Restore the player's original state when leaving game
     */
    public void restoreOriginalState() {
        if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
            bukkitPlayer.getInventory().setContents(originalInventory);
            bukkitPlayer.getInventory().setArmorContents(originalArmor);
            bukkitPlayer.teleport(originalLocation);
            bukkitPlayer.setTotalExperience(originalExp);
            bukkitPlayer.setExhaustion(originalExhaustion);
            bukkitPlayer.setSaturation(originalSaturation);
            bukkitPlayer.setFoodLevel(originalFoodLevel);
            bukkitPlayer.setHealth(bukkitPlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            
            // Clear any remaining potion effects
            bukkitPlayer.getActivePotionEffects().forEach(effect -> 
                bukkitPlayer.removePotionEffect(effect.getType()));
        }
    }
    
    /**
     * Record a kill by this player
     */
    public void recordKill(GamePlayer victim, boolean wasArrowKill) {
        sessionKills++;
        killStreak++;
        lastKillTime = System.currentTimeMillis();
        
        // Calculate score with multiplier
        int baseScore = 1;
        int scoreGained = (int) (baseScore * currentMultiplier);
        currentScore += scoreGained;
        
        // Arrow management for bow kills
        if (wasArrowKill) {
            arrows++; // Recover arrow on kill
            sessionArrowsHit++;
        }
    }
    
    /**
     * Record a death of this player
     */
    public void recordDeath(GamePlayer killer) {
        sessionDeaths++;
        killStreak = 0; // Reset kill streak on death
        arrows = 1; // Reset to 1 arrow on respawn
        hasShield = false; // Lose shield on death
        
        // Clear power-up effects on death
        clearAllPowerups();
        
        // Set respawning state
        setState(GamePlayerState.RESPAWNING);
        isRespawning = true;
    }
    
    /**
     * Record an arrow being fired
     */
    public void recordArrowFired() {
        if (arrows > 0) {
            arrows--;
            sessionArrowsFired++;
        }
    }
    
    /**
     * Record damage dealt to another player
     */
    public void recordDamage(double damage) {
        sessionDamageDealt += damage;
    }
    
    /**
     * Apply a power-up effect to this player
     */
    public void applyPowerup(PowerUpType powerup, long durationMs) {
        sessionPowerupsCollected++;
        long currentTime = System.currentTimeMillis();
        
        switch (powerup) {
            case SPEED_BOOST:
                hasSpeedBoost = true;
                speedBoostEndTime = currentTime + durationMs;
                break;
            case INVISIBILITY:
                hasInvisibility = true;
                invisibilityEndTime = currentTime + durationMs;
                break;
            case EXTRA_ARROW:
                arrows++; // Immediate effect
                break;
            case SHIELD:
                hasShield = true; // Permanent until used
                break;
            case SCORE_MULTIPLIER:
                hasScoreMultiplier = true;
                scoreMultiplierEndTime = currentTime + durationMs;
                currentMultiplier = 2.0; // 2x score multiplier
                break;
        }
    }
    
    /**
     * Check and remove expired power-up effects
     */
    public void updatePowerupEffects() {
        long currentTime = System.currentTimeMillis();
        
        if (hasSpeedBoost && currentTime >= speedBoostEndTime) {
            hasSpeedBoost = false;
        }
        
        if (hasInvisibility && currentTime >= invisibilityEndTime) {
            hasInvisibility = false;
        }
        
        if (hasScoreMultiplier && currentTime >= scoreMultiplierEndTime) {
            hasScoreMultiplier = false;
            currentMultiplier = 1.0;
        }
    }
    
    /**
     * Clear all power-up effects
     */
    public void clearAllPowerups() {
        hasSpeedBoost = false;
        hasInvisibility = false;
        hasScoreMultiplier = false;
        hasShield = false;
        currentMultiplier = 1.0;
        speedBoostEndTime = 0;
        invisibilityEndTime = 0;
        scoreMultiplierEndTime = 0;
    }
    
    /**
     * Calculate kill/death ratio
     */
    public double getKDRatio() {
        if (sessionDeaths == 0) {
            return sessionKills > 0 ? sessionKills : 0.0;
        }
        return (double) sessionKills / sessionDeaths;
    }
    
    /**
     * Calculate arrow accuracy percentage
     */
    public double getArrowAccuracy() {
        if (sessionArrowsFired == 0) {
            return 0.0;
        }
        return ((double) sessionArrowsHit / sessionArrowsFired) * 100.0;
    }
    
    /**
     * Check if player has any active power-ups
     */
    public boolean hasActivePowerups() {
        updatePowerupEffects(); // Update first
        return hasSpeedBoost || hasInvisibility || hasScoreMultiplier || hasShield;
    }
    
    // Getters and Setters
    public UUID getPlayerId() { return playerId; }
    public Player getBukkitPlayer() { return bukkitPlayer; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public GamePlayerState getState() { return state; }
    public void setState(GamePlayerState state) { this.state = state; }
    
    public int getCurrentScore() { return currentScore; }
    public void setCurrentScore(int currentScore) { this.currentScore = currentScore; }
    
    public int getKillStreak() { return killStreak; }
    public void setKillStreak(int killStreak) { this.killStreak = killStreak; }
    
    public int getArrows() { return arrows; }
    public void setArrows(int arrows) { this.arrows = arrows; }
    
    public boolean hasSword() { return hasSword; }
    public void setHasSword(boolean hasSword) { this.hasSword = hasSword; }
    
    public boolean hasShield() { return hasShield; }
    public void setHasShield(boolean hasShield) { this.hasShield = hasShield; }
    
    public long getLastKillTime() { return lastKillTime; }
    
    // Session statistics getters
    public int getSessionKills() { return sessionKills; }
    public int getSessionDeaths() { return sessionDeaths; }
    public int getSessionArrowsFired() { return sessionArrowsFired; }
    public int getSessionArrowsHit() { return sessionArrowsHit; }
    public double getSessionDamageDealt() { return sessionDamageDealt; }
    public int getSessionPowerupsCollected() { return sessionPowerupsCollected; }
    
    // Session statistics getters for external use
    public int getKills() { return sessionKills; }
    public int getDeaths() { return sessionDeaths; }
    public int getArrowsShot() { return sessionArrowsFired; }
    public int getArrowsHit() { return sessionArrowsHit; }
    
    // Power-up effect getters
    public boolean hasSpeedBoost() { return hasSpeedBoost; }
    public boolean hasInvisibility() { return hasInvisibility; }
    public boolean hasScoreMultiplier() { return hasScoreMultiplier; }
    public double getCurrentMultiplier() { return currentMultiplier; }
    
    // Respawn management
    public boolean isRespawning() { return isRespawning; }
    public void setRespawning(boolean respawning) { isRespawning = respawning; }
    public long getRespawnTime() { return respawnTime; }
    public void setRespawnTime(long respawnTime) { this.respawnTime = respawnTime; }
    public Location getRespawnLocation() { return respawnLocation; }
    public void setRespawnLocation(Location respawnLocation) { this.respawnLocation = respawnLocation; }
    
    /**
     * Power-up types available in the game
     */
    public enum PowerUpType {
        SPEED_BOOST,
        INVISIBILITY,
        EXTRA_ARROW,
        SHIELD,
        SCORE_MULTIPLIER
    }
}
