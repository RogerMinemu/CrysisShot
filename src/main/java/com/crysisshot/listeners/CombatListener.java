package com.crysisshot.listeners;

import com.crysisshot.CrysisShot;
import com.crysisshot.game.GameManager;
import com.crysisshot.game.GameSession;
import com.crysisshot.localization.MessageManager;
import com.crysisshot.models.GamePlayer;
import com.crysisshot.utils.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all combat-related events for CrysisShot
 * Implements instant-kill mechanics, arrow management, and melee combat
 */
public class CombatListener implements Listener {
    
    private final CrysisShot plugin;
    private final GameManager gameManager;
    private final MessageManager messageManager;
    
    // Track arrows fired by players for proper attribution
    private final Map<UUID, UUID> arrowOwners = new HashMap<>();
    
    public CombatListener(CrysisShot plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.messageManager = plugin.getMessageManager();
    }
    
    /**
     * Handle bow shooting events
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player shooter = (Player) event.getEntity();
        
        // Only handle players in active games
        if (!gameManager.isPlayerInGame(shooter)) {
            return;
        }
        
        GamePlayer gamePlayer = gameManager.getGamePlayer(shooter);
        if (gamePlayer == null) {
            return;
        }
        
        // Check if player has arrows
        if (gamePlayer.getArrows() <= 0) {
            event.setCancelled(true);
            messageManager.sendMessage(shooter, "game.no-arrows");
            return;
        }
        
        // Consume arrow
        gamePlayer.consumeArrow();
        
        // Track arrow ownership for kill attribution
        if (event.getProjectile() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getProjectile();
            arrowOwners.put(arrow.getUniqueId(), shooter.getUniqueId());
        }
        
        // Update arrows display
        messageManager.sendMessage(shooter, "game.arrows-remaining", 
            "count", String.valueOf(gamePlayer.getArrows()));
        
        // Track statistics
        gamePlayer.incrementArrowsFired();
        
        Logger.debug("Player " + shooter.getName() + " shot arrow (" + 
                    gamePlayer.getArrows() + " remaining)");
    }
    
    /**
     * Handle projectile hits (arrows)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) {
            return;
        }
        
        Arrow arrow = (Arrow) event.getEntity();
        UUID arrowId = arrow.getUniqueId();
        UUID shooterId = arrowOwners.remove(arrowId);
        
        if (shooterId == null) {
            return; // Not a tracked arrow
        }
        
        Player shooter = plugin.getServer().getPlayer(shooterId);
        if (shooter == null) {
            return;
        }
        
        // Check if arrow hit a player
        if (event.getHitEntity() instanceof Player) {
            Player victim = (Player) event.getHitEntity();
            
            // Prevent self-damage
            if (shooter.equals(victim)) {
                return;
            }
            
            // Only handle players in the same game
            if (!gameManager.isPlayerInGame(victim) || 
                !isInSameSession(shooter, victim)) {
                return;
            }
            
            // Cancel the normal damage and apply instant kill
            event.setCancelled(true);
            handleBowKill(shooter, victim, arrow);
        }
    }
    
    /**
     * Handle melee combat (sword attacks)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onMeleeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        // Only handle players in active games
        if (!gameManager.isPlayerInGame(attacker) || !gameManager.isPlayerInGame(victim)) {
            return;
        }
        
        // Check if they're in the same session
        if (!isInSameSession(attacker, victim)) {
            return;
        }
        
        // Check if attacker has a sword
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (!isSword(weapon)) {
            event.setCancelled(true);
            return;
        }
        
        // Cancel normal damage and apply instant kill
        event.setCancelled(true);
        handleMeleeKill(attacker, victim);
    }
    
    /**
     * Handle bow kills with instant death
     */
    private void handleBowKill(Player shooter, Player victim, Arrow arrow) {
        GamePlayer shooterGame = gameManager.getGamePlayer(shooter);
        GamePlayer victimGame = gameManager.getGamePlayer(victim);
        
        if (shooterGame == null || victimGame == null) {
            return;
        }
        
        // Track hit accuracy
        shooterGame.incrementArrowsHit();
        
        // Award arrow recovery
        shooterGame.addArrows(1);
        messageManager.sendMessage(shooter, "game.arrow-recovered");
        
        // Execute the kill
        executeKill(shooter, victim, "bow", shooterGame, victimGame);
        
        // Remove the arrow from the world
        arrow.remove();
        
        Logger.info("Bow kill: " + shooter.getName() + " -> " + victim.getName());
    }
    
    /**
     * Handle melee kills with instant death
     */
    private void handleMeleeKill(Player attacker, Player victim) {
        GamePlayer attackerGame = gameManager.getGamePlayer(attacker);
        GamePlayer victimGame = gameManager.getGamePlayer(victim);
        
        if (attackerGame == null || victimGame == null) {
            return;
        }
        
        // Award arrow for sword kill
        attackerGame.addArrows(1);
        messageManager.sendMessage(attacker, "game.arrow-recovered");
        
        // Execute the kill
        executeKill(attacker, victim, "sword", attackerGame, victimGame);
        
        Logger.info("Melee kill: " + attacker.getName() + " -> " + victim.getName());
    }
    
    /**
     * Execute a kill with proper statistics and respawn handling
     */
    private void executeKill(Player killer, Player victim, String weaponType, 
                           GamePlayer killerGame, GamePlayer victimGame) {
        
        // Update statistics
        killerGame.addKill();
        victimGame.addDeath();
        
        // Update kill streak
        killerGame.incrementKillStreak();
        victimGame.resetKillStreak();
        
        // Check for special achievements
        checkKillAchievements(killer, killerGame);
        
        // Broadcast kill message
        broadcastKillMessage(killer, victim, weaponType, killerGame);
        
        // Handle victim death
        handlePlayerDeath(victim, victimGame);
        
        // Check win conditions
        checkWinConditions(killer);
    }
    
