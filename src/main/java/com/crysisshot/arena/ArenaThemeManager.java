package com.crysisshot.arena;

import com.crysisshot.CrysisShot;
import com.crysisshot.localization.MessageManager;
import com.crysisshot.utils.Logger;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages arena themes, visual effects, and aesthetic configurations
 */
public class ArenaThemeManager {
    
    private final CrysisShot plugin;
    private final MessageManager messageManager;
    
    // Active theme effects per arena
    private final Map<String, BukkitTask> activeEffects = new ConcurrentHashMap<>();
    
    // Theme-specific configurations
    private final Map<Arena.Theme, ThemeConfiguration> themeConfigs = new HashMap<>();
    
    public ArenaThemeManager(CrysisShot plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        initializeThemeConfigurations();
    }
    
    /**
     * Initialize theme-specific configurations
     */
    private void initializeThemeConfigurations() {
        // Classic theme
        themeConfigs.put(Arena.Theme.CLASSIC, new ThemeConfiguration()
            .setAmbientParticle(Particle.FLAME)
            .setParticleInterval(60)
            .setAmbientSounds(Arrays.asList(Sound.AMBIENT_CAVE, Sound.BLOCK_FIRE_AMBIENT))
            .setSoundInterval(120)
            .setFogColor(Color.GRAY)
            .setBuildingMaterial(Material.COBBLESTONE)
            .setAccentMaterial(Material.STONE_BRICKS)
        );
        
        // Modern theme
        themeConfigs.put(Arena.Theme.MODERN, new ThemeConfiguration()
            .setAmbientParticle(Particle.CRIT)
            .setParticleInterval(80)
            .setAmbientSounds(Arrays.asList(Sound.BLOCK_BEACON_AMBIENT))
            .setSoundInterval(150)
            .setFogColor(Color.BLUE)
            .setBuildingMaterial(Material.QUARTZ_BLOCK)
            .setAccentMaterial(Material.IRON_BLOCK)
        );
        
        // Medieval theme
        themeConfigs.put(Arena.Theme.MEDIEVAL, new ThemeConfiguration()
            .setAmbientParticle(Particle.SMOKE_NORMAL)
            .setParticleInterval(100)
            .setAmbientSounds(Arrays.asList(Sound.AMBIENT_CAVE, Sound.ENTITY_BAT_AMBIENT))
            .setSoundInterval(180)
            .setFogColor(Color.fromRGB(101, 67, 33))
            .setBuildingMaterial(Material.STONE)
            .setAccentMaterial(Material.DARK_OAK_LOG)
        );
        
        // Futuristic theme
        themeConfigs.put(Arena.Theme.FUTURISTIC, new ThemeConfiguration()
            .setAmbientParticle(Particle.ELECTRIC_SPARK)
            .setParticleInterval(40)
            .setAmbientSounds(Arrays.asList(Sound.BLOCK_BEACON_POWER_SELECT))
            .setSoundInterval(100)
            .setFogColor(Color.LIME)
            .setBuildingMaterial(Material.PRISMARINE)
            .setAccentMaterial(Material.SEA_LANTERN)
        );
        
        // Desert theme
        themeConfigs.put(Arena.Theme.DESERT, new ThemeConfiguration()
            .setAmbientParticle(Particle.FALLING_DUST)
            .setParticleInterval(120)
            .setAmbientSounds(Arrays.asList(Sound.WEATHER_RAIN))
            .setSoundInterval(200)
            .setFogColor(Color.YELLOW)
            .setBuildingMaterial(Material.SANDSTONE)
            .setAccentMaterial(Material.TERRACOTTA)
        );
        
        // Winter theme
        themeConfigs.put(Arena.Theme.WINTER, new ThemeConfiguration()
            .setAmbientParticle(Particle.SNOWFLAKE)
            .setParticleInterval(50)
            .setAmbientSounds(Arrays.asList(Sound.WEATHER_RAIN))
            .setSoundInterval(160)
            .setFogColor(Color.WHITE)
            .setBuildingMaterial(Material.SNOW_BLOCK)
            .setAccentMaterial(Material.ICE)
        );
          // Urban theme
        themeConfigs.put(Arena.Theme.URBAN, new ThemeConfiguration()
            .setAmbientParticle(Particle.TOWN_AURA)
            .setParticleInterval(90)
            .setAmbientSounds(Arrays.asList(Sound.AMBIENT_CAVE))
            .setSoundInterval(140)
            .setFogColor(Color.GRAY)
            .setBuildingMaterial(Material.GRAY_CONCRETE)
            .setAccentMaterial(Material.GLASS)
        );
        
        // Hospital theme
        themeConfigs.put(Arena.Theme.HOSPITAL, new ThemeConfiguration()
            .setAmbientParticle(Particle.HEART)
            .setParticleInterval(150)
            .setAmbientSounds(Arrays.asList(Sound.BLOCK_BEACON_AMBIENT))
            .setSoundInterval(120)
            .setFogColor(Color.WHITE)
            .setBuildingMaterial(Material.WHITE_CONCRETE)
            .setAccentMaterial(Material.WHITE_GLAZED_TERRACOTTA)
        );
        
        // Temple theme
        themeConfigs.put(Arena.Theme.TEMPLE, new ThemeConfiguration()
            .setAmbientParticle(Particle.ENCHANTMENT_TABLE)
            .setParticleInterval(70)
            .setAmbientSounds(Arrays.asList(Sound.BLOCK_ENCHANTMENT_TABLE_USE))
            .setSoundInterval(130)
            .setFogColor(Color.PURPLE)
            .setBuildingMaterial(Material.PURPUR_BLOCK)
            .setAccentMaterial(Material.END_STONE_BRICKS)
        );
    }
    
