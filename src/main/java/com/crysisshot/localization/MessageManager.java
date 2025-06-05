package com.crysisshot.localization;

import com.crysisshot.CrysisShot;
import com.crysisshot.config.ConfigManager;
import com.crysisshot.utils.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages localized messages for the plugin
 */
public class MessageManager {
    
    private final CrysisShot plugin;
    private final ConfigManager configManager;
    private final Map<String, FileConfiguration> locales = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    
    private String defaultLanguage;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    public MessageManager(CrysisShot plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.defaultLanguage = configManager.getDefaultLanguage();
    }
    
    /**
     * Load all message files
     */
    public void loadMessages() {
        try {
            // Create locales folder
            File localesFolder = new File(plugin.getDataFolder(), "locales");
            if (!localesFolder.exists()) {
                localesFolder.mkdirs();
            }
            
            // Load supported languages
            for (String language : configManager.getSupportedLanguages()) {
                loadLanguage(language);
            }
            
            Logger.info("Loaded " + locales.size() + " language files!");
            
        } catch (Exception e) {
            Logger.severe("Failed to load message files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load a specific language file
     */
    private void loadLanguage(String language) throws IOException {
        File languageFile = new File(plugin.getDataFolder(), "locales/" + language + ".yml");
        
        // Create default language file if it doesn't exist
        if (!languageFile.exists()) {
            saveDefaultLanguageFile(language);
        }
        
        FileConfiguration locale = YamlConfiguration.loadConfiguration(languageFile);
        locales.put(language, locale);
        
        Logger.debug("Loaded language: " + language);
    }
    
    /**
     * Save default language file from resources
     */
    private void saveDefaultLanguageFile(String language) throws IOException {
        String fileName = "locales/" + language + ".yml";
        
        try (InputStream inputStream = plugin.getResource(fileName)) {
            if (inputStream != null) {
                File targetFile = new File(plugin.getDataFolder(), fileName);
                Files.copy(inputStream, targetFile.toPath());
                Logger.info("Created default language file: " + language + ".yml");
            } else {
                // Create empty file with basic structure if resource doesn't exist
                createEmptyLanguageFile(language);
            }
        }
    }
    
    /**
     * Create an empty language file with basic structure
     */
    private void createEmptyLanguageFile(String language) throws IOException {
        File languageFile = new File(plugin.getDataFolder(), "locales/" + language + ".yml");
        FileConfiguration locale = new YamlConfiguration();
        
        // Add basic message structure
        locale.set("messages.game.join-success", "&aYou joined the game queue!");
        locale.set("messages.game.leave-success", "&eYou left the game!");
        locale.set("messages.errors.no-permission", "&cYou don't have permission to use this command!");
        locale.set("messages.errors.player-not-found", "&cPlayer not found: {player}");
        
        locale.save(languageFile);
        Logger.info("Created empty language file: " + language + ".yml");
    }
    
    /**
     * Get a message for a player in their preferred language
     */
    public String getMessage(String key, Player player, String... placeholders) {
        String language = getPlayerLanguage(player);
        return getMessage(key, language, placeholders);
    }
    
    /**
     * Get a message in a specific language
     */
    public String getMessage(String key, String language, String... placeholders) {
        FileConfiguration locale = locales.getOrDefault(language, locales.get(defaultLanguage));
        
        if (locale == null) {
            Logger.warning("No locale found for language: " + language);
            return "Missing message: " + key;
        }
        
        String message = locale.getString("messages." + key);
        if (message == null) {
            // Try default language as fallback
            if (!language.equals(defaultLanguage)) {
                FileConfiguration defaultLocale = locales.get(defaultLanguage);
                if (defaultLocale != null) {
                    message = defaultLocale.getString("messages." + key);
                }
            }
            
            if (message == null) {
                Logger.warning("Missing message key: " + key);
                return "Missing message: " + key;
            }
        }
        
        // Replace placeholders
        message = replacePlaceholders(message, placeholders);
        
        // Convert color codes
        return LegacyComponentSerializer.legacyAmpersand().serialize(
            miniMessage.deserialize(message)
        );
    }
    
    /**
     * Send a message to a player
     */
    public void sendMessage(Player player, String key, String... placeholders) {
        String message = getMessage(key, player, placeholders);
        player.sendMessage(message);
    }
    
    /**
     * Send a message to a player as a Component
     */
    public void sendComponent(Player player, String key, String... placeholders) {
        String message = getMessage(key, player, placeholders);
        Component component = miniMessage.deserialize(message);
        player.sendMessage(component);
    }
    
    /**
     * Get a Component message for a player
     */
    public Component getComponent(String key, Player player, String... placeholders) {
        String message = getMessage(key, player, placeholders);
        return miniMessage.deserialize(message);
    }
    
    /**
     * Replace placeholders in a message
     */
    private String replacePlaceholders(String message, String... placeholders) {
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return message;
    }
    
    /**
     * Get the preferred language for a player
     */
    public String getPlayerLanguage(Player player) {
        if (player == null) {
            return defaultLanguage;
        }
        
        String language = playerLanguages.get(player.getUniqueId());
        
        if (language == null) {
            // Auto-detect language if enabled
            if (configManager.getConfig().getBoolean("locale.auto-detect", true)) {
                language = detectPlayerLanguage(player);
            }
            
            if (language == null || !locales.containsKey(language)) {
                language = defaultLanguage;
            }
            
            playerLanguages.put(player.getUniqueId(), language);
        }
        
        return language;
    }
    
    /**
     * Set the preferred language for a player
     */
    public void setPlayerLanguage(Player player, String language) {
        if (locales.containsKey(language)) {
            playerLanguages.put(player.getUniqueId(), language);
            sendMessage(player, "commands.language-changed", "language", language);
        } else {
            sendMessage(player, "errors.invalid-language", "language", language);
        }
    }
      /**
     * Detect player language from client locale
     */
    private String detectPlayerLanguage(Player player) {
        try {
            String clientLocale = player.locale().toString();
            if (clientLocale != null && clientLocale.length() >= 2) {
                String languageCode = clientLocale.substring(0, 2).toLowerCase();
                
                // Check if we have this language
                if (locales.containsKey(languageCode)) {
                    return languageCode;
                }
            }
        } catch (Exception e) {
            Logger.debug("Could not detect language for player " + player.getName() + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Remove player language preference when they leave
     */
    public void removePlayer(Player player) {
        playerLanguages.remove(player.getUniqueId());
    }
    
    /**
     * Get all available languages
     */
    public Map<String, FileConfiguration> getLocales() {
        return new HashMap<>(locales);
    }
    
    /**
     * Get default language
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    /**
     * Reload all message files
     */
    public void reload() {
        locales.clear();
        playerLanguages.clear();
        defaultLanguage = configManager.getDefaultLanguage();
        loadMessages();
    }
}
