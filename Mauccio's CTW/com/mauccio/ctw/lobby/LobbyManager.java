package com.mauccio.ctw.lobby;

import com.mauccio.ctw.CTW;
import com.mauccio.ctw.utils.LobbyItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.util.*;

public class LobbyManager implements Listener {

    private final CTW plugin;
    private final File lobbyFile;
    private final YamlConfiguration lobbyYml;
    private final Map<String, LobbyItem> items = new HashMap<>();
    private Scoreboard lobbyBoard;
    private List<String> lobbyTemplateLines = Collections.emptyList();
    private Team lobbyNeutral;


    public LobbyManager(CTW plugin) {
        this.plugin = plugin;
        this.lobbyFile = new File(plugin.getDataFolder()+File.separator+"lobby.yml");
        this.lobbyYml = YamlConfiguration.loadConfiguration(lobbyFile);
        registerEvents();
        loadItems();
    }

    public void loadItems() {
        items.clear();

        File file = new File(plugin.getDataFolder(), "lobby.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!config.contains("items")) return;

        for (String id : config.getConfigurationSection("items").getKeys(false)) {
            String path = "items." + id + ".";

            String name = config.getString(path + "name", "&fItem");
            List<String> desc = config.getStringList(path + "description");
            Material type = Material.matchMaterial(config.getString(path + "type", "STONE"));
            int data = config.getInt(path + "data", 0);
            int amount = config.getInt(path + "amount", 1);
            int slot = config.getInt(path + "slot", 0);
            boolean glide = config.getBoolean(path + "glide", false);
            String command = config.getString(path + "command", "");

            LobbyItem item = new LobbyItem(id, name, desc, type, data, amount, slot, glide, command);
            items.put(id, item);
        }
    }

    public void ensureLobbyNeutralTeam() {
        if (lobbyBoard == null) return;
        lobbyNeutral = lobbyBoard.getTeam("LOBBY_NEUTRAL");
        if (lobbyNeutral == null) {
            lobbyNeutral = lobbyBoard.registerNewTeam("LOBBY_NEUTRAL");
            lobbyNeutral.setPrefix(ChatColor.WHITE.toString());
            lobbyNeutral.setCanSeeFriendlyInvisibles(false);
            lobbyNeutral.setAllowFriendlyFire(false);
        }
    }


    public LobbyItem getItem(String id) {
        return items.get(id);
    }

    public Collection<LobbyItem> getAllItems() {
        return items.values();
    }

    private void registerEvents() {
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(this, plugin);
    }

    private String renderLine(String raw) {
        return raw
                .replace("%ROOMS%", String.valueOf(plugin.getRoomManager().getRooms().size()))
                .replace("%ACTIVE_ROOMS%", String.valueOf(plugin.getRoomManager().getEnabledRooms().size()))
                .replace("%PLAYERS%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%MAX_PLAYERS%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%SERVER_IP%", plugin.getLangManager().getText("server-ip"));
    }

    private List<String> uniquifyLines(List<String> rendered) {
        List<String> out = new ArrayList<>(rendered.size());
        Set<String> seen = new HashSet<>();
        for (String line : rendered) {
            String l = line.isEmpty() ? " " : line;
            while (seen.contains(l)) {
                l += ChatColor.RESET;
            }
            seen.add(l);
            out.add(l);
        }
        return out;
    }

    public void buildLobbyBoard() {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        lobbyBoard = sm.getNewScoreboard();

        Objective obj = lobbyBoard.registerNewObjective("lobby", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(plugin.getLangManager().getText("lobby-scoreboard.title"));

        lobbyTemplateLines = plugin.getLangManager().getStringList("lobby-scoreboard.lines");

        List<String> rendered = new ArrayList<>(lobbyTemplateLines.size());
        for (String raw : lobbyTemplateLines) rendered.add(renderLine(raw));
        List<String> unique = uniquifyLines(rendered);

        int score = unique.size();
        for (String line : unique) {
            obj.getScore(line).setScore(score--);
        }
    }

    public void refreshLobbyBoard() {
        if (lobbyBoard == null) return;

        Objective obj = lobbyBoard.getObjective("lobby");
        if (obj != null) {
            obj.unregister();
        }

        obj = lobbyBoard.registerNewObjective("lobby", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(plugin.getLangManager().getText("lobby-scoreboard.title"));

        List<String> rendered = new ArrayList<>(lobbyTemplateLines.size());
        for (String raw : lobbyTemplateLines) {
            String line = raw
                    .replace("%ROOMS%", String.valueOf(plugin.getRoomManager().getRooms().size()))
                    .replace("%ACTIVE_ROOMS%", String.valueOf(plugin.getRoomManager().getEnabledRooms().size()))
                    .replace("%PLAYERS%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                    .replace("%MAX_PLAYERS%", String.valueOf(Bukkit.getMaxPlayers()))
                    .replace("%SERVER_IP%", plugin.getLangManager().getText("server-ip"));

            rendered.add(line.isEmpty() ? " " : line);
        }

        List<String> unique = new ArrayList<>(rendered.size());
        Set<String> seen = new HashSet<>();
        for (String line : rendered) {
            String l = line;
            while (seen.contains(l)) {
                l += ChatColor.RESET;
            }
            seen.add(l);
            unique.add(l);
        }

        int score = unique.size();
        for (String line : unique) {
            obj.getScore(line).setScore(score--);
        }
    }

    public Scoreboard getLobbyBoard() {
        return lobbyBoard;
    }

    public void assignLobbyBoard(Player player) {
        if (lobbyBoard == null) buildLobbyBoard();
        player.setScoreboard(lobbyBoard);
    }

    public boolean isRoomEnabled(ItemStack is) {
        if (is == null || !is.hasItemMeta()) return false;
        ItemMeta im = is.getItemMeta();
        if (!im.hasDisplayName()) return false;

        String title = ChatColor.stripColor(im.getDisplayName());

        String enabledPattern = ChatColor.stripColor(plugin.getLangManager().getText("room-gui-enabled"));
        String disabledPattern = ChatColor.stripColor(plugin.getLangManager().getText("room-gui-disabled"));

        String enabledRegex = "^" + enabledPattern.replace("%ROOM%", "(.+)") + "$";
        String disabledRegex = "^" + disabledPattern.replace("%ROOM%", "(.+)") + "$";

        if (title.matches(enabledRegex)) return true;
        if (title.matches(disabledRegex)) return false;

        return false;
    }

    public boolean isPluginGUI(Inventory inv) {
        if (inv == null) return false;
        String title = ChatColor.stripColor(inv.getTitle());
        return title.equals(ChatColor.stripColor(plugin.getLangManager().getText("rooms-gui"))) ||
                title.equals(ChatColor.stripColor(plugin.getLangManager().getText("menus.kits.title")));
    }

    public boolean isRoomItem(ItemStack is) {
        if (is == null || !is.hasItemMeta()) return false;
        String title = ChatColor.stripColor(is.getItemMeta().getDisplayName());

        String enabledPrefix = ChatColor.stripColor(plugin.getLangManager().getText("room-gui-enabled")).split("%ROOM%")[0];
        String disabledPrefix = ChatColor.stripColor(plugin.getLangManager().getText("room-gui-disabled")).split("%ROOM%")[0];

        return title.startsWith(enabledPrefix) || title.startsWith(disabledPrefix);
    }


    public Inventory getRoomsGUI() {
        Inventory inv = Bukkit.createInventory(null, 9, plugin.getLangManager().getText("rooms-gui"));

        File file = new File(plugin.getDataFolder(), "rooms.yml");
        FileConfiguration rooms = YamlConfiguration.loadConfiguration(file);
        Set<String> roomKeys = rooms.getKeys(false);

        int slot = 0;
        for (String key : roomKeys) {
            boolean enabled = rooms.getBoolean(key + ".enabled");
            short colorData = (short) (enabled ? 13 : 14);

            ItemStack wool = new ItemStack(Material.WOOL, 1, colorData);
            ItemMeta meta = wool.getItemMeta();
            meta.setDisplayName((enabled
                    ? plugin.getLangManager().getText("room-gui-enabled").replace("%ROOM%", key)
                    : plugin.getLangManager().getText("room-gui-disabled").replace("%ROOM%", key)));
            wool.setItemMeta(meta);

            inv.setItem(slot, wool);
            slot++;
            if (slot >= inv.getSize()) break;
        }
        return inv;
    }

    /*
        Lobby Guard
     */

    public boolean isOnLobby(Player player) {
        return plugin.getWorldManager().isOnLobby(player);
    }

    @EventHandler
    public void lobbyBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        if(player.hasPermission("ctw.admin") || player.isOp()) return;
        if(isOnLobby(player)) {
            if(plugin.getConfigManager().isLobbyGuardEnabled()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void lobbyBreakPlace(BlockBreakEvent e) {
        Player player = e.getPlayer();
        if(player.hasPermission("ctw.admin") || player.isOp()) return;
        if(isOnLobby(player)) {
            if(plugin.getConfigManager().isLobbyGuardEnabled()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onLobbyHit(EntityDamageByEntityEvent e) {
        if(plugin.getConfigManager().isLobbyGuardEnabled()) {
            if(e.getEntity() instanceof Player) {
                Player player = (Player) e.getEntity();
                if(player.hasPermission("ctw.admin") || player.isOp()) return;
                if(e.getDamager() instanceof Player) {
                    Player damager = (Player) e.getDamager();
                    if(damager.hasPermission("ctw.admin") || damager.isOp()) return;
                    if(isOnLobby(player)) {
                        if(isOnLobby(damager)) {
                            e.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void lobbyHungry(FoodLevelChangeEvent e) {
        Player player = (Player) e.getEntity();
        if(player.hasPermission("ctw.admin") || player.isOp()) return;
        if(isOnLobby(player)) {
            if(plugin.getConfigManager().isLobbyGuardEnabled()) {
                e.setFoodLevel(20);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void lobbyDamage(EntityDamageEvent e) {
        Player player = (Player) e.getEntity();
        if(player.hasPermission("ctw.admin") || player.isOp()) return;
        if(isOnLobby(player)) {
            if(plugin.getConfigManager().isLobbyGuardEnabled()) {
                e.setCancelled(true);
            }
        }
    }
}
