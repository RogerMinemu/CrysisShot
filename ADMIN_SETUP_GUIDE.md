# CrysisShot Plugin - Admin Setup Guide

## Table of Contents
1. [Initial Plugin Setup](#initial-plugin-setup)
2. [Configuration Files](#configuration-files)
3. [Arena Setup Guide](#arena-setup-guide)
4. [Theme Configuration](#theme-configuration)
5. [Commands Reference](#commands-reference)
6. [Troubleshooting](#troubleshooting)

## Initial Plugin Setup

### Prerequisites
- Minecraft server running Paper/Spigot 1.20+
- Admin/operator permissions
- World edit permissions (recommended)

### Installation Steps

1. **Download and Install**
   ```
   1. Place CrysisShot.jar in your /plugins/ folder
   2. Restart the server
   3. Plugin will generate default configuration files
   ```

2. **Initial Configuration**
   ```
   /cs admin reload
   ```

3. **Verify Installation**
   ```
   /cs help
   ```

## Configuration Files

### Main Configuration (`config.yml`)

Located in `/plugins/CrysisShot/config.yml`

```yaml
# Game Settings
game:
  target-score: 20              # Points needed to win
  min-players: 4                # Minimum players to start
  max-players: 16               # Maximum players per game
  respawn-delay: 3              # Seconds before respawn
  starting-arrows: 1            # Arrows each player starts with
  combo-thresholds: [3, 5, 7]  # Kill streak thresholds for bonuses
  combo-multipliers: [2, 3, 5] # Score multipliers for streaks
  default-arena-world: "world"  # Default world for arenas

# Localization
locale:
  default-language: "en"        # Default language
  supported-languages: ["en", "es", "fr"]

# Debug
debug:
  enabled: false                # Enable debug messages
```

### Arena Configuration (`arenas.yml`)

Located in `/plugins/CrysisShot/arenas.yml`

```yaml
arenas:
  arena1:
    name: "Desert Arena"
    world: "arena_world"
    theme: "DESERT"
    min-players: 4
    max-players: 12
    spawn-points:
      - world: "arena_world"
        x: 100.5
        y: 64.0
        z: 200.5
        yaw: 0.0
        pitch: 0.0
      # Add more spawn points...
    bounds:
      min:
        x: 50
        y: 60
        z: 150
      max:
        x: 150
        y: 80
        z: 250
    enabled: true
```

## Arena Setup Guide

### Step 1: Create Arena World

1. **Create a new world** (recommended):
   ```
   /mv create arena_world normal
   ```

2. **Or use existing world**:
   - Note the world name for configuration

### Step 2: Build Arena Structure

1. **Design Requirements**:
   - Open area for bow combat (minimum 50x50 blocks)
   - Height clearance (recommend 20+ blocks)
   - Natural or built cover/obstacles
   - No water/lava in main combat area

2. **Recommended Arena Features**:
   - Multiple levels/platforms
   - Strategic cover points
   - Clear sight lines
   - Spawn point separation

### Step 3: Setup Arena Using Commands

#### Basic Arena Creation

1. **Start Arena Setup**:
   ```
   /cs admin arena create <arena-name>
   ```

2. **Set Arena World**:
   ```
   /cs admin arena set-world <arena-name> <world-name>
   ```

3. **Set Arena Theme**:
   ```
   /cs admin arena set-theme <arena-name> <theme>
   ```
   Available themes: `MEDIEVAL`, `FUTURISTIC`, `DESERT`, `FOREST`, `NETHER`, `END`

#### Configure Spawn Points

1. **Add Spawn Points** (stand at desired location):
   ```
   /cs admin arena add-spawn <arena-name>
   ```
   - Add 8-16 spawn points around the arena
   - Ensure balanced positioning
   - Face spawn points toward center

2. **List Current Spawns**:
   ```
   /cs admin arena list-spawns <arena-name>
   ```

3. **Remove Spawn Point**:
   ```
   /cs admin arena remove-spawn <arena-name> <index>
   ```

#### Set Arena Boundaries

1. **Method 1: WorldEdit Selection**:
   ```
   // Select area with WorldEdit wand
   //pos1 and //pos2
   /cs admin arena set-bounds <arena-name>
   ```

2. **Method 2: Manual Coordinates**:
   ```
   /cs admin arena set-bounds <arena-name> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>
   ```

#### Configure Arena Settings

1. **Set Player Limits**:
   ```
   /cs admin arena set-min-players <arena-name> <number>
   /cs admin arena set-max-players <arena-name> <number>
   ```

2. **Enable Arena**:
   ```
   /cs admin arena enable <arena-name>
   ```

### Step 4: Test Arena

1. **Test Arena Setup**:
   ```
   /cs admin arena test <arena-name>
   ```

2. **Join Arena for Testing**:
   ```
   /cs join <arena-name>
   ```

3. **Check Arena Status**:
   ```
   /cs admin arena info <arena-name>
   ```

## Theme Configuration

### Available Themes

| Theme | Description | Special Effects |
|-------|-------------|-----------------|
| `MEDIEVAL` | Castle/medieval setting | Stone particles, torch lighting |
| `FUTURISTIC` | Sci-fi environment | Ender particles, tech sounds |
| `DESERT` | Sand dunes and oasis | Sand particles, heat effects |
| `FOREST` | Woodland setting | Leaf particles, nature sounds |
| `NETHER` | Hellish landscape | Fire particles, lava effects |
| `END` | End dimension style | Void particles, ethereal sounds |

### Theme Commands

1. **Preview Theme Effects**:
   ```
   /cs admin theme preview <theme-name>
   ```

2. **Get Theme Guidelines**:
   ```
   /cs admin theme guidelines <theme-name>
   ```

3. **Apply Theme Effects**:
   ```
   /cs admin theme effects <arena-name>
   ```

### Custom Theme Configuration

Edit arena theme in `arenas.yml`:

```yaml
arenas:
  custom_arena:
    theme: "DESERT"
    theme-config:
      particle-effects: true
      ambient-sounds: true
      special-blocks: ["SAND", "SANDSTONE"]
      lighting: "TORCH"
```

## Commands Reference

### Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/cs admin reload` | `crysisshot.admin` | Reload plugin configuration |
| `/cs admin arena create <name>` | `crysisshot.admin` | Create new arena |
| `/cs admin arena delete <name>` | `crysisshot.admin` | Delete arena |
| `/cs admin arena enable <name>` | `crysisshot.admin` | Enable arena |
| `/cs admin arena disable <name>` | `crysisshot.admin` | Disable arena |
| `/cs admin arena info <name>` | `crysisshot.admin` | Show arena information |
| `/cs admin arena list` | `crysisshot.admin` | List all arenas |
| `/cs admin arena test <name>` | `crysisshot.admin` | Test arena configuration |

### Arena Setup Commands

| Command | Description |
|---------|-------------|
| `/cs admin arena set-world <arena> <world>` | Set arena world |
| `/cs admin arena set-theme <arena> <theme>` | Set arena theme |
| `/cs admin arena add-spawn <arena>` | Add spawn point at current location |
| `/cs admin arena remove-spawn <arena> <index>` | Remove spawn point |
| `/cs admin arena list-spawns <arena>` | List spawn points |
| `/cs admin arena set-bounds <arena>` | Set boundaries from WorldEdit selection |
| `/cs admin arena set-min-players <arena> <num>` | Set minimum players |
| `/cs admin arena set-max-players <arena> <num>` | Set maximum players |

### Theme Commands

| Command | Description |
|---------|-------------|
| `/cs admin theme preview <theme>` | Preview theme effects |
| `/cs admin theme guidelines <theme>` | Get building guidelines |
| `/cs admin theme effects <arena>` | Apply theme effects to arena |

## Troubleshooting

### Common Issues

1. **Plugin Won't Load**
   - Check server logs for errors
   - Verify Java version compatibility
   - Ensure all dependencies are present

2. **Arena Commands Not Working**
   - Verify admin permissions: `crysisshot.admin`
   - Check if arena name contains special characters
   - Ensure world exists and is loaded

3. **Players Can't Join Arena**
   - Check if arena is enabled: `/cs admin arena info <name>`
   - Verify spawn points are set: `/cs admin arena list-spawns <name>`
   - Check player count limits

4. **Spawn Points Not Working**
   - Ensure spawn points are within arena bounds
   - Check Y-level (players spawn 1 block above)
   - Verify world exists and chunks are loaded

### Debug Mode

Enable debug mode in `config.yml`:

```yaml
debug:
  enabled: true
```

Then reload: `/cs admin reload`

### Log Files

Check these log files for errors:
- `server.log` - Server console output
- `plugins/CrysisShot/debug.log` - Plugin debug information

### Performance Tips

1. **Arena Size**
   - Keep arenas reasonable size (100x100 max recommended)
   - Avoid excessive redstone/entities in arena areas

2. **Spawn Points**
   - Use 1.5-2x player count for spawn points
   - Distribute evenly around arena

3. **World Management**
   - Consider separate worlds for arenas
   - Preload arena chunks if possible

### Support

For additional support:
1. Check plugin documentation
2. Review server console for error messages
3. Test with minimal setup first
4. Verify all dependencies are up to date

---

## Quick Setup Checklist

- [ ] Plugin installed and loaded
- [ ] Configuration files generated
- [ ] Arena world created/selected
- [ ] Arena structure built
- [ ] Arena created with `/cs admin arena create`
- [ ] World set with `/cs admin arena set-world`
- [ ] Theme set with `/cs admin arena set-theme`
- [ ] Spawn points added (8-16 points)
- [ ] Boundaries set with `/cs admin arena set-bounds`
- [ ] Player limits configured
- [ ] Arena enabled with `/cs admin arena enable`
- [ ] Arena tested with `/cs admin arena test`

**Your CrysisShot arena is now ready for epic bow battles!** 🏹
