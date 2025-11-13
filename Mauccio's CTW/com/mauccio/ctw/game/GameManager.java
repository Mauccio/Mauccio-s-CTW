package com.mauccio.ctw.game;

import com.mauccio.ctw.libs.titleapi.TitleAPI;
import com.mauccio.ctw.CTW;
import com.mauccio.ctw.map.MapManager;
import com.mauccio.ctw.utils.Utils;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

import java.util.*;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

public class GameManager {

    int counter;

    public class Events {

        @SuppressWarnings("incomplete-switch")
        private boolean isProhibitedLocation(Location location, TeamManager.TeamId ti, Game game) {
            boolean ret = false;
            if (ti != null && ti != TeamManager.TeamId.SPECTATOR) {
                switch (ti) {
                    case BLUE:
                        for (Selection sel : game.bluePhoibitedAreas) {
                            if (sel.contains(location)) {
                                ret = true;
                                break;
                            }
                        }
                        break;
                    case RED:
                        for (Selection sel : game.redPhoibitedAreas) {
                            if (sel.contains(location)) {
                                ret = true;
                                break;
                            }
                        }
                        break;
                }
            }
            return ret;
        }

        public void cancelEditProtectedAreas(BlockPlaceEvent e) {
            Game game = worldGame.get(e.getBlock().getWorld());
            if (game != null) {
                if (isProtected(e.getBlock(), game)) {
                    e.setCancelled(true);
                } else {
                    TeamManager.TeamId ti = plugin.getPlayerManager().getTeamId(e.getPlayer());
                    if (isProhibitedLocation(e.getBlock().getLocation(), ti, game)) {
                        e.setCancelled(true);
                    }
                }
            }
        }

        public void cancelEditProtectedAreas(BlockBreakEvent e) {
            Game game = worldGame.get(e.getBlock().getWorld());
            if (game != null) {
                Material type = e.getBlock().getType();
                List<String> breakable = plugin.getConfigManager().getBreakableBlocks();
                if (breakable.contains(type.name())) {
                    return;
                }
                if (isProtected(e.getBlock(), game)) {
                    e.setCancelled(true);
                } else {
                    TeamManager.TeamId ti = plugin.getPlayerManager().getTeamId(e.getPlayer());
                    if (isProhibitedLocation(e.getBlock().getLocation(), ti, game)) {
                        e.setCancelled(true);
                    }
                }
            }
        }

        public void cancelCrafting(CraftItemEvent e) {
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player plr = (Player) e.getWhoClicked();

            Game game = worldGame.get(plr.getWorld());
            if (game == null) { return;}
            ItemStack result = e.getCurrentItem();
            if(result == null) return;

            List<String> noCrafteable = plugin.getConfigManager().getNoCrafteableItems();

            if(noCrafteable.contains(result.getType().name())) {
                e.setCancelled(true);
            }
        }

        public void cancelUseBukketOnProtectedAreas(PlayerBucketEmptyEvent e) {
            Game game = worldGame.get(e.getBlockClicked().getWorld());
            if (game != null) {
                if (isProtected(e.getBlockClicked(), game)) {
                    e.setCancelled(true);
                } else {
                    TeamManager.TeamId ti = plugin.getPlayerManager().getTeamId(e.getPlayer());
                    if (isProhibitedLocation(e.getBlockClicked().getLocation(), ti, game)) {
                        e.setCancelled(true);
                    }
                }
            }
        }

        private boolean isProtected(Block block, Game game) {
            boolean ret = false;
            Location loc = block.getLocation();
            if (block.getType() == Material.MOB_SPAWNER) {
                ret = true;
            } else {
                if (game.restaurationArea != null && !game.restaurationArea.contains(loc)) {
                    ret = true;
                } else {
                    for (Selection sel : game.mapData.protectedAreas) {
                        loc.setWorld(sel.getWorld());
                        if (sel.contains(loc)) {
                            ret = true;
                            break;
                        }
                    }
                }

            }
            return ret;
        }
    }

    /**
     * Game information.
     */
    private class Target {

        TeamManager.TeamId team;
        DyeColor color;
        Location location;
        boolean completed;
    }

    public enum GameState {

        IN_GAME, FINISHED, NOT_IN_GAME
    }

    protected class Game {

        String roomName;
        int redPlayers;
        int bluePlayers;
        MapManager.MapData mapData;
        World world;
        TreeMap<Location, Target> targets;
        BukkitTask bt;
        int step;
        TreeSet<Selection> bluePhoibitedAreas;
        TreeSet<Selection> redPhoibitedAreas;
        private Selection restaurationArea;
        private final Scoreboard board;
        private GameState state;

        public Game() {
            bluePhoibitedAreas = new TreeSet<>(new Utils.SelectionComparator());
            redPhoibitedAreas = new TreeSet<>(new Utils.SelectionComparator());
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            state = GameState.IN_GAME;
            plugin.getNametagManager().initializeTeams(board);
        }
    }

    private final CTW plugin;
    private final TreeMap<String, Game> games;
    private final TreeMap<World, Game> worldGame;
    public final Events events;
    private final String decorator;

