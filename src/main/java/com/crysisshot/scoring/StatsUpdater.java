package com.crysisshot.scoring;

import com.crysisshot.CrysisShot;
import com.crysisshot.database.DatabaseManager;
import com.crysisshot.models.GamePlayer;
import com.crysisshot.models.PlayerStats;
import com.crysisshot.ranking.RankingManager;
import com.crysisshot.utils.Logger;

/**
 * Handles updating persistent player statistics
 */
public class StatsUpdater {
    
    private final DatabaseManager databaseManager;
    private final RankingManager rankingManager;
    
    public StatsUpdater(CrysisShot plugin) {
        this.databaseManager = plugin.getDatabaseManager();
        this.rankingManager = plugin.getRankingManager();
    }
    
    /**
     * Update persistent stats after a kill
     */
    public void updateKillStats(GamePlayer killer, KillType killType) {
        if (killer == null) return;
        
        PlayerStats stats = killer.getStats();
        if (stats == null) {
            // Load stats from database if not cached
            stats = databaseManager.getPlayerStats(killer.getPlayerId());
            if (stats == null) {
                Logger.warning("No stats found for player " + killer.getPlayerId());
                return;
            }
            killer.setStats(stats);
        }
          // Update kill counts
        if (killType == KillType.BOW_KILL || killType == KillType.HEADSHOT || 
            killType == KillType.LONG_RANGE || killType == KillType.CLOSE_RANGE) {
            stats.incrementBowKills(); // This also increments total kills
            
            // Check for rank progression
            rankingManager.updatePlayerRank(killer.getPlayerId());
        } else if (killType == KillType.MELEE_KILL) {
            stats.incrementMeleeKills(); // This also increments total kills
        } else {
            stats.incrementKills(); // For other kill types
        }
        
        // Update kill streak if it's the longest
        if (killer.getKillStreak() > stats.getLongestKillStreak()) {
            stats.setLongestKillStreak(killer.getKillStreak());
        }
        
        // Save to database asynchronously
        databaseManager.savePlayerStats(stats);
    }
    
    /**
     * Update persistent stats after a death
     */
    public void updateDeathStats(GamePlayer victim) {
        if (victim == null) return;
        
        PlayerStats stats = victim.getStats();
        if (stats == null) {
            stats = databaseManager.getPlayerStats(victim.getPlayerId());
            if (stats == null) {
                Logger.warning("No stats found for player " + victim.getPlayerId());
                return;
            }
            victim.setStats(stats);
        }
          // Update death count
        stats.incrementDeaths();
        
        // Save to database asynchronously
        databaseManager.savePlayerStats(stats);
    }
    
    /**
     * Update stats for arrows fired and hit
     */
    public void updateArrowStats(GamePlayer player, boolean hit) {
        if (player == null) return;
        
        PlayerStats stats = player.getStats();
        if (stats == null) {
            stats = databaseManager.getPlayerStats(player.getPlayerId());
            if (stats == null) {
                Logger.warning("No stats found for player " + player.getPlayerId());
                return;
            }
            player.setStats(stats);
        }
          // Update arrow counts
        stats.incrementArrowsFired();
        if (hit) {
            stats.incrementArrowsHit();
        }
        
        // Save to database asynchronously
        databaseManager.savePlayerStats(stats);
    }
    
    /**
     * Update game completion stats
     */
    public void updateGameStats(GamePlayer player, boolean won) {
        if (player == null) return;
        
        PlayerStats stats = player.getStats();
        if (stats == null) {
            stats = databaseManager.getPlayerStats(player.getPlayerId());
            if (stats == null) {
                Logger.warning("No stats found for player " + player.getPlayerId());
                return;
            }
            player.setStats(stats);
        }
        
        // Update game counts
        stats.incrementGamesPlayed();
        if (won) {
            stats.incrementGamesWon();
        }
          // Update other session stats
        stats.addDamage(player.getSessionDamageDealt());
        
        // Add powerups collected to current total
        stats.setPowerupsCollected(stats.getPowerupsCollected() + player.getSessionPowerupsCollected());
        
        // Save to database asynchronously
        databaseManager.savePlayerStats(stats);
    }
}
