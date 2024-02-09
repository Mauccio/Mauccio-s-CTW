package com.mauccio.ctw.map;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.mauccio.ctw.CTW;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class SignManager {

    private final CTW plugin;
    private final YamlConfiguration signConfig;
    private final File signFile;
    private final TreeMap<String, Location> signs;
    private final Lock _signs_mutex;
    private String joinSignText;

    public SignManager(CTW plugin) {
        this.plugin = plugin;
        signFile = new File(plugin.getDataFolder(), "signs.yml");
        signConfig = new YamlConfiguration();
        signs = new TreeMap<>();
        _signs_mutex = new ReentrantLock(true);
        load();
    }

    public void load() {
        if (signFile.exists()) {
            try {
                signConfig.load(signFile);
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().severe(ex.toString());
            }
        }
        for (String roomName : signConfig.getKeys(false)) {
            String worldName = signConfig.getString(roomName + ".world");
            if (worldName == null) {
                continue;
            }
            World world = plugin.wm.loadWorld(worldName);
            if (world == null) {
                continue;
            }
            Location loc = new Location(world, signConfig.getInt(roomName + ".x"),
                    signConfig.getInt(roomName + ".y"),
                    signConfig.getInt(roomName + ".z"));
            if (loc.getBlock().getType() == Material.WALL_SIGN
                    || loc.getBlock().getType() == Material.SIGN_POST) {
                _signs_mutex.lock();
                try {
                    signs.put(roomName, loc);
                } finally {
                    _signs_mutex.unlock();
                }
                updateSign(loc);
            }
        }
        joinSignText = ChatColor.translateAlternateColorCodes('&', plugin.cf.getSignFirstLineReplacement());
    }

    public void persists() {
        _signs_mutex.lock();
        try {
            for (String roomName : signs.keySet()) {
                Location loc = signs.get(roomName);
                signConfig.set(roomName + ".x", loc.getBlockX());
                signConfig.set(roomName + ".y", loc.getBlockY());
                signConfig.set(roomName + ".z", loc.getBlockZ());
                signConfig.set(roomName + ".world", loc.getWorld().getName());
            }
        } finally {
            _signs_mutex.unlock();
        }
        try {
            signConfig.save(signFile);
        } catch (IOException ex) {
            plugin.getLogger().severe(ex.toString());
        }
    }

    public void updateSigns(String roomName) {
        Location loc = signs.get(roomName);
        if (loc != null) {
            updateSign(loc);
        }
    }

    private void updateSign(Location loc) {
        if (loc.getBlock().getType() == Material.WALL_SIGN
                || loc.getBlock().getType() == Material.SIGN_POST) {
            updateSign((Sign) loc.getBlock().getState());
        }
    }

    private void updateSign(Sign sign) {
        String roomName = sign.getLine(1);
        if (plugin.rm.exists(roomName)) {
            if (plugin.rm.isEnabled(roomName)) {
                String mapName = plugin.rm.getCurrentMap(roomName);
                if (mapName != null && plugin.mm.exists(mapName)) {
                    sign.setLine(2, mapName);
                    int maxPlayers = plugin.mm.getMaxPlayers(mapName);
                    int currentPlayers = plugin.gm.getPlayersIn(roomName);
                    String nowPlaying;
                    if (currentPlayers < maxPlayers) {
                        nowPlaying = ChatColor.GREEN + "" + currentPlayers;
                    } else {
                        nowPlaying = ChatColor.RED + "" + currentPlayers;
                    }
                    nowPlaying = nowPlaying + ChatColor.BLACK + " / "
                            + ChatColor.AQUA + maxPlayers;
                    sign.setLine(3, nowPlaying);
                    _signs_mutex.lock();
                    try {
                        signs.put(roomName, sign.getLocation());
                    } finally {
                        _signs_mutex.unlock();
                    }
                } else {
                    sign.setLine(0, ChatColor.translateAlternateColorCodes('&', plugin.cf.getTextForInvalidMaps()));
                }
            } else {
                sign.setLine(2, ChatColor.translateAlternateColorCodes('&', plugin.cf.getTextForDisabledMaps()));
                _signs_mutex.lock();
                try {
                    signs.put(roomName, sign.getLocation());
                } finally {
                    _signs_mutex.unlock();
                }
            }
        } else {
            sign.setLine(0, ChatColor.translateAlternateColorCodes('&', plugin.cf.getTextForInvalidRooms()));
        }
        sign.update();
    }

    public void checkForPlayerJoin(PlayerInteractEvent e) {
        Sign sign = (Sign) e.getClickedBlock().getState();
        if (sign.getLine(0).equals(joinSignText)) {
            e.setCancelled(true);
            if (plugin.rm.isEnabled(sign.getLine(1))) {
                plugin.gm.movePlayerToRoom(e.getPlayer(), sign.getLine(1));
            } else {
                plugin.lm.sendMessage("room-is-disabled", e.getPlayer());
            }
        }
    }

    public void checkForGameInPost(SignChangeEvent e) {
        if (e.getLine(0).toLowerCase().equalsIgnoreCase(plugin.cf.getSignFirstLine())) {
            e.setLine(0, joinSignText);
            final Location loc = e.getBlock().getLocation();
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    updateSign(loc);
                }
            }, 10L);
        }
    }
}