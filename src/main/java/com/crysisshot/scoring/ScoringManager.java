package com.crysisshot.scoring;

import com.crysisshot.CrysisShot;
import com.crysisshot.config.ConfigManager;
import com.crysisshot.game.GameSession;
import com.crysisshot.localization.MessageManager;
import com.crysisshot.models.GamePlayer;
import com.crysisshot.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages scoring, kill streaks, combos, and win conditions
 */
public class ScoringManager {
    
    private final CrysisShot plugin;
    private final ConfigManager config;
    private final MessageManager messages;
    
    public ScoringManager(CrysisShot plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.messages = plugin.getMessageManager();
    }
    
    /**
     * Process a kill and award points with multipliers
     */
    public void processKill(GameSession session, GamePlayer killer, GamePlayer victim, KillType killType) {
        if (session == null || killer == null || victim == null) {
            return;
        }
        
        // Base score award
        int basePoints = getBasePointsForKill(killType);
        
        // Calculate kill streak and combo multiplier
        killer.incrementKillStreak();
        int currentStreak = killer.getKillStreak();
        
        // Get combo multiplier based on kill streak
        double multiplier = getComboMultiplier(currentStreak);
        
        // Calculate final score
        int finalScore = (int) Math.round(basePoints * multiplier);
        
        // Award points
        killer.addScore(finalScore);
        
        // Reset victim's kill streak
        victim.resetKillStreak();
        
        // Log the kill
        Logger.info(String.format("Kill processed: %s killed %s (%s) - %d points (x%.1f multiplier, %d streak)", 
            killer.getPlayer().getName(), victim.getPlayer().getName(), 
            killType.name(), finalScore, multiplier, currentStreak));
        
        // Send messages and effects
        sendKillMessages(session, killer, victim, finalScore, multiplier, currentStreak, killType);
        
        // Check for game end condition
        checkWinCondition(session, killer);
        
        // Update session statistics
        session.updatePlayerStatistics(killer, victim);
    }
    
    /**
     * Get base points for different kill types
     */
    private int getBasePointsForKill(KillType killType) {
        switch (killType) {
            case BOW:
                return 1; // Standard bow kill
            case MELEE:
                return 1; // Melee kill (same as bow for balance)
            case ENVIRONMENTAL:
                return 0; // No points for environmental kills
            default:
                return 1;
        }
    }
    
    /**
     * Calculate combo multiplier based on kill streak
     */
    private double getComboMultiplier(int killStreak) {
        List<Integer> thresholds = config.getComboThresholds();
        List<Integer> multipliers = config.getComboMultipliers();
        
        if (thresholds.isEmpty() || multipliers.isEmpty()) {
            return 1.0; // No multipliers configured
        }
        
        // Find the highest applicable multiplier
        double multiplier = 1.0;
        for (int i = 0; i < thresholds.size() && i < multipliers.size(); i++) {
            if (killStreak >= thresholds.get(i)) {
                multiplier = multipliers.get(i);
            }
        }
        
        return multiplier;
    }
    
    /**
     * Send kill messages and effects to players
     */
    private void sendKillMessages(GameSession session, GamePlayer killer, GamePlayer victim, 
                                 int points, double multiplier, int killStreak, KillType killType) {
        
        Player killerPlayer = killer.getPlayer();
        Player victimPlayer = victim.getPlayer();
        
        if (killerPlayer == null || victimPlayer == null) {
            return;
        }
        
        // Message to killer
        String killerMessage;
        if (multiplier > 1.0) {
            killerMessage = messages.getMessage("game.kill.combo", killerPlayer)
                .replace("{victim}", victimPlayer.getName())
                .replace("{points}", String.valueOf(points))
                .replace("{multiplier}", String.format("%.1f", multiplier))
                .replace("{streak}", String.valueOf(killStreak));
        } else {
            killerMessage = messages.getMessage("game.kill.normal", killerPlayer)
                .replace("{victim}", victimPlayer.getName())
                .replace("{points}", String.valueOf(points));
        }
        killerPlayer.sendMessage(killerMessage);
        
        // Message to victim
        String victimMessage = messages.getMessage("game.death.killed", victimPlayer)
            .replace("{killer}", killerPlayer.getName())
            .replace("{killer_score}", String.valueOf(killer.getScore()));
        victimPlayer.sendMessage(victimMessage);
          // Send kill streak milestones
        if (isKillStreakMilestone(killStreak)) {
            String streakMessage = messages.getMessage("game.killstreak.milestone", "en")
                .replace("{player}", killerPlayer.getName())
                .replace("{streak}", String.valueOf(killStreak));
            session.broadcastMessage(streakMessage);
        }
        
        // Play sound effects
        killerPlayer.playSound(killerPlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
        victimPlayer.playSound(victimPlayer.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);
        
        // Special effects for combo kills
        if (multiplier > 1.0) {
            killerPlayer.playSound(killerPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            // TODO: Add particle effects for combo kills
        }
    }
    
    /**
     * Check if a kill streak number is a milestone worth announcing
     */
    private boolean isKillStreakMilestone(int killStreak) {
        List<Integer> thresholds = config.getComboThresholds();
        return thresholds.contains(killStreak) || killStreak % 5 == 0; // Announce every 5 kills too
    }
    
    /**
     * Check if the game should end based on win conditions
     */
    private void checkWinCondition(GameSession session, GamePlayer potentialWinner) {
        int targetScore = config.getTargetScore();
        
        if (potentialWinner.getScore() >= targetScore) {
            // We have a winner!
            endGame(session, potentialWinner);
        }
    }
    
    /**
     * End the game with a winner
     */
    private void endGame(GameSession session, GamePlayer winner) {
        if (session.getState() != GameSession.GameState.ACTIVE) {
            return; // Game already ending or ended
        }
        
        session.setState(GameSession.GameState.ENDING);
        
        // Announce winner
        String winMessage = messages.getMessage("game.winner.announced", "en")
            .replace("{winner}", winner.getPlayer().getName())
            .replace("{score}", String.valueOf(winner.getScore()));
        session.broadcastMessage(winMessage);
        
        // Generate final leaderboard
        generateFinalLeaderboard(session);
        
        // Schedule game end tasks
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            finalizeGameEnd(session);
        }, 100L); // 5 second delay for players to see results
    }
    
