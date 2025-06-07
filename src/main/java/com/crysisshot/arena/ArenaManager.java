package com.crysisshot.arena;

import com.crysisshot.CrysisShot;
import com.crysisshot.config.ConfigManager;
import com.crysisshot.localization.MessageManager;
import com.crysisshot.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all arena operations including loading, validation, and selection
 */
public class ArenaManager {
    
    private final CrysisShot plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    
    // Arena storage
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private final Map<String, Arena> availableArenas = new ConcurrentHashMap<>();
    
    // Arena configuration file
    private File arenasFile;
    private YamlConfiguration arenasConfig;
    
    // Arena selection algorithm settings
    private ArenaSelectionAlgorithm selectionAlgorithm = ArenaSelectionAlgorithm.LEAST_RECENTLY_USED;
    
    /**
     * Arena selection algorithms
     */
    public enum ArenaSelectionAlgorithm {
        RANDOM,
        LEAST_RECENTLY_USED,
        LEAST_PLAYED,
        ROUND_ROBIN
    }
    
    // Round robin tracking
    private int roundRobinIndex = 0;
    
    public ArenaManager(CrysisShot plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        
        initializeArenaSystem();
    }
    
    /**
     * Initialize the arena management system
     */
    private void initializeArenaSystem() {
        createArenasFile();
        loadArenasFromConfig();
        validateAllArenas();
        updateAvailableArenas();
        
        Logger.info("Arena Manager initialized with " + arenas.size() + " arenas");
        Logger.info("Available arenas: " + availableArenas.size());
    }
    
    /**
     * Create or load the arenas.yml file
     */
    private void createArenasFile() {
        arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        
        if (!arenasFile.exists()) {
            plugin.saveResource("arenas.yml", false);
        }
        
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
    }
    
    /**
     * Load all arenas from configuration
     */
    public void loadArenasFromConfig() {
        arenas.clear();
        
        ConfigurationSection arenasSection = arenasConfig.getConfigurationSection("arenas");
        if (arenasSection == null) {
            Logger.warning("No arenas section found in arenas.yml");
            return;
        }
        
        for (String arenaName : arenasSection.getKeys(false)) {
            try {
                Arena arena = loadArenaFromConfig(arenaName, arenasSection.getConfigurationSection(arenaName));
                if (arena != null) {
                    arenas.put(arenaName, arena);
                    Logger.info("Loaded arena: " + arenaName);
                }
            } catch (Exception e) {
                Logger.severe("Failed to load arena: " + arenaName + " - " + e.getMessage());
            }
        }
        
        updateAvailableArenas();
    }
    
    /**
     * Load a single arena from configuration section
     */
    private Arena loadArenaFromConfig(String name, ConfigurationSection section) {
        if (section == null) return null;
        
        Arena arena = new Arena(name);
        
        // Basic properties
        arena.setDisplayName(section.getString("display-name", name));
        arena.setWorldName(section.getString("world"));
        arena.setEnabled(section.getBoolean("enabled", false));
        arena.setTheme(Arena.Theme.fromString(section.getString("theme", "URBAN")));
        
        // Player configuration
        arena.setMinPlayers(section.getInt("min-players", 4));
        arena.setMaxPlayers(section.getInt("max-players", 16));
        
        // Load spawn locations
        arena.setLobbySpawn(loadLocation(section.getConfigurationSection("lobby-spawn"), arena.getWorldName()));
        arena.setSpectatorSpawn(loadLocation(section.getConfigurationSection("spectator-spawn"), arena.getWorldName()));
        
        // Load spawn points
        List<Location> spawnPoints = loadLocationList(section.getConfigurationSection("spawn-points"), arena.getWorldName());
        arena.setSpawnPoints(spawnPoints);
        
        // Load power-up locations
        List<Location> powerupLocations = loadLocationList(section.getConfigurationSection("powerup-locations"), arena.getWorldName());
        arena.setPowerupLocations(powerupLocations);
        
        // Load boundaries
        ConfigurationSection boundaries = section.getConfigurationSection("boundaries");
        if (boundaries != null) {
            arena.setBoundaryMin(loadLocation(boundaries.getConfigurationSection("min"), arena.getWorldName()));
            arena.setBoundaryMax(loadLocation(boundaries.getConfigurationSection("max"), arena.getWorldName()));
        }
        
        // Load arena-specific settings
        ConfigurationSection settings = section.getConfigurationSection("settings");
        if (settings != null) {
            arena.setTargetScore(settings.getInt("target-score", 20));
            arena.setPowerupInterval(settings.getInt("powerup-interval", 30));
            arena.setDescription(settings.getString("description", ""));
        }
        
        return arena;
    }
    
