package com.crysisshot.utils;

import com.crysisshot.CrysisShot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.logging.Level;

/**
 * Utility class for consistent logging throughout the plugin
 */
public class Logger {
    
    private static final String PREFIX = "[CrysisShot] ";
    
    /**
     * Log an info message
     */
    public static void info(String message) {
        log(Level.INFO, message);
    }
    
    /**
     * Log a warning message
     */
    public static void warning(String message) {
        log(Level.WARNING, message);
    }
    
    /**
     * Log a severe/error message
     */
    public static void severe(String message) {
        log(Level.SEVERE, message);
    }
    
    /**
     * Log a debug message (only if debug mode is enabled)
     */
    public static void debug(String message) {
        CrysisShot plugin = CrysisShot.getInstance();
        if (plugin != null && plugin.getConfigManager() != null && 
            plugin.getConfigManager().isDebugMode()) {
            log(Level.INFO, "[DEBUG] " + message);
        }
    }
    
    /**
     * Log a message with specified level
     */
    private static void log(Level level, String message) {
        if (Bukkit.getLogger() != null) {
            Bukkit.getLogger().log(level, PREFIX + message);
        } else {
            // Fallback for early initialization
            System.out.println(PREFIX + level.getName() + ": " + message);
        }
    }
    
    /**
     * Send a colored message to console
     */
    public static void console(String message) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8[&6CrysisShot&8] &r" + message));
    }
}
