package com.mauccio.ctw.utils;

import com.mauccio.ctw.CTW;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LobbyManager implements Listener {

    private final CTW plugin;
    private final File lobbyFile;
    private final YamlConfiguration lobbyYml;
    private final Map<String, LobbyItem> items = new HashMap<>();

    public LobbyManager(CTW plugin) {
        this.plugin = plugin;
        this.lobbyFile = new File(pl.getDataFolder()+File.separator+"lobby.yml");
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

    public Inventory getRoomsGUI() {
        Inventory inv = Bukkit.createInventory(null, 9, plugin.lm.getText("rooms-gui"));

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
                    ? ChatColor.DARK_GREEN + key + " Room enabled"
                    : ChatColor.RED + key + " Room disabled"));
            wool.setItemMeta(meta);

            inv.setItem(slot, wool);
            slot++;
            if (slot >= inv.getSize()) break;
        }
        return inv;
    }
}
