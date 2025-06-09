package com.crysisshot.commands;

import com.crysisshot.CrysisShot;
import com.crysisshot.arena.Arena;
import com.crysisshot.arena.ArenaSetupManager;
import com.crysisshot.game.GameManager;
import com.crysisshot.localization.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Main command handler for CrysisShot plugin
 */
public class CrysisShotCommand implements CommandExecutor, TabCompleter {
    private final CrysisShot plugin;
    private final GameManager gameManager;
    private final MessageManager messageManager;
    private final ArenaSetupManager arenaSetupManager;
    
    public CrysisShotCommand(CrysisShot plugin, MessageManager messageManager, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.messageManager = messageManager;
        this.arenaSetupManager = plugin.getArenaSetupManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                showHelp(sender);
                break;
                  case "join":
                handleJoin(sender);
                break;
                  case "leave":
                handleLeave(sender);
                break;
                
            case "queue":
                handleQueue(sender, args);
                break;
                
            case "stats":
                handleStats(sender, args);
                break;
                
            case "top":
            case "leaderboard":
                handleLeaderboard(sender);
                break;
                
            case "lang":
            case "language":
                handleLanguage(sender, args);
                break;
                
            case "admin":
                handleAdmin(sender, args);
                break;
                
            case "version":
                showVersion(sender);
                break;
                
            case "reload":
                handleReload(sender);
                break;
                
            default:
                if (sender instanceof Player) {
                    messageManager.sendMessage((Player) sender, "commands.invalid-args", 
                        "usage", "/" + label + " help");
                } else {
                    sender.sendMessage("Invalid arguments! Use /" + label + " help");
                }
                break;
        }
        
