package com.mauccio.ctw.files;

import com.mauccio.ctw.CTW;
import org.bukkit.configuration.file.*;
import java.io.*;
import java.util.List;

public class ConfigManager {
    private final CTW plugin;
    private FileConfiguration config;

    public ConfigManager(CTW plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        plugin.saveDefaultConfig();
        this.load(false);
    }

    public void load() {
        this.load(true);
    }

    public void persists() {
        this.plugin.saveConfig();
    }

    private void load(boolean reload) {
        if (reload) {
            this.plugin.reloadConfig();
        }
        this.validateSignText(this.getSignFirstLine(), "signs.first-line-text", "ctw");
        File defaultMapFile = new File(this.plugin.getDataFolder(), "defaultmap.yml");
        if (!defaultMapFile.exists()) {
            this.plugin.saveResource("defaultmap.yml", false);
        }
    }

    private void validateSignText(String text, String key, String defaultValue) {
        if (text.isEmpty() || text.length() > 16) {
            this.plugin.getLogger().warning("Config value \"".concat(key).concat("\" is incorrect."));
            this.config.set(key, (Object)defaultValue);
            this.plugin.getLogger().info("Config value \"".concat(key).concat("\" has been changed to \"").concat(defaultValue).concat("\"."));
        }
    }

    public String getSignFirstLine() {
        return this.config.getString("signs.first-line-text");
    }

    public boolean implementSpawnCmd() {
        return this.config.getBoolean("implement-spawn-cmd", false);
    }

    public boolean isVoidInstaKill() {
        return this.plugin.getConfig().getBoolean("instakill-on-void", false);
    }

    public boolean isFallDamage() {
        return this.plugin.getConfig().getBoolean("disable-fall-damage", false);
    }

    public boolean isKitSQL() {
        return this.plugin.getConfig().getBoolean("use-sql-for-kits", false);
    }

    public List<String> getBreakableBlocks() {
        return plugin.getConfig().getStringList("no-protected-blocks");
    }

    public List<String> getNoCrafteableItems() {
        return plugin.getConfig().getStringList("no-crafteable-items");
    }

    public boolean isSoundsEnabled() {
        return this.plugin.getConfig().getBoolean("sounds.enabled", true);
    }

    public boolean isBlockSpleafAlert() {
        return this.plugin.getConfig().getBoolean("block-spleef-alert", true);
    }

    public boolean isGlobalTablistEnabled() {
        return this.plugin.getConfig().getBoolean("global-tablist", false);
    }

    public boolean isLobbyGuardEnabled() {
        return this.plugin.getConfig().getBoolean("lobby.guard", false);
    }

    public boolean isLobbyItemsEnabled() {
        return this.plugin.getConfig().getBoolean("lobby.items", true);
    }

    public boolean isLobbyBoardEnabled() {
        return this.plugin.getConfig().getBoolean("lobby.scoreboard", true);
    }

    public boolean isKitMenuEnabled() {
        return this.plugin.getConfig().getBoolean("kit-menu", false);
    }

    public boolean isProtectedZoneMsg() {
        return this.plugin.getConfig().getBoolean("prohibited-msg", true);
    }
}