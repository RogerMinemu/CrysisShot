package com.crysisshot.config;

import com.crysisshot.CrysisShot;
import com.crysisshot.utils.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * Manages plugin configuration files
 */
public class ConfigManager {
    
    private final CrysisShot plugin;
    private FileConfiguration config;
    private FileConfiguration arenasConfig;
    private FileConfiguration messagesConfig; // Assuming you might add this later based on MessageManager
    
    // Configuration file objects
    private File configFile;
    private File arenasFile;
    private File messagesFile; // Assuming you might add this later
    
    public ConfigManager(CrysisShot plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize and load all configuration files
     */
    public void initialize() { // Renamed from loadConfigurations
        try {
            // Create plugin folder if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Load main config
            loadMainConfig();
            
            // Load arenas config
            loadArenasConfig();

            // Future: Load messages config if you create a separate one
            // loadMessagesConfig(); 
            
            Logger.info("Configuration files loaded successfully!");
            
        } catch (Exception e) {
            Logger.severe("Failed to load configuration files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load main configuration file
     */
    private void loadMainConfig() throws IOException {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            saveDefaultConfig("config.yml");
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Validate and update config if needed
        validateMainConfig();
    }
    
    /**
     * Load arenas configuration file
     */
    private void loadArenasConfig() throws IOException {
        arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        
        // Create default arenas config if it doesn't exist
        if (!arenasFile.exists()) {
            saveDefaultConfig("arenas.yml");
        }
        
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
    }
    
    /**
     * Save a default configuration file from resources
     */
    private void saveDefaultConfig(String fileName) throws IOException {
        try (InputStream inputStream = plugin.getResource(fileName)) {
            if (inputStream != null) {
                File targetFile = new File(plugin.getDataFolder(), fileName);
                Files.copy(inputStream, targetFile.toPath());
                Logger.info("Created default " + fileName);
            } else {
                Logger.warning("Default " + fileName + " not found in resources!");
            }
        }
    }
    
    /**
     * Validate main configuration and set defaults for missing values
     */
    private void validateMainConfig() {
        boolean modified = false;
        
        // Game settings
        if (!config.contains("game.target-score")) {
            config.set("game.target-score", 20);
            modified = true;
        }
        if (!config.contains("game.max-players")) {
            config.set("game.max-players", 16);
            modified = true;
        }
        if (!config.contains("game.min-players")) {
            config.set("game.min-players", 4);
            modified = true;
        }        if (!config.contains("game.respawn-delay")) {
            config.set("game.respawn-delay", 3);
            modified = true;
        }
        if (!config.contains("game.starting-arrows")) {
            config.set("game.starting-arrows", 1);
            modified = true;
        }
        if (!config.contains("game.combo-thresholds")) {
            config.set("game.combo-thresholds", List.of(3, 6));
            modified = true;
        }
        if (!config.contains("game.combo-multipliers")) {
            config.set("game.combo-multipliers", List.of(2, 3));
            modified = true;
        }
        
        // Power-up settings
        if (!config.contains("powerups.enabled")) {
            config.set("powerups.enabled", true);
            modified = true;
        }
        if (!config.contains("powerups.spawn-interval")) {
            config.set("powerups.spawn-interval", 30);
            modified = true;
        }
        if (!config.contains("powerups.duration.speed")) {
            config.set("powerups.duration.speed", 10);
            modified = true;
        }
        if (!config.contains("powerups.duration.invisibility")) {
            config.set("powerups.duration.invisibility", 7);
            modified = true;
        }
        
        // Arena settings
        if (!config.contains("arenas.selection-mode")) {
            config.set("arenas.selection-mode", "random");
            modified = true;
        }
        
        // Locale settings
        if (!config.contains("locale.default-language")) {
            config.set("locale.default-language", "en");
            modified = true;
        }
        if (!config.contains("locale.supported-languages")) {
            config.set("locale.supported-languages", List.of("en", "es", "fr", "de"));
            modified = true;
        }
        
        // Database settings
        if (!config.contains("database.type")) {
            config.set("database.type", "sqlite");
            modified = true;
        }
        if (!config.contains("database.file")) {
            config.set("database.file", "crysisshot.db");
            modified = true;
        }
        
        // Debug settings
        if (!config.contains("debug.enabled")) {
            config.set("debug.enabled", false);
            modified = true;
        }
        
        // Save config if modified
        if (modified) {
            saveMainConfig();
            Logger.info("Configuration updated with default values!");
        }
    }
    
    /**
     * Save main configuration file
     */
    public void saveMainConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            Logger.severe("Failed to save config.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reloads the main configuration file.
     */
    public void reloadConfig() {
        try {
            loadMainConfig();
            Logger.info("config.yml reloaded successfully!");
        } catch (IOException e) {
            Logger.severe("Failed to reload config.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Getter methods for main config
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public String getString(String path) {
        return config.getString(path);
    }
    
    public String getString(String path, String def) {
        return config.getString(path, def);
    }
    
    public int getInt(String path) {
        return config.getInt(path);
    }
    
    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }
    
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }
    
    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }
    
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    public org.bukkit.configuration.ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }
    
    // Getter methods for arenas config
    
    public FileConfiguration getArenasConfig() {
        return arenasConfig;
    }
    
    // Future: Getter methods for messages config
    // public FileConfiguration getMessagesConfig() {
    //     return messagesConfig;
    // }
    
    // Example of how you might load a separate messages.yml, if you decide to
    /*
    private void loadMessagesConfig() throws IOException {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveDefaultConfig("messages.yml");
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    */
    
    // Specific getter methods for common config values
    
    public String getDefaultLanguage() {
        return getString("locale.default-language", "en");
    }
    
    public List<String> getSupportedLanguages() {
        return getStringList("locale.supported-languages");
    }
    
    public int getTargetScore() {
        return getInt("game.target-score", 20);
    }
    
    public int getMaxPlayers() {
        return getInt("game.max-players", 16);
    }
    
    public int getMinPlayers() {
        return getInt("game.min-players", 4);
    }
    
    public int getMinPlayersPerSession() {
        return getMinPlayers();
    }
    
    public int getMaxPlayersPerSession() {
        return getMaxPlayers();
    }
    
    public int getRespawnDelay() {
        return getInt("game.respawn-delay", 3);
    }
    
    public int getStartingArrows() {
        return getInt("game.starting-arrows", 1);
    }
    
    public List<Integer> getComboThresholds() {
        return config.getIntegerList("game.combo-thresholds");
    }
    
    public List<Integer> getComboMultipliers() {
        return config.getIntegerList("game.combo-multipliers");
    }
    
    public boolean isDebugMode() {
        return getBoolean("debug.enabled", false);
    }
}