    /**
     * Load a location from configuration section
     */
    private Location loadLocation(ConfigurationSection section, String worldName) {
        if (section == null || worldName == null) return null;
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        
        double x = section.getDouble("x", 0);
        double y = section.getDouble("y", 64);
        double z = section.getDouble("z", 0);
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    /**
     * Load a list of locations from configuration
     */
    private List<Location> loadLocationList(ConfigurationSection section, String worldName) {
        List<Location> locations = new ArrayList<>();
        if (section == null || worldName == null) return locations;
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) return locations;
        
        for (String key : section.getKeys(false)) {
            ConfigurationSection locSection = section.getConfigurationSection(key);
            if (locSection != null) {
                Location loc = loadLocation(locSection, worldName);
                if (loc != null) {
                    locations.add(loc);
                }
            } else {
                // Handle inline format: - {x: 10.5, y: 64.0, z: 10.5, yaw: 45.0, pitch: 0.0}
                Object value = section.get(key);
                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> locMap = (Map<String, Object>) value;
                    double x = getDoubleFromMap(locMap, "x", 0);
                    double y = getDoubleFromMap(locMap, "y", 64);
                    double z = getDoubleFromMap(locMap, "z", 0);
                    float yaw = (float) getDoubleFromMap(locMap, "yaw", 0);
                    float pitch = (float) getDoubleFromMap(locMap, "pitch", 0);
                    
                    locations.add(new Location(world, x, y, z, yaw, pitch));
                }
            }
        }
        
