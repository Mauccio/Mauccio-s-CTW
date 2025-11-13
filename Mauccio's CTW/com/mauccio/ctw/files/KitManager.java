package com.mauccio.ctw.files;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

import com.mauccio.ctw.utils.ItemStackIO;
import net.milkbowl.vault.economy.Economy;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.google.gson.Gson;
import com.mauccio.ctw.CTW;
import org.bukkit.inventory.meta.ItemMeta;

public class KitManager implements Listener {

    private final CTW plugin;
    private final Gson gson = new Gson();
    private final boolean useSQL;
    private final Map<UUID, ItemStack[]> inventoryBackup = new HashMap<>();
    private final Map<UUID, ItemStack[]> kitCache = new HashMap<>();
    private final Map<UUID, Map<String, Long>> kitCooldowns = new HashMap<>();

    public KitManager(CTW plugin) {
        this.plugin = plugin;
        this.useSQL = plugin.getConfigManager().isKitSQL();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void saveKit(Player player, ItemStack[] kit) {
        if (useSQL) {
            kitCache.put(player.getUniqueId(), kit);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveKitSQL(player, kit));
        } else {
            try {
                saveKitYAML(player, kit);
                plugin.getLangManager().sendMessage("saved-kit-success", player);
            } catch (IOException e) {
                plugin.getLangManager().sendMessage("error-at-save-player-kit", player);
                plugin.getSoundManager().playErrorSound(player);
            }
        }
    }

    public ItemStack[] getKit(Player player) {
        if (useSQL) {
            return kitCache.computeIfAbsent(player.getUniqueId(), id -> getKitSQL(player));
        } else {
            return getKitYAML(player);
        }
    }

