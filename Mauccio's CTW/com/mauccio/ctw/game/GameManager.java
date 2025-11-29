package com.mauccio.ctw.game;

import com.mauccio.ctw.CTW;
import com.mauccio.ctw.libs.titleapi.TitleAPI;
import com.mauccio.ctw.listeners.SoundManager;
import com.mauccio.ctw.map.MapManager;
import com.mauccio.ctw.utils.Utils;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<UUID, EnumSet<DyeColor>> playersWithWool = new ConcurrentHashMap<>();


    public class Events {

        @SuppressWarnings("incomplete-switch")
        private boolean isProhibitedLocation(Location location, TeamManager.TeamId ti, Game game) {
            boolean ret = false;
            if (ti != null && ti != TeamManager.TeamId.SPECTATOR) {
                switch (ti) {
                    case BLUE:
                        for (Selection sel : game.blueProhibitedAreas) {
                            if (sel.contains(location)) {
                                ret = true;
                                break;
                            }
                        }
                        break;
                    case RED:
                        for (Selection sel : game.redProhibitedAreas) {
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
        TreeSet<Selection> blueProhibitedAreas;
        TreeSet<Selection> redProhibitedAreas;
        private Selection restaurationArea;
        private final Scoreboard board;
        private GameState state;
        private long startTime;

        public Game() {
            blueProhibitedAreas = new TreeSet<>(new Utils.SelectionComparator());
            redProhibitedAreas = new TreeSet<>(new Utils.SelectionComparator());
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            state = GameState.IN_GAME;
            plugin.getNametagManager().initializeTeams(board);
        }

        public void markStart() {
            this.startTime = System.currentTimeMillis();
        }

        public long getStartTime() {
            return startTime;
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

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Game game : games.values()) {
                if (game.state == GameState.IN_GAME) {
                    List<String> lines = plugin.getLangManager().getStringList("scoreboard.lines");
                    boolean hasTime = lines.stream().anyMatch(line -> line.contains("%TIME_ELAPSED%"));
                    if (hasTime) {
                        updateScoreBoard(game);
                    }
                }
            }
        }, 20L, 20L);
    }

    /**
     * Simple player control.
     */
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

    /**
     * Moves a player into a room.
     *
     * @param player the player who must be moved into a Room.
     * @param roomName Room's name.
     */
    public void movePlayerToRoom(Player player, String roomName) {
        World targetWorld = plugin.getRoomManager().getCurrentWorld(roomName);
        if (targetWorld != null) {
            plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("player-join-map")
                    .replace("%PLAYER%", plugin.getPlayerManager().getChatColor(player) + player.getName()), targetWorld, player);

            player.teleport(plugin.getMapManager().getSpawn(targetWorld));
            plugin.getTitleManager().sendJoinRoom(player);
            plugin.getLangManager().sendMessage("use-join", player);
        } else {
            plugin.getLangManager().sendMessage("room-has-no-map", player);
        }
    }

    /**
     * Choose team logic.
     *
     * @param player the player who must be moved into the new team.
     * @param teamId Id of the team where player must be put or null for random.
     */
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

            if (prevTeam != null && prevTeam != teamId && (game.redPlayers > 0 || game.bluePlayers > 0)) {
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
                    plugin.getTitleManager().sendJoinRed(player);
                    plugin.getSoundManager().playTeamJoinSound(player, SoundManager.SoundTeam.RED);
                } else {
                    teamId = TeamManager.TeamId.BLUE;
                    plugin.getTitleManager().sendJoinBlue(player);
                    plugin.getSoundManager().playTeamJoinSound(player, SoundManager.SoundTeam.BLUE);
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
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDBManager().addEvent(playerName, "JOIN|" + roomName + "|" + advert));
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

    /**
     * Moves a player outside of a Game.
     *
     * @param player the player who must be moved outside of a Game.
     */

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
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDBManager().addEvent(playerName, "LEFT|" + roomName));
        }
        plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("player-left-map")
                .replace("%PLAYER%", plugin.getPlayerManager().getChatColor(player) + player.getName()), player.getWorld(), player);

        plugin.getPlayerManager().clearTeam(player);
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            other.showPlayer(player);
        }

        if (roomName != null) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getSignManager().updateSigns(roomName));
        }

        if(plugin.getConfigManager().isLobbyBoardEnabled()) {
            plugin.getLobbyManager().assignLobbyBoard(player);
        } else {
            Scoreboard emptyBoard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(emptyBoard);
        }

        if(playersWithWool.containsKey(player.getUniqueId())) {
            clearPlayerWools(player.getUniqueId());
        }
        TitleAPI.clearTitle(player);
    }

    public Map<UUID, EnumSet<DyeColor>> getPlayersWithWool() {
        return playersWithWool;
    }

    public int getPlayersIn(String roomName) {
        Game game = games.get(roomName);
        if (game == null) {
            return 0;
        } else {
            return game.bluePlayers + game.redPlayers;
        }
    }

    public int getPlayersInRoom(String roomName) {
        Game game = games.get(roomName);
        if (game == null) {
            return 0;
        } else {
            return game.world.getPlayers().size();
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
                        for (Selection sel : game.blueProhibitedAreas) {
                            if (!sel.contains(e.getTo())) {
                                continue;
                            }
                            if (sel.contains(e.getFrom())) {
                                e.getPlayer().teleport(getBlueSpawn(roomName));
                            } else {
                                e.setCancelled(true);
                                e.getPlayer().teleport(e.getFrom());
                                if(plugin.getConfigManager().isProtectedZoneMsg()) {
                                    plugin.getLangManager().sendMessage("prohibited-area", e.getPlayer());
                                }
                            }
                        }
                        checkForSpectator(e.getPlayer());
                        break;
                    case RED:
                        for (Selection sel : game.redProhibitedAreas) {
                            if (!sel.contains(e.getTo())) {
                                continue;
                            }
                            if (sel.contains(e.getFrom())) {
                                e.getPlayer().teleport(getRedSpawn(roomName));
                            } else {
                                e.setCancelled(true);
                                e.getPlayer().teleport(e.getFrom());
                                if(plugin.getConfigManager().isProtectedZoneMsg()) {
                                    plugin.getLangManager().sendMessage("prohibited-area", e.getPlayer());
                                }
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
                game.blueProhibitedAreas.add(new CuboidSelection(game.world, sel.getNativeMinimumPoint(),
                        sel.getNativeMaximumPoint()));
            }
        }

        if (game.mapData.redInaccessibleAreas != null) {
            for (Selection sel : game.mapData.redInaccessibleAreas) {
                game.redProhibitedAreas.add(new CuboidSelection(game.world, sel.getNativeMinimumPoint(),
                        sel.getNativeMaximumPoint()));
            }
        }

        if (game.mapData.restaurationArea != null) {
            game.restaurationArea = new CuboidSelection(game.world,
                    game.mapData.restaurationArea.getNativeMinimumPoint(),
                    game.mapData.restaurationArea.getNativeMaximumPoint());
        }
        game.markStart();

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
                    break;
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
                    for (Selection sel : game.blueProhibitedAreas) {
                        if (sel.contains(chestLocation)) {
                            e.setCancelled(true);
                            break;
                        }
                    }
                    break;
                case RED:
                    for (Selection sel : game.redProhibitedAreas) {
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
                DyeColor color = wool.getColor();
                String woolName = plugin.getLangManager().getWoolName(color);
                String message = plugin.getLangManager().getText("wool-pickup-message")
                        .replace("%PLAYER%", plugin.getPlayerManager().getChatColor(player) + player.getName())
                        .replace("%WOOL%", Utils.toChatColor(color) + woolName);
                switch (plugin.getPlayerManager().getTeamId(player)) {
                    case BLUE:
                        for (String colorName : game.mapData.blueWoolWinPoints.keySet()) {
                            if (colorName.equals(wool.getColor().name())) {
                                boolean newly = addPlayerWool(player.getUniqueId(), color);
                                if(newly) {
                                    plugin.getLangManager().sendVerbatimMessageToTeam(message, player);
                                }
                                for (Player players : game.world.getPlayers()) {
                                    if (newly) {
                                        String colored = plugin.getPlayerManager().getChatColor(player) + player.getName();
                                        String woolTitle = Utils.toChatColor(color) + woolName;
                                        plugin.getTitleManager().sendWoolPickup(players, colored, woolTitle);

                                        TeamManager.TeamId team = plugin.getPlayerManager().getTeamId(players);
                                        if (team == TeamManager.TeamId.RED) {
                                            plugin.getSoundManager().playWoolPickupSound(players, SoundManager.SoundFor.ENEMY);
                                        } else if (team == TeamManager.TeamId.BLUE) {
                                            plugin.getSoundManager().playWoolPickupSound(players, SoundManager.SoundFor.SAME);
                                        } else {
                                            plugin.getSoundManager().playWoolPickupSound(players, SoundManager.SoundFor.GENERIC);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                        break;
                    case RED:
                        for (String colorName : game.mapData.redWoolWinPoints.keySet()) {
                            if (colorName.equals(wool.getColor().name())) {
                                boolean newly = addPlayerWool(player.getUniqueId(), color);
                                if(newly) {
                                    plugin.getLangManager().sendVerbatimMessageToTeam(message, player);
                                }
                                for (Player players : game.world.getPlayers()) {
                                    if (newly) {
                                        String colored = plugin.getPlayerManager().getChatColor(player) + player.getName();
                                        String woolTitle = Utils.toChatColor(color) + woolName;
                                        plugin.getTitleManager().sendWoolPickup(players, colored, woolTitle);

                                        TeamManager.TeamId team = plugin.getPlayerManager().getTeamId(players);
                                        if (team == TeamManager.TeamId.RED) {
                                            plugin.getSoundManager().playWoolPickupSound(players, SoundManager.SoundFor.ENEMY);
                                        } else if (team == TeamManager.TeamId.BLUE) {
                                            plugin.getSoundManager().playWoolPickupSound(players, SoundManager.SoundFor.SAME);
                                        } else {
                                            plugin.getSoundManager().playWoolPickupSound(players, SoundManager.SoundFor.GENERIC);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                        break;
                }
                if (plugin.getDBManager() != null) {
                    String playerName = player.getName();
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDBManager().addEvent(playerName, "WOOL-PICKUP|" + message));
                }
            }
        }
    }

    private boolean addPlayerWool(UUID id, DyeColor color) {
        EnumSet<DyeColor> set = playersWithWool.computeIfAbsent(id, k -> EnumSet.noneOf(DyeColor.class));
        synchronized (set) {
            return set.add(color);
        }
    }

    private boolean removePlayerWool(UUID id, DyeColor color) {
        EnumSet<DyeColor> set = playersWithWool.get(id);
        if (set == null) return false;
        synchronized (set) {
            boolean removed = set.remove(color);
            if (set.isEmpty()) {
                playersWithWool.remove(id);
            }
            return removed;
        }
    }

    public void clearPlayerWools(UUID id) {
        playersWithWool.remove(id);
    }

    private boolean playerHasWool(UUID id, DyeColor color) {
        EnumSet<DyeColor> set = playersWithWool.get(id);
        return set != null && set.contains(color);
    }

    public void advanceGame(World world) {
        Game game = worldGame.get(world);
        if (game != null) {
            game.step = 10;
            for (Player players : game.world.getPlayers()) {
                if (plugin.getConfig().getBoolean("keep-teams-on-win")) {
                    players.setGameMode(GameMode.ADVENTURE);
                    plugin.getPlayerManager().clearInventory(players);
                    players.setAllowFlight(true);
                    players.setFlying(true);
                    if (!players.isOnGround()) {
                        players.teleport(players.getLocation().add(0, 0.5, 0));
                    }
                } else {
                    plugin.getPlayerManager().addPlayerTo(players, TeamManager.TeamId.SPECTATOR);
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
                        UUID id = e.getPlayer().getUniqueId();
                        DyeColor placedColor = wool.getColor();
                        removePlayerWool(e.getPlayer().getUniqueId(), placedColor);

                        if (!decorator.isEmpty()) {
                            plugin.getLangManager().sendVerbatimTextToWorld(decorator, e.getBlock().getWorld(), null);
                        }

                        String woolName = plugin.getLangManager().getWoolName(t.color);
                        String winText = plugin.getLangManager().getText("win-wool-placed")
                                .replace("%PLAYER%", e.getPlayer().getName())
                                .replace("%WOOL%", Utils.toChatColor(wool.getColor()) + woolName);
                        plugin.getLangManager().sendVerbatimTextToWorld(winText, e.getBlock().getWorld(), null);

                        for (Player players : game.world.getPlayers()) {
                            String colored = plugin.getPlayerManager().getChatColor(e.getPlayer()) + e.getPlayer().getName();
                            String woolTitle = Utils.toChatColor(wool.getColor()) + woolName;
                            plugin.getTitleManager().sendWoolPlaced(players, colored, woolTitle);

                            TeamManager.TeamId team = plugin.getPlayerManager().getTeamId(players);
                            if(team == plugin.getPlayerManager().getTeamId(e.getPlayer())) {
                                plugin.getSoundManager().playWinWoolSound(players, SoundManager.SoundFor.SAME);
                            } else if(team == TeamManager.TeamId.SPECTATOR) {
                                plugin.getSoundManager().playWinWoolSound(players, SoundManager.SoundFor.GENERIC);
                            } else {
                                plugin.getSoundManager().playWinWoolSound(players, SoundManager.SoundFor.ENEMY);
                            }
                        }

                        checkForWin(game);

                        if (!decorator.isEmpty()) {
                            plugin.getLangManager().sendVerbatimTextToWorld(decorator, e.getBlock().getWorld(), null);
                        }

                        Utils.firework(plugin, e.getBlock().getLocation(),
                                wool.getColor().getColor(), wool.getColor().getColor(), wool.getColor().getColor(),
                                FireworkEffect.Type.BALL_LARGE);
                        updateScoreBoard(game);
                        if (plugin.getDBManager() != null) {
                            String playerName = e.getPlayer().getName();
                            String msg = plugin.getLangManager().getText("player-messages.add-points-capture");
                            e.getPlayer().sendMessage(msg);
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                plugin.getDBManager().addEvent(playerName, "WOOL-CAPTURE|" + t.color.toString() + "|" + game.roomName + "|" + winText);
                                plugin.getDBManager().incScore(playerName, plugin.getScores().capture);
                                if(plugin.getEconomy() != null) {
                                    plugin.getEconomy().depositPlayer(e.getPlayer(), plugin.getScores().coins_capture);
                                    String msgCoins = plugin.getLangManager().getText("player-messages.add-coins.capture");
                                    e.getPlayer().sendMessage(msgCoins);
                                }
                                plugin.getDBManager().incWoolCaptured(playerName, 1);
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
                    .replace("%SERVER_IP%", plugin.getLangManager().getText("server-ip"))
                    .replace("%ROOM_NAME%", game.roomName)
                    .replace("%PLAYERS%", String.valueOf(getPlayersInRoom(game.roomName)))
                    .replace("%MAX_PLAYERS%", String.valueOf(game.mapData.maxPlayers))
                    .replace("%RED_PLAYERS%", String.valueOf(game.redPlayers))
                    .replace("%BLUE_PLAYERS%", String.valueOf(game.bluePlayers))
                    .replace("%MAP_NAME%", plugin.getRoomManager().getCurrentMap(game.roomName))
                    .replace("%TIME_ELAPSED%", getElapsedTimeFormatted(game));

            addScoreLine(obj, line, score--);
        }
        for (Player player : game.world.getPlayers()) {
            player.setScoreboard(board);
            updateGameTabList(player, game);
        }
    }

    private long getElapsedTime(Game game) {
        return System.currentTimeMillis() - game.getStartTime();
    }

    private String getElapsedTimeFormatted(Game game) {
        long elapsed = getElapsedTime(game);

        long seconds = (elapsed / 1000) % 60;
        long minutes = (elapsed / (1000 * 60)) % 60;
        long hours   = (elapsed / (1000 * 60 * 60));

        if (hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private List<String> buildWoolLines(Game game, TeamManager.TeamId team) {
        List<String> lines = new ArrayList<>();
        for (Target t : game.targets.values()) {
            if (t.team != team) continue;
            String state = Utils.toChatColor(t.color) +
                    (t.completed ? plugin.getLangManager().getText("chars.wool.placed")
                            : plugin.getLangManager().getText("chars.wool.not-placed"));
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

            for (Player players : game.world.getPlayers()) {
                TeamManager.TeamId team = plugin.getPlayerManager().getTeamId(players);
                if(team == TeamManager.TeamId.RED) {
                    plugin.getSoundManager().playTeamWinSound(players, SoundManager.SoundFor.SAME);
                } else if(team == TeamManager.TeamId.BLUE) {
                    plugin.getSoundManager().playTeamWinSound(players, SoundManager.SoundFor.ENEMY);
                } else {
                    plugin.getSoundManager().playTeamWinSound(players, SoundManager.SoundFor.GENERIC);
                }
                if (plugin.getConfig().getBoolean("keep-teams-on-win")) {
                    players.setGameMode(GameMode.ADVENTURE);
                    plugin.getPlayerManager().clearInventory(players);
                    players.setAllowFlight(true);
                    players.setFlying(true);
                    if (plugin.getConfig().getBoolean("user-fireworks-on-win")) {

                        Utils.firework(plugin, players.getLocation(),
                                Color.GREEN, Color.RED, Color.BLUE,
                                FireworkEffect.Type.BALL_LARGE);
                    }
                    if (!players.isOnGround()) {
                        players.teleport(players.getLocation().add(0, 0.5, 0));
                    }
                } else {
                    plugin.getPlayerManager().addPlayerTo(players, TeamManager.TeamId.SPECTATOR);
                }
                Utils.firework(plugin, players.getLocation(),
                        Color.ORANGE, Color.RED, Color.FUCHSIA,
                        FireworkEffect.Type.BALL_LARGE);
                plugin.getTitleManager().sendWinRed(players);
                updateGameTabList(players, game);
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

            for (Player players : game.world.getPlayers()) {
                TeamManager.TeamId team = plugin.getPlayerManager().getTeamId(players);
                if(team == TeamManager.TeamId.BLUE) {
                    plugin.getSoundManager().playTeamWinSound(players, SoundManager.SoundFor.SAME);
                } else if(team == TeamManager.TeamId.RED) {
                    plugin.getSoundManager().playTeamWinSound(players, SoundManager.SoundFor.ENEMY);
                } else {
                    plugin.getSoundManager().playTeamWinSound(players, SoundManager.SoundFor.GENERIC);
                }
                if (plugin.getConfig().getBoolean("keep-teams-on-win")) {
                    players.setGameMode(GameMode.ADVENTURE);
                    plugin.getPlayerManager().clearInventory(players);
                    players.setAllowFlight(true);
                    players.setFlying(true);
                    if (plugin.getConfig().getBoolean("user-fireworks-on-win")) {

                        Utils.firework(plugin, players.getLocation(),
                                Color.GREEN, Color.RED, Color.BLUE,
                                FireworkEffect.Type.BALL_LARGE);
                    }
                    if (!players.isOnGround()) {
                        players.teleport(players.getLocation().add(0, 0.5, 0));
                    }
                } else {
                    plugin.getPlayerManager().addPlayerTo(players, TeamManager.TeamId.SPECTATOR);
                }
                Utils.firework(plugin, players.getLocation(),
                        Color.PURPLE, Color.TEAL, Color.BLUE,
                        FireworkEffect.Type.BALL_LARGE);
                plugin.getTitleManager().sendWinBlue(players);
                updateGameTabList(players, game);
            }

            if (!decorator.isEmpty()) {
                plugin.getLangManager().sendVerbatimTextToWorld(decorator, game.world, null);
            }
            game.step = 60;
            Bukkit.getScheduler().runTaskLater(plugin, () -> startNewRound(game), 60L);
        }
    }

    private void updateGameTabList(Player player, Game game) {
        String header = plugin.getLangManager().getText("tablist.header")
                .replace("%MAP_NAME%", plugin.getRoomManager().getCurrentMap(game.roomName))
                .replace("%SERVER_IP%", plugin.getLangManager().getText("server-ip"))
                .replace("%ROOM_NAME%", game.roomName)
                .replace("%PLAYERS%", String.valueOf(getPlayersInRoom(game.roomName)))
                .replace("%MAX_PLAYERS%", String.valueOf(game.mapData.maxPlayers));
        String footer = plugin.getLangManager().getText("tablist.footer")
                .replace("%MAP_NAME%", plugin.getRoomManager().getCurrentMap(game.roomName))
                .replace("%SERVER_IP%", plugin.getLangManager().getText("server-ip"))
                .replace("%ROOM_NAME%", game.roomName)
                .replace("%PLAYERS%", String.valueOf(getPlayersInRoom(game.roomName)))
                .replace("%MAX_PLAYERS%", String.valueOf(game.mapData.maxPlayers));;
        plugin.getTitleManager().sendTabList(player, header, footer);
    }

    public void ajustWeather(WeatherChangeEvent e) {
        Game game = this.worldGame.get(e.getWorld());
        if (game != null &&
                game.mapData.weather.fixed &&
                e.toWeatherState() != game.mapData.weather.storm)
            e.getWorld().setStorm(game.mapData.weather.storm);
    }

    private void startNewRound(Game game) {
        game.state = GameState.FINISHED;

        counter++;
        game.bt = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                switch (game.step) {

                    case 60:
                        plugin.getLangManager().sendMessageToWorld("thirty-seconds-to-start", game.world, null);
                        for (Player player : game.world.getPlayers())
                        {
                            plugin.getTitleManager().sendCountdown30(player);
                            plugin.getSoundManager().playReversingSound(player);
                        }
                        break;
                    case 40:
                        plugin.getLangManager().sendMessageToWorld("twenty-seconds-to-start", game.world, null);
                        for (Player player : game.world.getPlayers()) {
                            plugin.getTitleManager().sendCountdown20(player);
                            plugin.getSoundManager().playReversingSound(player);
                        }
                        break;
                    case 20:
                        plugin.getLangManager().sendMessageToWorld("ten-seconds-to-start", game.world, null);
                        for (Player player : game.world.getPlayers()) {
                            plugin.getTitleManager().sendCountdown10(player);
                            plugin.getSoundManager().playReversingSound(player);
                        }
                        break;
                    case 10:
                        plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("next-game-starts-in-five"), game.world, null);
                        for (Player player : game.world.getPlayers())
                        {
                            plugin.getTitleManager().sendCountdown5(player);
                            plugin.getSoundManager().playReversingSound(player);
                        }
                        break;
                    case 8:
                        plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("four-seconds-to-start"), game.world, null);
                        for (Player player : game.world.getPlayers())
                        {
                            plugin.getTitleManager().sendCountdown4(player);
                            plugin.getSoundManager().playReversingSound(player);
                        }
                        break;
                    case 6:
                        plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("three-seconds-to-start"), game.world, null);
                        for (Player player : game.world.getPlayers())
                        {
                            plugin.getTitleManager().sendCountdown3(player);
                            plugin.getSoundManager().playReversingSound(player);
                        }
                        break;
                    case 4:
                        plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("two-seconds-to-start"), game.world, null);
                        for (Player player : game.world.getPlayers())
                        {
                            plugin.getTitleManager().sendCountdown2(player);
                            plugin.getSoundManager().playReversingSound(player);
                        }
                        break;
                    case 2:
                        plugin.getLangManager().sendVerbatimTextToWorld(plugin.getLangManager().getText("one-second-to-start"), game.world, null);
                        for (Player player : game.world.getPlayers())
                        {
                            plugin.getTitleManager().sendCountdown1(player);
                            plugin.getSoundManager().playReversingSound(player);
                        }
                        break;
                    case 0:
                        TreeMap<Player, TeamManager.TeamId> currentTeams = new TreeMap<>(new Utils.PlayerComparator());
                        plugin.getRoomManager().swapMap(game.roomName);
                        World newMap = plugin.getRoomManager().getCurrentWorld(game.roomName);

                        plugin.getGameManager().removeGame(game.roomName);
                        Game newGame = plugin.getGameManager().addGame(game.roomName);
                        newGame.state = GameState.IN_GAME;

                        Location redSpawn = plugin.getGameManager().getRedSpawn(newMap.getName());
                        Location blueSpawn = plugin.getGameManager().getBlueSpawn(newMap.getName());

                        for (Player player : game.world.getPlayers()) {
                            if(playersWithWool.containsKey(player.getUniqueId())) {
                                clearPlayerWools(player.getUniqueId());
                            }
                            TeamManager.TeamId teamId = plugin.getPlayerManager().getTeamId(player);
                            currentTeams.put(player, teamId);

                            if (teamId == TeamManager.TeamId.RED && redSpawn != null) {
                                player.teleport(redSpawn);
                            } else if (teamId == TeamManager.TeamId.BLUE && blueSpawn != null) {
                                player.teleport(blueSpawn);
                            } else {
                                player.teleport(newMap.getSpawnLocation());
                            }

                            plugin.getTitleManager().sendChangeMap(player);
                            plugin.getSoundManager().playMapChangeSound(player);
                            updateGameTabList(player, newGame);
                        }

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            for (Player player : currentTeams.keySet()) {
                                plugin.getGameManager().movePlayerTo(player, currentTeams.get(player));
                                updateGameTabList(player, newGame);
                            }
                        }, 10);

                        plugin.getLangManager().sendMessageToWorld("starting-new-game", newMap, null);
                        break;

                    case -1:
                        game.bt.cancel();
                }
            } finally {
                game.step--;
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
