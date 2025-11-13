package com.mauccio.ctw.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.stream.Collectors;

public class LobbyItem {

    private final String id;
    private final String name;
    private final List<String> description;
    private final Material type;
    private final int data;
    private final int amount;
    private final int slot;
    private final boolean glide;
    private final String command;

    public LobbyItem(String id, String name, List<String> description, Material type, int data, int amount, int slot, boolean glide, String command) {
        this.id = id;
        this.name = ChatColor.translateAlternateColorCodes('&', name);
        this.description = description;
        this.type = type;
        this.data = data;
        this.amount = amount;
        this.slot = slot;
        this.glide = glide;
        this.command = command;
    }

    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(type, amount, (short) data);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        if (description != null && !description.isEmpty()) {
            meta.setLore(description.stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList()));
        }

        if (glide) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getDescription() { return description; }
    public Material getType() { return type; }
    public int getData() { return data; }
    public int getAmount() { return amount; }
    public int getSlot() { return slot; }
    public boolean isGlide() { return glide; }
    public String getCommand() { return command; }
}