    private void saveKitSQL(Player player, ItemStack[] kit) {
        String sql = "REPLACE INTO player_kits (player_uuid, kit_data) VALUES (?, ?)";
        try (Connection conn = plugin.getDBManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, ItemStackIO.toBase64(kit));
            ps.executeUpdate();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getLangManager().sendMessage("saved-kit-success", player));

        } catch (Exception e) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLangManager().sendMessage("error-at-save-player-kit", player);
                plugin.getSoundManager().playErrorSound(player);
            });
            plugin.getLogger().severe("Error saving kit to SQL: " + e.getMessage());
        }
    }

    private ItemStack[] getKitSQL(Player player) {
        String sql = "SELECT kit_data FROM player_kits WHERE player_uuid = ?";
        try (Connection conn = plugin.getDBManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ItemStackIO.fromBase64(rs.getString("kit_data"));
                }
            }
        } catch (Exception e) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLangManager().sendMessage("error-at-save-player-kit", player);
                plugin.getSoundManager().playErrorSound(player);
            });
            plugin.getLogger().severe("Error loading kit from SQL: " + e.getMessage());
        }
        return new ItemStack[0];
    }

    public void saveKitYAML(Player player, ItemStack[] kit) throws IOException {
        YamlConfiguration c = new YamlConfiguration();
        List<ItemStack> safeList = new ArrayList<>();
        for (ItemStack item : kit) {
            safeList.add(item == null ? new ItemStack(org.bukkit.Material.AIR) : item);
        }
        c.set("custom-globalkit", safeList);
        c.save(new File(plugin.getDataFolder() + File.separator + "player_kits", player.getUniqueId() + ".yml"));
    }

    public ItemStack[] getKitYAML(Player player) {
        File kitFile = new File(plugin.getDataFolder() + File.separator + "player_kits", player.getUniqueId() + ".yml");
        if (!kitFile.exists()) {
            return new ItemStack[0];
        }

        YamlConfiguration c = YamlConfiguration.loadConfiguration(kitFile);
        List<?> rawList = c.getList("custom-globalkit");
        if (rawList == null || rawList.isEmpty()) {
            return new ItemStack[0];
        }

        List<ItemStack> items = new ArrayList<>();
        for (Object o : rawList) {
            if (o instanceof ItemStack) {
                items.add((ItemStack) o);
            } else if (o == null) {
                items.add(new ItemStack(org.bukkit.Material.AIR));
            } else {
                plugin.getLogger().warning("Invalid element on player kit:" + player.getName() + ": " + o);
                items.add(new ItemStack(org.bukkit.Material.AIR));
            }
        }
        return items.toArray(new ItemStack[0]);
    }

    public void saveGlobalKitYAML(ItemStack[] kit) throws IOException {
        YamlConfiguration c = new YamlConfiguration();
        List<ItemStack> safeList = new ArrayList<>();
        for (ItemStack item : kit) {
            safeList.add(item == null ? new ItemStack(org.bukkit.Material.AIR) : item);
        }
        c.set("global-kit", safeList);
        c.save(new File(plugin.getDataFolder(), "globalkit.yml"));
    }

    public ItemStack[] getGlobalKitYAML() {
        File kitFile = new File(plugin.getDataFolder(), "globalkit.yml");
        if (!kitFile.exists()) {
            plugin.getLogger().warning("globalkit.yml is null.");
            return new ItemStack[0];
        }

        YamlConfiguration c = YamlConfiguration.loadConfiguration(kitFile);

        List<?> rawList = c.getList("global-kit");
        if (rawList == null || rawList.isEmpty()) {
            plugin.getLogger().warning("globalkit.yml doesn't exists or it's empty.");
            return new ItemStack[0];
        }

        List<ItemStack> items = new ArrayList<>();
        for (Object o : rawList) {
            if (o instanceof ItemStack) {
                items.add((ItemStack) o);
            } else if (o == null) {
                items.add(new ItemStack(org.bukkit.Material.AIR));
            } else {
                plugin.getLogger().warning("Invalid item on globalkit.yml: " + o);
                items.add(new ItemStack(org.bukkit.Material.AIR));
            }
        }

        return items.toArray(new ItemStack[0]);
    }

    public void invSaver(Player player, UUID uuid) {
        inventoryBackup.put(uuid, player.getInventory().getContents());
    }

    public void invRecover(Player player, UUID uuid) {
        if (inventoryBackup.containsKey(uuid)) {
            player.getInventory().setContents(inventoryBackup.get(uuid));
            inventoryBackup.remove(uuid);
        }
    }

    /*
            Kits Editor
     */

    public Inventory getKitGUI() {
        Inventory inv = Bukkit.createInventory(null, 54, plugin.getLangManager().getText("menus.kits.title"));

        File file = new File(plugin.getDataFolder(), "kits.yml");
        FileConfiguration kits = YamlConfiguration.loadConfiguration(file);
        Set<String> kitsKeys = kits.getKeys(false);

        int slot = 0;
        for (String key : kitsKeys) {
            ConfigurationSection kitSection = kits.getConfigurationSection(key);
            if (kitSection == null) continue;

            String displayName = ChatColor.translateAlternateColorCodes('&', kitSection.getString("name", key));
            List<String> lore = kitSection.getStringList("lore");
            int price = kitSection.getInt("price", 0);

            String iconName = kitSection.getString("icon", "BARRIER");
            Material mat = Material.matchMaterial(iconName);
            if (mat == null) mat = Material.BARRIER;
            ItemStack item = new ItemStack((mat));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(displayName);

            List<String> finalLore = new ArrayList<>();
            for (String line : lore) {
                finalLore.add(ChatColor.translateAlternateColorCodes('&', line.replace("%PRICE%", String.valueOf(price))));
            }
            meta.setLore(finalLore);

            item.setItemMeta(meta);

            inv.setItem(slot, item);
            slot++;
        }
        return inv;
    }

    private Enchantment resolveEnchantLegacy(String name) {
        if (name == null) return null;
        switch (name.toUpperCase()) {
            case "SHARPNESS": return Enchantment.DAMAGE_ALL;
            case "SMITE": return Enchantment.DAMAGE_UNDEAD;
            case "BANE_OF_ARTHROPODS": return Enchantment.DAMAGE_ARTHROPODS;
            case "UNBREAKING": return Enchantment.DURABILITY;
            case "EFFICIENCY": return Enchantment.DIG_SPEED;
            case "FORTUNE": return Enchantment.LOOT_BONUS_BLOCKS;
            case "POWER": return Enchantment.ARROW_DAMAGE;
            case "PUNCH": return Enchantment.ARROW_KNOCKBACK;
            case "FLAME": return Enchantment.ARROW_FIRE;
            case "INFINITY": return Enchantment.ARROW_INFINITE;
            case "PROTECTION": return Enchantment.PROTECTION_ENVIRONMENTAL;
            case "FIRE_PROTECTION": return Enchantment.PROTECTION_FIRE;
            case "FEATHER_FALLING": return Enchantment.PROTECTION_FALL;
            case "BLAST_PROTECTION": return Enchantment.PROTECTION_EXPLOSIONS;
            case "PROJECTILE_PROTECTION": return Enchantment.PROTECTION_PROJECTILE;
            case "RESPIRATION": return Enchantment.OXYGEN;
            case "AQUA_AFFINITY": return Enchantment.WATER_WORKER;
            case "THORNS": return Enchantment.THORNS;
            case "DEPTH_STRIDER": return Enchantment.DEPTH_STRIDER;
            case "LOOTING": return Enchantment.LOOT_BONUS_MOBS;
            case "FIRE_ASPECT": return Enchantment.FIRE_ASPECT;
            case "KNOCKBACK": return Enchantment.KNOCKBACK;
            case "SILK_TOUCH": return Enchantment.SILK_TOUCH;
            default: return Enchantment.getByName(name.toUpperCase());
        }
    }

    public void giveKit(Player player, String kitKey) {
        File file = new File(plugin.getDataFolder(), "kits.yml");
        FileConfiguration kits = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection kitSection = kits.getConfigurationSection(kitKey);
        if (kitSection == null) return;

        int price = kitSection.getInt("price", 0);

        Economy econ = plugin.getEconomy();
        if(econ != null && price > 0) {
            if(econ.getBalance(player) < price) {
                String msg = plugin.getLangManager().getMessage("menus.kits.no-money")
                        .replace("%BALANCE%", String.valueOf(econ.getBalance(player)));
                player.sendMessage(msg);
                return;
            }
            econ.withdrawPlayer(player, price);
            String msg = plugin.getLangManager().getMessage("menus.kits.purchased")
                            .replace("%BALANCE%", String.valueOf(econ.getBalance(player)));
            player.sendMessage(msg);
        }

        ConfigurationSection receive = kitSection.getConfigurationSection("receive");
        if (receive == null) return;

        for (String itemKey : receive.getKeys(false)) {
            String[] parts = itemKey.split("_");
            String matName = String.join("_", Arrays.copyOf(parts, parts.length - 1));
            Material mat = Material.matchMaterial(matName);
            if (mat == null) {
                Bukkit.getLogger().warning("Invalid material on kits.yml: " + matName);
                continue;
            }

            int amount = receive.getInt(itemKey + ".amount", 1);
            ItemStack item = new ItemStack(mat, amount);

            Object enchObj = receive.get(itemKey + ".enchanted");
            if (enchObj instanceof String) {
                String[] enchParts = ((String) enchObj).split(":");
                if (enchParts.length == 2) {
                    Enchantment ench = resolveEnchantLegacy(enchParts[0]);
                    int level = Integer.parseInt(enchParts[1]);
                    if (ench != null) {
                        item.addUnsafeEnchantment(ench, level);
                    } else {
                        Bukkit.getLogger().warning("Invalid enchantment on kits.yml: " + enchParts[0]);
                    }
                }
            }
            player.getInventory().addItem(item);
        }
    }

    public void reloadKits() {
        File file = new File(plugin.getDataFolder(), "kits.yml");
        FileConfiguration kits = YamlConfiguration.loadConfiguration(file);
        kitCooldowns.clear();
        kitCache.clear();
    }

    public boolean isKitEditorGUI(Inventory inv) {
        if (inv == null) return false;
        String title = ChatColor.stripColor(inv.getTitle());
        return title.equals(ChatColor.stripColor(plugin.getLangManager().getText("menus.kits.kit-editor")));
    }

    public boolean isKitGUI(Inventory inv) {
        if (inv == null) return false;
        String title = ChatColor.stripColor(inv.getTitle());
        return title.equals(ChatColor.stripColor(plugin.getLangManager().getText("menus.kits.title")));
    }

    public boolean kitExists(String kitName) {
        File file = new File(plugin.getDataFolder(), "kits.yml");
        FileConfiguration kits = YamlConfiguration.loadConfiguration(file);
        return kits.contains(kitName);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (!isKitGUI(e.getInventory())) return;
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        File file = new File(plugin.getDataFolder(), "kits.yml");
        FileConfiguration kits = YamlConfiguration.loadConfiguration(file);

        String kitKey = null;
        for (String key : kits.getKeys(false)) {
            String kitName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', kits.getString(key + ".name", key)));
            if (kitName.equalsIgnoreCase(displayName)) {
                kitKey = key;
                break;
            }
        }
        if (kitKey == null) {
            plugin.getLangManager().sendMessage("kit-doesnot-exists", player);
            return;
        }

        boolean requiresPerm = kits.getBoolean(kitKey + ".permission", false);
        if (requiresPerm && !player.hasPermission("ctw.kits." + kitKey)) {
            plugin.getLangManager().sendMessage("kit-permission", player);
            return;
        }
        int cooldown = kits.getInt(kitKey + ".cooldown", 0);
        if (cooldown > 0) {
            long lastUse = getLastUse(player.getUniqueId(), kitKey);
            long elapsed = System.currentTimeMillis() - lastUse;
            long remaining = (cooldown * 1000L - elapsed) / 1000L;
            if (elapsed < cooldown * 1000L) {
                String msg = plugin.getLangManager().getMessage("kit-cooldown-message").replace("%COOLOWN%", String.valueOf(remaining));
                player.sendMessage(msg);
                return;
            }
            setLastUse(player.getUniqueId(), kitKey);
        }
        if(kits.getInt(kitKey + ".price") == 0) {
            plugin.getLangManager().sendMessage("kit-free", player);
        }
        giveKit(player, kitKey);
        player.closeInventory();
    }


    public Inventory getKitEditor(String kitName) {
        File file = new File(plugin.getDataFolder(), "kits.yml");
        FileConfiguration kits = YamlConfiguration.loadConfiguration(file);

        Inventory inv = Bukkit.createInventory(null, 54, plugin.getLangManager().getText("menus.kits.kit-editor") + kitName);

        ConfigurationSection kitSection = kits.getConfigurationSection(kitName);
        if (kitSection != null && kitSection.isConfigurationSection("receive")) {
            ConfigurationSection receive = kitSection.getConfigurationSection("receive");

            int slot = 0;
            for (String itemKey : receive.getKeys(false)) {
                String[] parts = itemKey.split("_");
                String matName = String.join("_", Arrays.copyOf(parts, parts.length - 1));

                int slotIndex = 0;
                try {
                    slotIndex = Integer.parseInt(parts[parts.length - 1]);
                } catch (NumberFormatException ignored) {}

                Material mat = Material.matchMaterial(matName);
                if (mat == null) continue;

                int amount = receive.getInt(itemKey + ".amount", 1);
                ItemStack item = new ItemStack(mat, amount);

                Object enchObj = receive.get(itemKey + ".enchanted");
                if (enchObj instanceof String) {
                    String[] enchParts = ((String) enchObj).split(":");
                    if (enchParts.length == 2) {
                        Enchantment ench = Enchantment.getByName(enchParts[0]);
                        int level = Integer.parseInt(enchParts[1]);
                        if (ench != null) item.addEnchantment(ench, level);
                    }
                }
                inv.setItem(slotIndex, item);
            }
        }
        return inv;
    }

    public void createKit(String kitName, Player player) {
        File file = new File(plugin.getDataFolder(), "kits.yml");
        FileConfiguration kits = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection kitSection = kits.createSection(kitName);
        kitSection.set("icon", Material.BARRIER.name());
        kitSection.set("name", "&f" + kitName);
        kitSection.set("lore", Arrays.asList(
                "&fYou will receive:",
                "&7Price: %PRICE%"
        ));
        kitSection.set("price", 0);
        kitSection.set("cooldown", 0);
        kitSection.set("permission", false);
        kitSection.createSection("receive");

        try {
            kits.save(file);
            plugin.getLangManager().sendMessage("kit-created", player);
        } catch (IOException e) {
            plugin.getLangManager().sendMessage("kit-creating-error", player);
            e.printStackTrace();
            return;
        }
        player.openInventory(getKitEditor(kitName));
    }

    public void openKitEditor(String kitName, Player player) {
        if(kitExists(kitName)) {
            Inventory inv = getKitEditor(kitName);
            player.openInventory(inv);
        }
    }

    public void openKitGUI(Player player) {
        player.openInventory(getKitGUI());
    }

    public void deleteKit(String kitName, Player player) {
        File file = new File(plugin.getDataFolder(), "kits.yml");
        FileConfiguration kits = YamlConfiguration.loadConfiguration(file);

        kits.set(kitName, null);

        try {
            kits.save(file);
            plugin.getLangManager().sendMessage("kit-deleted", player);
        } catch (IOException e) {
            plugin.getLangManager().sendMessage("kit-deleting-error", player);
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (!title.startsWith(plugin.getLangManager().getText("menus.kits.kit-editor"))) {
            return;
        }
        Player player = (Player) e.getPlayer();
        String kitName = title.replace(plugin.getLangManager().getText("menus.kits.kit-editor"), "");

        Inventory inv = e.getInventory();
        ItemStack[] contents = inv.getContents();

        boolean hasItems = false;
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) {
            plugin.getLangManager().sendMessage("kit-saved", player);
            return;
        }
        File file = new File(plugin.getDataFolder(), "kits.yml");
        FileConfiguration kits = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection kitSection = kits.getConfigurationSection(kitName);
        if (kitSection == null) {
            kitSection = kits.createSection(kitName);
        }
        if (kitSection.isConfigurationSection("receive")) {
            kitSection.set("receive", null);
        }
        ConfigurationSection receive = kitSection.createSection("receive");

        int index = 0;
        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) continue;

            String key = item.getType().name() + "_" + index;
            ConfigurationSection itemSec = receive.createSection(key);
            itemSec.set("amount", item.getAmount());

            if (item.getEnchantments().isEmpty()) {
                itemSec.set("enchanted", false);
            } else {
                Map.Entry<Enchantment, Integer> ench = item.getEnchantments().entrySet().iterator().next();
                itemSec.set("enchanted", ench.getKey().getName() + ":" + ench.getValue());
            }
            index++;
        }

        try {
            kits.save(file);
            plugin.getLangManager().sendMessage("kit-saved", player);
        } catch (IOException ex) {
            plugin.getLangManager().sendMessage("kit-saving-error", player);
            ex.printStackTrace();
        }
    }

    public long getLastUse(UUID uuid, String kitName) {
        return kitCooldowns.getOrDefault(uuid, Collections.emptyMap())
                .getOrDefault(kitName, 0L);
    }

    public void setLastUse(UUID uuid, String kitName) {
        kitCooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(kitName, System.currentTimeMillis());
    }
}