    public GameManager(CTW plugin) {
        this.plugin = plugin;
        games = new TreeMap<>();
        events = new Events();
        worldGame = new TreeMap<>(new Utils.WorldComparator());
        decorator = plugin.getConfig().getString("message-decorator");

        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                spawnWool(games);
            }
        }, 300, 300);

        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                controlPlayers();
            }
        }, 40, 40);
    }

    private void controlPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getPlayerManager().getTeamId(player) == null) {
                if (plugin.getMapManager().isMap(player.getWorld())
                        && !player.hasPermission("ctw.admin")) {
                    plugin.getLogger().log(Level.INFO, "Unexpected event: Player {0} has no team and was on {1}",
                            new Object[]{player.getName(), player.getWorld().getName()});
                    player.teleport(plugin.getWorldManager().getNextLobbySpawn());
                }
            } else {
                if (!plugin.getRoomManager().isInGame(player.getWorld())) {
                    plugin.getLogger().log(Level.INFO, "Unexpected event: Player {0} has team and was on {1}",
                            new Object[]{player.getName(), player.getWorld().getName()});
                    plugin.getPlayerManager().clearTeam(player);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void movePlayerToRoom(Player player, String roomName) {
        World targetWorld = plugin.getRoomManager().getCurrentWorld(roomName);
        if (targetWorld != null) {
            plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("player-join-map")
                    .replace("%PLAYER%", plugin.getPlayerManager().getChatColor(player) + player.getName()), targetWorld, player);

            player.teleport(plugin.getMapManager().getSpawn(targetWorld));
            TitleAPI.sendFullTitle(player, 10, 30, 10, plugin.getLangManager().getTitleMessage("titles.join-room-title"), plugin.getLangManager().getTitleMessage("titles.join-room-subtitle"));
            plugin.getLangManager().sendMessage("use-join", player);
        } else {
            plugin.getLangManager().sendMessage("room-has-no-map", player);
        }
    }

    public boolean joinInTeam(Player player, TeamManager.TeamId teamId) {

        if (teamId == TeamManager.TeamId.SPECTATOR || plugin.hasPermission(player, "choseteam")) {
            movePlayerTo(player, teamId);
        } else {
            plugin.getLangManager().sendMessage("not-teamselect-perm", player);
        }
        return true;
    }

    /**
     * Moves a player into a new team.
     *
     * @param player the player who must be moved into the new team.
     * @param teamId Id of the team where player must be put or null for random.
     */
    @SuppressWarnings({ "incomplete-switch", "deprecation" })
    public void movePlayerTo(Player player, TeamManager.TeamId teamId) {
        String roomName = plugin.getRoomManager().getRoom(player.getWorld());
        if (roomName != null) {
            Game game = games.get(roomName);
            if (game == null || game.mapData == null) {
                plugin.getLogger().log(Level.WARNING, "Improvising non-created game: {0} (please report)", roomName);
                game = addGame(roomName);
            }

            if (teamId != TeamManager.TeamId.SPECTATOR && !plugin.hasPermission(player, "override-limit")
                    && getPlayersIn(roomName) >= game.mapData.maxPlayers) {
                plugin.getLangManager().sendMessage("no-free-slots", player);
                return;
            }

            TeamManager.TeamId prevTeam = plugin.getPlayerManager().getTeamId(player);
            if (prevTeam != null) {
                switch (prevTeam) {
                    case BLUE:
                        game.bluePlayers--;
                        break;
                    case RED:
                        game.redPlayers--;
                        break;
                }
            }

            String advert;
            if (teamId == null) {
                if (game.redPlayers <= game.bluePlayers) {
                    teamId = TeamManager.TeamId.RED;
                    TitleAPI.sendFullTitle(player, 10, 30, 10, plugin.getLangManager().getTitleMessage("titles.join-red-title"), plugin.getLangManager().getTitleMessage("titles.join-red-subtitle"));
                    plugin.getSoundManager().playTeamJoinSound(player);
                } else {
                    teamId = TeamManager.TeamId.BLUE;
                    TitleAPI.sendFullTitle(player, 10, 30, 10, plugin.getLangManager().getTitleMessage("titles.join-blue-title"), plugin.getLangManager().getTitleMessage("titles.join-blue-subtitle"));
                    plugin.getSoundManager().playTeamJoinSound(player);
                }
            }

            switch (teamId) {
                case BLUE:
                    game.bluePlayers++;
                    advert = plugin.getLangManager().getText("player-join-blue");
                    break;
                case RED:
                    game.redPlayers++;
                    advert = plugin.getLangManager().getText("player-join-red");
                    break;
                default:
                    advert = plugin.getLangManager().getText("player-join-spect");
            }
            plugin.getPlayerManager().addPlayerTo(player, teamId);

            if (plugin.getDBManager() != null) {
                String playerName = player.getName();
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    @Override
                    public void run() {
                        plugin.getDBManager().addEvent(playerName, "JOIN|" + roomName + "|" + advert);
                    }
                });
            }

            player.sendMessage(advert.replace("%PLAYER%", player.getName()));
            plugin.getSignManager().updateSigns(roomName);
            takeToSpawn(player);
            player.setScoreboard(game.board);
            updateScoreBoard(game);
            plugin.getNametagManager().updateNametag(player, teamId, game.board);


            if (teamId != TeamManager.TeamId.SPECTATOR) {
                if (game.mapData.kitArmour) {
                    ItemStack air = new ItemStack(Material.AIR);
                    player.getInventory().setBoots(air);
                    player.getInventory().setChestplate(air);
                    player.getInventory().setHelmet(air);
                    player.getInventory().setLeggings(air);
                }

                ItemStack[] kit = plugin.getKitManager().getKit(player);
                if (kit == null || kit.length == 0) {
                    kit = plugin.getKitManager().getGlobalKitYAML();
                }

                if (kit != null && kit.length > 0) {
                    player.getInventory().setContents(kit);
                } else {
                    plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&',
                            "&4Global Kit is not set, use &c/saveglobalkit &4to set!"));
                }
            }
        }
    }

    @SuppressWarnings("incomplete-switch")
    public void playerLeftGame(Player player) {
        String roomName = plugin.getRoomManager().getRoom(player.getWorld());
        TeamManager.TeamId teamId = plugin.getPlayerManager().getTeamId(player);
        if (roomName != null && teamId != null) {
            Game game = games.get(roomName);
            if (game == null) {
                plugin.getLogger().log(Level.WARNING, "Improvising non-created game: {0} (please report)", roomName);
                game = addGame(roomName);
            }

            for (Team team : game.board.getTeams()) {
                team.removeEntry(player.getName());
            }
            switch (teamId) {
                case BLUE:
                    if (game.bluePlayers > 0) {
                        game.bluePlayers--;
                    }
                    break;
                case RED:
                    if (game.redPlayers > 0) {
                        game.redPlayers--;
                    }
                    break;
            }
        }

        if (plugin.getDBManager() != null && roomName != null) {
            String playerName = player.getName();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    plugin.getDBManager().addEvent(playerName, "LEFT|" + roomName);
                }
            });
        }
        plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("player-left-map")
                .replace("%PLAYER%", plugin.getPlayerManager().getChatColor(player) + player.getName()), player.getWorld(), player);

        plugin.getPlayerManager().clearTeam(player);
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            other.showPlayer(player);
        }
        if (roomName != null) {
            plugin.getSignManager().updateSigns(roomName);
        }

        plugin.getLobbyManager().assignLobbyBoard(player);
    }

    public int getPlayersIn(String roomName) {
        Game game = games.get(roomName);
        if (game == null) {
            return 0;
        } else {
            return game.bluePlayers + game.redPlayers;
        }
    }

    public void checkForSpectator(Player player) {

        for (Player spectator : player.getWorld().getPlayers()) {
            if (plugin.getPlayerManager().getTeamId(spectator) != TeamManager.TeamId.SPECTATOR) {
                continue;
            }
            if (player.getLocation().distance(spectator.getLocation()) < 4) {
                spectator.teleport(spectator.getLocation().add(0, 5, 0));
                spectator.setFlying(true);
            }
        }
    }

    @SuppressWarnings("incomplete-switch")
    public void denyEnterToProhibitedZone(PlayerMoveEvent e) {
        TeamManager.TeamId ti = plugin.getPlayerManager().getTeamId(e.getPlayer());
        if (ti == null || ti == TeamManager.TeamId.SPECTATOR || e.getPlayer().getGameMode().equals(GameMode.SPECTATOR)) {
            return;
        }
        String roomName = plugin.getRoomManager().getRoom(e.getPlayer().getWorld());
        if (roomName != null) {
            Game game = games.get(roomName);
            if (game != null) {
                switch (ti) {
                    case BLUE:
                        for (Selection sel : game.bluePhoibitedAreas) {
                            if (!sel.contains(e.getTo())) {
                                continue;
                            }
                            if (sel.contains(e.getFrom())) {
                                e.getPlayer().teleport(getBlueSpawn(roomName));
                            } else {
                                e.setCancelled(true);
                                e.getPlayer().teleport(e.getFrom());
                            }
                        }
                        checkForSpectator(e.getPlayer());
                        break;
                    case RED:
                        for (Selection sel : game.redPhoibitedAreas) {
                            if (!sel.contains(e.getTo())) {
                                continue;
                            }
                            if (sel.contains(e.getFrom())) {
                                e.getPlayer().teleport(getRedSpawn(roomName));
                            } else {
                                e.setCancelled(true);
                                e.getPlayer().teleport(e.getFrom());
                            }
                        }
                        checkForSpectator(e.getPlayer());
                        break;
                }
            }
        }
    }

    public Location getRedSpawn(String roomName) {
        Game game = games.get(roomName);
        if (game == null || game.mapData == null) return null;
        return new Location(game.world, game.mapData.redSpawn.getBlockX(),
                game.mapData.redSpawn.getBlockY(), game.mapData.redSpawn.getBlockZ(), game.mapData.redSpawn.getYaw(), game.mapData.redSpawn.getPitch());
    }

    public Location getBlueSpawn(String roomName) {
        Game game = games.get(roomName);
        if (game == null || game.mapData == null) return null;
        return new Location(game.world, game.mapData.blueSpawn.getBlockX(),
                game.mapData.blueSpawn.getBlockY(), game.mapData.blueSpawn.getBlockZ(), game.mapData.blueSpawn.getYaw(), game.mapData.blueSpawn.getPitch());
    }

    public GameState getState(String roomName) {
        Game game = games.get(roomName);
        if (game == null) {
            return GameState.NOT_IN_GAME;
        } else {
            return game.state;
        }
    }

    /**
     *
     * @param roomName
     * @return Game
     */
    public Game addGame(String roomName) {
        Game game = new Game();
        game.roomName = roomName;

        game.mapData = plugin.getMapManager().getMapData(plugin.getRoomManager().getCurrentMap(roomName));
        game.world = plugin.getRoomManager().getCurrentWorld(roomName);
        games.put(roomName, game);
        worldGame.put(game.world, game);
        game.targets = new TreeMap<>(new Utils.LocationBlockComparator());

        for (String color : game.mapData.redWoolWinPoints.keySet()) {
            Target t = new Target();
            t.color = DyeColor.valueOf(color);
            Location tempLoc = game.mapData.redWoolWinPoints.get(color);
            t.location = new Location(game.world, tempLoc.getBlockX(),
                    tempLoc.getBlockY(), tempLoc.getBlockZ());
            t.team = TeamManager.TeamId.RED;
            game.targets.put(t.location, t);
        }

        for (String color : game.mapData.blueWoolWinPoints.keySet()) {
            Target t = new Target();
            t.color = DyeColor.valueOf(color);
            Location tempLoc = game.mapData.blueWoolWinPoints.get(color);
            t.location = new Location(game.world, tempLoc.getBlockX(),
                    tempLoc.getBlockY(), tempLoc.getBlockZ());
            t.team = TeamManager.TeamId.BLUE;
            game.targets.put(t.location, t);
        }

        if (game.mapData.blueInaccessibleAreas != null) {
            for (Selection sel : game.mapData.blueInaccessibleAreas) {
                game.bluePhoibitedAreas.add(new CuboidSelection(game.world, sel.getNativeMinimumPoint(),
                        sel.getNativeMaximumPoint()));
            }
        }

        if (game.mapData.redInaccessibleAreas != null) {
            for (Selection sel : game.mapData.redInaccessibleAreas) {
                game.redPhoibitedAreas.add(new CuboidSelection(game.world, sel.getNativeMinimumPoint(),
                        sel.getNativeMaximumPoint()));
            }
        }

        if (game.mapData.restaurationArea != null) {
            game.restaurationArea = new CuboidSelection(game.world,
                    game.mapData.restaurationArea.getNativeMinimumPoint(),
                    game.mapData.restaurationArea.getNativeMaximumPoint());
        }

        updateScoreBoard(game);

        if (game.mapData.weather.fixed) {
            game.world.setStorm(game.mapData.weather.storm);
        }

        return game;
    }

    public void removeGame(String roomName) {
        games.remove(roomName);
    }

    public void takeToSpawn(Player player) {
        Game game = worldGame.get(player.getWorld());
        if (game == null || game.mapData == null) return;
        TeamManager.TeamId teamId = plugin.getPlayerManager().getTeamId(player);
        Location spawn;
        if (teamId != null) {
            switch (teamId) {
                case BLUE:
                    spawn = game.mapData.blueSpawn;
                    break;
                case RED:
                    spawn = game.mapData.redSpawn;
                    break;
                default:
                    spawn = game.mapData.mapSpawn;
            }
            spawn.setWorld(game.world);
            player.teleport(spawn);
        }
    }

    public void checkTarget(InventoryClickEvent e) {
        checkTarget((Player) e.getWhoClicked(), e.getCurrentItem());
    }

    public void checkTarget(PlayerPickupItemEvent e) {
        checkTarget(e.getPlayer(), e.getItem().getItemStack());
    }

    @SuppressWarnings("incomplete-switch")
    public void cancelProtectedChest(InventoryOpenEvent e) {
        Player player = (Player) e.getPlayer();
        Game game = worldGame.get(player.getWorld());
        if (game != null && (e.getInventory().getHolder() instanceof Chest
                || e.getInventory().getHolder() instanceof DoubleChest)) {
            TeamManager.TeamId teamId = plugin.getPlayerManager().getTeamId(player);
            Location chestLocation;
            if (e.getInventory().getHolder() instanceof Chest) {
                Chest chest = (Chest) e.getInventory().getHolder();
                chestLocation = chest.getLocation();
            } else {
                DoubleChest chest = (DoubleChest) e.getInventory().getHolder();
                chestLocation = chest.getLocation();
            }
            switch (teamId) {
                case BLUE:
                    for (Selection sel : game.bluePhoibitedAreas) {
                        if (sel.contains(chestLocation)) {
                            e.setCancelled(true);
                            break;
                        }
                    }
                    break;
                case RED:
                    for (Selection sel : game.redPhoibitedAreas) {
                        if (sel.contains(chestLocation)) {
                            e.setCancelled(true);
                            break;
                        }
                    }
                    break;
            }
        }
    }

    @SuppressWarnings({ "incomplete-switch", "deprecation" })
    public void checkTarget(Player player, ItemStack is) {
        Game game = worldGame.get(player.getWorld());
        if (game != null) {
            if (player.getInventory().containsAtLeast(is, 1)) {
                return;
            }
            if (is == null) {
                return;
            }
            if (is.getType() == Material.WOOL) {
                Wool wool = new Wool(is.getTypeId(), is.getData().getData());
                String woolName = plugin.getLangManager().getWoolName(wool.getColor());
                String message = plugin.getLangManager().getText("wool-pickup-message")
                        .replace("%PLAYER%", plugin.getPlayerManager().getChatColor(player) + player.getName())
                        .replace("%WOOL%", Utils.toChatColor(wool.getColor()) + woolName);
                switch (plugin.getPlayerManager().getTeamId(player)) {
                    case BLUE:
                        for (String colorName : game.mapData.blueWoolWinPoints.keySet()) {
                            if (colorName.equals(wool.getColor().name())) {
                                plugin.getLangManager().sendVerbatimMessageToTeam(message, player);
                                for (Player allPlayers : game.world.getPlayers()) {
                                    TitleAPI.sendFullTitle(allPlayers, 10, 60, 10, plugin.getLangManager().getTitleMessage("titles.wool-pickup-title"), plugin.getLangManager().getTitleMessage("titles.wool-pickup-subtitle")
                                            .replace("%PLAYER%", plugin.getPlayerManager().getChatColor(player) + player.getName())
                                            .replace("%WOOL%", Utils.toChatColor(wool.getColor()) + woolName));
                                    plugin.getSoundManager().playWoolPickupSound(allPlayers);
                                }
                                break;
                            }
                        }
                        break;
                    case RED:
                        for (String colorName : game.mapData.redWoolWinPoints.keySet()) {
                            if (colorName.equals(wool.getColor().name())) {
                                plugin.getLangManager().sendVerbatimMessageToTeam(message, player);
                                for (Player allPlayers : game.world.getPlayers()) {
                                    TitleAPI.sendFullTitle(allPlayers, 10, 60, 10, plugin.getLangManager().getTitleMessage("titles.wool-pickup-title"), plugin.getLangManager().getTitleMessage("titles.wool-pickup-subtitle")
                                            .replace("%PLAYER%", plugin.getPlayerManager().getChatColor(player) + player.getName())
                                            .replace("%WOOL%", Utils.toChatColor(wool.getColor()) + woolName));
                                    plugin.getSoundManager().playWoolPickupSound(allPlayers);
                                }
                                break;
                            }
                        }
                        break;
                }

                if (plugin.getDBManager() != null) {
                    String playerName = player.getName();
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getDBManager().addEvent(playerName, "WOOL-PICKUP|" + message);
                        }
                    });
                }
            }
        }
    }

    public void advanceGame(World world) {
        Game game = worldGame.get(world);
        if (game != null) {
            game.step = 10;
            for (Player allPlayers : game.world.getPlayers()) {
                if (plugin.getConfig().getBoolean("keep-teams-on-win")) {
                    allPlayers.setGameMode(GameMode.SPECTATOR);
                    plugin.getPlayerManager().clearInventory(allPlayers);
                    allPlayers.setAllowFlight(true);
                    allPlayers.setFlying(true);
                    if (!allPlayers.isOnGround()) {
                        allPlayers.teleport(allPlayers.getLocation().add(0, 0.5, 0));
                    }
                } else {
                    plugin.getPlayerManager().addPlayerTo(allPlayers, TeamManager.TeamId.SPECTATOR);
                }
            }
            startNewRound(game);
        }
    }

    @SuppressWarnings("deprecation")
    public void checkTarget(BlockPlaceEvent e) {
        Game game = worldGame.get(e.getBlock().getWorld());
        if (game != null) {
            Target t = game.targets.get(e.getBlock().getLocation());

            if (t != null) {
                if (e.getBlock().getType() == Material.WOOL) {
                    Wool wool = new Wool(e.getBlock().getType(), e.getBlock().getData());

                    if (wool.getColor() == t.color && t.team == plugin.getPlayerManager().getTeamId(e.getPlayer())) {
                        e.setCancelled(false);
                        t.completed = true;

                        if (!decorator.isEmpty()) {
                            plugin.getLangManager().sendVerbatimTextToWorld(ChatColor.GOLD + "" + ChatColor.BOLD + decorator, e.getBlock().getWorld(), null);
                        }

                        String woolName = plugin.getLangManager().getWoolName(t.color);
                        String winText = plugin.getLangManager().getText("win-wool-placed")
                                .replace("%PLAYER%", e.getPlayer().getName())
                                .replace("%WOOL%", woolName);
                        plugin.getLangManager().sendVerbatimTextToWorld(winText, e.getBlock().getWorld(), null);

                        for (Player allPlayers : game.world.getPlayers()) {
                            TitleAPI.sendFullTitle(allPlayers, 10, 60, 10, plugin.getLangManager().getTitleMessage("titles.wool-placed-title"), plugin.getLangManager().getTitleMessage("titles.wool-placed-subtitle")
                                    .replace("%PLAYER%", e.getPlayer().getName())
                                    .replace("%WOOL%", woolName));
                            plugin.getSoundManager().playWinWoolSound(allPlayers);
                        }
                        checkForWin(game);

                        if (!decorator.isEmpty()) {
                            plugin.getLangManager().sendVerbatimTextToWorld(ChatColor.GOLD + "" + ChatColor.BOLD + decorator, e.getBlock().getWorld(), null);
                        }

                        Utils.firework(plugin, e.getBlock().getLocation(),
                                wool.getColor().getColor(), wool.getColor().getColor(), wool.getColor().getColor(),
                                FireworkEffect.Type.BALL_LARGE);
                        updateScoreBoard(game);
                        if (plugin.getDBManager() != null) {
                            String playerName = e.getPlayer().getName();
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    plugin.getDBManager().addEvent(playerName, "WOOL-CAPTURE|" + t.color.toString() + "|" + game.roomName + "|" + winText);
                                    plugin.getDBManager().incScore(playerName, plugin.getScores().capture);
                                    String msg = plugin.getLangManager().getText("player-messages.add-points-capture");
                                    e.getPlayer().sendMessage(msg);
                                    if(plugin.getEconomy() != null) {
                                        plugin.getEconomy().depositPlayer(e.getPlayer(), plugin.getScores().coins_capture);
                                        String msgCoins = plugin.getLangManager().getText("player-messages.add-coins.capture");
                                        e.getPlayer().sendMessage(msgCoins);
                                    }
                                    plugin.getDBManager().incWoolCaptured(playerName, 1);
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    public Scoreboard getBoardForWorld(World world) {
        Game game = worldGame.get(world);
        return (game != null) ? game.board : null;
    }
    
    private void updateScoreBoard(Game game) {
        Scoreboard board = game.board;

        Objective old = board.getObjective("wools");
        if (old != null) old.unregister();

        Objective obj = board.registerNewObjective("wools", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(plugin.getLangManager().getText("scoreboard.title"));
        List<String> lines = plugin.getLangManager().getStringList("scoreboard.lines");
        int score = lines.size() + game.targets.size();

        for (String raw : lines) {
            if (raw.equalsIgnoreCase("%BLUE_WOOLS%")) {
                for (String woolLine : buildWoolLines(game, TeamManager.TeamId.BLUE)) {
                    addScoreLine(obj, woolLine, score--);
                }
                continue;
            }
            if (raw.equalsIgnoreCase("%RED_WOOLS%")) {
                for (String woolLine : buildWoolLines(game, TeamManager.TeamId.RED)) {
                    addScoreLine(obj, woolLine, score--);
                }
                continue;
            }
            String line = raw
                    .replace("%BLUE_TEAM_NAME%", plugin.getLangManager().getText("scoreboard.blue-team-name"))
                    .replace("%RED_TEAM_NAME%", plugin.getLangManager().getText("scoreboard.red-team-name"))
                    .replace("%SERVER_IP%", plugin.getLangManager().getText("server-ip"));

            addScoreLine(obj, line, score--);
        }
        for (Player player : game.world.getPlayers()) {
            player.setScoreboard(board);
        }
    }


    private List<String> buildWoolLines(Game game, TeamManager.TeamId team) {
        List<String> lines = new ArrayList<>();
        for (Target t : game.targets.values()) {
            if (t.team != team) continue;
            String state = Utils.toChatColor(t.color) +
                    (t.completed ? plugin.getLangManager().getChar("chars.wool.placed")
                            : plugin.getLangManager().getChar("chars.wool.not-placed"));
            String woolName = plugin.getLangManager().getWoolName(t.color);
            String line = state + " " + ChatColor.WHITE + woolName;
            lines.add(line);
        }
        return lines;
    }

    private void addScoreLine(Objective obj, String text, int score) {
        String line = ChatColor.translateAlternateColorCodes('&', text);
        if (line.length() > 32) line = line.substring(0, 32);
        obj.getScore(line).setScore(score);
    }

    private void checkForWin(Game game) {
        boolean redComplete = true;
        boolean blueComplete = true;
        for (Target target : game.targets.values()) {
            if (!target.completed) {
                if (target.team == TeamManager.TeamId.BLUE) {
                    blueComplete = false;
                } else {
                    redComplete = false;
                }
            }
        }
        if (redComplete) {
            if (!decorator.isEmpty()) {
                plugin.getLangManager().sendVerbatimTextToWorld(decorator, game.world, null);
            }
            plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("red-win-game"), game.world, null);
            for (Player allPlayers : game.world.getPlayers()) {
                if (plugin.getConfig().getBoolean("keep-teams-on-win")) {
                    allPlayers.setGameMode(GameMode.SPECTATOR);
                    plugin.getPlayerManager().clearInventory(allPlayers);
                    allPlayers.setAllowFlight(true);
                    allPlayers.setFlying(true);
                    if (plugin.getConfig().getBoolean("user-fireworks-on-win")) {

                        Utils.firework(plugin, allPlayers.getLocation(),
                                Color.GREEN, Color.RED, Color.BLUE,
                                FireworkEffect.Type.BALL_LARGE);
                    }
                    if (!allPlayers.isOnGround()) {
                        allPlayers.teleport(allPlayers.getLocation().add(0, 0.5, 0));
                    }
                } else {
                    plugin.getPlayerManager().addPlayerTo(allPlayers, TeamManager.TeamId.SPECTATOR);
                }
                Utils.firework(plugin, allPlayers.getLocation(),
                        Color.ORANGE, Color.RED, Color.FUCHSIA,
                        FireworkEffect.Type.BALL_LARGE);
                plugin.getSoundManager().playTeamWinSound(allPlayers);
                TitleAPI.sendFullTitle(allPlayers, 10, 60, 10, plugin.getLangManager().getTitleMessage("titles.team-win-title.red"), plugin.getLangManager().getTitleMessage("titles.team-win-subtitle"));
            }
            if (!decorator.isEmpty()) {
                plugin.getLangManager().sendVerbatimTextToWorld(decorator, game.world, null);
            }
            game.step = 60;
            Bukkit.getScheduler().runTaskLater(plugin, () -> startNewRound(game), 60L);
        } else if (blueComplete) {
            if (!decorator.isEmpty()) {
                plugin.getLangManager().sendVerbatimTextToWorld(decorator, game.world, null);
            }
            plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("blue-win-game"), game.world, null);

            for (Player allPlayers : game.world.getPlayers()) {
                if (plugin.getConfig().getBoolean("keep-teams-on-win")) {
                    allPlayers.setGameMode(GameMode.SPECTATOR);
                    plugin.getPlayerManager().clearInventory(allPlayers);
                    allPlayers.setAllowFlight(true);
                    allPlayers.setFlying(true);
                    if (plugin.getConfig().getBoolean("user-fireworks-on-win")) {

                        Utils.firework(plugin, allPlayers.getLocation(),
                                Color.GREEN, Color.RED, Color.BLUE,
                                FireworkEffect.Type.BALL_LARGE);
                    }
                    if (!allPlayers.isOnGround()) {
                        allPlayers.teleport(allPlayers.getLocation().add(0, 0.5, 0));
                    }
                } else {
                    plugin.getPlayerManager().addPlayerTo(allPlayers, TeamManager.TeamId.SPECTATOR);
                }
                Utils.firework(plugin, allPlayers.getLocation(),
                        Color.PURPLE, Color.TEAL, Color.BLUE,
                        FireworkEffect.Type.BALL_LARGE);
                plugin.getSoundManager().playTeamWinSound(allPlayers);
                TitleAPI.sendFullTitle(allPlayers, 10, 60, 10, plugin.getLangManager().getTitleMessage("titles.team-win-title.blue"), plugin.getLangManager().getTitleMessage("titles.team-win-subtitle"));
            }

            if (!decorator.isEmpty()) {
                plugin.getLangManager().sendVerbatimTextToWorld(decorator, game.world, null);
            }
            game.step = 60;
            Bukkit.getScheduler().runTaskLater(plugin, () -> startNewRound(game), 60L);
        }
    }

    public void ajustWeather(WeatherChangeEvent e) {
        Game game = this.worldGame.get(e.getWorld());
        if (game != null &&
                game.mapData.weather.fixed &&
                e.toWeatherState() != game.mapData.weather.storm)
            e.getWorld().setStorm(game.mapData.weather.storm);
    }

    @SuppressWarnings("deprecation")
    private void startNewRound(Game game) {
        game.state = GameState.FINISHED;

        counter++;
        game.bt = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    switch (game.step) {

                        case 60:
                            plugin.getLangManager().sendMessageToWorld("thirty-seconds-to-start", game.world, null);
                            for (Player player : game.world.getPlayers())
                            {
                                TitleAPI.sendFullTitle(player, 10, 30, 10, plugin.getLangManager().getTitleMessage("titles.countdown-titles.thirty-seconds"), "§7");
                                plugin.getSoundManager().playReversingSound(player);
                            }
                            break;
                        case 40:
                            plugin.getLangManager().sendMessageToWorld("twenty-seconds-to-start", game.world, null);
                            for (Player player : game.world.getPlayers()) {
                                TitleAPI.sendFullTitle(player, 10, 30, 10,
                                        plugin.getLangManager().getTitleMessage("titles.countdown-titles.twenty-seconds"), "§7");
                                plugin.getSoundManager().playReversingSound(player);
                            }
                            break;
                        case 20:
                            plugin.getLangManager().sendMessageToWorld("ten-seconds-to-start", game.world, null);
                            for (Player player : game.world.getPlayers()) {
                                TitleAPI.sendFullTitle(player, 10, 30, 10,
                                        plugin.getLangManager().getTitleMessage("titles.countdown-titles.ten-seconds"), "§7");
                                plugin.getSoundManager().playReversingSound(player);
                            }
                            break;
                        case 10:
                            plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("next-game-starts-in-five"), game.world, null);
                            for (Player player : game.world.getPlayers())
                            {
                                TitleAPI.sendFullTitle(player, 10, 30, 10, plugin.getLangManager().getTitleMessage("titles.countdown-titles.five-seconds"), "§7");
                                plugin.getSoundManager().playReversingSound(player);
                            }
                            break;
                        case 8:
                            plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("four-seconds-to-start"), game.world, null);
                            for (Player player : game.world.getPlayers())
                            {
                                TitleAPI.sendFullTitle(player, 10, 30, 10, plugin.getLangManager().getTitleMessage("titles.countdown-titles.four-seconds"), "§7");
                                plugin.getSoundManager().playReversingSound(player);
                            }
                            break;
                        case 6:
                            plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("three-seconds-to-start"), game.world, null);
                            for (Player player : game.world.getPlayers())
                            {
                                TitleAPI.sendFullTitle(player, 10, 30, 10, plugin.getLangManager().getTitleMessage("titles.countdown-titles.three-seconds"), "§7");
                                plugin.getSoundManager().playReversingSound(player);
                            }
                            break;
                        case 4:
                            plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("two-seconds-to-start"), game.world, null);
                            for (Player player : game.world.getPlayers())
                            {
                                TitleAPI.sendFullTitle(player, 10, 30, 10, plugin.getLangManager().getTitleMessage("titles.countdown-titles.two-seconds"), "§7");
                                plugin.getSoundManager().playReversingSound(player);
                            }
                            break;
                        case 2:
                            plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("one-seconds-to-start"), game.world, null);
                            for (Player player : game.world.getPlayers())
                            {
                                TitleAPI.sendFullTitle(player, 10, 30, 10, plugin.getLangManager().getTitleMessage("titles.countdown-titles.one-seconds"), "§7");
                                plugin.getSoundManager().playReversingSound(player);
                            }
                            break;
                        case 0:
                            TreeMap<Player, TeamManager.TeamId> currentTeams = new TreeMap<>(new Utils.PlayerComparator());
                            plugin.getRoomManager().swapMap(game.roomName);
                            World newMap = plugin.getRoomManager().getCurrentWorld(game.roomName);
                            Location redSpawn = plugin.getGameManager().getRedSpawn(newMap.getName());
                            Location blueSpawn = plugin.getGameManager().getBlueSpawn(newMap.getName());
                            for (Player player : game.world.getPlayers()) {
                                TeamManager.TeamId teamId = plugin.getPlayerManager().getTeamId(player);
                                currentTeams.put(player, teamId);
                                if (teamId == TeamManager.TeamId.RED && redSpawn != null) {
                                    player.teleport(redSpawn);
                                } else if (teamId == TeamManager.TeamId.BLUE && blueSpawn != null) {
                                    player.teleport(blueSpawn);
                                } else {
                                    player.teleport(newMap.getSpawnLocation());
                                }
                                TitleAPI.sendFullTitle(player, 10, 30, 10, "", plugin.getLangManager().getTitleMessage("titles.change-map"));
                                plugin.getSoundManager().playMapChangeSound(player);
                            }


                            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    for (Player player : currentTeams.keySet()) {
                                        plugin.getGameManager().movePlayerTo(player, currentTeams.get(player));
                                    }
                                }
                            }, 10);

                            plugin.getGameManager().removeGame(game.roomName);
                            Game newGame = plugin.getGameManager().addGame(game.roomName);
                            newGame.state = GameState.IN_GAME;
                            for (Map.Entry<Player, TeamManager.TeamId> entry : currentTeams.entrySet()) {
                                plugin.getPlayerManager().addPlayerTo(entry.getKey(), entry.getValue());
                            }
                            plugin.getLangManager().sendMessageToWorld("starting-new-game", game.world, null);
                            break;
                        case -1:
                            game.bt.cancel();
                    }
                } finally {
                    game.step--;
                }
            }
        }, 10, 10);
    }

    private void spawnWool(TreeMap<String, Game> games) {
        for (Game game : games.values()) {
            if (game.mapData.woolSpawners != null) {
                for (String woolColor : game.mapData.woolSpawners.keySet()) {
                    DyeColor dyeColor = DyeColor.valueOf(woolColor);
                    Wool wool = new Wool(dyeColor);
                    ItemStack stack = wool.toItemStack(1);
                    Location loc = new Location(game.world,
                            game.mapData.woolSpawners.get(woolColor).getBlockX(),
                            game.mapData.woolSpawners.get(woolColor).getBlockY(),
                            game.mapData.woolSpawners.get(woolColor).getBlockZ());
                    for (Player player : game.world.getPlayers()) {
                        if (player.getLocation().distance(loc) <= 6
                                && !plugin.getPlayerManager().isSpectator(player)) {
                            game.world.dropItem(loc, stack);
                        }
                    }
                }
            }
        }
    }
}