        return true;
    }
    
    private void showHelp(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            messageManager.sendMessage(player, "commands.help.header");
            messageManager.sendMessage(player, "commands.help.join");
            messageManager.sendMessage(player, "commands.help.leave");
            messageManager.sendMessage(player, "commands.help.stats");
            messageManager.sendMessage(player, "commands.help.leaderboard");
            messageManager.sendMessage(player, "commands.help.language");
              if (player.hasPermission("crysisshot.admin")) {
                messageManager.sendMessage(player, "commands.admin-help.header");
                messageManager.sendMessage(player, "commands.admin-help.setup");
                messageManager.sendMessage(player, "commands.admin-help.reload");
            }
        } else {
            sender.sendMessage("§6--- CrysisShot Commands ---");
            sender.sendMessage("§e/cs admin reload §7- Reload plugin");
            sender.sendMessage("§e/cs version §7- Show plugin version");
        }
    }    private void handleJoin(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("crysisshot.join")) {
            messageManager.sendMessage(player, "commands.no-permission");
            return;
        }
        
        // Check if player is already in a game
        if (gameManager.isPlayerInGame(player)) {
            messageManager.sendMessage(player, "error.already-in-game");
            return;
        }
        
        // For now, try to join a default session or create one
        String sessionId = "default";
        if (gameManager.getSession(sessionId) == null) {
            gameManager.createSession(sessionId, "default-arena");
        }
        
        if (gameManager.addPlayerToGame(player, sessionId)) {
            messageManager.sendMessage(player, "game.joined-successfully");
        } else {
            messageManager.sendMessage(player, "error.join-failed");
        }
    }
      private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("crysisshot.leave")) {
            messageManager.sendMessage(player, "commands.no-permission");
            return;
        }
        
        // Check if player is in a game
        if (!gameManager.isPlayerInGame(player)) {
            messageManager.sendMessage(player, "error.not-in-game");
            return;
        }
        
        if (gameManager.removePlayerFromGame(player, true)) {
            messageManager.sendMessage(player, "game.left-successfully");
        } else {
            messageManager.sendMessage(player, "error.leave-failed");
        }
    }
    
    private void handleQueue(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("crysisshot.queue")) {
            messageManager.sendMessage(player, "commands.no-permission");
            return;
        }
        
        // Default to "join" if no subcommand provided
        String action = (args.length > 1) ? args[1].toLowerCase() : "join";
        
        switch (action) {
            case "join":
                if (gameManager.addPlayerToQueue(player)) {
                    int position = gameManager.getQueuePosition(player);
                    messageManager.sendMessage(player, "game.queue-joined", "position", String.valueOf(position));
                }
                break;
                
            case "leave":
                if (gameManager.removePlayerFromQueue(player)) {
                    messageManager.sendMessage(player, "game.queue-left");
                } else {
                    messageManager.sendMessage(player, "error.not-in-queue");
                }
                break;
                
            case "status":
                if (gameManager.isPlayerInQueue(player)) {
                    int position = gameManager.getQueuePosition(player);
                    int size = gameManager.getQueueSize();
                    messageManager.sendMessage(player, "game.queue-position", 
                        "position", String.valueOf(position), 
                        "total", String.valueOf(size));
                } else {
                    messageManager.sendMessage(player, "error.not-in-queue");
                }
                break;
                
            default:
                messageManager.sendMessage(player, "commands.queue.usage");
                break;
        }
    }
    
    private void handleStats(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("crysisshot.stats")) {
            messageManager.sendMessage(player, "commands.no-permission");
            return;
        }
        
        // TODO: Implement stats viewing logic
        messageManager.sendMessage(player, "stats.header", "player", player.getName());
        messageManager.sendMessage(player, "stats.total-kills", "kills", "0");
        messageManager.sendMessage(player, "stats.games-played", "games", "0");
    }
    
    private void handleLeaderboard(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("crysisshot.leaderboard")) {
            messageManager.sendMessage(player, "commands.no-permission");
            return;
        }
        
        // TODO: Implement leaderboard logic
        messageManager.sendMessage(player, "leaderboard.header");
    }
    
    private void handleLanguage(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            messageManager.sendMessage(player, "commands.invalid-args", 
                "usage", "/cs lang <language>");
            return;
        }
        
        String language = args[1].toLowerCase();
        messageManager.setPlayerLanguage(player, language);
    }
      private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("crysisshot.admin")) {
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "commands.no-permission");
            } else {
                sender.sendMessage("No permission!");
            }
            return;
        }
        
        if (args.length < 2) {
            showAdminHelp(sender);
            return;
        }
        
        String adminCommand = args[1].toLowerCase();
        
        switch (adminCommand) {
            case "reload":
                handleReload(sender);
                break;
                
            case "setup":
                handleSetupCommands(sender, args);
                break;
            
            case "theme": // Added theme case
                handleThemeCommands(sender, args);
                break;
                
            default:
                if (sender instanceof Player) {
                    messageManager.sendMessage((Player) sender, "commands.invalid-args", 
                        "usage", "/cs admin <reload|setup|theme>"); // Updated usage
                } else {
                    sender.sendMessage("Invalid admin command! Use: reload, setup, theme"); // Updated usage
                }
                break;
        }
    }
    
    private void showAdminHelp(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            messageManager.sendMessage(player, "commands.admin-help.header");
            messageManager.sendMessage(player, "commands.admin-help.reload");
            messageManager.sendMessage(player, "commands.admin-help.setup");
            messageManager.sendMessage(player, "commands.admin-help.theme"); // Added theme help
        } else {
            sender.sendMessage("§6--- CrysisShot Admin Commands ---");
            sender.sendMessage("§e/cs admin reload §7- Reload plugin configuration");
            sender.sendMessage("§e/cs admin setup <command> §7- Arena setup commands");
            sender.sendMessage("§e/cs admin theme <command> §7- Arena theme commands"); // Added theme help
            sender.sendMessage("§e/cs admin setup help §7- Show setup command help");
        }
    }
    
    private void handleSetupCommands(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Setup commands can only be used by players!");
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("crysisshot.admin.setup")) {
            messageManager.sendMessage(player, "commands.no-permission");
            return;
        }
        
        if (args.length < 3) {
            showSetupCommandHelp(player);
            return;
        }
        
        String setupCommand = args[2].toLowerCase();
        
        switch (setupCommand) {
            case "start":
                if (args.length < 4) {
                    messageManager.sendMessage(player, "arena.setup.start-usage");
                    return;
                }
                String arenaName = args[3];
                arenaSetupManager.startSetup(player, arenaName);
                break;
                
            case "end":
            case "finish":
                boolean save = true;
                if (args.length >= 4 && args[3].equalsIgnoreCase("nosave")) {
                    save = false;
                }
                arenaSetupManager.endSetup(player, save);
                break;
                
            case "cancel":
                arenaSetupManager.endSetup(player, false);
                break;
                
            case "gui":
                if (arenaSetupManager.isInSetupMode(player)) {
                    arenaSetupManager.openSetupGUI(player);
                } else {
                    messageManager.sendMessage(player, "arena.setup.not-in-setup");
                }
                break;
                
            case "test":
                handleArenaTest(player, args);
                break;
                
            case "list":
                handleArenaList(player);
                break;
                
            case "help":
                showSetupCommandHelp(player);
                break;
                
            default:
                // If player is in setup mode, delegate to setup manager
                if (arenaSetupManager.isInSetupMode(player)) {
                    // Create new args array starting from "setup" command
                    String[] setupArgs = new String[args.length - 1];
                    setupArgs[0] = "setup";
                    System.arraycopy(args, 2, setupArgs, 1, args.length - 2);
                    arenaSetupManager.handleSetupCommand(player, setupArgs);
                } else {
                    messageManager.sendMessage(player, "arena.setup.not-in-setup-for-command");
                }
                break;
        }
    }
    
    private void showSetupCommandHelp(Player player) {
        messageManager.sendMessage(player, "commands.setup-help.header");
        messageManager.sendMessage(player, "commands.setup-help.start");
        messageManager.sendMessage(player, "commands.setup-help.end");
        messageManager.sendMessage(player, "commands.setup-help.cancel");
        messageManager.sendMessage(player, "commands.setup-help.gui");
        messageManager.sendMessage(player, "commands.setup-help.test");
        messageManager.sendMessage(player, "commands.setup-help.list");
        
        if (arenaSetupManager.isInSetupMode(player)) {
            messageManager.sendMessage(player, "commands.setup-help.in-setup-header");
            messageManager.sendMessage(player, "commands.setup-help.lobby");
            messageManager.sendMessage(player, "commands.setup-help.spectator");
            messageManager.sendMessage(player, "commands.setup-help.spawn");
            messageManager.sendMessage(player, "commands.setup-help.powerup");
            messageManager.sendMessage(player, "commands.setup-help.bounds");
            messageManager.sendMessage(player, "commands.setup-help.theme");
            messageManager.sendMessage(player, "commands.setup-help.players");
            messageManager.sendMessage(player, "commands.setup-help.validate");
            messageManager.sendMessage(player, "commands.setup-help.info");
        }
    }
    
    private void handleArenaTest(Player player, String[] args) {
        if (args.length < 4) {
            messageManager.sendMessage(player, "arena.test.usage");
            return;
        }
        
        String arenaName = args[3];
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            messageManager.sendMessage(player, "arena.not-exists", "arena", arenaName);
            return;
        }
        
        // Validate arena before testing
        List<String> errors = plugin.getArenaManager().validateArena(arena);
        if (!errors.isEmpty()) {
            messageManager.sendMessage(player, "arena.test.validation-failed", 
                "arena", arenaName, "errors", String.join(", ", errors));
            return;
        }
        
        // Teleport player to arena lobby for testing
        if (arena.getLobbySpawn() != null) {
            player.teleport(arena.getLobbySpawn());
            messageManager.sendMessage(player, "arena.test.started", "arena", arenaName);
        } else {
            messageManager.sendMessage(player, "arena.test.no-lobby", "arena", arenaName);
        }
    }
      private void handleArenaList(Player player) {
        Collection<Arena> arenaCollection = plugin.getArenaManager().getAllArenas();
        List<Arena> arenas = new ArrayList<>(arenaCollection);
        
        if (arenas.isEmpty()) {
            messageManager.sendMessage(player, "arena.list.empty");
            return;
        }
        
        messageManager.sendMessage(player, "arena.list.header");
        for (Arena arena : arenas) {
            String status = arena.getState() == Arena.ArenaState.DISABLED ? "§c[DISABLED]" : 
                           arena.getState() == Arena.ArenaState.MAINTENANCE ? "§e[MAINTENANCE]" : "§a[ACTIVE]";
            
            player.sendMessage(String.format("§7- §b%s %s §7(World: %s, Players: %d-%d)", 
                arena.getName(), status, arena.getWorldName(), 
                arena.getMinPlayers(), arena.getMaxPlayers()));
        }
    }
    
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("crysisshot.admin.config")) {
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "commands.no-permission");
            } else {
                sender.sendMessage("No permission!");
            }
            return;
        }
        
        try {
            plugin.reloadPlugin();
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "commands.reload-success");
            } else {
                sender.sendMessage("§aConfiguration reloaded successfully!");
            }
        } catch (Exception e) {
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "errors.general");
            } else {
                sender.sendMessage("§cFailed to reload configuration: " + e.getMessage());
            }
        }
    }
    
    private void handleThemeCommands(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Theme commands can only be used by players!");
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("crysisshot.admin.theme")) {
            messageManager.sendMessage(player, "commands.no-permission");
            return;
        }
        
        if (args.length < 3) {
            showThemeCommandHelp(player);
            return;
        }
        
        String themeCommand = args[2].toLowerCase();
        
        switch (themeCommand) {
            case "preview":
                handleThemePreview(player, args);
                break;
                
            case "guidelines":
                handleThemeGuidelines(player, args);
                break;
                
            case "effects":
                handleThemeEffects(player, args);
                break;
                
            default:
                showThemeCommandHelp(player);
                break;
        }
    }
    
    private void showThemeCommandHelp(Player player) {
        messageManager.sendMessage(player, "commands.theme-help.header");
        messageManager.sendMessage(player, "commands.theme-help.preview");
        messageManager.sendMessage(player, "commands.theme-help.guidelines");
        messageManager.sendMessage(player, "commands.theme-help.effects");
    }
    
    private void handleThemePreview(Player player, String[] args) {
        if (args.length < 4) {
            messageManager.sendMessage(player, "arena.theme.preview-usage");
            return;
        }
        
        String themeName = args[3].toUpperCase();
        
        try {
            Arena.Theme theme = Arena.Theme.valueOf(themeName);
            
            messageManager.sendMessage(player, "arena.theme.preview-header", "theme", theme.getDisplayName());
            
            // Send theme description
            String descriptionKey = "arena.theme." + theme.name().toLowerCase() + "-desc";
            messageManager.sendMessage(player, descriptionKey); // Use the direct key for description
            
            // Show building materials
            plugin.getArenaThemeManager().showThemePreview(player, theme);
            
        } catch (IllegalArgumentException e) {
            String availableThemes = String.join(", ", getAvailableThemes());
            messageManager.sendMessage(player, "arena.theme.invalid-theme", "themes", availableThemes);
        }
    }
    
    private void handleThemeGuidelines(Player player, String[] args) {
        if (args.length < 4) {
            messageManager.sendMessage(player, "arena.theme.guidelines-usage");
            return;
        }
        
        String themeName = args[3].toUpperCase();
        
        try {
            Arena.Theme theme = Arena.Theme.valueOf(themeName);
            
            messageManager.sendMessage(player, "arena.theme.guidelines-header", "theme", theme.getDisplayName());
            
            // Get building guidelines from the theme manager
            List<String> guidelines = plugin.getArenaThemeManager().getBuildingGuidelines(theme);
            for (String guideline : guidelines) {
                player.sendMessage(guideline); // Guidelines are already formatted with color codes
            }
            
        } catch (IllegalArgumentException e) {
            String availableThemes = String.join(", ", getAvailableThemes());
            messageManager.sendMessage(player, "arena.theme.invalid-theme", "themes", availableThemes);
        }
    }
    
    private void handleThemeEffects(Player player, String[] args) {
        if (args.length < 5) {
            messageManager.sendMessage(player, "commands.theme-effects-usage"); // Added new message key
            return;
        }
        
        String action = args[3].toLowerCase();
        String arenaName = args[4];
        
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            messageManager.sendMessage(player, "arena.not-exists", "arena", arenaName);
            return;
        }
        
        switch (action) {
            case "start":
                plugin.getArenaThemeManager().startThemeEffects(arena);
                messageManager.sendMessage(player, "arena.theme.effects-started", "arena", arenaName);
                break;
                
            case "stop":
                plugin.getArenaThemeManager().stopThemeEffects(arena);
                messageManager.sendMessage(player, "arena.theme.effects-stopped", "arena", arenaName);
                break;
                
            default:
                messageManager.sendMessage(player, "commands.theme-effects-usage"); // Added new message key
                break;
        }
    }
    
    private String[] getAvailableThemes() {
        Arena.Theme[] themes = Arena.Theme.values();
        String[] themeNames = new String[themes.length];
        for (int i = 0; i < themes.length; i++) {
            themeNames[i] = themes[i].name();
        }
        return themeNames;
    }

    private void showVersion(CommandSender sender) {
        // Use PluginMeta instead of deprecated getDescription()
        String version = plugin.getPluginMeta().getVersion();
        String authors = String.join(", ", plugin.getPluginMeta().getAuthors());
        
        sender.sendMessage("§6CrysisShot §7v" + version);
        sender.sendMessage("§7Authors: " + authors);
        sender.sendMessage("§7Running on: " + plugin.getServer().getName() + " " + plugin.getServer().getVersion());
    }
      @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String partial = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            // Main subcommands
            List<String> subCommands = new ArrayList<>(Arrays.asList("help", "join", "leave", "stats", "top", "lang", "version", "queue"));
            
            if (sender.hasPermission("crysisshot.admin")) {
                subCommands.addAll(Arrays.asList("admin", "reload"));
            }
            
            for (String subCmd : subCommands) {
                if (subCmd.startsWith(partial)) {
                    completions.add(subCmd);
                }
            }
        } else if (args.length == 2) {
            String mainCommand = args[0].toLowerCase();
            
            if ("admin".equals(mainCommand) && sender.hasPermission("crysisshot.admin")) {
                List<String> adminCommands = Arrays.asList("reload", "setup", "theme"); // Added "theme"
                for (String adminCommand : adminCommands) {
                    if (adminCommand.startsWith(partial)) {
                        completions.add(adminCommand);
                    }
                }
            } else if ("lang".equals(mainCommand) || "language".equals(mainCommand)) {
                // TODO: Get available languages from MessageManager
                List<String> languages = Arrays.asList("en", "es", "fr", "de"); // Example languages
                for (String language : languages) {
                    if (language.startsWith(partial)) {
                        completions.add(language);
                    }
                }
            } else if ("queue".equals(mainCommand) && sender.hasPermission("crysisshot.queue")) {
                 List<String> queueCommands = Arrays.asList("join", "leave", "status");
                 for (String queueCmd : queueCommands) {
                     if (queueCmd.startsWith(partial)) {
                         completions.add(queueCmd);
                     }
                 }
            }
        } else if (args.length == 3) {
            String mainCommand = args[0].toLowerCase();
            String subCommand = args[1].toLowerCase();
            
            if ("admin".equals(mainCommand) && sender.hasPermission("crysisshot.admin")) {
                if ("setup".equals(subCommand) && sender.hasPermission("crysisshot.admin.setup")) {
                    List<String> setupCommands = new ArrayList<>(Arrays.asList("start", "end", "finish", "cancel", "gui", "test", "list", "help"));
                    if (sender instanceof Player && arenaSetupManager.isInSetupMode((Player) sender)) {
                        setupCommands.addAll(Arrays.asList("lobby", "spectator", "spawn", "powerup", "bounds", "theme", "players", "validate", "info"));
                    }
                    for (String setupCmd : setupCommands) {
                        if (setupCmd.startsWith(partial)) {
                            completions.add(setupCmd);
                        }
                    }
                } else if ("theme".equals(subCommand) && sender.hasPermission("crysisshot.admin.theme")) {
                    List<String> themeCommands = Arrays.asList("preview", "guidelines", "effects", "help");
                    for (String themeCmd : themeCommands) {
                        if (themeCmd.startsWith(partial)) {
                            completions.add(themeCmd);
                        }
                    }
                }
            }
        } else if (args.length == 4) {
            String mainCommand = args[0].toLowerCase();
            String subCommand = args[1].toLowerCase();
            String actionCommand = args[2].toLowerCase();

            if ("admin".equals(mainCommand) && sender.hasPermission("crysisshot.admin")) {
                if ("setup".equals(subCommand) && sender.hasPermission("crysisshot.admin.setup")) {
                    if ("start".equals(actionCommand) || "test".equals(actionCommand)) {
                        // Suggest arena names for start/test
                        for (Arena arena : plugin.getArenaManager().getAllArenas()) {
                            if (arena.getName().toLowerCase().startsWith(partial)) {
                                completions.add(arena.getName());
                            }
                        }
                         if ("new_arena_name".startsWith(partial) && "start".equals(actionCommand)) { // Suggest a placeholder for new arena
                            completions.add("new_arena_name");
                        }
                    } else if ("spawn".equals(actionCommand) || "powerup".equals(actionCommand)) {
                        List<String> addRemove = Arrays.asList("add", "remove");
                        for (String arCmd : addRemove) {
                            if (arCmd.startsWith(partial)) {
                                completions.add(arCmd);
                            }
                        }
                    } else if ("bounds".equals(actionCommand)) {
                        List<String> minMax = Arrays.asList("min", "max");
                        for (String mmCmd : minMax) {
                            if (mmCmd.startsWith(partial)) {
                                completions.add(mmCmd);
                            }
                        }
                    } else if ("theme".equals(actionCommand)) {
                        for (Arena.Theme theme : Arena.Theme.values()) {
                            if (theme.name().toLowerCase().startsWith(partial)) {
                                completions.add(theme.name().toLowerCase());
                            }
                        }
                    } else if ("players".equals(actionCommand)) {
                        if ("<min_players>".startsWith(partial)) {
                             completions.add("<min_players>");
                        }
                    }
                } else if ("theme".equals(subCommand) && sender.hasPermission("crysisshot.admin.theme")) {
                    if ("preview".equals(actionCommand) || "guidelines".equals(actionCommand)) {
                        for (Arena.Theme theme : Arena.Theme.values()) {
                            if (theme.name().toLowerCase().startsWith(partial)) {
                                completions.add(theme.name().toLowerCase());
                            }
                        }
                    } else if ("effects".equals(actionCommand)) {
                        List<String> startStop = Arrays.asList("start", "stop");
                        for (String ssCmd : startStop) {
                            if (ssCmd.startsWith(partial)) {
                                completions.add(ssCmd);
                            }
                        }
                    }
                }
            }
        } else if (args.length == 5) {
            String mainCommand = args[0].toLowerCase();
            String subCommand = args[1].toLowerCase();
            String actionCommand = args[2].toLowerCase();
            String fourthArg = args[3].toLowerCase();

            if ("admin".equals(mainCommand) && sender.hasPermission("crysisshot.admin")) {
                if ("setup".equals(subCommand) && "players".equals(actionCommand) && sender.hasPermission("crysisshot.admin.setup")) {
                     if ("<max_players>".startsWith(partial)) {
                        completions.add("<max_players>");
                    }
                } else if ("theme".equals(subCommand) && "effects".equals(actionCommand) && sender.hasPermission("crysisshot.admin.theme")) {
                    if ("start".equals(fourthArg) || "stop".equals(fourthArg)) {
                        for (Arena arena : plugin.getArenaManager().getAllArenas()) {
                            if (arena.getName().toLowerCase().startsWith(partial)) {
                                completions.add(arena.getName());
                            }
                        }
                    }
                }
            }
        }
        
        return completions;
    }
}
