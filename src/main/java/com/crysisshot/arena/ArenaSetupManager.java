package com.crysisshot.arena;

import com.crysisshot.CrysisShot;
import com.crysisshot.localization.MessageManager;
import com.crysisshot.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages arena setup mode and configuration for administrators
 */
public class ArenaSetupManager {
    
    private final CrysisShot plugin;
    private final ArenaManager arenaManager;
    private final MessageManager messageManager;
    
    // Track players in setup mode
    private final Map<UUID, ArenaSetupSession> setupSessions = new ConcurrentHashMap<>();
    
    public ArenaSetupManager(CrysisShot plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
        this.messageManager = plugin.getMessageManager();
    }
    
    /**
     * Start arena setup mode for a player
     */
    public boolean startSetup(Player player, String arenaName) {
        if (setupSessions.containsKey(player.getUniqueId())) {
            messageManager.sendMessage(player, "arena.setup.already-in-setup");
            return false;
        }
        
        // Check if arena already exists
        Arena existingArena = arenaManager.getArena(arenaName);
        Arena arena;
        
        if (existingArena != null) {
            arena = existingArena;
            messageManager.sendMessage(player, "arena.setup.editing-existing", "arena", arenaName);
        } else {
            arena = new Arena(arenaName);
            arena.setWorldName(player.getWorld().getName());
            messageManager.sendMessage(player, "arena.setup.creating-new", "arena", arenaName);
        }
        
        ArenaSetupSession session = new ArenaSetupSession(player, arena);
        setupSessions.put(player.getUniqueId(), session);
        
        // Give setup tools
        giveSetupTools(player);
        
        messageManager.sendMessage(player, "arena.setup.started", "arena", arenaName);
        showSetupHelp(player);
        
        return true;
    }
    
    /**
     * End setup mode for a player
     */
    public boolean endSetup(Player player, boolean save) {
        ArenaSetupSession session = setupSessions.remove(player.getUniqueId());
        if (session == null) {
            messageManager.sendMessage(player, "arena.setup.not-in-setup");
            return false;
        }
        
        if (save) {
            Arena arena = session.getArena();
            List<String> errors = arenaManager.validateArena(arena);
            
            if (errors.isEmpty()) {
                // Save arena to configuration
                saveArenaToConfig(arena);
                arenaManager.reload(); // Reload to pick up changes
                messageManager.sendMessage(player, "arena.setup.saved", "arena", arena.getName());
            } else {
                messageManager.sendMessage(player, "arena.validation-failed", "errors", String.join(", ", errors));
                return false;
            }
        } else {
            messageManager.sendMessage(player, "arena.setup.cancelled");
        }
        
        // Remove setup tools
        removeSetupTools(player);
        
        return true;
    }
    
