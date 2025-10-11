package com.mauccio.ctw.files;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.google.gson.Gson;
import com.mauccio.ctw.CTW;

public class KitManager {

    private final CTW plugin;
    private final Gson gson = new Gson();
    private final boolean useSQL;
    private final Map<UUID, ItemStack[]> inventoryBackup = new HashMap<>();

    public KitManager(CTW plugin) {
        this.plugin = plugin;
        this.useSQL = plugin.getConfig().getBoolean("use-sql-for-kits");
    }
    
    public void saveKitYAML(UUID playerUUID, ItemStack[] kit) throws IOException {
        YamlConfiguration c = new YamlConfiguration();
        c.set("custom-globalkit", kit);
        c.save(new File(plugin.getDataFolder() + File.separator + "Kits", playerUUID+".yml"));
    }

    public ItemStack[] getKitYAML(UUID playerUUID) throws NullPointerException {
        YamlConfiguration c = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + File.separator + "Kits", playerUUID +".yml"));
        ItemStack[] content = (ItemStack[])((List)c.get("custom-globalkit")).toArray((Object[])new ItemStack[0]);
        return content;
    }
    
    public void saveGlobalKitYAML(ItemStack[] kit) throws IOException {
        YamlConfiguration c = new YamlConfiguration();
        c.set("global-kit", kit);
        c.save(new File(plugin.getDataFolder() + File.separator + "globalkit.yml"));
    }
    
    public ItemStack[] getGlobalKitYAML() {
        YamlConfiguration c = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + File.separator + "globalkit.yml"));
        ItemStack[] items = (ItemStack[])((List)c.get("global-kit")).toArray((Object[])new ItemStack[0]);
        return items;
    }

    public void invSaver(Player player, UUID uuid) {
        inventoryBackup.put(uuid, player.getInventory().getContents());
    }

    public void invRecover(Player player, UUID uuid) {
        if (inventoryBackup.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(inventoryBackup.get(player.getUniqueId()));
            inventoryBackup.remove(player.getUniqueId());
        }
    }
}
