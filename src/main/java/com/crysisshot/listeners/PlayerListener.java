package com.crysisshot.listeners;

import com.crysisshot.game.GameManager;
import com.crysisshot.localization.MessageManager;
import com.crysisshot.models.GamePlayer;
import com.crysisshot.utils.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Handles player-related events for CrysisShot
 * Manages player connections, disconnections, and world changes
 */
public class PlayerListener implements Listener {
      private final GameManager gameManager;
    private final MessageManager messageManager;
    
    public PlayerListener(GameManager gameManager, MessageManager messageManager) {
        this.gameManager = gameManager;
        this.messageManager = messageManager;
    }
    
    /**
     * Handle player joining the server
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Logger.debug("Player " + player.getName() + " joined the server");
        
        // Initialize player data if needed
        // This is handled by DatabaseManager when player statistics are queried
    }
    
    /**
     * Handle player leaving the server
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerDisconnect(event.getPlayer(), "quit");
    }
    
    /**
     * Handle player being kicked from the server
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerKick(PlayerKickEvent event) {
        handlePlayerDisconnect(event.getPlayer(), "kicked");
    }
    
    /**
     * Handle player changing worlds
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        
        // Remove player from any active game if they change worlds
        if (gameManager.isPlayerInGame(player)) {
            Logger.info("Player " + player.getName() + " changed worlds while in game, removing from game");
            gameManager.removePlayerFromGame(player, false);
            messageManager.sendMessage(player, "error.left-game-world-change");
        }
    }
    
    /**
     * Handle commands that might conflict with the game
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        
        // Check if player is in game
        if (!gameManager.isPlayerInGame(player)) {
            return;
        }
        
        // Block certain commands while in game
        String[] blockedCommands = {
            "/tp", "/teleport", "/spawn", "/home", "/warp", "/fly", "/gamemode",
            "/give", "/clear", "/kill", "/suicide"
        };
        
        for (String blockedCommand : blockedCommands) {
            if (command.startsWith(blockedCommand + " ") || command.equals(blockedCommand)) {
                event.setCancelled(true);
                messageManager.sendMessage(player, "error.command-blocked-in-game", 
                    "command", blockedCommand);
                return;
            }
        }
        
        // Allow plugin commands
        if (command.startsWith("/crysisshot") || command.startsWith("/cs")) {
            return;
        }
        
        // Block most other commands with some exceptions
        String[] allowedCommands = {
            "/msg", "/tell", "/whisper", "/r", "/reply", "/list", "/who", "/help"
        };
        
        boolean isAllowed = false;
        for (String allowedCommand : allowedCommands) {
            if (command.startsWith(allowedCommand + " ") || command.equals(allowedCommand)) {
                isAllowed = true;
                break;
            }
        }
        
        if (!isAllowed) {
            event.setCancelled(true);
            messageManager.sendMessage(player, "error.command-not-allowed-in-game");
        }
    }
    
    /**
     * Common handler for player disconnection
     */
    private void handlePlayerDisconnect(Player player, String reason) {
        Logger.debug("Player " + player.getName() + " disconnected (" + reason + ")");
        
        // Remove player from any active game
        if (gameManager.isPlayerInGame(player)) {
            Logger.info("Player " + player.getName() + " disconnected while in game, removing from game");
            
            GamePlayer gamePlayer = gameManager.getGamePlayer(player);
            if (gamePlayer != null) {
                // Log disconnect statistics
                Logger.info("Player disconnect stats - Kills: " + gamePlayer.getSessionKills() + 
                           ", Deaths: " + gamePlayer.getSessionDeaths());
            }
            
            gameManager.removePlayerFromGame(player, false);
        }
        
        // Remove from queue if present
        gameManager.removePlayerFromQueue(player);
    }
}
