package com.mauccio.ctw.commands;

import com.mauccio.ctw.CTW;
import com.mauccio.ctw.game.TeamManager;
import com.mauccio.ctw.map.MapManager;
import com.mauccio.ctw.utils.PlayerStats;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class CommandManager implements CommandExecutor {

    private final CTW plugin;
    private final TreeSet<String> allowedInGameCmds;

    public CommandManager(CTW plugin) {
        this.plugin = plugin;
        this.allowedInGameCmds = new TreeSet<>();
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
        } finally {
            if (file.exists()) file.delete();
        }

        if (pluginYml.getConfigurationSection("commands") != null) {
            for (String commandName : pluginYml.getConfigurationSection("commands").getKeys(false)) {
                PluginCommand pc = plugin.getCommand(commandName);
                if (pc != null) pc.setExecutor(this);
                allowedInGameCmds.add(commandName);
            }
        }

        if (!plugin.getConfigManager().implementSpawnCmd()) {
            unRegisterBukkitCommand(plugin.getCommand("spawn"));
        }
    }

    private static Object getPrivateField(Object object, String field)
            throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = object.getClass();
        Field objectField = clazz.getDeclaredField(field);
        objectField.setAccessible(true);
        Object result = objectField.get(object);
        objectField.setAccessible(false);
        return result;
    }

    private void unRegisterBukkitCommand(PluginCommand cmd) {
        if (cmd == null) return;
        try {
            Object pluginManager = plugin.getServer().getPluginManager();
            Field fCommandMap = pluginManager.getClass().getDeclaredField("commandMap");
            fCommandMap.setAccessible(true);
            Object commandMap = fCommandMap.get(pluginManager);
            fCommandMap.setAccessible(false);

            Field fKnown = commandMap.getClass().getDeclaredField("knownCommands");
            fKnown.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) fKnown.get(commandMap);
            fKnown.setAccessible(false);

            knownCommands.remove(cmd.getName());

            for (String alias : cmd.getAliases()) {
                Command existing = knownCommands.get(alias);
                if (existing != null && existing.toString().contains(plugin.getName())) {
                    knownCommands.remove(alias);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            plugin.getLogger().severe("No se pudo desregistrar el comando: " + e.getMessage());
        }
    }

    private Player getPlayerOrNotify(CommandSender cs) {
        if (cs instanceof Player) return (Player) cs;
        plugin.getLogger().info("Este comando es de jugador.");
        return null;
    }

    private boolean requireLobby(Player player) {
        if (!plugin.getWorldManager().isOnLobby(player)) {
            plugin.getLangManager().sendMessage("not-in-lobby-cmd", player);
            plugin.getSoundManager().playErrorSound(player);
            return false;
        }
        return true;
    }

    private boolean requireInRoom(Player player) {
        if (plugin.getPlayerManager().getTeamId(player) == null) {
            plugin.getLangManager().sendMessage("not-in-room-cmd", player);
            plugin.getSoundManager().playErrorSound(player);
            return false;
        }
        return true;
    }

    private boolean requireSetupPerm(Player player) {
        if (!player.hasPermission("ctw.setup")) {
            plugin.getLangManager().sendMessage("incorrect-parameters", player);
            plugin.getSoundManager().playErrorSound(player);
            return false;
        }
        return true;
    }

    private boolean requireWorldEdit(Player player) {
        if (plugin.getServer().getPluginManager().getPlugin("WorldEdit") == null
                || !plugin.getServer().getPluginManager().getPlugin("WorldEdit").isEnabled()) {
            plugin.getLangManager().sendMessage("we-not-enabled", player);
            plugin.getSoundManager().playErrorSound(player);
            return false;
        }
        return true;
    }

    private Selection getSelection(Player player) {
        WorldEditPlugin we = (WorldEditPlugin) plugin.getServer().getPluginManager().getPlugin("WorldEdit");
        return (we != null) ? we.getSelection(player) : null;
    }

    private boolean requireSelectionOrTip(Player player) {
        Selection sel = getSelection(player);
        if (sel == null) {
            plugin.getLangManager().sendMessage("area-not-selected", player);
            plugin.getSoundManager().playErrorSound(player);
            return false;
        }
        return true;
    }

    private void openRoomsGUI(Player player) {
        player.openInventory(plugin.getLobbyManager().getRoomsGUI());
        plugin.getSoundManager().playGuiSound(player);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String label, String[] args) {
        switch (cmnd.getName().toLowerCase()) {
            case "ctw":          return handleCtw(cs, args);
            case "stats":        return handleStats(cs, args);
            case "rooms":        return handleRooms(cs, args);
            case "kit":          return handleKit(cs, args);
            case "saveglobalkit":return handleSaveGlobalKit(cs, args);
            case "kiteditor":    return handleKitEditor(cs, args);
            case "spawn":        return handleSpawn(cs, args);
            case "ctwsetup":     return handleCtwSetup(cs, args);
            case "createworld":
            case "gotoworld":    return handleWorldNav(cs, cmnd.getName(), args);
            case "savekit":      return handleSaveKit(cs, args);
            case "g":            return handleG(cs, args);
            case "toggle":       return handleToggle(cs, args);
            case "leave":        return handleLeave(cs, args);
            case "join":         return handleJoin(cs, args);
            case "alert":        return handleAlert(cs, args);
            default:             return true;
        }
    }

    private boolean handleCtw(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (args.length != 1) {
            if (player != null) {
                plugin.getLangManager().sendText("commands.ctw", player);
                plugin.getSoundManager().playTipSound(player);
            } else {
                plugin.getLogger().info("Uso: /ctw [reload|save|mapcycle]");
            }
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reload();
                plugin.getLangManager().sendMessage("cmd-success", cs);
                return true;
            case "save":
                plugin.save();
                plugin.getLangManager().sendMessage("cmd-success", cs);
                return true;
            case "mapcycle":
                if (player == null) return true;
                if (!requireInRoom(player)) return true;
                plugin.getGameManager().advanceGame(player.getWorld());
                return true;
            default:
                if (player != null) {
                    plugin.getLangManager().sendText("commands.ctw", player);
                    plugin.getSoundManager().playTipSound(player);
                }
                return true;
        }
    }

    private boolean handleStats(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        try {
            if (plugin.getDBManager() == null) {
                plugin.getLangManager().sendMessage("stats-not-enabled", player);
                plugin.getSoundManager().playErrorSound(player);
                return true;
            }
            PlayerStats cached = plugin.getDBManager().getPlayerStats(player.getName());
            enviarStats(player, cached);
            plugin.getSoundManager().playYourStatsSound(player);
        } catch (Exception e) {
            plugin.getLangManager().sendMessage("stats-not-enabled", player);
            plugin.getSoundManager().playErrorSound(player);
        }
        return true;
    }

    private void enviarStats(Player player, PlayerStats stats) {
        player.sendMessage(plugin.getConfig().getString("message-decorator"));
        player.sendMessage(plugin.getLangManager().getText("stats.title"));
        player.sendMessage(plugin.getLangManager().getText("stats.points").replace("%SCORE%", String.valueOf(stats.score)));
        player.sendMessage(plugin.getLangManager().getText("stats.kills").replace("%KILLS%", String.valueOf(stats.kills)));
        player.sendMessage(plugin.getLangManager().getText("stats.deaths").replace("%DEATHS%", String.valueOf(stats.deaths)));
        player.sendMessage(plugin.getLangManager().getText("stats.placed-wools").replace("%WOOLS_PLACED%", String.valueOf(stats.wools)));
        if(plugin.getEconomy() != null) {
            player.sendMessage(plugin.getLangManager().getText("stats.coins").replace("%COINS%", String.valueOf(plugin.getEconomy().getBalance(player))));
        }
        player.sendMessage(plugin.getConfig().getString("message-decorator"));
    }

    private boolean handleKit(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        if(plugin.getPlayerManager().getTeamId(player) == null
                || plugin.getPlayerManager().getTeamId(player) == TeamManager.TeamId.SPECTATOR) {
            plugin.getLangManager().sendMessage("not-in-game-cmd", player);
            return true;
        }
        if (!plugin.getConfigManager().isKitMenuEnabled()) {
            plugin.getLangManager().sendMessage("kits-not-enabled", player);
            plugin.getSoundManager().playErrorSound(player);
            return true;
        }
        plugin.getKitManager().openKitGUI(player);
        plugin.getSoundManager().playGuiSound(player);
        return true;
    }

    private boolean handleRooms(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        if (plugin.getPlayerManager().getTeamId(player) != null) {
            plugin.getLangManager().sendMessage("not-in-lobby-cmd", player);
            plugin.getSoundManager().playErrorSound(player);
            return true;
        }
        openRoomsGUI(player);
        return true;
    }

    private boolean handleSaveGlobalKit(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        if (plugin.getPlayerManager().getTeamId(player) != null) {
            plugin.getLangManager().sendMessage("not-in-lobby-cmd", player);
            plugin.getSoundManager().playErrorSound(player);
            return true;
        }
        try {
            plugin.getKitManager().saveGlobalKitYAML(player.getInventory().getContents());
            plugin.getLangManager().sendMessage("starting-kit-set", player);
        } catch (IOException e) {
            plugin.getLangManager().sendMessage("error-at-save-kit", player);
            plugin.getSoundManager().playErrorSound(player);
        }
        return true;
    }

    private boolean handleKitEditor(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        if (!requireLobby(player)) return true;

        plugin.getKitManager().invSaver(player, player.getUniqueId());
        ItemStack[] globalKit = plugin.getKitManager().getGlobalKitYAML();
        player.getInventory().clear();
        player.getInventory().setContents(globalKit);
        plugin.getLangManager().sendMessage("edit-your-kit", player);
        plugin.getLangManager().sendMessage("save-your-kit-with", player);
        return true;
    }

    private boolean handleSpawn(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        player.teleport(plugin.getWorldManager().getNextLobbySpawn());
        player.setPlayerListName(player.getName());
        player.setDisplayName(player.getName());
        return true;
    }

    private boolean handleCtwSetup(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        if (args.length > 1) {
            processCtwSetup(player, args);
            return true;
        }
        if (args.length == 0) {
            plugin.getLangManager().sendText("commands.ctwsetup", player);
            plugin.getSoundManager().playTipSound(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "lobby":      plugin.getLangManager().sendText("commands.ctwsetup-lobby", player); plugin.getSoundManager().playTipSound(player); return true;
            case "map":        plugin.getLangManager().sendText("commands.ctwsetup-map", player); plugin.getSoundManager().playTipSound(player); return true;
            case "mapconfig":  plugin.getLangManager().sendText("commands.ctwsetup-mapconfig", player); plugin.getSoundManager().playTipSound(player); return true;
            case "room":       plugin.getLangManager().sendText("commands.ctwsetup-room", player); plugin.getSoundManager().playTipSound(player); return true;
            default:           plugin.getLangManager().sendText("commands.ctwsetup", player); plugin.getSoundManager().playTipSound(player); return true;
        }
    }

    private boolean handleWorldNav(CommandSender cs, String name, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        if (args.length != 1) {
            plugin.getLangManager().sendMessage("incorrect-parameters", cs);
            plugin.getSoundManager().playErrorSound(player);
            plugin.getLangManager().sendText("commands." + name, player);
            return true;
        }
        World world = name.equals("createworld")
                ? plugin.getWorldManager().createEmptyWorld(args[0])
                : plugin.getWorldManager().loadWorld(args[0]);

        if (world == null) {
            plugin.getLangManager().sendMessage("world-doesnot-exists", cs);
            plugin.getSoundManager().playErrorSound(player);
        } else {
            player.teleport(world.getSpawnLocation());
            plugin.getLangManager().sendMessage("cmd-success", player);
        }
        return true;
    }

    private boolean handleSaveKit(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        if (plugin.getPlayerManager().getTeamId(player) != null) {
            plugin.getLangManager().sendMessage("not-in-lobby-cmd", player);
            plugin.getSoundManager().playErrorSound(player);
            return true;
        }
        plugin.getKitManager().saveKit(player, player.getInventory().getContents());
        player.getInventory().clear();
        plugin.getKitManager().invRecover(player, player.getUniqueId());
        return true;
    }

    private boolean handleG(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        if (args.length == 0) {
            plugin.getLangManager().sendText("commands.g", player);
            plugin.getSoundManager().playTipSound(player);
            return true;
        }
        if (plugin.getPlayerManager().getTeamId(player) != null) {
            ChatColor cc = plugin.getPlayerManager().getChatColor(player);
            String message = String.join(" ", args).trim();
            String senderName = player.getDisplayName().replace(player.getName(), cc + player.getName());
            for (Player receiver : player.getWorld().getPlayers()) {
                receiver.sendMessage(senderName + ChatColor.RESET + ": " + message);
            }
        } else {
            plugin.getLangManager().sendMessage("not-in-room-cmd", player);
            plugin.getSoundManager().playErrorSound(player);
        }
        return true;
    }

    private boolean handleToggle(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        if (args.length != 1) {
            plugin.getLangManager().sendText("commands.toggle", player);
            plugin.getSoundManager().playTipSound(player);
            return true;
        }
        if (!requireInRoom(player)) return true;

        switch (args[0].toLowerCase()) {
            case "obs":
                if (plugin.getPlayerManager().toggleSeeOthersSpectators(player)) {
                    plugin.getLangManager().sendMessage("obs-true", player);
                } else {
                    plugin.getLangManager().sendMessage("obs-false", player);
                }
                plugin.getSoundManager().playTipSound(player);
                break;
            case "dms":
                if (plugin.getPlayerManager().toogleOthersDeathMessages(player)) {
                    plugin.getLangManager().sendMessage("dms-true", player);
                } else {
                    plugin.getLangManager().sendMessage("dms-false", player);
                }
                plugin.getSoundManager().playTipSound(player);
                break;
            case "blood":
                if (plugin.getPlayerManager().toggleBloodEffect(player)) {
                    plugin.getLangManager().sendMessage("blood-true", player);
                } else {
                    plugin.getLangManager().sendMessage("blood-false", player);
                }
                plugin.getSoundManager().playTipSound(player);
                break;
            default:
                plugin.getLangManager().sendText("commands.toggle", player);
                plugin.getSoundManager().playTipSound(player);
                break;
        }
        return true;
    }

    private boolean handleLeave(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        if (plugin.getPlayerManager().getTeamId(player) != null) {
            player.teleport(plugin.getWorldManager().getNextLobbySpawn());
            player.setPlayerListName(player.getName());
            player.setDisplayName(player.getName());
        } else {
            plugin.getLangManager().sendMessage("not-in-room-cmd", player);
            plugin.getSoundManager().playErrorSound(player);
        }
        return true;
    }

    private boolean handleJoin(CommandSender cs, String[] args) {
        Player player = getPlayerOrNotify(cs);
        if (player == null) return true;
        TeamManager.TeamId teamId = plugin.getPlayerManager().getTeamId(player);
        if (teamId != TeamManager.TeamId.SPECTATOR) {
            plugin.getLangManager().sendMessage("join-in-team", player);
            plugin.getSoundManager().playErrorSound(player);
            return true;
        }
        player.openInventory(plugin.getTeamManager().getMenuInv());
        plugin.getSoundManager().playJoinCommandSound(player);
        return true;
    }

    private boolean handleAlert(CommandSender cs, String[] args) {
        if (args.length == 0) {
            if (cs instanceof Player) {
                Player player = (Player) cs;
                plugin.getLangManager().sendText("commands.alert", player);
                plugin.getSoundManager().playTipSound(player);
            } else {
                plugin.getLogger().info("Uso: /alert [mensaje]");
            }
            return true;
        }
        String message = String.join(" ", args) + " ";
        for (Player receiver : Bukkit.getOnlinePlayers()) {
            receiver.sendMessage(plugin.getLangManager().getText("alert-prefix") + " " + message);
            plugin.getSoundManager().playAlertSound(receiver);
        }
        return true;
    }

    private void processCtwSetup(Player player, String[] args) {
        if (!requireSetupPerm(player)) return;

        String section = args[0].toLowerCase();

        if ("lobby".equals(section)) {
            if (args.length < 2) {
                plugin.getLangManager().sendText("commands.ctwsetup-lobby", player);
                plugin.getSoundManager().playTipSound(player);
                return;
            }
            String action = args[1].toLowerCase();
            switch (action) {
                case "addspawn": {
                    Location l = player.getLocation();
                    World currentLobby = plugin.getWorldManager().getLobbyWorld();
                    if (currentLobby != null && !currentLobby.getName().equals(l.getWorld().getName())) {
                        plugin.getLangManager().sendMessage("lobby-spawnpoint-missmatch", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    plugin.getWorldManager().addSpawnLocation(l);
                    plugin.getWorldManager().persist();
                    plugin.getLangManager().sendMessage("lobby-spawnpoint-set.message-0", player);
                    plugin.getLangManager().sendMessage("lobby-spawnpoint-set.help-0", player);
                    plugin.getLangManager().sendMessage("lobby-spawnpoint-set.help-1", player);
                    plugin.getLangManager().sendMessage("lobby-spawnpoint-set.help-2", player);
                    return;
                }
                case "listspawn": {
                    List<Location> spawns = plugin.getWorldManager().getLobbySpawnLocations();
                    if (spawns.isEmpty()) {
                        plugin.getLangManager().sendMessage("lobby-spawnpoint-empty", player);
                        return;
                    }
                    for (Location loc : spawns) {
                        player.sendMessage(ChatColor.GRAY + "Spawn: " +
                                loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " @ " +
                                ChatColor.AQUA + loc.getWorld().getName());
                    }
                    return;
                }
                case "clear": {
                    plugin.getWorldManager().clearLobbyInformation();
                    plugin.getWorldManager().persist();
                    plugin.getLangManager().sendMessage("lobby-cleared", player);
                    return;
                }
                case "setworld": {
                    World w = (args.length >= 3) ? plugin.getServer().getWorld(args[2]) : player.getWorld();
                    if (w == null) {
                        plugin.getLangManager().sendMessage("world-doesnot-exists", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    if (plugin.getWorldManager().getLobbySpawnLocations().isEmpty()) {
                        plugin.getWorldManager().addSpawnLocation(w.getSpawnLocation());
                    }
                    plugin.getWorldManager().persist();
                    plugin.getLangManager().sendMessage("lobby-world-set", player);
                    return;
                }
                default:
                    plugin.getLangManager().sendText("commands.ctwsetup-lobby", player);
                    plugin.getSoundManager().playTipSound(player);
                    return;
            }
        }
        if ("map".equals(section)) {
            if (args.length < 2) {
                plugin.getLangManager().sendText("commands.ctwsetup-map", player);
                plugin.getSoundManager().playTipSound(player);
                return;
            }
            String action = args[1].toLowerCase();
            switch (action) {
                case "add": {
                    World w = player.getWorld();
                    if (plugin.getWorldManager().getLobbyWorld() != null && plugin.getWorldManager().getLobbyWorld().getName().equals(w.getName())) {
                        plugin.getLangManager().sendMessage("map-cannot-be-lobby", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    if (plugin.getMapManager().add(w)) {
                        plugin.getLangManager().sendMessage(plugin.getLangManager().getText("map-successfully-added").replace("%MAP%", w.getName()), player);
                    } else {
                        plugin.getLangManager().sendMessage(plugin.getLangManager().getText("map-already-exists").replace("%MAP%", w.getName()), player);
                    }
                    plugin.getMapManager().setupTip(player);
                    return;
                }
                case "remove": {
                    World w = player.getWorld();
                    if (!plugin.getMapManager().exists(w.getName())) {
                        plugin.getLangManager().sendMessage("not-in-map", player);
                        return;
                    }
                    plugin.getMapManager().deleteMap(w);
                    plugin.getMapManager().persist();
                    plugin.getLangManager().sendMessage("map-deleted", player);
                    return;
                }
                case "list": {
                    Set<String> maps = plugin.getMapManager().getMaps();
                    if (maps.isEmpty()) {
                        plugin.getLangManager().sendMessage("map-list-empty", player);
                        return;
                    }
                    plugin.getLangManager().sendMessage("available-maps", player);
                    for (String mapName : maps) {
                        player.sendMessage(ChatColor.GREEN + " - " + ChatColor.AQUA + mapName);
                    }
                    return;
                }
                case "copy": {
                    if (args.length < 3) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    World source = player.getWorld();
                    String dest = args[2];
                    World cloned = plugin.getWorldManager().cloneWorld(source, dest);
                    if (cloned == null) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    plugin.getMapManager().cloneMap(source, cloned);
                    plugin.getLangManager().sendMessage("world-created", player);
                    return;
                }
                default:
                    plugin.getLangManager().sendText("commands.ctwsetup-map", player);
                    plugin.getSoundManager().playTipSound(player);
                    return;
            }
        }
        if ("mapconfig".equals(section)) {
            if (args.length < 2) {
                plugin.getLangManager().sendText("commands.ctwsetup-mapconfig", player);
                plugin.getSoundManager().playTipSound(player);
                return;
            }
            String action = args[1].toLowerCase();
            World w = player.getWorld();
            if (!plugin.getMapManager().exists(w.getName())) {
                plugin.getLangManager().sendMessage("not-in-map", player);
                return;
            }


            switch (action) {
                case "spawn": {
                    plugin.getMapManager().setSpawn(player.getLocation());
                    plugin.getLangManager().sendMessage("mapspawn-set", player);
                    plugin.getMapManager().persist();
                    plugin.getMapManager().setupTip(player);
                    return;
                }
                case "redspawn": {
                    plugin.getMapManager().setRedSpawn(player.getLocation());
                    plugin.getLangManager().sendMessage("redspawn-set", player);
                    plugin.getMapManager().persist();
                    plugin.getMapManager().setupTip(player);
                    return;
                }
                case "bluespawn": {
                    plugin.getMapManager().setBlueSpawn(player.getLocation());
                    plugin.getLangManager().sendMessage("bluespawn-set", player);
                    plugin.getMapManager().persist();
                    plugin.getMapManager().setupTip(player);
                    return;
                }
                case "maxplayers": {
                    if (args.length < 3) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    try {
                        int max = Integer.parseInt(args[2]);
                        if (max < 2) {
                            plugin.getLangManager().sendMessage("incorrect-parameters", player);
                            plugin.getSoundManager().playErrorSound(player);
                            return;
                        }
                        plugin.getMapManager().setMaxPlayers(w, max);
                        plugin.getLangManager().sendMessage("maxplayers-set", player);
                        plugin.getMapManager().persist();
                        plugin.getMapManager().setupTip(player);
                    } catch (NumberFormatException ex) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                    }
                    return;
                }
                case "redwinwool": {
                    plugin.getEventManager().registerSetupEvents(player, com.mauccio.ctw.listeners.EventManager.SetUpAction.RED_WIN_WOOL);
                    plugin.getLangManager().sendMessage("add-red-wool-winpoint.description", player);
                    plugin.getLangManager().sendMessage("add-red-wool-winpoint.help-0", player);
                    plugin.getLangManager().sendMessage("add-red-wool-winpoint.help-1", player);
                    plugin.getMapManager().persist();
                    plugin.getMapManager().setupTip(player);
                    return;
                }
                case "bluewinwool": {
                    plugin.getEventManager().registerSetupEvents(player, com.mauccio.ctw.listeners.EventManager.SetUpAction.BLUE_WIN_WOOL);
                    plugin.getLangManager().sendMessage("add-blue-wool-winpoint.description", player);
                    plugin.getLangManager().sendMessage("add-blue-wool-winpoint.help-0", player);
                    plugin.getLangManager().sendMessage("add-blue-wool-winpoint.help-1", player);
                    plugin.getMapManager().persist();
                    plugin.getMapManager().setupTip(player);

                    return;
                }
                case "rednoaccess": {
                    if (!requireWorldEdit(player) || !requireSelectionOrTip(player)) return;
                    Selection sel = getSelection(player);
                    if (plugin.getMapManager().isRedNoAccessArea(w, sel)) {
                        plugin.getLangManager().sendMessage("area-na-already-red", player);
                    } else {
                        plugin.getMapManager().addRedNoAccessArea(w, sel);
                        plugin.getMapManager().persist();
                        plugin.getLangManager().sendMessage("area-na-done", player);
                    }
                    return;
                }
                case "bluenoaccess": {
                    if (!requireWorldEdit(player) || !requireSelectionOrTip(player)) return;
                    Selection sel = getSelection(player);
                    if (plugin.getMapManager().isBlueNoAccessArea(w, sel)) {
                        plugin.getLangManager().sendMessage("area-na-already-blue", player);
                    } else {
                        plugin.getMapManager().addBlueNoAccessArea(w, sel);
                        plugin.getMapManager().persist();
                        plugin.getLangManager().sendMessage("area-na-done", player);
                    }
                    return;
                }
                case "protected": {
                    if (!requireWorldEdit(player) || !requireSelectionOrTip(player)) return;
                    Selection sel = getSelection(player);
                    if (plugin.getMapManager().isProtectedArea(w, sel)) {
                        plugin.getLangManager().sendMessage("area-na-already-protected", player);
                    } else {
                        plugin.getMapManager().addProtectedArea(w, sel);
                        plugin.getMapManager().persist();
                        plugin.getLangManager().sendMessage("area-na-done", player);
                    }
                    return;
                }
                case "weather": {
                    if (args.length < 3) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    String param = args[2].toLowerCase();
                    if (param.startsWith("fixed=sun")) {
                        plugin.getMapManager().setWeather(w, true, false);
                        plugin.getLangManager().sendMessage("sunny-set", player);
                        plugin.getMapManager().setupTip(player);
                    } else if (param.startsWith("fixed=storm")) {
                        plugin.getMapManager().setWeather(w, true, true);
                        plugin.getLangManager().sendMessage("storm-set", player);
                        plugin.getMapManager().setupTip(player);
                    } else if (param.startsWith("random")) {
                        plugin.getMapManager().setWeather(w, false, false);
                        plugin.getLangManager().sendMessage("random-set", player);
                        plugin.getMapManager().setupTip(player);
                    } else {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                    }
                    return;
                }
                case "woolspawner": {
                    plugin.getEventManager().registerSetupEvents(player, com.mauccio.ctw.listeners.EventManager.SetUpAction.WOOL_SPAWNER);
                    plugin.getLangManager().sendMessage("add-wool-spawners.description", player);
                    plugin.getLangManager().sendMessage("add-wool-spawners.help-0", player);
                    plugin.getLangManager().sendMessage("add-wool-spawners.help-1", player);
                    plugin.getMapManager().setupTip(player);
                    plugin.getMapManager().persist();
                    return;
                }
                case "restore": {
                    if (!requireWorldEdit(player) || !requireSelectionOrTip(player)) return;
                    Selection sel = getSelection(player);
                    if(sel == null) return;
                    plugin.getMapManager().setRestaurationArea(sel);
                    plugin.getMapManager().setupTip(player);
                    plugin.getMapManager().persist();
                    return;
                }
                case "continue": {
                    plugin.getEventManager().unregisterSetUpEvents(player);
                    plugin.getLangManager().sendMessage("cmd-success", player);
                    plugin.getMapManager().persist();
                    plugin.getMapManager().setupTip(player);
                    return;
                }
                case "toggleleather": {
                    boolean active = !plugin.getMapManager().getKitarmour(w);
                    plugin.getMapManager().setKitarmour(w, active);
                    plugin.getLangManager().sendMessage(active ? "default-armour-on" : "default-armour-off", player);
                    plugin.getMapManager().persist();
                    plugin.getMapManager().setupTip(player);
                    return;
                }
                case "removeregion": {
                    if (!requireWorldEdit(player)) return;
                    plugin.getMapManager().removeRegion(player);
                    plugin.getMapManager().persist();
                    plugin.getMapManager().setupTip(player);
                    return;
                }
                case "no-drop": {
                    if (plugin.getMapManager().setNoDrop(player)) {
                        plugin.getLangManager().sendMessage("repeated-material-ok", player);
                        plugin.getMapManager().persist();
                        plugin.getMapManager().setupTip(player);
                    }
                    return;
                }
                default:
                    plugin.getLangManager().sendText("commands.ctwsetup-mapconfig", player);
                    plugin.getSoundManager().playTipSound(player);
                    return;
            }
        }
        if ("room".equals(section)) {
            if (args.length < 2) {
                plugin.getLangManager().sendText("commands.ctwsetup-room", player);
                plugin.getSoundManager().playTipSound(player);
                return;
            }
            String action = args[1].toLowerCase();
            switch (action) {
                case "add": {
                    if (args.length < 3) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    String roomName = args[2];
                    if (plugin.getRoomManager().add(roomName)) {
                        plugin.getRoomManager().persist();
                        plugin.getLangManager().sendMessage("room-added.message-0", player);
                        plugin.getLangManager().sendMessage("room-added.help-1", player);
                    } else {
                        plugin.getLangManager().sendMessage("duplicated-room", player);
                    }
                    return;
                }
                case "remove": {
                    if (args.length < 3) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    String roomName = args[2];
                    if (!plugin.getRoomManager().exists(roomName)) {
                        plugin.getLangManager().sendMessage("room-doesnot-exists", player);
                        return;
                    }
                    if (plugin.getRoomManager().isEnabled(roomName)) {
                        plugin.getLangManager().sendMessage("room-enabled-remove", player);
                        return;
                    }
                    if (plugin.getRoomManager().remove(roomName)) {
                        plugin.getRoomManager().persist();
                        plugin.getLangManager().sendMessage("cmd-success", player);
                    } else {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                    }
                    return;
                }
                case "list": {
                    List<String> list = plugin.getRoomManager().list();
                    if (list.isEmpty()) {
                        plugin.getLangManager().sendMessage("room-list-empty", player);
                        return;
                    }
                    for (String line : list) player.sendMessage(line);
                    return;
                }
                case "enable": {
                    if (args.length < 3) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    String roomName = args[2];
                    if (!plugin.getRoomManager().exists(roomName)) {
                        plugin.getLangManager().sendMessage("room-doesnot-exists", player);
                        return;
                    }
                    if (plugin.getRoomManager().isEnabled(roomName)) {
                        plugin.getLangManager().sendMessage("room-already-enabled", player);
                        return;
                    }
                    if (!plugin.getRoomManager().hasMaps(roomName)) {
                        plugin.getLangManager().sendMessage("room-has-no-map", player);
                        return;
                    }
                    plugin.getRoomManager().enable(roomName);
                    plugin.getRoomManager().persist();
                    plugin.getLangManager().sendMessage("cmd-success", player);
                    return;
                }
                case "disable": {
                    if (args.length < 3) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    String roomName = args[2];
                    if (!plugin.getRoomManager().exists(roomName)) {
                        plugin.getLangManager().sendMessage("room-doesnot-exists", player);
                        return;
                    }
                    if (!plugin.getRoomManager().isEnabled(roomName)) {
                        plugin.getLangManager().sendMessage("room-already-disabled", player);
                        return;
                    }
                    plugin.getRoomManager().disable(roomName);
                    plugin.getRoomManager().persist();
                    plugin.getLangManager().sendMessage("cmd-success", player);
                    return;
                }
                case "addmap": {
                    if (args.length < 4) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    String roomName = args[2];
                    String mapName = args[3];
                    if (!plugin.getRoomManager().exists(roomName)) {
                        plugin.getLangManager().sendMessage("room-doesnot-exists", player);
                        return;
                    }
                    if (plugin.getRoomManager().isEnabled(roomName)) {
                        plugin.getLangManager().sendMessage("edit-enabled-room", player);
                        return;
                    }
                    World map = plugin.getServer().getWorld(mapName);
                    if (map == null || !plugin.getMapManager().exists(mapName)) {
                        plugin.getLangManager().sendMessage("world-doesnot-exists", player);
                        return;
                    }
                    if (plugin.getRoomManager().hasMap(roomName, map)) {
                        plugin.getLangManager().sendMessage("room-already-has-this-map", player);
                        return;
                    }
                    if (plugin.getRoomManager().addMap(roomName, map)) {
                        plugin.getRoomManager().persist();
                        plugin.getLangManager().sendMessage("cmd-success", player);
                    } else {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                    }
                    return;
                }
                case "removemap": {
                    if (args.length < 4) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    String roomName = args[2];
                    String mapName = args[3];
                    if (!plugin.getRoomManager().exists(roomName)) {
                        plugin.getLangManager().sendMessage("room-doesnot-exists", player);
                        return;
                    }
                    if (plugin.getRoomManager().isEnabled(roomName)) {
                        plugin.getLangManager().sendMessage("edit-enabled-room", player);
                        return;
                    }
                    World map = plugin.getServer().getWorld(mapName);
                    if (map == null) {
                        plugin.getLangManager().sendMessage("world-doesnot-exists", player);
                        return;
                    }
                    if (!plugin.getRoomManager().hasMap(roomName, map)) {
                        plugin.getLangManager().sendMessage("room-doesnot-has-this-map", player);
                        return;
                    }
                    if (plugin.getRoomManager().removeMap(roomName, map)) {
                        plugin.getRoomManager().persist();
                        plugin.getLangManager().sendMessage("cmd-success", player);
                    } else {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                    }
                    return;
                }
                default:
                    plugin.getLangManager().sendText("commands.ctwsetup-room", player);
                    plugin.getSoundManager().playTipSound(player);
                    return;
            }
        }
        if ("kit".equals(section)) {
            if (args.length < 2) {
                plugin.getLangManager().sendText("commands.ctwsetup-kit", player);
                plugin.getSoundManager().playTipSound(player);
                return;
            }
            if(!plugin.getConfigManager().isKitMenuEnabled()) {
                plugin.getLangManager().sendMessage("kits-not-enabled", player);
                plugin.getSoundManager().playErrorSound(player);
                return;
            }
            String action = args[1].toLowerCase();
            switch (action) {
                case "create": {
                    if (args.length < 3) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    String kitName = args[2];
                    if (plugin.getKitManager().kitExists(kitName)) {
                        plugin.getLangManager().sendMessage("kits-already-exists", player);
                        return;
                    }
                    plugin.getKitManager().createKit(kitName, player);
                    plugin.getLangManager().sendMessage("kit-edit-tip", player);
                    return;
                }
                case "edit": {
                    if (args.length < 3) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    String kitName = args[2];
                    if (!plugin.getKitManager().kitExists(kitName)) {
                        plugin.getLangManager().sendMessage("kit-doesnot-exists", player);
                        return;
                    }
                    plugin.getKitManager().openKitEditor(kitName, player);
                    plugin.getLangManager().sendMessage("cmd-success", player);
                    return;
                }
                case "delete": {
                    if (args.length < 3) {
                        plugin.getLangManager().sendMessage("incorrect-parameters", player);
                        plugin.getSoundManager().playErrorSound(player);
                        return;
                    }
                    String kitName = args[2];
                    if (!plugin.getKitManager().kitExists(kitName)) {
                        plugin.getLangManager().sendMessage("kit-doesnot-exists", player);
                        return;
                    }
                    plugin.getKitManager().deleteKit(kitName, player);
                    plugin.getLangManager().sendMessage("cmd-success", player);
                    return;
                }
                default:
                    plugin.getLangManager().sendText("commands.ctwsetup-kit", player);
                    plugin.getSoundManager().playTipSound(player);
                    return;
            }
        }
        plugin.getLangManager().sendText("commands.ctwsetup", player);
        plugin.getSoundManager().playTipSound(player);
    }

    public boolean isAllowedInGameCmd(String cmd) {
        return allowedInGameCmds.contains(cmd);
    }
}
