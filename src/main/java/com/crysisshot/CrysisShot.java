package com.crysisshot;

import com.crysisshot.commands.CrysisShotCommand;
import com.crysisshot.config.ConfigManager;
import com.crysisshot.database.DatabaseManager;
import com.crysisshot.game.GameManager;
import com.crysisshot.integration.EconomyManager;
import com.crysisshot.integration.PlaceholderExpansion;
import com.crysisshot.listeners.GameListener;
import com.crysisshot.listeners.PlayerListener;
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
public class CrysisShot extends JavaPlugin {
    
    private static CrysisShot instance;
    
    // Core managers
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private GameManager gameManager;
    
    // Integration managers
    private EconomyManager economyManager;
    
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
            Logger.info("Version: " + getDescription().getVersion());
            Logger.info("Authors: " + String.join(", ", getDescription().getAuthors()));
            
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
            
            Logger.info("Disabling CrysisShot plugin...");
            
            // End all active games
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
        messageManager.loadMessages();
        
        // Database manager
        databaseManager = new DatabaseManager(this, configManager);
        databaseManager.initialize();
        
        // Game manager (depends on database and config)
        gameManager = new GameManager(this, databaseManager, messageManager, configManager);
        gameManager.initialize();
        
        Logger.info("All managers initialized successfully!");
    }
    
    /**
     * Register plugin commands
     */
    private void registerCommands() {
        Logger.info("Registering commands...");
        
        CrysisShotCommand mainCommand = new CrysisShotCommand(this, gameManager, messageManager);
        getCommand("crysisshot").setExecutor(mainCommand);
        getCommand("crysisshot").setTabCompleter(mainCommand);
        
        Logger.info("Commands registered successfully!");
    }
    
    /**
     * Register event listeners
     */
    private void registerEvents() {
        Logger.info("Registering event listeners...");
        
        Bukkit.getPluginManager().registerEvents(
            new PlayerListener(gameManager, messageManager), this);
        Bukkit.getPluginManager().registerEvents(
            new GameListener(gameManager, messageManager), this);
        
        Logger.info("Event listeners registered successfully!");
    }
    
    /**
     * Setup external plugin integrations
     */
    private void setupIntegrations() {
        Logger.info("Setting up external integrations...");
        
        // Economy integration (Vault)
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            economyManager = new EconomyManager(this);
            if (economyManager.setupEconomy()) {
                Logger.info("Vault economy integration enabled!");
            } else {
                Logger.warning("Vault found but no economy plugin detected!");
            }
        }
        
        // PlaceholderAPI integration
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderExpansion(this, gameManager, databaseManager).register();
            Logger.info("PlaceholderAPI integration enabled!");
        }
        
        Logger.info("External integrations setup complete!");
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
            
            // Reinitialize game manager with new config
            gameManager.reload();
            
            Logger.info("Plugin reloaded successfully!");
            
        } catch (Exception e) {
            Logger.severe("Failed to reload plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Getters for managers
    public static CrysisShot getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public boolean isPluginEnabled() {
        return pluginEnabled;
    }
}
