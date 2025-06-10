package com.crysisshot.listeners;

import com.crysisshot.game.GameManager;
import com.crysisshot.game.GameSession;
import com.crysisshot.localization.MessageManager;
import com.crysisshot.models.GamePlayer;
import com.crysisshot.utils.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles game-related events for CrysisShot
 * Manages general game mechanics and restrictions
 */
public class GameListener implements Listener {
      private final GameManager gameManager;
    private final MessageManager messageManager;
    
    public GameListener(GameManager gameManager, MessageManager messageManager) {
        this.gameManager = gameManager;
        this.messageManager = messageManager;
    }
    
    /**
     * Prevent block breaking in game
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (gameManager.isPlayerInGame(player)) {
            event.setCancelled(true);
            messageManager.sendMessage(player, "error.cannot-break-blocks-in-game");
        }
    }
    
    /**
     * Prevent block placing in game
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        if (gameManager.isPlayerInGame(player)) {
            event.setCancelled(true);
            messageManager.sendMessage(player, "error.cannot-place-blocks-in-game");
        }
    }
    
    /**
     * Handle item dropping (prevent in most cases)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        if (gameManager.isPlayerInGame(player)) {
            event.setCancelled(true);
            messageManager.sendMessage(player, "error.cannot-drop-items-in-game");
        }
    }
      /**
     * Handle item pickup (prevent in most cases)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        if (gameManager.isPlayerInGame(player)) {
            // Only allow specific item types if needed
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle inventory clicking (prevent modification in game)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        if (gameManager.isPlayerInGame(player)) {
            // Prevent inventory modification during games
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle food level changes (prevent hunger in game)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        if (gameManager.isPlayerInGame(player)) {
            // Prevent hunger in games
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle environmental damage (fall damage, etc.)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        
        // Handle different damage causes
        switch (event.getCause()) {
            case FALL:
                // Prevent fall damage in games
                event.setCancelled(true);
                break;
                
            case DROWNING:            case LAVA:
            case FIRE:
            case FIRE_TICK:
                // Environmental deaths - teleport player back to spawn
                event.setCancelled(true);
                handleEnvironmentalDeath(player);
                break;
                
            case VOID:
                // Void death - respawn immediately
                event.setCancelled(true);
                handleVoidDeath(player);
                break;
                
            case STARVATION:
            case POISON:
            case WITHER:
                // Prevent these damage types
                event.setCancelled(true);
                break;
                
            default:
                // Allow other damage types (handled by CombatListener)
                break;
        }
    }
    
    /**
     * Handle player interactions with game items
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        
        // Additional interaction handling can be added here
        // For now, most interactions are handled by CombatListener
    }
    
    /**
     * Handle player movement (boundary checking)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        
        // Check if player is moving out of arena bounds
        String sessionId = gameManager.getPlayerSession(player);
        if (sessionId != null) {
            GameSession session = gameManager.getSession(sessionId);
            if (session != null && session.isActive()) {
                // TODO: Implement arena boundary checking in later steps
                // For now, just prevent going too high or low
                if (event.getTo().getY() > 300) {
                    player.teleport(event.getFrom());
                    messageManager.sendMessage(player, "error.arena-boundary");
                } else if (event.getTo().getY() < -50) {
                    handleVoidDeath(player);
                }
            }
        }
    }
    
    /**
     * Handle environmental deaths (lava, fire, etc.)
     */
    private void handleEnvironmentalDeath(Player player) {
        GamePlayer gamePlayer = gameManager.getGamePlayer(player);
        if (gamePlayer == null) {
            return;
        }
        
        String sessionId = gameManager.getPlayerSession(player);
        if (sessionId == null) {
            return;
        }
        
        GameSession session = gameManager.getSession(sessionId);
        if (session == null) {
            return;
        }
        
        // Record death
        gamePlayer.addDeath();
        gamePlayer.resetKillStreak();
        
        // Teleport to spawn and give equipment
        // This will be handled by respawn logic
        messageManager.sendMessage(player, "game.environmental-death");
        
        Logger.info("Environmental death: " + player.getName());
    }
    
    /**
     * Handle void deaths
     */
    private void handleVoidDeath(Player player) {
        GamePlayer gamePlayer = gameManager.getGamePlayer(player);
        if (gamePlayer == null) {
            return;
        }
        
        // Record death
        gamePlayer.addDeath();
        gamePlayer.resetKillStreak();
        
        // Immediate respawn for void deaths
        player.setHealth(20.0);
        messageManager.sendMessage(player, "game.void-death");
        
        Logger.info("Void death: " + player.getName());
    }
}
