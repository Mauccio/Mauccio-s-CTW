package com.mauccio.ctw.commands;

import com.mauccio.ctw.CTW;
import com.mauccio.ctw.game.TeamManager;
import com.mauccio.ctw.listeners.EventManager;
import com.nametagedit.plugin.NametagEdit;
import com.sk89q.worldedit.bukkit.selections.Selection;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class CommandManager implements CommandExecutor {

    private final CTW plugin;
    private final TreeSet<String> allowedInGameCmds;

    public CommandManager(CTW plugin) {
        this.plugin = plugin;
        allowedInGameCmds = new TreeSet<>();
        register();
    }

    private void register() {
        plugin.saveResource("plugin.yml", true);
        File file = new File(plugin.getDataFolder(), "plugin.yml");
        YamlConfiguration pluginYml = new YamlConfiguration();
        try {
            pluginYml.load(file);
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.getLogger().severe(ex.toString());
            plugin.getPluginLoader().disablePlugin(plugin);
            return;
        }
        file.delete();
        for (String commandName : pluginYml.getConfigurationSection("commands").getKeys(false)) {
            plugin.getCommand(commandName).setExecutor(this);
            allowedInGameCmds.add(commandName);
        }

        if (!plugin.cf.implementSpawnCmd()) {
            unRegisterBukkitCommand(plugin.getCommand("spawn"));
        }
    }

    private static Object getPrivateField(Object object, String field) throws SecurityException,
            NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Class<?> clazz = object.getClass();
        Field objectField = clazz.getDeclaredField(field);
        objectField.setAccessible(true);
        Object result = objectField.get(object);
        objectField.setAccessible(false);
        return result;
    }

    public void unRegisterBukkitCommand(PluginCommand cmd) {
        try {
            Object result = getPrivateField(plugin.getServer().getPluginManager(), "commandMap");
            SimpleCommandMap commandMap = (SimpleCommandMap) result;
            Object map = getPrivateField(commandMap, "knownCommands");
            @SuppressWarnings("unchecked")
            HashMap<String, Command> knownCommands = (HashMap<String, Command>) map;
            knownCommands.remove(cmd.getName());
            for (String alias : cmd.getAliases()) {
                if (knownCommands.containsKey(alias) && knownCommands.get(alias).toString().contains(plugin.getName())) {
                    knownCommands.remove(alias);
                }
            }
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            plugin.getLogger().severe(e.toString());
        }
    }

    /*
    double NPCrange = 100;
    double NPChealth = 24;
    double NPCReachEasy = 2.0;
    double NPCprojectileRange = 50;
    int NPCattackRate = 2;
    double NPCspeed = 0.8;
     */
    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] args) {
        Player player;
        if (cs instanceof Player) {
            player = (Player) cs;
        } else {
            player = null;
            plugin.getLogger().info(plugin.lm.getText("not-a-player"));
        }
        switch (cmnd.getName()) {
            case "ctw":
                if (args.length != 1) {
                    plugin.lm.sendText("commands.ctw", player);
                } else {
                    switch (args[0].toLowerCase()) {
                        case "reload":
                            plugin.reload();
                            plugin.lm.sendMessage("cmd-success", cs);
                            break;
                        case "save":
                            plugin.save();
                            plugin.lm.sendMessage("cmd-success", cs);
                            break;
                        case "mapcycle":
                            if (player == null) {
                                plugin.lm.sendMessage("not-in-game-cmd", player);
                            } else {
                                if (plugin.pm.getTeamId(player) == null) {
                                    plugin.lm.sendMessage("not-in-game-cmd", player);
                                } else {
                                    plugin.gm.advanceGame(player.getWorld());
                                }
                            }
                            break;
                        default:
                            plugin.lm.sendText("commands.ctw", player);
                            break;
                    }
                }

                break;
            case "stats":
                if (player == null) {
                    plugin.lm.sendMessage("not-in-game-cmd", player);
                }

                try {
                    player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.your-stats")), 10.0F, 1);
                    player.sendMessage(plugin.getConfig().getString("message-decorator"));
                    player.sendMessage(plugin.lm.getText("stats.title"));
                    player.sendMessage(plugin.lm.getText("stats.points") + plugin.db.getScore(player.getName()));
                    player.sendMessage(plugin.lm.getText("stats.kills") + plugin.db.getKill(player.getName()));
                    player.sendMessage(plugin.lm.getText("stats.placed-wools") + plugin.db.getWoolCaptured(player.getName()));
                    player.sendMessage(plugin.getConfig().getString("message-decorator"));
                } catch (NullPointerException e) {
                    plugin.lm.sendMessage("stats-not-enabled", player);
                }
                break;

            case "saveglobalkit":
                if (player == null) {
                    plugin.lm.sendMessage("not-in-game-cmd", player);
                }
                if (plugin.pm.getTeamId(player) != null) {
                    plugin.lm.sendMessage("not-in-lobby-cmd", player);
                    player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.error")), 10.0f, 1.0f);
                }
                try {
                    plugin.mm.saveGlobalKit(player);
                    plugin.lm.sendMessage("starting-kit-set", player);
                } catch (IOException e) {
                    plugin.lm.sendMessage("error-at-save-kit", player);
                }
                break;
            case "kiteditor":
                if (player == null) {
                    plugin.lm.sendMessage("not-in-game-cmd", player);
                }
                if (plugin.pm.getTeamId(player) != null) {
                    plugin.lm.sendMessage("not-in-lobby-cmd", player);
                    player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.error")), 10.0f, 1.0f);
                }
                ItemStack[] globalKit = plugin.mm.getGlobalKit();
                player.getInventory().clear();
                player.getInventory().setContents(globalKit);
                plugin.lm.sendMessage("edit-your-kit", player);
                plugin.lm.sendMessage("save-your-kit-with", player);
                break;
            case "spawn":
                if (player == null) {
                    plugin.lm.sendMessage("not-in-game-cmd", player);
                    return true;
                }
                player.teleport(plugin.wm.getNextLobbySpawn());
                player.setPlayerListName(player.getName());
                player.setDisplayName(player.getName());

                break;
            case "ctwsetup":
                if (player == null) {
                    plugin.lm.sendMessage("not-in-game-cmd", player);
                    return true;
                }
                if (args.length > 1) {
                    processCtwSetup(player, args);
                } else {
                    if (args.length == 0) {
                        plugin.lm.sendText("commands.ctwsetup", player);
                    } else {
                        switch (args[0].toLowerCase()) {
                            case "lobby":
                                plugin.lm.sendText("commands.ctwsetup-lobby", player);
                                return true;
                            case "map":
                                plugin.lm.sendText("commands.ctwsetup-map", player);
                                return true;
                            case "mapconfig":
                                plugin.lm.sendText("commands.ctwsetup-mapconfig", player);
                                return true;
                            case "room":
                                plugin.lm.sendText("commands.ctwsetup-room", player);
                                return true;
                            default:
                                plugin.lm.sendText("commands.ctwsetup", player);
                        }
                    }
                }
                break;
            case "createworld":
            case "gotoworld":
                if (player == null) {
                    plugin.lm.sendMessage("not-in-game-cmd", player);
                    return true;
                }
                if (args.length != 1) {
                    plugin.lm.sendMessage("incorrect-parameters", cs);
                    plugin.lm.sendText("commands." + cmnd.getName(), (Player) cs);
                    return true;
                }
            {
                World world;
                if (cmnd.getName().equals("createworld")) {
                    world = plugin.wm.createEmptyWorld(args[0]);
                } else {
                    world = plugin.wm.loadWorld(args[0]);
                }

                if (world == null) {
                    plugin.lm.sendMessage("world-doesnot-exists", cs);
                } else {
                    player.teleport(world.getSpawnLocation());
                }
            }
            break;
            case "savekit":
                if (player == null) {
                    plugin.lm.sendMessage("not-in-game-cmd", player);
                    return true;
                }
                if (plugin.pm.getTeamId(player) != null) {
                    plugin.lm.sendMessage("not-in-lobby-cmd", player);
                    player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.error")), 10.0f, 1.0f);
                    return true;
                }
                try {
                    plugin.mm.saveKit(player);
                    plugin.lm.sendMessage("saved-kit-success", player);
                    player.getInventory().clear();
                }
                catch (IOException e) {
                    plugin.lm.sendMessage("error-saved-kit", player);
                    player.getInventory().clear();
                }
                break;
            case "g":
                if (player != null) {
                    if (args.length != 0) {
                        if (plugin.pm.getTeamId(player) != null) {
                            ChatColor cc = plugin.pm.getChatColor(player);
                            String message = "";
                            for (String word : args) {
                                message = message.concat(word + " ");
                            }
                            String senderName = player.getDisplayName().replace(player.getName(),
                                    cc + player.getName());
                            for (Player receiver : player.getWorld().getPlayers()) {
                                receiver.sendMessage("" + senderName + ChatColor.RESET + ": " + message);
                            }

                        } else {
                            plugin.lm.sendMessage("not-in-room-cmd", player);
                        }
                    } else {
                        plugin.lm.sendText("commands.g", player);
                    }
                } else {
                    plugin.lm.sendMessage("not-in-game-cmd", player);
                }
                break;
            case "toggle":
                if (player != null) {
                    if (args.length == 1) {
                        if (plugin.pm.getTeamId(player) != null) {
                            switch (args[0].toLowerCase()) {
                                case "obs":
                                    if (plugin.pm.toggleSeeOthersSpectators(player)) {
                                        plugin.lm.sendMessage("obs-true", player);
                                    } else {
                                        plugin.lm.sendMessage("obs-false", player);
                                    }
                                    break;
                                case "dms":
                                    if (plugin.pm.toogleOthersDeathMessages(player)) {
                                        plugin.lm.sendMessage("dms-true", player);
                                    } else {
                                        plugin.lm.sendMessage("dms-false", player);
                                    }
                                    break;
                                case "blood":
                                    if (plugin.pm.toggleBloodEffect(player)) {
                                        plugin.lm.sendMessage("blood-true", player);
                                    } else {
                                        plugin.lm.sendMessage("blood-false", player);
                                    }

                            }
                        } else {
                            plugin.lm.sendMessage("not-in-room-cmd", player);
                        }
                    } else {
                        plugin.lm.sendText("commands.toggle", player);
                    }
                } else {
                    plugin.lm.sendMessage("not-in-game-cmd", player);
                }

                break;
            case "leave":
                if (player != null) {
                    if (plugin.pm.getTeamId(player) != null) {
                        player.teleport(plugin.wm.getNextLobbySpawn());
                        NametagEdit.getApi().clearNametag(player);
                        player.setPlayerListName(player.getName());
                        player.setDisplayName(player.getName());
                    } else {
                        plugin.lm.sendMessage("not-in-room-cmd", player);
                    }
                } else {
                    plugin.lm.sendMessage("not-in-game-cmd", player);
                }
                break;
            case "join":
                if (player != null) {
                    if (args.length == 1) {
                        TeamManager.TeamId playerTeam = plugin.pm.getTeamId(player);
                        if (playerTeam != null) {
                            String teamToJoin = args[0].toLowerCase();
                            TeamManager.TeamId desiredTeam;
                            if (teamToJoin.startsWith("obs") || teamToJoin.startsWith("spect")) {
                                desiredTeam = TeamManager.TeamId.SPECTATOR;
                            } else {
                                switch (teamToJoin) {
                                    case "red":
                                        plugin.gm.movePlayerTo(player, TeamManager.TeamId.RED);
                                        desiredTeam = TeamManager.TeamId.RED;
                                        break;
                                    case "blue":
                                        plugin.gm.movePlayerTo(player, TeamManager.TeamId.BLUE);
                                        desiredTeam = TeamManager.TeamId.BLUE;
                                        break;
                                    case "random":
                                    case "rand":
                                    case "*":
                                        plugin.gm.movePlayerTo(player, null);
                                        desiredTeam = null;
                                        break;
                                    default:
                                        plugin.lm.sendMessage("incorrect-parameters", player);
                                        desiredTeam = null;
                                        break;
                                }
                            }
                            if (desiredTeam != null) {
                                if (desiredTeam == playerTeam | playerTeam == TeamManager.TeamId.BLUE | playerTeam == TeamManager.TeamId.RED) {
                                    plugin.lm.sendMessage("already-in-this-team", player);
                                } else {
                                    plugin.gm.joinInTeam(player, desiredTeam);
                                }
                            }
                        } else {
                            plugin.lm.sendMessage("not-in-room-cmd", player);

                        }
                    } else if(plugin.pm.getTeamId(player) == TeamManager.TeamId.SPECTATOR) {
                        // plugin.lm.sendText("commands.join", player);
                        player.openInventory(plugin.tm.getMenuInv());
                        player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.join-command")), 10.0F, 2);
                    } else {
                        plugin.lm.sendMessage("join-in-team", player);
                    }
                } else {
                    plugin.lm.sendMessage("not-in-game-cmd", player);
                }

                break;
                /*
            case "tutorial":
            	if(!plugin.wm.isOnLobby(player)) {
            		plugin.lm.sendMessage("not-in-room-cmd", player);
            	}
            	if(plugin.gm.getPlayersIn("Tutorial") == 1) {
            		plugin.lm.sendMessage("tutorial.full", player);
            	}
            	plugin.gm.movePlayerToRoom(player, "Tutorial");
            	player.sendMessage(plugin.lm.getMessage("tutorial-part1").replace("%PLAYER%", player.getDisplayName()));

            	break;
            	*/
            case "alert":

                if (args.length != 0) {

                    String message = "";
                    for (String word : args) {
                        message = message.concat(word + " ");
                    }
                    for (Player receiver : player.getServer().getOnlinePlayers()) {
                        receiver.sendMessage(plugin.lm.getText("alert-prefix") + " " + message);
                    }

                } else {
                    plugin.lm.sendText("commands.alert", player);
                }
                break;
        }

        return true;
    }



    private void processCtwSetup(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "lobby":
                switch (args[1].toLowerCase()) {
                    case "addspawn":
                        if (plugin.wm.getLobbyWorld() == null) {
                            if (plugin.mm.isMap(player.getWorld())) {
                                plugin.lm.sendMessage("map-cannot-be-lobby", player);
                                return;
                            }
                            plugin.lm.sendMessage("lobby-world-set", player);
                        }
                        if (!plugin.wm.getLobbySpawnLocations().isEmpty()) {
                            if (!player.getLocation().getWorld().getName().equals(plugin.wm.getLobbyWorld().getName())) {
                                plugin.lm.sendMessage("lobby-spawnpoint-missmatch", player);
                                return;
                            }
                        }
                        plugin.wm.addSpawnLocation(player.getLocation());
                        plugin.lm.sendText("lobby-spawnpoint-set", player);
                        break;
                    case "listspawn": {
                        if (plugin.wm.getLobbySpawnLocations().isEmpty()) {
                            plugin.lm.sendMessage("lobby-spawnpoint-empty", player);
                        } else {
                            player.sendMessage(plugin.lm.getMessage("world") + ": " + plugin.wm.getLobbyWorld().getName());
                            int pos = 0;
                            for (Location loc : plugin.wm.getLobbySpawnLocations()) {
                                player.sendMessage(plugin.lm.getMessagePrefix() + " spawn #:" + pos + " X=" + loc.getBlockX()
                                        + ", Y=" + loc.getBlockY() + ", Z=" + loc.getBlockZ());
                                pos++;
                            }
                        }
                    }
                    break;
                    case "clear":
                        plugin.wm.clearLobbyInformation();
                        plugin.lm.sendMessage("lobby-cleared", player);
                        break;
                    default:
                        plugin.lm.sendText("commands.ctwsetup-lobby", player);
                        break;
                }
                break;
            case "map":
                switch (args[1].toLowerCase()) {
                    case "add":
                        if (plugin.wm.isOnLobby(player)) {
                            plugin.lm.sendMessage("lobby-cannot-be-map", player);
                            return;
                        }
                        if(plugin.wm.getLobbySpawnLocations().isEmpty()) {
                            plugin.lm.sendMessage("unconfigured-lobby.error", player);
                        }
                        if (plugin.mm.add(player.getWorld())) {
                            player.sendMessage(plugin.lm.getMessage("map-successfully-added")
                                    .replace("%MAP%", player.getWorld().getName()));
                        } else {
                            player.sendMessage(plugin.lm.getMessage("map-already-exists")
                                    .replace("%MAP%", player.getWorld().getName()));
                        }
                        plugin.mm.setupTip(player);
                        break;
                    case "del":
                    case "remove":
                        if (plugin.mm.isMap(player.getWorld())) {
                            plugin.mm.deleteMap(player.getWorld());
                            plugin.lm.sendMessage("map-deleted", player);
                        } else {
                            plugin.lm.sendMessage("not-in-map", player);
                        }
                        break;
                    case "list": {
                        Set<String> list = plugin.mm.getMaps();
                        if (list.isEmpty()) {
                            plugin.lm.sendMessage("map-list-empty", player);
                        } else {
                            player.sendMessage(plugin.lm.getMessage("available-maps") + " " + list.toString());
                        }
                    }
                    break;
                    case "copy":
                        if (args.length != 3) {
                            plugin.lm.sendMessage("incorrect-parameters", player);
                            plugin.lm.sendMessage("commands.ctwsetup-map.help-5", player);
                        } else {
                            if (plugin.wm.cloneWorld(player.getWorld(), args[2]) != null) {
                                World newWorld = plugin.getServer().getWorld(args[2]);
                                plugin.mm.cloneMap(player.getWorld(), newWorld);
                                player.sendMessage(plugin.lm.getMessage("world-created")
                                        .replace("%WORLD%", args[2]));
                            }
                        }
                        break;
                    default:
                        plugin.lm.sendText("commands.ctwsetup-map", player);
                        break;
                }
                break;
            case "mapconfig":
                if (!plugin.mm.isMap(player.getWorld())) {
                    plugin.lm.sendMessage("not-in-map", player);
                    return;
                }
                switch (args[1].toLowerCase()) {
                    case "kit":
                        try {
                            plugin.mm.setGlobalKit(player);
                        } catch (NullPointerException e) {
                            plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&4Global Kit is not setted, use &c/saveglobalkit &4to set!"));
                        }
                        plugin.lm.sendMessage("starting-kit-set", player);
                        break;
                    case "toggleleather":
                        if (plugin.mm.getKitarmour(player.getWorld())) {
                            plugin.lm.sendMessage("default-armour-on", player);
                            plugin.mm.setKitarmour(player.getWorld(), false);
                        } else {
                            plugin.lm.sendMessage("default-armour-off", player);
                            plugin.mm.setKitarmour(player.getWorld(), true);
                        }
                        break;
                    case "removeregion":
                        plugin.mm.removeRegion(player);
                        plugin.lm.sendMessage("cmd-success", player);
                        break;
                    case "spawn":
                        plugin.mm.setSpawn(player.getLocation());
                        plugin.lm.sendMessage("mapspawn-set", player);
                        player.setPlayerListName(player.getName());
                        player.setDisplayName(player.getName());
                        NametagEdit.getApi().clearNametag(player);
                        break;
                    case "redspawn":
                        plugin.mm.setRedSpawn(player.getLocation());
                        plugin.lm.sendMessage("redspawn-set", player);
                        break;
                    case "bluespawn":
                        plugin.mm.setBlueSpawn(player.getLocation());
                        plugin.lm.sendMessage("bluespawn-set", player);
                        break;
                    case "maxplayers":
                        if (args.length != 3) {
                            plugin.lm.sendMessage("incorrect-parameters", player);
                            plugin.lm.sendMessage("commands.ctwsetup-mapconfig.help-5", player);
                        } else {
                            int numPlayers;
                            try {
                                numPlayers = Integer.parseInt(args[2]);
                            } catch (NumberFormatException ex) {
                                plugin.lm.sendMessage("commands.ctwsetup-mapconfig.help-5", player);
                                return;
                            }
                            if (numPlayers < 2) {
                                plugin.lm.sendMessage("commands.ctwsetup-mapconfig.help-5", player);
                                return;
                            }
                            if (plugin.mm.isMap(player.getWorld())) {
                                plugin.mm.setMaxPlayers(player.getWorld(), numPlayers);
                                plugin.lm.sendMessage("maxplayers-set", player);
                            } else {
                                plugin.lm.sendMessage("not-in-map", player);
                            }
                        }
                        break;
                    case "redwinwool":
                        plugin.lm.sendText("add-red-wool-winpoint", player);
                        plugin.em.registerSetupEvents(player, EventManager.SetUpAction.RED_WIN_WOOL);
                        return;
                    case "bluewinwool":
                        plugin.lm.sendText("add-blue-wool-winpoint", player);
                        plugin.em.registerSetupEvents(player, EventManager.SetUpAction.BLUE_WIN_WOOL);
                        return;
                    case "woolspawner":
                        plugin.lm.sendText("add-wool-spawners", player);
                        plugin.em.registerSetupEvents(player, EventManager.SetUpAction.WOOL_SPAWNER);
                        return;
                    case "continue":
                        plugin.em.unregisterSetUpEvents(player);
                        plugin.lm.sendMessage("cmd-success", player);
                        break;
                    case "rednoaccess":
                    case "bluenoaccess":
                    case "protected":
                        if (plugin.mm.isMap(player.getWorld())) {
                            Selection sel = plugin.we.getSelection(player);
                            if (sel == null) {
                                plugin.lm.sendMessage("area-not-selected", player);
                                return;
                            } else {
                                if (plugin.mm.isProtectedArea(player.getWorld(), sel)) {
                                    plugin.lm.sendMessage("area-na-already-protected", player);
                                    return;
                                }
                                if (plugin.mm.isRedNoAccessArea(player.getWorld(), sel)) {
                                    plugin.lm.sendMessage("area-na-already-red", player);
                                    return;
                                }
                                if (plugin.mm.isBlueNoAccessArea(player.getWorld(), sel)) {
                                    plugin.lm.sendMessage("area-na-already-blue", player);
                                    return;
                                }
                                switch (args[1].toLowerCase()) {
                                    case "rednoaccess":
                                        plugin.mm.addRedNoAccessArea(player.getWorld(), sel);
                                        break;
                                    case "bluenoaccess":
                                        plugin.mm.addBlueNoAccessArea(player.getWorld(), sel);
                                        break;
                                    default:
                                        plugin.mm.addProtectedArea(player.getWorld(), sel);
                                        break;
                                }
                                plugin.lm.sendMessage("area-na-done", player);
                                return;
                            }
                        } else {
                            plugin.lm.sendMessage("not-in-map", player);
                        }
                        break;
                    case "weather":
                        if (args.length != 3) {
                            plugin.lm.sendMessage("incorrect-parameters", player);
                            return;
                        }
                        switch (args[2].toLowerCase()) {
                            case "random":
                                plugin.mm.setWeather(player.getWorld(), false, false);
                                plugin.lm.sendMessage("random-set", player);
                                break;
                            case "fixed=sun":
                                plugin.mm.setWeather(player.getWorld(), true, true);
                                plugin.lm.sendMessage("sunny-set", player);
                                break;
                            case "fixed=storm":
                                plugin.mm.setWeather(player.getWorld(), true, false);
                                plugin.lm.sendMessage("storm-set", player);
                                break;
                            default:
                                plugin.lm.sendMessage("incorrect-parameters", player);
                                return;
                        }
                        break;
                    case "restore":
                        Selection sel = plugin.we.getSelection(player);
                        if (sel == null) {
                            plugin.lm.sendMessage("area-not-selected", player);
                            return;
                        }
                        plugin.mm.setRestaurationArea(sel);
                        plugin.lm.sendMessage("cmd-success", player);
                        break;
                    case "no-drop":
                        if (plugin.mm.setNoDrop(player)) {
                            plugin.lm.sendMessage("cmd-success", player);
                        }
                        break;
                    default:
                        plugin.lm.sendText("commands.ctwsetup-mapconfig", player);
                        break;
                }
                plugin.mm.setupTip(player);
                break;
            case "room":
                switch (args[1].toLowerCase()) {
                    case "add":
                        if (args.length != 3) {
                            plugin.lm.sendMessage("incorrect-parameters", player);
                            plugin.lm.sendMessage("commands.ctwsetup-room.help-2", player);
                            return;
                        }
                        if (plugin.rm.add(args[2])) {
                            plugin.lm.sendText("room-added", player);
                        } else {
                            plugin.lm.sendMessage("duplicated-room", player);
                        }
                        break;
                    case "remove":
                        if (args.length != 3) {
                            plugin.lm.sendMessage("incorrect-parameters", player);
                            plugin.lm.sendMessage("commands.ctwsetup-room.help-3", player);
                            return;
                        }
                        if (!plugin.rm.exists(args[2])) {
                            plugin.lm.sendMessage("room-doesnot-exists", player);
                        }
                        if (plugin.rm.isEnabled(args[2])) {
                            plugin.lm.sendMessage("edit-enabled-room", player);
                            return;
                        }
                        if (plugin.rm.remove(args[2])) {
                            plugin.lm.sendMessage("cmd-success", player);
                        }

                        break;
                    case "list": {
                        List<String> list = plugin.rm.list();
                        if (list.isEmpty()) {
                            plugin.lm.sendMessage("room-list-empty", player);
                        } else {
                            for (String line : list) {
                                player.sendMessage(line);
                            }
                        }
                        break;
                    }
                    case "enable":
                        if (args.length != 3) {
                            plugin.lm.sendMessage("incorrect-parameters", player);
                            plugin.lm.sendMessage("commands.ctwsetup-room.help-5", player);
                            return;
                        }
                        if (!plugin.rm.exists(args[2])) {
                            plugin.lm.sendMessage("room-doesnot-exists", player);
                        }
                        if (plugin.rm.isEnabled(args[2])) {
                            plugin.lm.sendMessage("room-already-enabled", player);
                            return;
                        }

                        if (!plugin.rm.hasMaps(args[2])) {
                            plugin.lm.sendMessage("room-has-no-map", player);
                            return;
                        }

                        if (plugin.rm.enable(args[2])) {
                            plugin.lm.sendMessage("cmd-success", player);
                        }
                        break;
                    case "disable":
                        if (args.length != 3) {
                            plugin.lm.sendMessage("incorrect-parameters", player);
                            plugin.lm.sendMessage("commands.ctwsetup-room.help-6", player);
                            return;
                        }
                        if (!plugin.rm.exists(args[2])) {
                            plugin.lm.sendMessage("room-doesnot-exists", player);
                        }
                        if (!plugin.rm.isEnabled(args[2])) {
                            plugin.lm.sendMessage("room-already-disabled", player);
                            return;
                        }
                        if (plugin.rm.disable(args[2])) {
                            plugin.lm.sendMessage("cmd-success", player);
                        }
                        break;
                    case "addmap":
                        if (args.length != 4) {
                            plugin.lm.sendMessage("incorrect-parameters", player);
                            plugin.lm.sendMessage("commands.ctwsetup-room.help-7", player);
                            return;
                        }
                        if (!plugin.rm.exists(args[2])) {
                            plugin.lm.sendMessage("room-doesnot-exists", player);
                        }
                        if (plugin.rm.isEnabled(args[2])) {
                            plugin.lm.sendMessage("edit-enabled-room", player);
                            return;
                        }
                    {
                        World map = plugin.wm.loadWorld(args[3]);
                        if (map == null) {
                            plugin.lm.sendMessage("world-doesnot-exists", player);
                            return;
                        }

                        if (plugin.rm.hasMap(args[2], map)) {
                            plugin.lm.sendMessage("room-already-has-this-map", player);
                            return;
                        }

                        if (plugin.rm.addMap(args[2], map)) {
                            plugin.lm.sendMessage("cmd-success", player);
                        }
                    }
                    break;
                    case "removemap":
                        if (args.length != 4) {
                            plugin.lm.sendMessage("incorrect-parameters", player);
                            plugin.lm.sendMessage("commands.ctwsetup-room.help-8", player);
                            return;
                        }
                        if (!plugin.rm.exists(args[2])) {
                            plugin.lm.sendMessage("room-doesnot-exists", player);
                        }
                        if (plugin.rm.isEnabled(args[2])) {
                            plugin.lm.sendMessage("edit-enabled-room", player);
                            return;
                        }
                    {
                        World map = plugin.wm.loadWorld(args[3]);
                        if (map == null) {
                            plugin.lm.sendMessage("world-doesnot-exists", player);
                            return;
                        }

                        if (!plugin.rm.hasMap(args[2], map)) {
                            plugin.lm.sendMessage("room-doesnot-has-this-map", player);
                            return;
                        }

                        if (plugin.rm.removeMap(args[2], map)) {
                            plugin.lm.sendMessage("cmd-success", player);
                        }
                    }
                    break;
                    default:
                        plugin.lm.sendText("commands.ctwsetup-room", player);
                }

                break;
            default:
                plugin.lm.sendText("commands.ctwsetup", player);
                break;
        }
    }

    public boolean isAllowedInGameCmd(String cmd) {
        return allowedInGameCmds.contains(cmd);
    }
}
