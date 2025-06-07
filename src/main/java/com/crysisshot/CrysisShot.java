package com.crysisshot;

import com.crysisshot.arena.ArenaManager;
import com.crysisshot.arena.ArenaSetupManager;
import com.crysisshot.commands.CrysisShotCommand;
import com.crysisshot.config.ConfigManager;
import com.crysisshot.database.DatabaseManager;
import com.crysisshot.game.GameManager;
// TODO: Uncomment when implemented in later steps
// import com.crysisshot.integration.EconomyManager;
// import com.crysisshot.integration.PlaceholderExpansion;
// import com.crysisshot.listeners.GameListener;
// import com.crysisshot.listeners.PlayerListener;
import com.crysisshot.localization.MessageManager;
import com.crysisshot.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for CrysisShot
 * A competitive PvP minigame plugin based on "One In The Chamber" game mode
 * 
 * @author Roger Lebron Serra
 * @version 1.0.0
 */
public class CrysisShot extends JavaPlugin {    private static CrysisShot instance;      // Core managers
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private ArenaManager arenaManager;
    private ArenaSetupManager arenaSetupManager;
    private GameManager gameManager;
    // private EconomyManager economyManager;
    
    // Plugin state
    private boolean pluginEnabled = false;
    
    @Override
    public void onLoad() {
        instance = this;
        Logger.info("Loading CrysisShot plugin...");
    }
    
    @Override
    public void onEnable() {
        try {
            // Initialize core systems
            initializeManagers();
            
            // Register commands and listeners
            registerCommands();
            registerEvents();
            
            // Setup integrations
            setupIntegrations();
            
            // Mark plugin as enabled
            pluginEnabled = true;
              Logger.info("CrysisShot plugin has been enabled successfully!");
            // Use PluginMeta instead of deprecated getDescription()
            Logger.info("Version: " + getPluginMeta().getVersion());
            Logger.info("Authors: " + String.join(", ", getPluginMeta().getAuthors()));
            
        } catch (Exception e) {
            Logger.severe("Failed to enable CrysisShot plugin: " + e.getMessage());
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            pluginEnabled = false;
            
            Logger.info("Disabling CrysisShot plugin...");            // End all active games
            if (gameManager != null) {
                gameManager.shutdown();
            }
            
            // Close database connections
            if (databaseManager != null) {
                databaseManager.shutdown();
            }
            
            // Cancel all tasks
            Bukkit.getScheduler().cancelTasks(this);
            
            Logger.info("CrysisShot plugin has been disabled successfully!");
            
        } catch (Exception e) {
            Logger.severe("Error during plugin shutdown: " + e.getMessage());
            e.printStackTrace();
        } finally {
            instance = null;
        }
    }
    
    /**
     * Initialize all plugin managers
     */
    private void initializeManagers() {
        Logger.info("Initializing plugin managers...");
        
        // Configuration manager (first priority)
        configManager = new ConfigManager(this);
        configManager.loadConfigurations();
        
        // Message manager (depends on config)
        messageManager = new MessageManager(this, configManager);
        messageManager.loadMessages();        // Database manager
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            throw new RuntimeException("Failed to initialize database");
        }
          // Arena manager (depends on config and message managers)
        arenaManager = new ArenaManager(this);
        
        // Arena setup manager (depends on arena manager)
        arenaSetupManager = new ArenaSetupManager(this);
        
        // Game manager (depends on database and config)
        gameManager = new GameManager(this);
        
        Logger.info("All managers initialized successfully!");
    }    /**
     * Register plugin commands
     */
    private void registerCommands() {
        Logger.info("Registering commands...");
        
        CrysisShotCommand mainCommand = new CrysisShotCommand(this, messageManager, gameManager);
        getCommand("crysisshot").setExecutor(mainCommand);
        getCommand("crysisshot").setTabCompleter(mainCommand);
        
        Logger.info("Commands registered successfully!");
    }
      /**
     * Register event listeners
     */
    private void registerEvents() {
        Logger.info("Registering event listeners...");
        
        // TODO: Implement in Step 2.1 when GameManager and listeners are implemented
        // Bukkit.getPluginManager().registerEvents(
        //     new PlayerListener(gameManager, messageManager), this);
        // Bukkit.getPluginManager().registerEvents(
        //     new GameListener(gameManager, messageManager), this);
        
        Logger.info("Event listeners will be registered in Step 2.1!");
    }
      /**
     * Setup external plugin integrations
     */
    private void setupIntegrations() {
        Logger.info("Setting up external integrations...");
        
        // TODO: Implement in Step 7.1 when integration managers are implemented
        // Economy integration (Vault)
        // if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
        //     economyManager = new EconomyManager(this);
        //     if (economyManager.setupEconomy()) {
        //         Logger.info("Vault economy integration enabled!");
        //     } else {
        //         Logger.warning("Vault found but no economy plugin detected!");
        //     }
        // }
        
        // PlaceholderAPI integration
        // if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
        //     new PlaceholderExpansion(this, gameManager, databaseManager).register();
        //     Logger.info("PlaceholderAPI integration enabled!");
        // }
        
        Logger.info("External integrations setup will be completed in Step 7.1!");
    }
      /**
     * Reload plugin configuration and managers
     */
    public void reloadPlugin() {
        try {
            Logger.info("Reloading CrysisShot plugin...");
            
            // Reload configurations
            configManager.loadConfigurations();
            messageManager.loadMessages();
            
            // TODO: Implement in Step 2.1 when GameManager is available
            // Reinitialize game manager with new config
            // gameManager.reload();
            
            Logger.info("Plugin reloaded successfully!");
            
        } catch (Exception e) {
            Logger.severe("Failed to reload plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get plugin instance
     */
    public static CrysisShot getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
      /**
     * Get the arena manager
     */
    public ArenaManager getArenaManager() {
        return arenaManager;
    }
    
    /**
     * Get the arena setup manager
     */
    public ArenaSetupManager getArenaSetupManager() {
        return arenaSetupManager;
    }
    
    /**
     * Get the game manager
     */
    public GameManager getGameManager() {
        return gameManager;
    }
    
    // TODO: Implement in Step 7.1 when EconomyManager is available
    // public EconomyManager getEconomyManager() {
    //     return economyManager;
    // }
    
    public boolean isPluginEnabled() {
        return pluginEnabled;
    }
}
