package com.crysisshot.scoring;

import com.crysisshot.CrysisShot;
import com.crysisshot.game.GameSession;
import com.crysisshot.localization.MessageManager;
import com.crysisshot.models.GamePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Handles kill streaks, combos, and related multipliers
 */
public class ComboSystem {
    
    private final FileConfiguration config;
    private final MessageManager messages;
    
    public ComboSystem(CrysisShot plugin) {
        this.config = plugin.getConfigManager().getConfig();
        this.messages = plugin.getMessageManager();
    }
    
    /**
     * Get combo multiplier based on kill streak
     */
    public double getComboMultiplier(int killStreak) {
        if (killStreak >= 10) return config.getDouble("scoring.combo.legendary", 3.0);
        if (killStreak >= 7) return config.getDouble("scoring.combo.epic", 2.5);
        if (killStreak >= 5) return config.getDouble("scoring.combo.great", 2.0);
        if (killStreak >= 3) return config.getDouble("scoring.combo.good", 1.5);
        return 1.0; // No combo
    }
    
    /**
     * Get combo name for display
     */
    public String getComboName(int killStreak) {
        if (killStreak >= 10) return "legendary";
        if (killStreak >= 7) return "epic";
        if (killStreak >= 5) return "great";
        if (killStreak >= 3) return "good";
        return "none";
    }
      /**
     * Handle kill streak announcements and effects
     */
    public void handleKillStreak(GameSession session, GamePlayer killer, int killStreak) {
        Player player = killer.getBukkitPlayer();
        if (player == null || session == null) return;
        
        String comboName = getComboName(killStreak);
        if (!"none".equals(comboName)) {
            // Send combo message
            String comboMessage = messages.getMessage("game.combo." + comboName, player,
                "{player}", player.getName(),
                "{streak}", String.valueOf(killStreak));
            
            // Play combo sound
            Sound comboSound = getComboSound(comboName);
            if (comboSound != null) {
                player.playSound(player.getLocation(), comboSound, 1.0f, 1.0f);
            }
            
            // Broadcast combo achievement
            session.broadcastMessage(comboMessage);
        }
    }
    
    /**
     * Reset kill streak and handle streak end
     */
    public void resetKillStreak(GameSession session, GamePlayer player) {
        int previousStreak = player.getKillStreak();
        player.resetKillStreak();
        
        // Handle streak end announcement if it was significant
        if (previousStreak >= 5 && session != null) {
            Player bukkitPlayer = player.getBukkitPlayer();
            if (bukkitPlayer != null) {
                String streakEndMessage = messages.getMessage("game.streak.ended", bukkitPlayer,
                    "{player}", bukkitPlayer.getName(),
                    "{streak}", String.valueOf(previousStreak));
                
                session.broadcastMessage(streakEndMessage);
            }
        }
    }
    
    /**
     * Get appropriate sound for combo level
     */
    private Sound getComboSound(String comboName) {
        return switch (comboName) {
            case "legendary" -> Sound.ENTITY_ENDER_DRAGON_GROWL;
            case "epic" -> Sound.ENTITY_WITHER_SPAWN;
            case "great" -> Sound.ENTITY_PLAYER_LEVELUP;
            case "good" -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            default -> null;
        };
    }
}