    /**
     * Start theme effects for an arena
     */
    public void startThemeEffects(Arena arena) {
        if (arena == null || arena.getTheme() == null) return;
        
        stopThemeEffects(arena);
        
        ThemeConfiguration config = themeConfigs.get(arena.getTheme());
        if (config == null) return;
        
        BukkitTask effectTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (arena.getBoundaryMin() != null && arena.getBoundaryMax() != null) {
                spawnThemeParticles(arena, config);
                playThemeSounds(arena, config);
            }
        }, 0L, config.getParticleInterval());
        
        activeEffects.put(arena.getName(), effectTask);
        
        Logger.info("Started theme effects for arena: " + arena.getName() + " (Theme: " + arena.getTheme().getDisplayName() + ")");
    }
    
    /**
     * Stop theme effects for an arena
     */
    public void stopThemeEffects(Arena arena) {
        if (arena == null) return;
        
        BukkitTask task = activeEffects.remove(arena.getName());
        if (task != null) {
            task.cancel();
            Logger.info("Stopped theme effects for arena: " + arena.getName());
        }
    }
    
    /**
     * Spawn theme-specific particles in the arena
     */
    private void spawnThemeParticles(Arena arena, ThemeConfiguration config) {
        if (arena.getBoundaryMin() == null || arena.getBoundaryMax() == null) return;
        
        Location min = arena.getBoundaryMin();
        Location max = arena.getBoundaryMax();
        World world = min.getWorld();
        
        if (world == null) return;
        
        // Calculate random positions within arena bounds
        for (int i = 0; i < 3; i++) {
            double x = min.getX() + Math.random() * (max.getX() - min.getX());
            double y = min.getY() + Math.random() * (max.getY() - min.getY());
            double z = min.getZ() + Math.random() * (max.getZ() - min.getZ());
            
            Location particleLoc = new Location(world, x, y, z);
            
            // Spawn particles
            if (config.getAmbientParticle() != null) {
                world.spawnParticle(config.getAmbientParticle(), particleLoc, 1, 0.5, 0.5, 0.5, 0.01);
            }
        }
    }
    
    /**
     * Play theme-specific ambient sounds
     */
    private void playThemeSounds(Arena arena, ThemeConfiguration config) {
        if (config.getAmbientSounds() == null || config.getAmbientSounds().isEmpty()) return;
        
        // Play sounds for players in the arena
        Collection<Player> playersInArena = getPlayersInArena(arena);
        if (playersInArena.isEmpty()) return;
        
        Sound sound = config.getAmbientSounds().get(
            new Random().nextInt(config.getAmbientSounds().size())
        );
        
        for (Player player : playersInArena) {
            player.playSound(player.getLocation(), sound, 0.3f, 1.0f);
        }
    }
    
    /**
     * Get players currently in the arena
     */
    private Collection<Player> getPlayersInArena(Arena arena) {
        Collection<Player> players = new ArrayList<>();
        
        if (arena.getBoundaryMin() == null || arena.getBoundaryMax() == null) {
            return players;
        }
        
        Location min = arena.getBoundaryMin();
        Location max = arena.getBoundaryMax();
        World world = min.getWorld();
        
        if (world == null) return players;
        
        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            if (isLocationInBounds(loc, min, max)) {
                players.add(player);
            }
        }
        
        return players;
    }
    
    /**
     * Check if a location is within arena bounds
     */
    private boolean isLocationInBounds(Location loc, Location min, Location max) {
        return loc.getX() >= Math.min(min.getX(), max.getX()) &&
               loc.getX() <= Math.max(min.getX(), max.getX()) &&
               loc.getY() >= Math.min(min.getY(), max.getY()) &&
               loc.getY() <= Math.max(min.getY(), max.getY()) &&
               loc.getZ() >= Math.min(min.getZ(), max.getZ()) &&
               loc.getZ() <= Math.max(min.getZ(), max.getZ());
    }
    
    /**
     * Apply theme preview effects for a player
     */
    public void showThemePreview(Player player, Arena.Theme theme) {
        ThemeConfiguration config = themeConfigs.get(theme);
        if (config == null) return;
        
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        // Show particle effects around player
        for (int i = 0; i < 10; i++) {
            double angle = 2 * Math.PI * i / 10;
            double x = loc.getX() + 2 * Math.cos(angle);
            double z = loc.getZ() + 2 * Math.sin(angle);
            Location particleLoc = new Location(world, x, loc.getY() + 1, z);
            
            if (config.getAmbientParticle() != null) {
                world.spawnParticle(config.getAmbientParticle(), particleLoc, 3, 0.2, 0.2, 0.2, 0.05);
            }
        }
        
        // Play theme sound
        if (config.getAmbientSounds() != null && !config.getAmbientSounds().isEmpty()) {
            Sound sound = config.getAmbientSounds().get(0);
            player.playSound(loc, sound, 0.5f, 1.0f);
        }
        
        // Send theme information
        messageManager.sendMessage(player, "arena.theme-preview", 
            "theme", theme.getDisplayName(),
            "description", theme.getDescription());
    }
    
    /**
     * Get theme configuration
     */
    public ThemeConfiguration getThemeConfiguration(Arena.Theme theme) {
        return themeConfigs.get(theme);
    }
    
    /**
     * Get building guidelines for a theme
     */
    public List<String> getBuildingGuidelines(Arena.Theme theme) {
        ThemeConfiguration config = themeConfigs.get(theme);
        if (config == null) return Arrays.asList("No guidelines available for this theme.");
        
        List<String> guidelines = new ArrayList<>();
        guidelines.add("§6Building Guidelines for " + theme.getDisplayName() + ":");
        guidelines.add("§7Primary Material: §e" + config.getBuildingMaterial().name());
        guidelines.add("§7Accent Material: §e" + config.getAccentMaterial().name());
        guidelines.add("§7" + theme.getDescription());
        
        switch (theme) {
            case CLASSIC:
                guidelines.add("§7• Use cobblestone and stone brick structures");
                guidelines.add("§7• Include torches for lighting");
                guidelines.add("§7• Add medieval-style decorations");
                break;
            case MODERN:
                guidelines.add("§7• Clean lines and geometric shapes");
                guidelines.add("§7• Use glass and metal materials");
                guidelines.add("§7• Incorporate modern lighting");
                break;
            case MEDIEVAL:
                guidelines.add("§7• Castle-like architecture");
                guidelines.add("§7• Stone and wood construction");
                guidelines.add("§7• Include battlements and towers");
                break;
            case FUTURISTIC:
                guidelines.add("§7• Use prismarine and sea lanterns");
                guidelines.add("§7• Add glowing elements");
                guidelines.add("§7• Incorporate sci-fi structures");
                break;
            case DESERT:
                guidelines.add("§7• Sandstone and terracotta materials");
                guidelines.add("§7• Include cactus and sand features");
                guidelines.add("§7• Add oasis elements");
                break;
            case WINTER:
                guidelines.add("§7• Snow and ice materials");
                guidelines.add("§7• Include frozen water features");
                guidelines.add("§7• Add pine tree decorations");
                break;
            case URBAN:
                guidelines.add("§7• Concrete and glass buildings");
                guidelines.add("§7• Include street-like pathways");
                guidelines.add("§7• Add urban furniture");
                break;
            case HOSPITAL:
                guidelines.add("§7• Clean white surfaces");
                guidelines.add("§7• Include medical equipment");
                guidelines.add("§7• Add sterile lighting");
                break;
            case TEMPLE:
                guidelines.add("§7• Mystical purpur structures");
                guidelines.add("§7• Include enchanting elements");
                guidelines.add("§7• Add magical decorations");
                break;
        }
        
        return guidelines;
    }
    
    /**
     * Shutdown all active effects
     */
    public void shutdown() {
        for (BukkitTask task : activeEffects.values()) {
            task.cancel();
        }
        activeEffects.clear();
        Logger.info("Arena theme effects shut down");
    }
    
    /**
     * Theme configuration class
     */
    public static class ThemeConfiguration {
        private Particle ambientParticle;
        private int particleInterval = 60;
        private List<Sound> ambientSounds = new ArrayList<>();
        private int soundInterval = 120;
        private Color fogColor = Color.GRAY;
        private Material buildingMaterial = Material.STONE;
        private Material accentMaterial = Material.STONE_BRICKS;
        
        // Getters and setters
        public Particle getAmbientParticle() { return ambientParticle; }
        public ThemeConfiguration setAmbientParticle(Particle particle) { this.ambientParticle = particle; return this; }
        
        public int getParticleInterval() { return particleInterval; }
        public ThemeConfiguration setParticleInterval(int interval) { this.particleInterval = interval; return this; }
        
        public List<Sound> getAmbientSounds() { return ambientSounds; }
        public ThemeConfiguration setAmbientSounds(List<Sound> sounds) { this.ambientSounds = sounds; return this; }
        
        public int getSoundInterval() { return soundInterval; }
        public ThemeConfiguration setSoundInterval(int interval) { this.soundInterval = interval; return this; }
        
        public Color getFogColor() { return fogColor; }
        public ThemeConfiguration setFogColor(Color color) { this.fogColor = color; return this; }
        
        public Material getBuildingMaterial() { return buildingMaterial; }
        public ThemeConfiguration setBuildingMaterial(Material material) { this.buildingMaterial = material; return this; }
        
        public Material getAccentMaterial() { return accentMaterial; }
        public ThemeConfiguration setAccentMaterial(Material material) { this.accentMaterial = material; return this; }
    }
}