    /**
     * Handle setup commands
     */
    public boolean handleSetupCommand(Player player, String[] args) {
        if (args.length < 2) {
            showSetupCommands(player);
            return true;
        }
        
        ArenaSetupSession session = setupSessions.get(player.getUniqueId());
        if (session == null) {
            messageManager.sendMessage(player, "arena.setup.not-in-setup");
            return false;
        }
        
        String command = args[1].toLowerCase();
        Arena arena = session.getArena();
        
        switch (command) {
            case "lobby":
                arena.setLobbySpawn(player.getLocation());
                messageManager.sendMessage(player, "arena.setup.lobby-set");
                break;
                
            case "spectator":
                arena.setSpectatorSpawn(player.getLocation());
                messageManager.sendMessage(player, "arena.setup.spectator-set");
                break;
                
            case "spawn":
                if (args.length >= 3 && args[2].equalsIgnoreCase("add")) {
                    List<Location> spawns = arena.getSpawnPoints();
                    if (spawns == null) spawns = new ArrayList<>();
                    spawns.add(player.getLocation());
                    arena.setSpawnPoints(spawns);
                    messageManager.sendMessage(player, "arena.setup.spawn-added", "count", String.valueOf(spawns.size()));
                } else if (args.length >= 3 && args[2].equalsIgnoreCase("remove")) {
                    List<Location> spawns = arena.getSpawnPoints();
                    if (spawns != null && !spawns.isEmpty()) {
                        Location closest = findClosestLocation(player.getLocation(), spawns);
                        spawns.remove(closest);
                        arena.setSpawnPoints(spawns);
                        messageManager.sendMessage(player, "arena.setup.spawn-removed", "count", String.valueOf(spawns.size()));
                    } else {
                        messageManager.sendMessage(player, "arena.setup.no-spawns");
                    }
                } else {
                    messageManager.sendMessage(player, "arena.setup.spawn-usage");
                }
                break;
                
            case "powerup":
                if (args.length >= 3 && args[2].equalsIgnoreCase("add")) {
                    List<Location> powerups = arena.getPowerupLocations();
                    if (powerups == null) powerups = new ArrayList<>();
                    powerups.add(player.getLocation());
                    arena.setPowerupLocations(powerups);
                    messageManager.sendMessage(player, "arena.setup.powerup-added", "count", String.valueOf(powerups.size()));
                } else if (args.length >= 3 && args[2].equalsIgnoreCase("remove")) {
                    List<Location> powerups = arena.getPowerupLocations();
                    if (powerups != null && !powerups.isEmpty()) {
                        Location closest = findClosestLocation(player.getLocation(), powerups);
                        powerups.remove(closest);
                        arena.setPowerupLocations(powerups);
                        messageManager.sendMessage(player, "arena.setup.powerup-removed", "count", String.valueOf(powerups.size()));
                    } else {
                        messageManager.sendMessage(player, "arena.setup.no-powerups");
                    }
                } else {
                    messageManager.sendMessage(player, "arena.setup.powerup-usage");
                }
                break;
                
            case "bounds":
                if (args.length >= 3) {
                    String boundType = args[2].toLowerCase();
                    if (boundType.equals("min")) {
                        arena.setBoundaryMin(player.getLocation());
                        messageManager.sendMessage(player, "arena.setup.bounds-min-set");
                    } else if (boundType.equals("max")) {
                        arena.setBoundaryMax(player.getLocation());
                        messageManager.sendMessage(player, "arena.setup.bounds-max-set");
                    } else {
                        messageManager.sendMessage(player, "arena.setup.bounds-usage");
                    }
                } else {
                    messageManager.sendMessage(player, "arena.setup.bounds-usage");
                }
                break;
                
            case "theme":
                if (args.length >= 3) {
                    try {
                        Arena.Theme theme = Arena.Theme.valueOf(args[2].toUpperCase());
                        arena.setTheme(theme);
                        messageManager.sendMessage(player, "arena.setup.theme-set", "theme", theme.getDisplayName());
                    } catch (IllegalArgumentException e) {
                        messageManager.sendMessage(player, "arena.setup.invalid-theme");
                    }
                } else {
                    messageManager.sendMessage(player, "arena.setup.theme-usage");
                }
                break;
                
            case "players":
                if (args.length >= 4) {
                    try {
                        int min = Integer.parseInt(args[2]);
                        int max = Integer.parseInt(args[3]);
                        arena.setMinPlayers(min);
                        arena.setMaxPlayers(max);
                        messageManager.sendMessage(player, "arena.setup.players-set", "min", String.valueOf(min), "max", String.valueOf(max));
                    } catch (NumberFormatException e) {
                        messageManager.sendMessage(player, "arena.setup.invalid-number");
                    }
                } else {
                    messageManager.sendMessage(player, "arena.setup.players-usage");
                }
                break;
                
            case "validate":
                List<String> errors = arenaManager.validateArena(arena);
                if (errors.isEmpty()) {
                    messageManager.sendMessage(player, "arena.validation-passed");
                } else {
                    messageManager.sendMessage(player, "arena.validation-failed", "errors", String.join(", ", errors));
                }
                break;
                
            case "info":
                showArenaInfo(player, arena);
                break;
                
            case "gui":
                openSetupGUI(player);
                break;
                
            case "help":
                showSetupHelp(player);
                break;
                
            default:
                showSetupCommands(player);
                break;
        }
        
        return true;
    }
    
