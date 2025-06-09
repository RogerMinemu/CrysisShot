package com.crysisshot.arena;

import org.bukkit.Location;
import org.bukkit.World;
import java.util.List;
import java.util.Map;

/**
 * Represents a CrysisShot arena with all necessary configuration and properties
 */
public class Arena {
      /**
     * Arena themes for different visual styles
     */
    public enum Theme {
        CLASSIC("Classic", "§7A traditional arena setting"),
        MODERN("Modern", "§bA sleek, contemporary environment"),
        MEDIEVAL("Medieval", "§6Ancient castle and fortress style"),
        FUTURISTIC("Futuristic", "§aCyber-tech advanced setting"),
        DESERT("Desert", "§eArid wasteland environment"),
        WINTER("Winter", "§fFrozen tundra landscape"),
        URBAN("Urban", "§8City streets and buildings"),
        HOSPITAL("Hospital", "§fSterile medical facility"),
        TEMPLE("Temple", "§dMystical ancient temple");
        
        private final String displayName;
        private final String description;
        
        Theme(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static Theme fromString(String theme) {
            for (Theme t : values()) {
                if (t.name().equalsIgnoreCase(theme)) {
                    return t;
                }
            }
            return CLASSIC; // Default fallback
        }
    }
    
    /**
     * Arena state for management purposes
     */
    public enum ArenaState {
        AVAILABLE,    // Ready for games
        IN_USE,       // Currently hosting a game
        DISABLED,     // Temporarily disabled
        MAINTENANCE   // Under maintenance/setup
    }
    
    // Basic properties
    private final String name;
    private String displayName;
    private String worldName;
    private boolean enabled;
    private Theme theme;
    private ArenaState state;
    
    // Player configuration
    private int minPlayers;
    private int maxPlayers;
    
    // Spawn locations
    private Location lobbySpawn;
    private Location spectatorSpawn;
    private List<Location> spawnPoints;
    private List<Location> powerupLocations;
    
    // Arena boundaries
    private Location boundaryMin;
    private Location boundaryMax;
    
    // Arena-specific settings
    private int targetScore;
    private int powerupInterval;
    private String description;
    
    // Additional metadata
    private long lastUsed;
    private int gamesPlayed;
    private Map<String, Object> customSettings;
    
    /**
     * Constructor for creating a new arena
     */
    public Arena(String name) {
        this.name = name;
        this.displayName = name;
        this.enabled = false;
        this.theme = Theme.CLASSIC;
        this.state = ArenaState.DISABLED;
        this.minPlayers = 4;
        this.maxPlayers = 16;
        this.targetScore = 20;
        this.powerupInterval = 30;
        this.lastUsed = 0;
        this.gamesPlayed = 0;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { 
        this.enabled = enabled;
        if (!enabled && state == ArenaState.AVAILABLE) {
            state = ArenaState.DISABLED;
        }
    }
    
    public Theme getTheme() { return theme; }
    public void setTheme(Theme theme) { this.theme = theme; }
    
    public ArenaState getState() { return state; }
    public void setState(ArenaState state) { this.state = state; }
    
    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = Math.max(2, minPlayers); }
    
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = Math.max(this.minPlayers, maxPlayers); }
    
    public Location getLobbySpawn() { return lobbySpawn; }
    public void setLobbySpawn(Location lobbySpawn) { this.lobbySpawn = lobbySpawn; }
    
    public Location getSpectatorSpawn() { return spectatorSpawn; }
    public void setSpectatorSpawn(Location spectatorSpawn) { this.spectatorSpawn = spectatorSpawn; }
    
    public List<Location> getSpawnPoints() { return spawnPoints; }
    public void setSpawnPoints(List<Location> spawnPoints) { this.spawnPoints = spawnPoints; }
    
    public List<Location> getPowerupLocations() { return powerupLocations; }
    public void setPowerupLocations(List<Location> powerupLocations) { this.powerupLocations = powerupLocations; }
    
    public Location getBoundaryMin() { return boundaryMin; }
    public void setBoundaryMin(Location boundaryMin) { this.boundaryMin = boundaryMin; }
    
    public Location getBoundaryMax() { return boundaryMax; }
    public void setBoundaryMax(Location boundaryMax) { this.boundaryMax = boundaryMax; }
    
    public int getTargetScore() { return targetScore; }
    public void setTargetScore(int targetScore) { this.targetScore = Math.max(1, targetScore); }
    
    public int getPowerupInterval() { return powerupInterval; }
    public void setPowerupInterval(int powerupInterval) { this.powerupInterval = Math.max(10, powerupInterval); }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public long getLastUsed() { return lastUsed; }
    public void setLastUsed(long lastUsed) { this.lastUsed = lastUsed; }
    
    public int getGamesPlayed() { return gamesPlayed; }
    public void incrementGamesPlayed() { this.gamesPlayed++; }
    
    public Map<String, Object> getCustomSettings() { return customSettings; }
    public void setCustomSettings(Map<String, Object> customSettings) { this.customSettings = customSettings; }
    
    // Utility methods
    
    /**
     * Check if the arena is ready to host games
     */
    public boolean isReady() {
        return enabled && 
               state == ArenaState.AVAILABLE && 
               lobbySpawn != null && 
               spawnPoints != null && 
               spawnPoints.size() >= minPlayers &&
               isWorldLoaded();
    }
    
    /**
     * Check if the arena's world is loaded
     */
    public boolean isWorldLoaded() {
        if (worldName == null) return false;
        World world = org.bukkit.Bukkit.getWorld(worldName);
        return world != null;
    }
    
    /**
     * Check if a location is within arena boundaries
     */
    public boolean isWithinBoundaries(Location location) {
        if (boundaryMin == null || boundaryMax == null) return true;
        if (!location.getWorld().getName().equals(worldName)) return false;
        
        return location.getX() >= boundaryMin.getX() && location.getX() <= boundaryMax.getX() &&
               location.getY() >= boundaryMin.getY() && location.getY() <= boundaryMax.getY() &&
               location.getZ() >= boundaryMin.getZ() && location.getZ() <= boundaryMax.getZ();
    }
    
    /**
     * Get a random spawn point for players
     */
    public Location getRandomSpawnPoint() {
        if (spawnPoints == null || spawnPoints.isEmpty()) return lobbySpawn;
        return spawnPoints.get((int) (Math.random() * spawnPoints.size()));
    }
    
    /**
     * Get a random power-up location
     */
    public Location getRandomPowerupLocation() {
        if (powerupLocations == null || powerupLocations.isEmpty()) return null;
        return powerupLocations.get((int) (Math.random() * powerupLocations.size()));
    }
    
    /**
     * Mark arena as used (for statistics)
     */
    public void markAsUsed() {
        this.lastUsed = System.currentTimeMillis();
        incrementGamesPlayed();
    }
    
    /**
     * Check if arena can accommodate the specified number of players
     */
    public boolean canAccommodate(int playerCount) {
        return playerCount >= minPlayers && 
               playerCount <= maxPlayers && 
               (spawnPoints == null || spawnPoints.size() >= playerCount);
    }
    
    @Override
    public String toString() {
        return String.format("Arena{name='%s', displayName='%s', enabled=%s, state=%s, players=%d-%d, theme=%s}", 
            name, displayName, enabled, state, minPlayers, maxPlayers, theme);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Arena arena = (Arena) obj;
        return name.equals(arena.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
