package com.crysisshot.scoring;

import com.crysisshot.CrysisShot;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Handles score calculations and base point awards
 */
public class ScoreCalculator {
    
    private final FileConfiguration config;
    
    public ScoreCalculator(CrysisShot plugin) {
        this.config = plugin.getConfigManager().getConfig();
    }
      /**
     * Get base points for different kill types
     */
    public int getBasePointsForKill(KillType killType) {
        return switch (killType) {
            case BOW_KILL -> config.getInt("scoring.points.bow-kill", 100);
            case MELEE_KILL -> config.getInt("scoring.points.melee-kill", 150);
            case HEADSHOT -> config.getInt("scoring.points.headshot", 200);
            case LONG_RANGE -> config.getInt("scoring.points.long-range", 175);
            case CLOSE_RANGE -> config.getInt("scoring.points.close-range", 125);
            case REVENGE_KILL -> config.getInt("scoring.points.revenge", 120);
            case ENVIRONMENTAL -> 0; // No points for environmental kills
        };
    }
    
    /**
     * Calculate final score with multiplier
     */
    public int calculateFinalScore(int basePoints, double multiplier) {
        return (int) Math.round(basePoints * multiplier);
    }
    
    /**
     * Get win condition score
     */
    public int getWinScore() {
        return config.getInt("game.win-condition.score", 1000);
    }
    
    /**
     * Get points for game placement
     */
    public int getPlacementPoints(int placement, int totalPlayers) {
        return switch (placement) {
            case 1 -> config.getInt("scoring.placement.first", 300);
            case 2 -> config.getInt("scoring.placement.second", 200);
            case 3 -> config.getInt("scoring.placement.third", 100);
            default -> Math.max(0, config.getInt("scoring.placement.participation", 50) - (placement - 4) * 10);
        };
    }
    
    /**
     * Calculate accuracy bonus points
     */
    public int calculateAccuracyBonus(int arrowsHit, int arrowsFired) {
        if (arrowsFired == 0) return 0;
        
        double accuracy = (double) arrowsHit / arrowsFired;
        int baseBonus = config.getInt("scoring.bonus.accuracy-base", 50);
        
        if (accuracy >= 0.8) return baseBonus * 3; // Excellent accuracy
        if (accuracy >= 0.6) return baseBonus * 2; // Good accuracy
        if (accuracy >= 0.4) return baseBonus;     // Decent accuracy
        
        return 0; // Poor accuracy, no bonus
    }
}
