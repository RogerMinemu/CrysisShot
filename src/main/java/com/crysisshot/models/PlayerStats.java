package com.crysisshot.models;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Data model for player statistics in CrysisShot
 * Represents all statistical data tracked for a player
 */
public class PlayerStats {
    
    private UUID playerId;
    private String playerName;
    private int totalKills;
    private int totalDeaths;
    private int gamesPlayed;
    private int gamesWon;
    private int longestKillStreak;
    private int totalArrowsFired;
    private int totalArrowsHit;
    private double totalDamageDealt;
    private int powerupsCollected;
    private long totalPlaytime; // in seconds
    private Timestamp firstJoin;
    private Timestamp lastSeen;
    private boolean isActive;
    
    // Default constructor
    public PlayerStats() {}
    
    // Constructor with UUID and name
    public PlayerStats(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.totalKills = 0;
        this.totalDeaths = 0;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.longestKillStreak = 0;
        this.totalArrowsFired = 0;
        this.totalArrowsHit = 0;
        this.totalDamageDealt = 0.0;
        this.powerupsCollected = 0;
        this.totalPlaytime = 0;
        this.firstJoin = new Timestamp(System.currentTimeMillis());
        this.lastSeen = new Timestamp(System.currentTimeMillis());
        this.isActive = true;
    }
    
    // Getters
    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getTotalKills() { return totalKills; }
    public int getTotalDeaths() { return totalDeaths; }
    public int getGamesPlayed() { return gamesPlayed; }
    public int getGamesWon() { return gamesWon; }
    public int getLongestKillStreak() { return longestKillStreak; }
    public int getTotalArrowsFired() { return totalArrowsFired; }
    public int getTotalArrowsHit() { return totalArrowsHit; }
    public double getTotalDamageDealt() { return totalDamageDealt; }
    public int getPowerupsCollected() { return powerupsCollected; }
    public long getTotalPlaytime() { return totalPlaytime; }
    public Timestamp getFirstJoin() { return firstJoin; }
    public Timestamp getLastSeen() { return lastSeen; }
    public boolean isActive() { return isActive; }
    
    // Setters
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public void setTotalKills(int totalKills) { this.totalKills = totalKills; }
    public void setTotalDeaths(int totalDeaths) { this.totalDeaths = totalDeaths; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }
    public void setGamesWon(int gamesWon) { this.gamesWon = gamesWon; }
    public void setLongestKillStreak(int longestKillStreak) { this.longestKillStreak = longestKillStreak; }
    public void setTotalArrowsFired(int totalArrowsFired) { this.totalArrowsFired = totalArrowsFired; }
    public void setTotalArrowsHit(int totalArrowsHit) { this.totalArrowsHit = totalArrowsHit; }
    public void setTotalDamageDealt(double totalDamageDealt) { this.totalDamageDealt = totalDamageDealt; }
    public void setPowerupsCollected(int powerupsCollected) { this.powerupsCollected = powerupsCollected; }
    public void setTotalPlaytime(long totalPlaytime) { this.totalPlaytime = totalPlaytime; }
    public void setFirstJoin(Timestamp firstJoin) { this.firstJoin = firstJoin; }
    public void setLastSeen(Timestamp lastSeen) { this.lastSeen = lastSeen; }
    public void setActive(boolean active) { this.isActive = active; }
    
    // Calculated getters
    public double getKillDeathRatio() {
        return totalDeaths == 0 ? totalKills : (double) totalKills / totalDeaths;
    }
    
    public double getWinRate() {
        return gamesPlayed == 0 ? 0.0 : (double) gamesWon / gamesPlayed * 100;
    }
    
    public double getAccuracy() {
        return totalArrowsFired == 0 ? 0.0 : (double) totalArrowsHit / totalArrowsFired * 100;
    }
    
    public double getAverageDamagePerGame() {
        return gamesPlayed == 0 ? 0.0 : totalDamageDealt / gamesPlayed;
    }
    
    // Utility methods
    public void incrementKills() { this.totalKills++; }
    public void incrementDeaths() { this.totalDeaths++; }
    public void incrementGamesPlayed() { this.gamesPlayed++; }
    public void incrementGamesWon() { this.gamesWon++; }
    public void incrementArrowsFired() { this.totalArrowsFired++; }
    public void incrementArrowsHit() { this.totalArrowsHit++; }
    public void incrementPowerupsCollected() { this.powerupsCollected++; }
    
    public void addDamage(double damage) { this.totalDamageDealt += damage; }
    public void addPlaytime(long seconds) { this.totalPlaytime += seconds; }
    
    public void updateKillStreak(int streak) {
        if (streak > this.longestKillStreak) {
            this.longestKillStreak = streak;
        }
    }
    
    public void updateLastSeen() {
        this.lastSeen = new Timestamp(System.currentTimeMillis());
    }
    
    @Override
    public String toString() {
        return String.format("PlayerStats{UUID=%s, Name=%s, K/D=%.2f, Games=%d, WinRate=%.1f%%}", 
                           playerId, playerName, getKillDeathRatio(), gamesPlayed, getWinRate());
    }
}