    /**
     * Open setup GUI for easier configuration
     */
    public void openSetupGUI(Player player) {
        ArenaSetupSession session = setupSessions.get(player.getUniqueId());
        if (session == null) {
            messageManager.sendMessage(player, "arena.setup.not-in-setup");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "Arena Setup: " + session.getArena().getName());
          // Add setup items
        gui.setItem(10, createSetupItem(Material.RED_BED, "Set Lobby Spawn", "Click to set lobby spawn at your location"));
        gui.setItem(11, createSetupItem(Material.GLASS, "Set Spectator Spawn", "Click to set spectator spawn at your location"));
        gui.setItem(12, createSetupItem(Material.EMERALD_BLOCK, "Add Spawn Point", "Click to add a spawn point at your location"));
        gui.setItem(13, createSetupItem(Material.REDSTONE_BLOCK, "Remove Spawn Point", "Click to remove nearest spawn point"));
        gui.setItem(14, createSetupItem(Material.DIAMOND_BLOCK, "Add Power-up Location", "Click to add a power-up location"));
        gui.setItem(15, createSetupItem(Material.COAL_BLOCK, "Remove Power-up Location", "Click to remove nearest power-up location"));
        gui.setItem(16, createSetupItem(Material.BARRIER, "Set Min Boundary", "Click to set minimum boundary"));
        gui.setItem(19, createSetupItem(Material.BEDROCK, "Set Max Boundary", "Click to set maximum boundary"));
        gui.setItem(20, createSetupItem(Material.COMPASS, "Validate Arena", "Click to validate arena configuration"));
        gui.setItem(21, createSetupItem(Material.BOOK, "Arena Info", "Click to view arena information"));
        gui.setItem(22, createSetupItem(Material.EMERALD, "Save Arena", "Click to save and finish setup"));
        gui.setItem(23, createSetupItem(Material.REDSTONE, "Cancel Setup", "Click to cancel setup without saving"));
        
        player.openInventory(gui);
    }
    
    /**
     * Create an item for the setup GUI
     */
    private ItemStack createSetupItem(Material material, String name, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6" + name);
            meta.setLore(Arrays.asList("§7" + description));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Give setup tools to player
     */
    private void giveSetupTools(Player player) {
        ItemStack wandItem = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wandItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Arena Setup Wand");
            meta.setLore(Arrays.asList(
                "§7Left click: Set spawn point",
                "§7Right click: Set power-up location",
                "§7Shift + Left click: Set lobby spawn",
                "§7Shift + Right click: Set spectator spawn"
            ));
            wandItem.setItemMeta(meta);
        }
        
        player.getInventory().addItem(wandItem);
        messageManager.sendMessage(player, "arena.setup.tools-given");
    }
    
    /**
     * Remove setup tools from player
     */
    private void removeSetupTools(Player player) {
        player.getInventory().remove(Material.BLAZE_ROD);
    }
    
    /**
     * Show setup commands to player
     */
    private void showSetupCommands(Player player) {
        player.sendMessage("§6=== Arena Setup Commands ===");
        player.sendMessage("§e/cs setup lobby §7- Set lobby spawn");
        player.sendMessage("§e/cs setup spectator §7- Set spectator spawn");
        player.sendMessage("§e/cs setup spawn add/remove §7- Manage spawn points");
        player.sendMessage("§e/cs setup powerup add/remove §7- Manage power-up locations");
        player.sendMessage("§e/cs setup bounds min/max §7- Set arena boundaries");
        player.sendMessage("§e/cs setup theme <URBAN|HOSPITAL|TEMPLE> §7- Set arena theme");
        player.sendMessage("§e/cs setup players <min> <max> §7- Set player limits");
        player.sendMessage("§e/cs setup validate §7- Validate arena");
        player.sendMessage("§e/cs setup info §7- Show arena info");
        player.sendMessage("§e/cs setup gui §7- Open setup GUI");
        player.sendMessage("§e/cs setup help §7- Show this help");
    }
    