    /**
     * Check for kill streak achievements and combos
     */
    private void checkKillAchievements(Player killer, GamePlayer killerGame) {
        int streak = killerGame.getKillStreak();
        
        // Check for first blood
        String sessionId = gameManager.getPlayerSession(killer);
        if (sessionId != null) {
            GameSession session = gameManager.getSession(sessionId);
            if (session != null && session.getTotalKills() == 1) {
                broadcastToSession(sessionId, "game.kill-first-blood", "killer", killer.getName());
            }
        }
        
        // Check for kill streaks
        if (streak == 3) {
            broadcastToSession(gameManager.getPlayerSession(killer), 
                "game.combo-achieved", 
                "player", killer.getName(),
                "streak", String.valueOf(streak));
        } else if (streak == 6) {
            broadcastToSession(gameManager.getPlayerSession(killer), 
                "game.combo-unstoppable", 
                "player", killer.getName(),
                "streak", String.valueOf(streak));
        }
    }
    
    /**
     * Broadcast kill message to all players in the session
     */
    private void broadcastKillMessage(Player killer, Player victim, String weaponType, GamePlayer killerGame) {
        String sessionId = gameManager.getPlayerSession(killer);
        if (sessionId == null) return;
        
        String messageKey = weaponType.equals("bow") ? "game.kill-bow" : "game.kill-sword";
        
        broadcastToSession(sessionId, messageKey,
            "killer", killer.getName(),
            "victim", victim.getName());
    }
    
    /**
     * Handle player death with respawn mechanics
     */
    private void handlePlayerDeath(Player victim, GamePlayer victimGame) {
        // Set player to dead state
        victimGame.setState(GamePlayer.GamePlayerState.DEAD);
        
        // Clear inventory and set to spectator-like mode
        victim.getInventory().clear();
        victim.setHealth(20.0);
        victim.setFoodLevel(20);
        
        // Schedule respawn
        int respawnDelay = plugin.getConfigManager().getRespawnDelay();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            respawnPlayer(victim, victimGame);
        }, respawnDelay * 20L); // Convert seconds to ticks
        
        // Notify about respawn countdown
        messageManager.sendMessage(victim, "game.respawn-countdown", 
            "seconds", String.valueOf(respawnDelay));
    }
    
    /**
     * Respawn a player back into the game
     */
    private void respawnPlayer(Player player, GamePlayer gamePlayer) {
        if (!gameManager.isPlayerInGame(player)) {
            return; // Player left the game
        }
        
        String sessionId = gameManager.getPlayerSession(player);
        if (sessionId == null) {
            return;
        }
        
        GameSession session = gameManager.getSession(sessionId);
        if (session == null || session.getCurrentState() != GameSession.GameState.ACTIVE) {
            return; // Game ended
        }
        
        // Respawn the player
        gamePlayer.respawn();
        
        // Give basic equipment
        giveBasicEquipment(player, gamePlayer);
        
        Logger.debug("Player " + player.getName() + " respawned");
    }
    
    /**
     * Give basic equipment to a player
     */
    private void giveBasicEquipment(Player player, GamePlayer gamePlayer) {
        player.getInventory().clear();
        
        // Give bow and arrows
        player.getInventory().addItem(new ItemStack(Material.BOW, 1));
        gamePlayer.setArrows(plugin.getConfigManager().getStartingArrows());
        
        // Give sword
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD, 1));
        
        // Set health and food
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20);
        
        // Update arrows display
        messageManager.sendMessage(player, "game.arrows-remaining", 
            "count", String.valueOf(gamePlayer.getArrows()));
    }
    
    /**
     * Check if weapon is a sword
     */
    private boolean isSword(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        Material type = item.getType();
        return type == Material.WOODEN_SWORD || 
               type == Material.STONE_SWORD || 
               type == Material.IRON_SWORD || 
               type == Material.GOLDEN_SWORD || 
               type == Material.DIAMOND_SWORD || 
               type == Material.NETHERITE_SWORD;
    }
    
    /**
     * Check if two players are in the same session
     */
    private boolean isInSameSession(Player player1, Player player2) {
        String session1 = gameManager.getPlayerSession(player1);
        String session2 = gameManager.getPlayerSession(player2);
        
        return session1 != null && session1.equals(session2);
    }
    
    /**
     * Check win conditions after a kill
     */
    private void checkWinConditions(Player killer) {
        String sessionId = gameManager.getPlayerSession(killer);
        if (sessionId == null) return;
        
        GameSession session = gameManager.getSession(sessionId);
        if (session == null) return;
        
        GamePlayer killerGame = gameManager.getGamePlayer(killer);
        if (killerGame == null) return;
        
        int targetScore = plugin.getConfigManager().getTargetScore();
        if (killerGame.getCurrentScore() >= targetScore) {
            session.endGame(killer.getName() + " reached target score!");
        }
    }
    
    /**
     * Broadcast message to all players in a session
     */
    private void broadcastToSession(String sessionId, String messageKey, String... placeholders) {
        if (sessionId == null) return;
        
        GameSession session = gameManager.getSession(sessionId);
        if (session == null) return;
        
        for (UUID playerId : session.getPlayers().keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                messageManager.sendMessage(player, messageKey, placeholders);
            }
        }
    }
}