        return locations;
    }
    
    /**
     * Helper method to safely get double values from map
     */
    private double getDoubleFromMap(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    /**
     * Validate all loaded arenas
     */
    public void validateAllArenas() {
        List<String> invalidArenas = new ArrayList<>();
        
        for (Arena arena : arenas.values()) {
            List<String> errors = validateArena(arena);
            if (!errors.isEmpty()) {
                invalidArenas.add(arena.getName());
                arena.setEnabled(false);
                Logger.warning("Arena " + arena.getName() + " failed validation: " + String.join(", ", errors));
            }
        }
        
        if (!invalidArenas.isEmpty()) {
            Logger.warning("Disabled " + invalidArenas.size() + " invalid arenas: " + String.join(", ", invalidArenas));
        }
    }
    
    /**
     * Validate a single arena
     */
    public List<String> validateArena(Arena arena) {
        List<String> errors = new ArrayList<>();
        
        // Check world exists
        if (arena.getWorldName() == null || !arena.isWorldLoaded()) {
            errors.add("World not found or not loaded: " + arena.getWorldName());
        }
        
        // Check required spawns
        if (arena.getLobbySpawn() == null) {
            errors.add("Lobby spawn not set");
        }
        
        // Check spawn points
        if (arena.getSpawnPoints() == null || arena.getSpawnPoints().isEmpty()) {
            errors.add("No spawn points defined");
        } else if (arena.getSpawnPoints().size() < arena.getMinPlayers()) {
            errors.add("Not enough spawn points for minimum players (" + 
                      arena.getSpawnPoints().size() + " < " + arena.getMinPlayers() + ")");
        }
        
        // Check spawn point distances
        if (arena.getSpawnPoints() != null && arena.getSpawnPoints().size() > 1) {
            double minDistance = getValidationSetting("min-spawn-distance", 5.0);
            double maxDistance = getValidationSetting("max-spawn-distance", 100.0);
            
            for (int i = 0; i < arena.getSpawnPoints().size(); i++) {
                for (int j = i + 1; j < arena.getSpawnPoints().size(); j++) {
                    Location loc1 = arena.getSpawnPoints().get(i);
                    Location loc2 = arena.getSpawnPoints().get(j);
                    double distance = loc1.distance(loc2);
                    
                    if (distance < minDistance) {
                        errors.add("Spawn points too close: " + distance + " < " + minDistance);
                    } else if (distance > maxDistance) {
                        errors.add("Spawn points too far: " + distance + " > " + maxDistance);
                    }
                }
            }
        }
        
        // Check power-up locations
        int minPowerupLocations = (int) getValidationSetting("min-powerup-locations", 3);
        if (arena.getPowerupLocations() == null || arena.getPowerupLocations().size() < minPowerupLocations) {
            errors.add("Not enough power-up locations (minimum: " + minPowerupLocations + ")");
        }
        
        return errors;
    }
    
    /**
     * Get validation setting from config
     */
    private double getValidationSetting(String key, double defaultValue) {
        ConfigurationSection validation = arenasConfig.getConfigurationSection("validation");
        if (validation != null) {
            return validation.getDouble(key, defaultValue);
        }
        return defaultValue;
    }
    
    /**
     * Update the list of available arenas
     */
    public void updateAvailableArenas() {
        availableArenas.clear();
        
        for (Arena arena : arenas.values()) {
            if (arena.isReady()) {
                availableArenas.put(arena.getName(), arena);
            }
        }
        
        Logger.info("Updated available arenas: " + availableArenas.size() + "/" + arenas.size());
    }
    
    /**
     * Select an arena for a game with specified player count
     */
    public Arena selectArena(int playerCount) {
        List<Arena> suitableArenas = availableArenas.values().stream()
            .filter(arena -> arena.canAccommodate(playerCount))
            .collect(Collectors.toList());
        
        if (suitableArenas.isEmpty()) {
            return null;
        }
        
        return selectArenaByAlgorithm(suitableArenas);
    }
    
    /**
     * Select arena using the configured algorithm
     */
    private Arena selectArenaByAlgorithm(List<Arena> arenas) {
        switch (selectionAlgorithm) {
            case RANDOM:
                return arenas.get(new Random().nextInt(arenas.size()));
                
            case LEAST_RECENTLY_USED:
                return arenas.stream()
                    .min(Comparator.comparing(Arena::getLastUsed))
                    .orElse(arenas.get(0));
                    
            case LEAST_PLAYED:
                return arenas.stream()
                    .min(Comparator.comparing(Arena::getGamesPlayed))
                    .orElse(arenas.get(0));
                    
            case ROUND_ROBIN:
                Arena selected = arenas.get(roundRobinIndex % arenas.size());
                roundRobinIndex++;
                return selected;
                
            default:
                return arenas.get(0);
        }
    }
    
    /**
     * Save arena configuration to file
     */
    public void saveArenasConfig() {
        try {
            arenasConfig.save(arenasFile);
            Logger.info("Saved arenas configuration");
        } catch (IOException e) {
            Logger.severe("Failed to save arenas configuration: " + e.getMessage());
        }
    }
    
    // Public API methods
    
    public Arena getArena(String name) {
        return arenas.get(name);
    }
    
    public Collection<Arena> getAllArenas() {
        return arenas.values();
    }
    
    public Collection<Arena> getAvailableArenas() {
        return availableArenas.values();
    }
    
    public boolean isArenaAvailable(String name) {
        return availableArenas.containsKey(name);
    }
    
    public void setArenaState(String name, Arena.ArenaState state) {
        Arena arena = arenas.get(name);
        if (arena != null) {
            arena.setState(state);
            updateAvailableArenas();
        }
    }
    
    public ArenaSelectionAlgorithm getSelectionAlgorithm() {
        return selectionAlgorithm;
    }
    
    public void setSelectionAlgorithm(ArenaSelectionAlgorithm algorithm) {
        this.selectionAlgorithm = algorithm;
    }
    
    /**
     * Reload arena configuration
     */
    public void reload() {
        Logger.info("Reloading arena configuration...");
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        loadArenasFromConfig();
        validateAllArenas();
        updateAvailableArenas();
        Logger.info("Arena configuration reloaded");
    }
    
    /**
     * Get arena statistics
     */
    public Map<String, Object> getArenaStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_arenas", arenas.size());
        stats.put("available_arenas", availableArenas.size());
        stats.put("total_games_played", arenas.values().stream().mapToInt(Arena::getGamesPlayed).sum());
        
        // Most played arena
        Arena mostPlayed = arenas.values().stream()
            .max(Comparator.comparing(Arena::getGamesPlayed))
            .orElse(null);
        if (mostPlayed != null) {
            stats.put("most_played_arena", mostPlayed.getName());
            stats.put("most_played_games", mostPlayed.getGamesPlayed());
        }
        
        return stats;
    }
}