    /**
     * Show setup help to player
     */
    private void showSetupHelp(Player player) {
        player.sendMessage("§6=== Arena Setup Mode ===");
        player.sendMessage("§7You are now in arena setup mode.");
        player.sendMessage("§7Use §e/cs setup help §7to see all commands.");
        player.sendMessage("§7Use §e/cs setup gui §7for an easier setup interface.");
        player.sendMessage("§7Use §e/cs admin setup save §7to save when done.");
        player.sendMessage("§7Use §e/cs admin setup cancel §7to cancel without saving.");
    }
    
    /**
     * Show arena information to player
     */
    private void showArenaInfo(Player player, Arena arena) {
        player.sendMessage("§6=== Arena Information ===");
        player.sendMessage("§eArena: §f" + arena.getName());
        player.sendMessage("§eDisplay Name: §f" + arena.getDisplayName());
        player.sendMessage("§eWorld: §f" + arena.getWorldName());
        player.sendMessage("§eTheme: §f" + arena.getTheme().getDisplayName());
        player.sendMessage("§ePlayers: §f" + arena.getMinPlayers() + "-" + arena.getMaxPlayers());
        player.sendMessage("§eEnabled: §f" + arena.isEnabled());
        
        player.sendMessage("§6--- Spawn Points ---");
        player.sendMessage("§eLobby: §f" + (arena.getLobbySpawn() != null ? "Set" : "Not set"));
        player.sendMessage("§eSpectator: §f" + (arena.getSpectatorSpawn() != null ? "Set" : "Not set"));
        
        int spawnCount = arena.getSpawnPoints() != null ? arena.getSpawnPoints().size() : 0;
        player.sendMessage("§ePlayer Spawns: §f" + spawnCount);
        
        int powerupCount = arena.getPowerupLocations() != null ? arena.getPowerupLocations().size() : 0;
        player.sendMessage("§ePower-up Locations: §f" + powerupCount);
        
        player.sendMessage("§6--- Boundaries ---");
        player.sendMessage("§eMin Boundary: §f" + (arena.getBoundaryMin() != null ? "Set" : "Not set"));
        player.sendMessage("§eMax Boundary: §f" + (arena.getBoundaryMax() != null ? "Set" : "Not set"));
    }
    
    /**
     * Find the closest location to the given location
     */
    private Location findClosestLocation(Location target, List<Location> locations) {
        if (locations.isEmpty()) return null;
        
        Location closest = locations.get(0);
        double closestDistance = target.distance(closest);
        
        for (Location loc : locations) {
            double distance = target.distance(loc);
            if (distance < closestDistance) {
                closest = loc;
                closestDistance = distance;
            }
        }
        
        return closest;
    }
    
    /**
     * Save arena to configuration
     */
    private void saveArenaToConfig(Arena arena) {
        // This would typically save to arenas.yml
        // For now, we'll just log that it should be saved
        Logger.info("Arena " + arena.getName() + " should be saved to configuration");
        // TODO: Implement actual saving to arenas.yml
    }
    
    // Getters
    public boolean isInSetupMode(Player player) {
        return setupSessions.containsKey(player.getUniqueId());
    }
    
    public ArenaSetupSession getSetupSession(Player player) {
        return setupSessions.get(player.getUniqueId());
    }
    
    /**
     * Clean up when player leaves
     */
    public void handlePlayerLeave(Player player) {
        setupSessions.remove(player.getUniqueId());
    }
    
    /**
     * Inner class to track setup sessions
     */
    public static class ArenaSetupSession {
        private final Player player;
        private final Arena arena;
        private final long startTime;
        
        public ArenaSetupSession(Player player, Arena arena) {
            this.player = player;
            this.arena = arena;
            this.startTime = System.currentTimeMillis();
        }
        
        public Player getPlayer() { return player; }
        public Arena getArena() { return arena; }
        public long getStartTime() { return startTime; }
    }
}
