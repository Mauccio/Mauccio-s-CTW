package com.mauccio.ctw.listeners;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeMap;

import com.mauccio.ctw.CTW;
import com.mauccio.ctw.game.PlayerManager;
import com.mauccio.ctw.game.TeamManager;
import com.mauccio.ctw.utils.LobbyItem;
import com.mauccio.ctw.utils.Utils;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Wool;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class EventManager {

    private final CTW plugin;
    private final GameListeners gameEvents;
    private final TreeMap<Player, SetupListeners> playerSetup;

    /**
     *
     */
    public enum SetUpAction {

        RED_WIN_WOOL, BLUE_WIN_WOOL, WOOL_SPAWNER
    }

    private class SetupListeners implements Listener {

        private final SetUpAction action;

        public SetupListeners(SetUpAction action) {
            this.action = action;
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockBreakEvent(BlockBreakEvent e) {
            if (!playerSetup.containsKey(e.getPlayer())) {
                return;
            }
            Location currLoc;
            if (e.getBlock().getType() != Material.WOOL) {
                if (e.getBlock().getType() == Material.MOB_SPAWNER) {
                    @SuppressWarnings("deprecation")
                    Wool wool = new Wool(e.getBlock().getType(), e.getBlock().getData());
                    currLoc = plugin.getMapManager().getWoolSpawnerLocation(e.getBlock().getWorld(), wool.getColor());
                    if (currLoc != null) {
                        plugin.getLangManager().sendMessage("spawner-deleted", e.getPlayer());
                        return;
                    }
                }
                plugin.getLangManager().sendMessage("not-a-wool", e.getPlayer());
                e.setCancelled(true);
                return;
            }
            @SuppressWarnings("deprecation")
            Wool wool = new Wool(e.getBlock().getType(), e.getBlock().getData());
            currLoc = plugin.getMapManager().getBlueWoolWinLocation(e.getBlock().getWorld(), wool.getColor());
            if (currLoc != null) {
                plugin.getLangManager().sendMessage("cappoint-deleted", e.getPlayer());
                plugin.getMapManager().delBlueWoolWinPoint(e.getBlock());
                return;
            }

            currLoc = plugin.getMapManager().getRedWoolWinLocation(e.getBlock().getWorld(), wool.getColor());
            if (currLoc != null) {
                plugin.getLangManager().sendMessage("cappoint-deleted", e.getPlayer());
                plugin.getMapManager().delRedWoolWinPoint(e.getBlock());
                return;
            }

        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockPlaceEvent(BlockPlaceEvent e) {
            if (!playerSetup.containsKey(e.getPlayer())) {
                return;
            }
            if (e.getBlock().getType() != Material.WOOL) {
                plugin.getLangManager().sendMessage("not-a-wool", e.getPlayer());
                e.setCancelled(true);
                return;
            }
            @SuppressWarnings("deprecation")
            Wool wool = new Wool(e.getBlock().getType(), e.getBlock().getData());
            Location currLoc;
            if (action == SetUpAction.BLUE_WIN_WOOL || action == SetUpAction.RED_WIN_WOOL) {
                currLoc = plugin.getMapManager().getBlueWoolWinLocation(e.getBlock().getWorld(), wool.getColor());
                if (currLoc != null) {
                    e.getPlayer().sendMessage(plugin.getLangManager().getText("woolwin-already-blueteam")
                            .replace("%X%", currLoc.getBlockX() + "").replace("%Y%", currLoc.getBlockY() + "")
                            .replace("%Z%", currLoc.getBlockZ() + ""));
                    e.setCancelled(true);
                    return;
                }
                currLoc = plugin.getMapManager().getRedWoolWinLocation(e.getBlock().getWorld(), wool.getColor());
                if (currLoc != null) {
                    e.getPlayer().sendMessage(plugin.getLangManager().getText("woolwin-already-redteam")
                            .replace("%X%", currLoc.getBlockX() + "").replace("%Y%", currLoc.getBlockY() + "")
                            .replace("%Z%", currLoc.getBlockZ() + ""));
                    e.setCancelled(true);
                    return;
                }
            } else if(action == SetUpAction.WOOL_SPAWNER){
                currLoc = plugin.getMapManager().getWoolSpawnerLocation(e.getBlock().getWorld(), wool.getColor());
                if (currLoc != null) {
                    e.getPlayer().sendMessage(plugin.getLangManager().getText("spawner-already-exists")
                            .replace("%X%", currLoc.getBlockX() + "").replace("%Y%", currLoc.getBlockY() + "")
                            .replace("%Z%", currLoc.getBlockZ() + ""));
                    e.setCancelled(true);
                    return;
                }
            }

            switch (action) {
                case BLUE_WIN_WOOL:
                    if (plugin.getMapManager().addBlueWoolWinPoint(e.getBlock())) {
                        plugin.getLangManager().sendMessage("blue-wool-winpoint-placed", e.getPlayer());
                    }
                    break;
                case RED_WIN_WOOL:
                    if (plugin.getMapManager().addRedWoolWinPoint(e.getBlock())) {
                        plugin.getLangManager().sendMessage("red-wool-winpoint-placed", e.getPlayer());
                    }
                    break;
                case WOOL_SPAWNER:
                    if (plugin.getMapManager().addwoolSpawner(e.getBlock())) {
                        e.getPlayer().sendMessage(plugin.getLangManager().getText("spawner-placed")
                                .replace("%WOOL%", wool.getColor().toString()));
                    }
                    break;
            }
        }

    }

    private class GameListeners implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void PlayerBucketEmptyEvent(PlayerBucketEmptyEvent e) {
            plugin.getGameManager().events.cancelUseBukketOnProtectedAreas(e);

        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onWeatherChange(WeatherChangeEvent e) {
            plugin.getGameManager().ajustWeather(e);
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerMove(PlayerMoveEvent e) {
            plugin.getGameManager().denyEnterToProhibitedZone(e);
            if (!e.isCancelled()) {
                if (plugin.getMapManager().isMap(e.getPlayer().getWorld())) {
                    plugin.getMapManager().announceAreaBoundering(e);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onVoid(PlayerMoveEvent e) {
            Player p = e.getPlayer();
            if (plugin.getConfigManager().isVoidInstaKill()) {
                if (p.getHealth() <= 0) {
                    return;
                }

                if (p.getLocation().getBlockY() <= 0) {
                    EntityDamageEvent damageEvent = new EntityDamageEvent(
                            p,
                            EntityDamageEvent.DamageCause.VOID,
                            p.getHealth() + 1.0
                    );
                    Bukkit.getPluginManager().callEvent(damageEvent);
                    if (!damageEvent.isCancelled()) {
                        p.setLastDamageCause(damageEvent);
                        p.setHealth(0.0);
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onRespawn(PlayerRespawnEvent e) {
            String roomName = plugin.getRoomManager().getRoom(e.getPlayer().getWorld());
            if (roomName != null) {
                TeamManager.TeamId ti = plugin.getPlayerManager().getTeamId(e.getPlayer());
                if (ti == null) {
                    return;
                }
                switch (ti) {
                    case RED:
                        e.setRespawnLocation(plugin.getGameManager().getRedSpawn(roomName));
                        plugin.getPlayerManager().disguise(e.getPlayer(), TeamManager.TeamId.RED);
                        break;
                    case BLUE:
                        e.setRespawnLocation(plugin.getGameManager().getBlueSpawn(roomName));
                        plugin.getPlayerManager().disguise(e.getPlayer(), TeamManager.TeamId.BLUE);
                        break;
                    default:
                        return;
                }
                String mapName = plugin.getRoomManager().getCurrentMap(roomName);
                if (plugin.getMapManager().getKitarmour(mapName)) {
                    ItemStack air = new ItemStack(Material.AIR);
                    e.getPlayer().getInventory().setBoots(air);
                    e.getPlayer().getInventory().setChestplate(air);
                    e.getPlayer().getInventory().setHelmet(air);
                    e.getPlayer().getInventory().setLeggings(air);
                }

                ItemStack[] kit = plugin.getKitManager().getKit(e.getPlayer());

                if (kit.length > 0) {
                    e.getPlayer().getInventory().setContents(kit);
                } else {
                    ItemStack[] globalKit = plugin.getKitManager().getGlobalKitYAML();
                    if (globalKit.length > 0) {
                        e.getPlayer().getInventory().setContents(globalKit);
                    } else {
                        plugin.getLangManager().sendMessage("global-kit-error", e.getPlayer());
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onDeath(PlayerDeathEvent e) {
            plugin.getTeamManager().manageDeath(e);
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onSignChange(SignChangeEvent e) {
            plugin.getSignManager().checkForGameInPost(e);
        }

        @EventHandler(ignoreCancelled = true)
        public void onItemSpawnEvent(ItemSpawnEvent e) {
            plugin.getTeamManager().onArmourDrop(e);
            if (!e.isCancelled()) {
                if (plugin.getRoomManager().isInGame(e.getEntity().getWorld())) {
                    if (plugin.getRoomManager().isProhibited(e.getEntity())) {
                        e.setCancelled(true);
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
            if (plugin.getPlayerManager().getTeamId(e.getPlayer()) != null) {
                if (!plugin.hasPermission(e.getPlayer(), "ingame-extra-cmds")) {
                    String cmd = e.getMessage().split(" ")[0].replaceFirst("/", "");
                    if (!plugin.getCommandManager().isAllowedInGameCmd(cmd)) {
                        e.setCancelled(true);
                        String errorMessage = ChatColor.RED + "/" + cmd + " " + plugin.getLangManager().getText("disabled") + ".";
                        plugin.getLangManager().sendMessage(errorMessage, e.getPlayer());
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerChat(AsyncPlayerChatEvent e) {
            plugin.getTeamManager().playerChat(e);
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
        public void onPlayerInteract(PlayerInteractEvent e) {

            plugin.getTeamManager().cancelSpectator(e);

            if (e.isCancelled()) {
                return;
            }

            if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                    && e.getClickedBlock().getType() == Material.WORKBENCH
                    && !e.getPlayer().isSneaking()) {
                if (plugin.getPlayerManager().getTeamId(e.getPlayer()) != null) {
                    e.setCancelled(true);
                    e.getPlayer().openWorkbench(null, true);
                }

            }

            if (e.isCancelled()) {
                return;
            }

            if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                    || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                if (e.getClickedBlock().getType() == Material.WALL_SIGN
                        || e.getClickedBlock().getType() == Material.SIGN_POST) {
                    plugin.getSignManager().checkForPlayerJoin(e);

                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerDrop(PlayerDropItemEvent e
        ) {
            plugin.getTeamManager().cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
        public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
            if (e.isCancelled() && e.getEntity() instanceof Player) {
                TeamManager.TeamId teamId = plugin.getPlayerManager().getTeamId((Player) e.getEntity());
                if (teamId != null && teamId != TeamManager.TeamId.SPECTATOR) {
                    e.setCancelled(false);
                }
            }
            if (!e.isCancelled()) {
                plugin.getTeamManager().cancelSpectatorOrSameTeam(e);
            }
        }

        @EventHandler
        public void onHit(EntityDamageByEntityEvent event) {
            if (!(event.getDamager() instanceof Player)) return;
            Player damager = (Player) event.getDamager();
            if(event.getEntity() instanceof Player) {
                if(plugin.getPlayerManager().getTeamId(damager) != null) {
                    if(plugin.getPlayerManager().getTeamId(damager) != TeamManager.TeamId.SPECTATOR) {
                        if (plugin.getPlayerManager().canSeeBloodEffect(damager)) {
                            Location loc = event.getEntity().getLocation().add(0, 1, 0);
                            damager.playEffect(loc, Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
                        }
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
        public void onPlayerFish(PlayerFishEvent e) {
            plugin.getTeamManager().cancelSameTeam(e);
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
        public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
            plugin.getTeamManager().cancelSpectator(e);
        }


        @EventHandler
        public void onLobbyItemUse(PlayerInteractEvent e) {
            Player p = e.getPlayer();
            Action a = e.getAction();
            if (!(a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK)) return;

            ItemStack item = e.getItem();
            if (item == null || item.getType() == Material.AIR) return;

            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            for (LobbyItem lobbyItem : plugin.getLobbyManager().getAllItems()) {
                if (meta.getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', lobbyItem.getName()))) {
                    String command = lobbyItem.getCommand();
                    if (command != null && !command.isEmpty()) {
                        p.performCommand(command);
                    }
                    e.setCancelled(true);
                    break;
                }
            }

        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
        public void onInventoryOpenEvent(InventoryOpenEvent e) {
            plugin.getGameManager().cancelProtectedChest(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onInventoryClick(InventoryClickEvent e) {
            plugin.getTeamManager().cancelSpectator(e);
            if (!e.isCancelled()) {
                plugin.getGameManager().checkTarget(e);
            }
        }

        @EventHandler
        public void onInvClick(InventoryClickEvent event) {
            if (event.getCurrentItem() == null) return;

            ItemStack clicked = event.getCurrentItem();
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            for (LobbyItem item : plugin.getLobbyManager().getAllItems()) {
                if (meta.getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', item.getName()))) {
                    event.setCancelled(true);
                    break;
                }
            }
        }

        @EventHandler
        public void onDropItem(PlayerDropItemEvent event) {
            ItemStack dropped = event.getItemDrop().getItemStack();
            ItemMeta meta = dropped.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            for (LobbyItem item : plugin.getLobbyManager().getAllItems()) {
                if (meta.getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', item.getName()))) {
                    event.setCancelled(true);
                    break;
                }
            }
        }

        @EventHandler
        public void cancelPluginGUI(InventoryClickEvent e) {
            if (!plugin.getLobbyManager().isPluginGUI(e.getInventory())) return;
            e.setCancelled(true);
        }

        @EventHandler
        public void alertOnJoin(PlayerJoinEvent e) {
            if(e.getPlayer().hasPermission("ctw.admin") || e.getPlayer().isOp()) {
                if(plugin.getEconomy() == null) {
                    if(plugin.getConfigManager().isKitMenuEnabled()) {
                        plugin.getLangManager().sendMessage("no-vault-alert", e.getPlayer());
                    }
                }
            }
        }

        @EventHandler
        public void onRoomsClick(InventoryClickEvent event) {
            Player player = (Player) event.getWhoClicked();
            if(plugin.getLobbyManager().isPluginGUI(event.getInventory())) {
                if (event.getClickedInventory() == null || event.getClickedInventory().getType() != InventoryType.CHEST)
                    return;

                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || !clicked.hasItemMeta()) return;

                ItemMeta im = clicked.getItemMeta();

                if (plugin.getLobbyManager().isRoomItem(clicked)) {

                    File file = new File(plugin.getDataFolder(), "rooms.yml");
                    FileConfiguration rooms = YamlConfiguration.loadConfiguration(file);
                    Set<String> roomKeys = rooms.getKeys(false);

                    String displayName = ChatColor.stripColor(im.getDisplayName());
                    String roomName = null;

                    for (String key : roomKeys) {
                        if (displayName.contains(key)) {
                            roomName = key;
                            break;
                        }
                    }

                    if (roomName == null) return;
                    player.closeInventory();

                    boolean isEnabled = plugin.getLobbyManager().isRoomEnabled(clicked);

                    if (isEnabled) {
                        plugin.getGameManager().movePlayerToRoom(player, roomName);
                    } else {
                        plugin.getLangManager().sendMessage("room-is-disabled", player);
                    }
                }
            }
        }

        @EventHandler
        public void onInventoryDrag(InventoryDragEvent event) {
            if (event.getView().getTitle().equals(plugin.getLangManager().getText("rooms-gui"))) {
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerTeleport(PlayerTeleportEvent e) {
            if (!e.getFrom().getWorld().getName().equals(e.getTo().getWorld().getName())) {
                if (plugin.getRoomManager().isInGame(e.getTo().getWorld())) {
                    Player player = e.getPlayer();
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getGameManager().movePlayerTo(player, TeamManager.TeamId.SPECTATOR);
                        }
                    }, 5);
                } else {
                    if (plugin.getRoomManager().isInGame(e.getFrom().getWorld())) {
                        plugin.getGameManager().playerLeftGame(e.getPlayer());
                    }
                }
            }
        }

        @EventHandler
        public void onPlayerLobby(PlayerTeleportEvent e) throws IOException {
            if(e.getTo().getWorld().equals(plugin.getWorldManager().getLobbyWorld())) {
                Player plr = e.getPlayer();
                PlayerInventory inv = plr.getInventory();
                inv.clear();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Scoreboard sb = plr.getScoreboard();
                    for (Team team : sb.getTeams()) {
                        team.removeEntry(plr.getName());
                    }
                    plugin.getLobbyManager().assignLobbyBoard(plr);

                    for (LobbyItem lobbyItem : plugin.getLobbyManager().getAllItems()) {
                        inv.setItem(lobbyItem.getSlot(), lobbyItem.toItemStack());
                    }
                }, 2L);
            }
        }

        @EventHandler
        public void onTeleport(PlayerTeleportEvent e) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getPlayerManager().updateTablistFor(e.getPlayer());
            }, 2L);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerQuit(PlayerQuitEvent e) {
            Player player = e.getPlayer();
            e.setQuitMessage(null);

            if (plugin.getWorldManager().getLobbyWorld() != null) {
                Location lobbySpawn = plugin.getWorldManager().getNextLobbySpawn();
                if (lobbySpawn != null) {
                    player.teleport(lobbySpawn);
                } else {
                    plugin.getLogger().warning("Lobby spawn not configured for player quit: " + player.getName());
                }
            } else {
                plugin.getLogger().warning("Lobby world not configured for player quit: " + player.getName());
            }
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());

            if (plugin.getWorldManager().getLobbyWorld() != null) {
                String leftMessage = plugin.getLangManager().getText("left-message")
                        .replace("%PLAYER%", player.getDisplayName());
                for (Player lobbyPlayer : plugin.getWorldManager().getLobbyWorld().getPlayers()) {
                    plugin.getLangManager().sendMessage(leftMessage, lobbyPlayer);
                    plugin.getLobbyManager().refreshLobbyBoard();
                }
            }

            if (plugin.getRoomManager().isInGame(player.getWorld())) {
                plugin.getGameManager().playerLeftGame(player);
            }
        }

        @EventHandler
        public void onJoinTablist(PlayerJoinEvent e) {
            plugin.getPlayerManager().updateTablistFor(e.getPlayer());
        }

        @EventHandler
        public void onWorldChange(PlayerChangedWorldEvent e) {
            plugin.getPlayerManager().updateTablistFor(e.getPlayer());
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onJoinEvent(PlayerJoinEvent e) {
            World lobbyWorld = plugin.getWorldManager().getLobbyWorld();
            if (lobbyWorld == null) {
                if (plugin.hasPermission(e.getPlayer(), "setup")) {
                    Player player = e.getPlayer();
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getLangManager().sendText("unconfigured-lobby", player);
                            try {
                                for (LobbyItem lobbyItem : plugin.getLobbyManager().getAllItems()) {
                                    player.getInventory().setItem(lobbyItem.getSlot(), lobbyItem.toItemStack());
                                }
                            } catch (NullPointerException ex) {
                                player.sendMessage("Error at load lobby items.");
                            }
                        }
                    }, 30);
                }
            } else {
                e.setJoinMessage(null);

                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        e.getPlayer().teleport(plugin.getWorldManager().getNextLobbySpawn());

                    }
                }, 10);

                String joinMessage = plugin.getLangManager().getText("join-message")
                        .replace("%PLAYER%", e.getPlayer().getDisplayName());
                for (Player player : plugin.getWorldManager().getLobbyWorld().getPlayers()) {
                    plugin.getLangManager().sendMessage(joinMessage, player);
                    plugin.getLobbyManager().refreshLobbyBoard();
                }
                if (plugin.getPlayerManager().getTeamId(e.getPlayer()) != null) {
                    plugin.getPlayerManager().clearTeam(e.getPlayer());
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockPlaceEvent(BlockPlaceEvent e) {
            plugin.getTeamManager().cancelSpectator(e);
            if (!e.isCancelled()) {
                plugin.getGameManager().events.cancelEditProtectedAreas(e);
            }
            if (e.isCancelled()) {
                plugin.getGameManager().checkTarget(e);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onCrafting(CraftItemEvent e) {
            plugin.getGameManager().events.cancelCrafting(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockBreakEvent(BlockBreakEvent e) {
            plugin.getTeamManager().cancelSpectator(e);
            plugin.getGameManager().events.cancelEditProtectedAreas(e);
            if (!e.isCancelled()) {
                if (plugin.getPlayerManager().getTeamId(e.getPlayer()) != null) {
                    Location blockLoc = e.getBlock().getLocation();
                    for (Entity entity : e.getPlayer().getWorld().getNearbyEntities(blockLoc,1, 2, 1)) {
                        if(entity instanceof Player) {
                            Player other = (Player) entity;
                            if (other.getName().equals(e.getPlayer().getName())) {
                                continue;
                            }
                            if (other.getLocation().getBlockX() == e.getBlock().getLocation().getBlockX()
                                    && other.getLocation().getBlockY() >= e.getBlock().getLocation().getBlockY()
                                    && other.getLocation().getBlockY() < e.getBlock().getLocation().getBlockY() + 2
                                    && other.getLocation().getBlockZ() == e.getBlock().getLocation().getBlockZ()
                                    && e.getBlock().getType().isSolid()){

                                String spleafText = plugin.getLangManager().getText("block-spleaf");

                                plugin.getLangManager().sendVerbatimTextToWorld(
                                        spleafText.replace("%DAMAGER%",
                                                        plugin.getPlayerManager().getChatColor(e.getPlayer()) + e.getPlayer().getName())
                                                .replace("%VICTIM%",
                                                        plugin.getPlayerManager().getChatColor(other) + other.getName()), other.getWorld(), null);
                                break;
                            }
                        }
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerPickupItem(PlayerPickupItemEvent e) {
            plugin.getTeamManager().cancelSpectator(e);
            if (!e.isCancelled()) {
                plugin.getGameManager().checkTarget(e);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onEntityTarget(EntityTargetEvent e
        ) {
            plugin.getTeamManager().cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockDamage(BlockDamageEvent e
        ) {
            plugin.getTeamManager().cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onEntityDamage(EntityDamageEvent e) {
            plugin.getTeamManager().cancelSpectator(e);
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onFallDamage(EntityDamageEvent e) {
            if(e.getCause() == DamageCause.FALL) {
                if(e.getEntity() instanceof Player) {
                    Player plr = (Player) e.getEntity();
                    if(plugin.getPlayerManager().getTeamId(plr) != null) {
                        e.setCancelled(plugin.getConfigManager().isFallDamage());
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onFoodLevelChange(FoodLevelChangeEvent e) {
            plugin.getTeamManager().cancelSpectator(e);
            if (e.getEntity() instanceof Player && !e.isCancelled()) {
                Player player = (Player) e.getEntity();
                TeamManager.TeamId ti = plugin.getPlayerManager().getTeamId(player);
                if (ti != null && player.getFoodLevel() > e.getFoodLevel()) {
                    if ((Math.random() * ((10) + 1)) > 4) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    public EventManager(CTW plugin) {
        this.plugin = plugin;
        gameEvents = new GameListeners();
        playerSetup = new TreeMap<>(new Utils.PlayerComparator());
        registerGameEvents();
    }

    public void registerGameEvents() {
        plugin.getServer().getPluginManager().registerEvents(gameEvents, plugin);
    }

    public void registerSetupEvents(Player player, SetUpAction action) {
        unregisterSetUpEvents(player);
        SetupListeners sl = new SetupListeners(action);
        plugin.getServer().getPluginManager().registerEvents(sl, plugin);
        playerSetup.put(player, sl);
    }

    public void unregisterGameEvents() {
        HandlerList.unregisterAll(gameEvents);
    }

    public void unregisterSetUpEvents(Player player) {
        SetupListeners sl = playerSetup.remove(player);
        if (sl != null) {
            HandlerList.unregisterAll(sl);
        }
    }
}
