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
    
    // Configuration file objects
    private File configFile;
    private File arenasFile;
    
    public ConfigManager(CrysisShot plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all configuration files
     */
    public void loadConfigurations() {
        try {
            // Create plugin folder if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Load main config
            loadMainConfig();
            
            // Load arenas config
            loadArenasConfig();
            
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
        }
        if (!config.contains("game.respawn-delay")) {
            config.set("game.respawn-delay", 3);
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
        }
    }
    
    /**
     * Save arenas configuration file
     */
    public void saveArenasConfig() {
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            Logger.severe("Failed to save arenas.yml: " + e.getMessage());
        }
    }
    
    // Getter methods for configuration values
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getArenasConfig() {
        return arenasConfig;
    }
    
    // Game settings
    public int getTargetScore() {
        return config.getInt("game.target-score", 20);
    }
    
    public int getMaxPlayers() {
        return config.getInt("game.max-players", 16);
    }
    
    public int getMinPlayers() {
        return config.getInt("game.min-players", 4);
    }
    
    public int getRespawnDelay() {
        return config.getInt("game.respawn-delay", 3);
    }
    
    public List<Integer> getComboThresholds() {
        return config.getIntegerList("game.combo-thresholds");
    }
    
    public List<Integer> getComboMultipliers() {
        return config.getIntegerList("game.combo-multipliers");
    }
    
    // Power-up settings
    public boolean arePowerupsEnabled() {
        return config.getBoolean("powerups.enabled", true);
    }
    
    public int getPowerupSpawnInterval() {
        return config.getInt("powerups.spawn-interval", 30);
    }
    
    public int getSpeedDuration() {
        return config.getInt("powerups.duration.speed", 10);
    }
    
    public int getInvisibilityDuration() {
        return config.getInt("powerups.duration.invisibility", 7);
    }
    
    // Arena settings
    public String getArenaSelectionMode() {
        return config.getString("arenas.selection-mode", "random");
    }
    
    // Locale settings
    public String getDefaultLanguage() {
        return config.getString("locale.default-language", "en");
    }
    
    public List<String> getSupportedLanguages() {
        return config.getStringList("locale.supported-languages");
    }
    
    // Database settings
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }
    
    public String getDatabaseFile() {
        return config.getString("database.file", "crysisshot.db");
    }
    
    // Debug settings
    public boolean isDebugMode() {
        return config.getBoolean("debug.enabled", false);
    }
    
    // Session-specific methods for matchmaking
    public int getMinPlayersPerSession() {
        return getMinPlayers();
    }
    
    public int getMaxPlayersPerSession() {
        return getMaxPlayers();
    }
}