    /**
     * Generate and display final leaderboard
     */
    private void generateFinalLeaderboard(GameSession session) {        List<GamePlayer> sortedPlayers = session.getPlayersCollection().stream()
            .sorted(Comparator.comparingInt(GamePlayer::getScore).reversed())
            .collect(Collectors.toList());
          // Broadcast leaderboard header
        String headerMessage = messages.getMessage("game.leaderboard.header", "en");
        session.broadcastMessage(headerMessage);
        
        // Show top players
        for (int i = 0; i < Math.min(sortedPlayers.size(), 5); i++) {
            GamePlayer player = sortedPlayers.get(i);
            String position = getPositionString(i + 1);
            
            String playerEntry = messages.getMessage("game.leaderboard.entry", "en")
                .replace("{position}", position)
                .replace("{player}", player.getPlayer().getName())
                .replace("{score}", String.valueOf(player.getScore()))
                .replace("{kills}", String.valueOf(player.getKills()))
                .replace("{deaths}", String.valueOf(player.getDeaths()));
            
            session.broadcastMessage(playerEntry);
        }
          // Play victory/defeat sounds
        Player winner = sortedPlayers.get(0).getPlayer();
        for (GamePlayer gamePlayer : session.getPlayersCollection()) {
            Player player = gamePlayer.getPlayer();
            if (player != null) {
                if (player.equals(winner)) {
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }
        }
    }
    
    /**
     * Get position string (1st, 2nd, 3rd, etc.)
     */
    private String getPositionString(int position) {
        switch (position) {
            case 1: return "1st";
            case 2: return "2nd";
            case 3: return "3rd";
            default: return position + "th";
        }
    }
    
    /**
     * Finalize game end and clean up
     */
    private void finalizeGameEnd(GameSession session) {
        // Save player statistics to database
        savePlayerStatistics(session);
        
        // Reset session state
        session.setState(GameSession.GameState.RESETTING);
          // Restore players and remove from session
        for (GamePlayer gamePlayer : session.getPlayersCollection()) {
            Player player = gamePlayer.getPlayer();
            if (player != null) {
                // Thank you message
                String thankYouMessage = messages.getMessage("game.end.thankyou", player);
                player.sendMessage(thankYouMessage);
                  // Remove from session (this will restore player state)
                plugin.getGameManager().removePlayerFromGame(player, false);
            }
        }
        
        // Clear session
        session.getPlayers().clear();
        session.setState(GameSession.GameState.WAITING);
        
        Logger.info("Game session ended and cleaned up");
    }
    
    /**
     * Save player statistics to database
     */
    private void savePlayerStatistics(GameSession session) {
        // TODO: Implement database saving when DatabaseManager is integrated
        // For now, just log the statistics
        Logger.info("Saving player statistics for session " + session.getSessionId());
        
        for (GamePlayer gamePlayer : session.getPlayersCollection()) {
            Logger.info(String.format("Player %s: Score=%d, Kills=%d, Deaths=%d, Arrows=%d", 
                gamePlayer.getPlayer().getName(),
                gamePlayer.getScore(),
                gamePlayer.getKills(),
                gamePlayer.getDeaths(),
                gamePlayer.getArrowsUsed()));
        }
    }
    
    /**
     * Get current leaderboard for a session
     */
    public List<GamePlayer> getLeaderboard(GameSession session) {        return session.getPlayersCollection().stream()
            .sorted(Comparator.comparingInt(GamePlayer::getScore).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Types of kills for scoring purposes
     */
    public enum KillType {
        BOW,
        MELEE,
        ENVIRONMENTAL
    }
}
