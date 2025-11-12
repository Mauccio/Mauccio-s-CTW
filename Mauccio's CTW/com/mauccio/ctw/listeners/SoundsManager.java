package com.mauccio.ctw.listeners;

import com.mauccio.ctw.CTW;
import org.bukkit.Sound;

public class SoundManager {

    /*
    Config.yml Sounds
    sounds:
        enabled: true
        alert: NOTE_PLING
        win-wool-sound: VILLAGER_YES
        wool-pickup-sound: FIREWORK_LAUNCH
        reversing-sound: ORB_PICKUP
        team-join-sound: LEVEL_UP
        join-command: CHEST_OPEN
        map-change: LEVEL_UP
        room-gui: CHEST_OPEN
        team-win-sound: WITHER_DEATH
        your-stats: ORB_PICKUP
        error: ENDERDRAGON_HIT
        tip: ITEM_PICKUP
        headshot: BLAZE_DEATH
     */

    private final CTW plugin;
    private Sound errorSound;
    private Sound alertSound;
    private Sound tipSound;
    private Sound roomGuiSound;
    private Sound joinCommandSound;
    private Sound teamJoinSound;
    private Sound mapChangeSound;
    private Sound woolPickupSound;
    private Sound reversingSound;
    private Sound teamWinSound;
    private Sound winWoolSound;
    private Sound yourStatsSound;
    private Sound headshotSound;

    public SoundManager(CTW plugin) {
        this.plugin = plugin;
    }

    public void loadSounds() {
        errorSound = Sound.valueOf(plugin.getConfig().getString("sounds.error", "ENDERDRAGON_HIT"));
        alertSound = Sound.valueOf(plugin.getConfig().getString("sounds.alert", "NOTE_PLING"));
        tipSound = Sound.valueOf(plugin.getConfig().getString("sounds.tip", "ITEM_PICKUP"));
        roomGuiSound = Sound.valueOf(plugin.getConfig().getString("sounds.room-gui", "CHEST_OPEN"));
        joinCommandSound = Sound.valueOf(plugin.getConfig().getString("sounds.join-command", "CHEST_OPEN"));
        teamJoinSound = Sound.valueOf(plugin.getConfig().getString("sounds.team-join-sound", "LEVEL_UP"));
        mapChangeSound = Sound.valueOf(plugin.getConfig().getString("sounds.map-change", "LEVEL_UP"));
        woolPickupSound = Sound.valueOf(plugin.getConfig().getString("sounds.wool-pickup-sound", "FIREWORK_LAUNCH"));
        reversingSound = Sound.valueOf(plugin.getConfig().getString("sounds.reversing-sound", "ORB_PICKUP"));
        teamWinSound = Sound.valueOf(plugin.getConfig().getString("sounds.team-win-sound", "WITHER_DEATH"));
        winWoolSound = Sound.valueOf(plugin.getConfig().getString("sounds.win-wool-sound", "VILLAGER_YES"));
        yourStatsSound = Sound.valueOf(plugin.getConfig().getString("sounds.your-stats", "ORB_PICKUP"));
        headshotSound = Sound.valueOf(plugin.getConfig().getString("sounds.headshot", "BLAZE_DEATH"));
    }

    public Sound getErrorSound() {
        return errorSound;
    }

    public Sound getAlertSound() {
        return alertSound;
    }

    public Sound getTipSound() {
        return tipSound;
    }

    public Sound getRoomGuiSound() {
        return roomGuiSound;
    }

    public Sound getJoinCommandSound() {
        return joinCommandSound;
    }

    public Sound getTeamJoinSound() {
        return teamJoinSound;
    }

    public Sound getMapChangeSound() {
        return mapChangeSound;
    }

    public Sound getWoolPickupSound() {
        return woolPickupSound;
    }

    public Sound getReversingSound() {
        return reversingSound;
    }

    public Sound getTeamWinSound() {
        return teamWinSound;
    }

    public Sound getWinWoolSound() {
        return winWoolSound;
    }

    public Sound getYourStatsSound() {
        return yourStatsSound;
    }

    public Sound getHeadshotSound() {
        return headshotSound;
    }

    public void playErrorSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getErrorSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Error sound is not configured properly.");
            }
        }
    }

    public void playAlertSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getAlertSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Alert sound is not configured properly.");
            }
        }
    }

    public void playTipSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getTipSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Tip sound is not configured properly.");
            }
        }
    }

    public void playHeadshotSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getHeadshotSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Headshot sound is not configured properly.");
            }
        }
    }

    public void playWinWoolSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getWinWoolSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Win wool sound is not configured properly.");
            }
        }
    }

    public void playWoolPickupSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getWoolPickupSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Wool pickup sound is not configured properly.");
            }
        }
    }

    public void playReversingSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getReversingSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Reversing sound is not configured properly.");
            }
        }
    }

    public void playTeamWinSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getTeamWinSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Team win sound is not configured properly.");
            }
        }
    }

    public void playYourStatsSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getYourStatsSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Your stats sound is not configured properly.");
            }
        }
    }

    public void playTeamJoinSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getTeamJoinSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Team join sound is not configured properly.");
            }
        }
    }

    public void playJoinCommandSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getJoinCommandSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Join command sound is not configured properly.");
            }
        }
    }

    public void playMapChangeSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getMapChangeSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Map change sound is not configured properly.");
            }
        }
    }

    public void playRoomGuiSound(org.bukkit.entity.Player player) {
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            try {
                player.playSound(player.getLocation(), getRoomGuiSound(), 1.0f, 1.0f);
            } catch (NullPointerException e) {
                plugin.getLogger().warning("Room GUI sound is not configured properly.");
            }
        }
    }
}
