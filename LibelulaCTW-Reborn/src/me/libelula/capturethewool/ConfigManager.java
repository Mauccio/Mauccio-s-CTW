
package me.libelula.capturethewool;

import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final Main plugin;
    private final FileConfiguration config;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
        plugin.saveDefaultConfig();
        load(false);
    }

    public void load() {
        load(true);
    }
    
    public void persists() {
        plugin.saveConfig();
    }

    private void load(boolean reload) {
        if (reload) {
            plugin.reloadConfig();
        }
        
        validateSignText(getSignFirstLine(), "signs.first-line-text", "ctw");
                validateSignText(getSignFirstLineReplacement(), "signs.first-line-text-replacement",
                "&1LIBELULA&4CTW");
        validateSignText(getTextForInvalidRooms(), "signs.on-invalid-room-replacement",
                "&4INVALID ROOM");
        validateSignText(getTextForInvalidMaps(), "signs.on-invalid-map-replacement",
                "&4INVALID MAP");
        File defaultMapFile = new File(plugin.getDataFolder(), "defaultmap.yml");
        if (!defaultMapFile.exists()) {
            plugin.saveResource("defaultmap.yml", false);
        }
    }

    private void validateSignText(String text, String key, String defaultValue) {
        if (text.length() < 1 || text.length() > 16) {
            plugin.getLogger().warning("Config value \"".concat(key).concat("\" is incorrect."));
            config.set(key, defaultValue);
            plugin.getLogger().info("Config value \"".concat(key).concat("\" has been changed to \"")
                    .concat(defaultValue).concat("\"."));
        }

    }

    public String getSignFirstLine() {
        return config.getString("signs.first-line-text");
    }

    public String getSignFirstLineReplacement() {
        return config.getString("signs.first-line-text-replacement");
    }
    
    public String getTextForInvalidRooms() {
        return config.getString("signs.on-invalid-room-replacement");
    }

    public String getTextForInvalidMaps() {
        return config.getString("signs.on-invalid-map-replacement");
    }
    
    public String getTextForDisabledMaps() {
        return config.getString("signs.on-disabled-map");
    }
    
    public boolean implementSpawnCmd() {
        return config.getBoolean("implement-spawn-cmd", false);
    }

}
