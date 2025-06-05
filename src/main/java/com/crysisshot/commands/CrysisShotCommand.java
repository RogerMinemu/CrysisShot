package com.crysisshot.commands;

import com.crysisshot.CrysisShot;
import com.crysisshot.game.GameManager;
import com.crysisshot.localization.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main command handler for CrysisShot plugin
 */
public class CrysisShotCommand implements CommandExecutor, TabCompleter {
    
    private final CrysisShot plugin;
    private final GameManager gameManager;
    private final MessageManager messageManager;
    
    public CrysisShotCommand(CrysisShot plugin, GameManager gameManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.messageManager = messageManager;
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
                messageManager.sendMessage(player, "commands.admin-help.create");
                messageManager.sendMessage(player, "commands.admin-help.reload");
            }
        } else {
            sender.sendMessage("§6--- CrysisShot Commands ---");
            sender.sendMessage("§e/cs admin reload §7- Reload plugin");
            sender.sendMessage("§e/cs version §7- Show plugin version");
        }
    }
    
    private void handleJoin(CommandSender sender) {
        if (!(sender instanceof Player)) {
            messageManager.sendMessage((Player) sender, "commands.player-only");
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("crysisshot.join")) {
            messageManager.sendMessage(player, "commands.no-permission");
            return;
        }
        
        // TODO: Implement game joining logic
        messageManager.sendMessage(player, "game.join-success");
    }
    
    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            messageManager.sendMessage((Player) sender, "commands.player-only");
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("crysisshot.leave")) {
            messageManager.sendMessage(player, "commands.no-permission");
            return;
        }
        
        // TODO: Implement game leaving logic
        messageManager.sendMessage(player, "game.leave-success");
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
            if (sender instanceof Player) {
                messageManager.sendMessage((Player) sender, "commands.admin-help.header");
            } else {
                sender.sendMessage("Available admin commands: reload, version");
            }
            return;
        }
        
        String adminCommand = args[1].toLowerCase();
        
        switch (adminCommand) {
            case "reload":
                handleReload(sender);
                break;
                
            case "create":
                // TODO: Implement arena creation
                sender.sendMessage("Arena creation not yet implemented!");
                break;
                
            default:
                if (sender instanceof Player) {
                    messageManager.sendMessage((Player) sender, "commands.invalid-args", 
                        "usage", "/cs admin <reload|create>");
                } else {
                    sender.sendMessage("Invalid admin command!");
                }
                break;
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
    
    private void showVersion(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        String authors = String.join(", ", plugin.getDescription().getAuthors());
        
        sender.sendMessage("§6CrysisShot §7v" + version);
        sender.sendMessage("§7Authors: " + authors);
        sender.sendMessage("§7Running on: " + plugin.getServer().getName() + " " + plugin.getServer().getVersion());
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main subcommands
            List<String> subCommands = Arrays.asList("help", "join", "leave", "stats", "top", "lang", "version");
            
            if (sender.hasPermission("crysisshot.admin")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.addAll(Arrays.asList("admin", "reload"));
            }
            
            String partial = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if ("admin".equals(subCommand) && sender.hasPermission("crysisshot.admin")) {
                List<String> adminCommands = Arrays.asList("reload", "create", "delete", "enable", "disable");
                String partial = args[1].toLowerCase();
                
                for (String adminCommand : adminCommands) {
                    if (adminCommand.startsWith(partial)) {
                        completions.add(adminCommand);
                    }
                }
            } else if ("lang".equals(subCommand) || "language".equals(subCommand)) {
                // TODO: Get available languages from MessageManager
                List<String> languages = Arrays.asList("en", "es", "fr", "de");
                String partial = args[1].toLowerCase();
                
                for (String language : languages) {
                    if (language.startsWith(partial)) {
                        completions.add(language);
                    }
                }
            }
        }
        
        return completions;
    }
}
