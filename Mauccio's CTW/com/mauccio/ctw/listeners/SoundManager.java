package com.mauccio.ctw.listeners;

import com.mauccio.ctw.CTW;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SoundManager {

    private final CTW plugin;

    private final Map<String, String> nbsMap = new HashMap<>();

    private enum SoundMode { GAME, NOTEBLOCK, MIXED }

    public enum SoundFor { SAME, ENEMY, GENERIC }

    public enum SoundTeam { RED, BLUE }

    public SoundManager(CTW plugin) {
        this.plugin = plugin;
    }

    private static final String[] DEFAULT_NBS = {
            "sounds/alert.nbs",
            "sounds/countdown",
            "sounds/error.nbs",
            "sounds/gui.nbs",
            "sounds/headshot.nbs",
            "sounds/join-command.nbs",
            "sounds/map-change.nbs",
            "sounds/stats.nbs",
            "sounds/success.nbs",
            "sounds/team-join-blue.nbs",
            "sounds/team-join-red.nbs",
            "sounds/team-win-enemy.nbs",
            "sounds/team-win-generic.nbs",
            "sounds/team-win-same.nbs",
            "sounds/tip.nbs",
            "sounds/win-wool-enemy.nbs",
            "sounds/win-wool-generic.nbs",
            "sounds/win-wool-same.nbs",
            "sounds/wool-placed-enemy.nbs",
            "sounds/wool-placed-generic.nbs",
            "sounds/wool-placed-same.nbs"
    };

    /*
            General
     */
    private SoundMode getSoundMode() {
        String mode = plugin.getConfig().getString("sounds.type", "GAME");
        try {
            return SoundMode.valueOf(mode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound type: " + mode + " — using GAME default type.");
            return SoundMode.GAME;
        }
    }

    private boolean isNoteBlockApiAvailable() {
        return Bukkit.getPluginManager().getPlugin("NoteBlockAPI") != null;
    }

    public void playErrorSound(Player player) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        soundModeHandler(player, "error", "error.nbs");
    }

    public void playAlertSound(Player player) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        soundModeHandler(player, "alert", "alert.nbs");
    }

    public void playTipSound(Player player) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        soundModeHandler(player, "tip", "tip.nbs");
    }

    public void playHeadshotSound(Player player) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        soundModeHandler(player, "headshot", "headshot.nbs");
    }

    public void playWinWoolSound(Player player, SoundFor soundFor) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        String suffix = soundFor == SoundFor.SAME ? "same" : soundFor == SoundFor.ENEMY ? "enemy" : "generic";
        soundModeHandler(player, "win-wool." + suffix, "win-wool-" + suffix + ".nbs");
    }

    public void playWoolPickupSound(Player player, SoundFor soundFor) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        String suffix = soundFor == SoundFor.SAME ? "same" : soundFor == SoundFor.ENEMY ? "enemy" : "generic";
        soundModeHandler(player, "wool-pickup." + suffix, "wool-pickup-" + suffix + ".nbs");
    }

    public void playReversingSound(Player player) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        soundModeHandler(player, "countdown", "countdown.nbs");
    }

    public void playTeamWinSound(Player player, SoundFor soundFor) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        String suffix = soundFor == SoundFor.SAME ? "same" : soundFor == SoundFor.ENEMY ? "enemy" : "generic";
        soundModeHandler(player, "team-win." + suffix, "team-win-" + suffix + ".nbs");
    }

    public void playStatsSound(Player player) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        soundModeHandler(player, "stats", "stats.nbs");
    }

    public void playTeamJoinSound(Player player, SoundTeam soundTeam) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        String teamKey = soundTeam == SoundTeam.RED ? "red" : "blue";
        soundModeHandler(player, "team-join." + teamKey, "team-join-" + teamKey + ".nbs");
    }

    public void playJoinCommandSound(Player player) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        soundModeHandler(player, "join-command", "join-command.nbs");
    }

    public void playMapChangeSound(Player player) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        soundModeHandler(player, "map-change", "map-change.nbs");
    }

    public void playGuiSound(Player player) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;
        soundModeHandler(player, "gui", "gui.nbs");
    }

    /**
     *
     * Handler for Sounds, Type of Sounds.
     *
     * @param player : For NBS player
     * @param soundPath : Sound Path
     * @param nbsFallback : NBS NotNull
     */
    private void soundModeHandler(Player player, String soundPath, String nbsFallback) {
        if (player == null || !player.isOnline()) return;
        if (!plugin.getConfigManager().isSoundsEnabled()) return;

        SoundMode mode = getSoundMode();
        String cfgVal = plugin.getConfig().getString("sounds." + soundPath, null);
        if (cfgVal == null || cfgVal.trim().isEmpty()) return;
        cfgVal = cfgVal.trim();

        switch (mode) {
            case GAME: {
                if ("NONE".equalsIgnoreCase(cfgVal)) return;
                try {
                    Sound s = Sound.valueOf(cfgVal.toUpperCase(Locale.ROOT));
                    player.playSound(player.getLocation(), s, 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid sound enum for sounds." + soundPath + ": " + cfgVal);
                }
                break;
            }

            case NOTEBLOCK: {
                String nbsName = null;
                if (nbsMap.containsKey(soundPath)) nbsName = nbsMap.get(soundPath);
                if (nbsName == null && cfgVal.toLowerCase(Locale.ROOT).endsWith(".nbs")) nbsName = cfgVal;
                if (nbsName == null) {
                    String candidate = soundPath.endsWith(".nbs") ? soundPath : soundPath + ".nbs";
                    File f = new File(plugin.getDataFolder(), "sounds" + File.separator + candidate);
                    if (f.exists()) nbsName = candidate;
                }
                if (nbsName == null) nbsName = nbsFallback;
                File songFile = new File(plugin.getDataFolder(), "sounds" + File.separator + nbsName);
                if (!songFile.exists()) {
                    plugin.getLogger().warning("NBS not found for " + soundPath + " (tested also with: " + nbsName + ")");
                    return;
                }
                if (!isNoteBlockApiAvailable()) {
                    plugin.getLogger().warning("NoteBlockAPI is not available, error at playing: " + nbsName);
                    return;
                }
                Song song = NBSDecoder.parse(songFile);
                if (song == null || song.getLayerHashMap() == null || song.getLayerHashMap().isEmpty()) {
                    plugin.getLogger().warning("Invalid or empty NBS: " + nbsName + " for " + soundPath);
                    return;
                }
                SongPlayer sp = new RadioSongPlayer(song);
                sp.setAutoDestroy(true);
                sp.addPlayer(player);
                sp.setPlaying(true);
                break;
            }

            case MIXED: {
                if ("NONE".equalsIgnoreCase(cfgVal)) return;
                if ("NOTEBLOCK".equalsIgnoreCase(cfgVal)) {
                    String nbsName = null;
                    if (nbsMap.containsKey(soundPath)) nbsName = nbsMap.get(soundPath);
                    if (nbsName == null && cfgVal.toLowerCase(Locale.ROOT).endsWith(".nbs")) nbsName = cfgVal;
                    if (nbsName == null) {
                        String candidate = soundPath.endsWith(".nbs") ? soundPath : soundPath + ".nbs";
                        File f = new File(plugin.getDataFolder(), "sounds" + File.separator + candidate);
                        if (f.exists()) nbsName = candidate;
                    }
                    if (nbsName == null) nbsName = nbsFallback;

                    File songFile = new File(plugin.getDataFolder(), "sounds" + File.separator + nbsName);
                    if (!songFile.exists()) {
                        plugin.getLogger().warning("NBS not found for: " + soundPath + " (tested: " + nbsName + ")");
                        return;
                    }
                    if (!isNoteBlockApiAvailable()) {
                        plugin.getLogger().warning("NoteBlockAPI not available, error at playing: " + nbsName);
                        return;
                    }
                    Song song = NBSDecoder.parse(songFile);
                    if (song == null || song.getLayerHashMap() == null || song.getLayerHashMap().isEmpty()) {
                        plugin.getLogger().warning("Invalid or Empty NBS: " + nbsName + " for " + soundPath);
                        return;
                    }
                    SongPlayer spMixed = new RadioSongPlayer(song);
                    spMixed.setAutoDestroy(true);
                    spMixed.addPlayer(player);
                    spMixed.setPlaying(true);
                } else {
                    try {
                        Sound s2 = Sound.valueOf(cfgVal.toUpperCase(Locale.ROOT));
                        player.playSound(player.getLocation(), s2, 1.0f, 1.0f);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid MIXED value for sounds." + soundPath + ": " + cfgVal);
                    }
                }
                break;
            }
        }
    }

    /*
            NBS Scanner and Extractor
     */
    public void ensureNbsExtractedAndScanned() {
        File soundsDir = new File(plugin.getDataFolder(), "sounds");
        if (!soundsDir.exists() && !soundsDir.mkdirs()) {
            plugin.getLogger().warning("Error at creating /sounds/ folder in plugin folder.");
            return;
        }

        File marker = new File(soundsDir, ".extracted");
        if (!marker.exists()) {
            int extracted = extractDefaultNbs();
            scanNbsFolder();
            boolean allPresent = true;
            for (String res : DEFAULT_NBS) {
                String name = new File(res).getName();
                if (!new File(soundsDir, name).exists()) {
                    allPresent = false;
                    break;
                }
            }
            if (allPresent) {
                try {
                    if (marker.createNewFile()) plugin.getLogger().info("NBS Extraction complete at .extracted mark.");
                } catch (Exception e) {
                    plugin.getLogger().warning("NBS Extraction error at .extracted mark: " + e.getMessage());
                }
            } else {
                plugin.getLogger().warning("NBS Extraction incomplete at .extracted mark. Extracted files: " + extracted +
                        ". Some NBS were not found; check logs for more details.");
            }
        } else {
            scanNbsFolder();
            extractMissingNbs();
        }
    }

    public int extractDefaultNbs() {
        File soundsDir = new File(plugin.getDataFolder(), "sounds");
        if (!soundsDir.exists() && !soundsDir.mkdirs()) {
            plugin.getLogger().warning("Error at creating /sounds/ folder in plugin folder.");
            return 0;
        }

        int extracted = 0;
        for (String res : DEFAULT_NBS) {
            File out = new File(plugin.getDataFolder(), res);
            if (out.exists()) continue;
            try {
                plugin.saveResource(res, false);
                plugin.getLogger().info("Extracted: " + res);
                extracted++;
            } catch (IllegalArgumentException iae) {
                plugin.getLogger().warning("NBS not found in JAR file: " + res + " please, report this in github.com");
            } catch (Exception e) {
                plugin.getLogger().warning("Error at " + res + " extraction:" + e.getMessage());
            }
        }
        return extracted;
    }

    public int scanNbsFolder() {
        File soundsDir = new File(plugin.getDataFolder(), "sounds");
        if (!soundsDir.exists() || !soundsDir.isDirectory()) {
            plugin.getLogger().warning("/sounds/ folder doesn't exists.");
            nbsMap.clear();
            return 0;
        }

        File[] files = soundsDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".nbs"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("NBS Files missing in /sounds/.");
            nbsMap.clear();
            return 0;
        }

        nbsMap.clear();
        int count = 0;
        for (File f : files) {
            String fileName = f.getName();
            String lowerFile = fileName.toLowerCase(Locale.ROOT);
            String base = lowerFile.substring(0, lowerFile.length() - 4); // without ".nbs"

            String keyPlain = base;                          // "team-win-same"
            String keyUnderscore = base.replace('-', '_');   // "team_win_same"
            String keyDot = base.replace('-', '.');          // "team.win.same"
            String keyNoSep = base.replace("-", "");         // "teamwinsame"
            String keyOriginal = fileName;                   // "team-win-same.nbs"

            putIfAbsent(nbsMap, keyPlain, fileName);
            putIfAbsent(nbsMap, keyUnderscore, fileName);
            putIfAbsent(nbsMap, keyDot, fileName);
            putIfAbsent(nbsMap, keyNoSep, fileName);
            putIfAbsent(nbsMap, keyOriginal, fileName);

            plugin.getLogger().info("NBS: " + fileName + " found -> keys: " +
                    keyPlain + ", " + keyUnderscore + ", " + keyDot + ", " + keyNoSep);
            count++;
        }

        plugin.getLogger().info("Loaded NBS: " + count);
        return count;
    }

    private void putIfAbsent(Map<String, String> map, String key, String value) {
        if (key == null || key.isEmpty()) return;
        if (!map.containsKey(key)) map.put(key, value);
    }

    public void extractMissingNbs() {
        File soundsDir = new File(plugin.getDataFolder(), "sounds");
        if (!soundsDir.exists() || !soundsDir.isDirectory()) {
            plugin.getLogger().warning("The folder /sounds/ doesn't exists at trying file extraction.");
            return;
        }

        int extracted = 0;
        for (String res : DEFAULT_NBS) {
            String name = new File(res).getName();
            File f = new File(soundsDir, name);
            if (f.exists()) continue;

            try {
                plugin.saveResource(res, false);
                plugin.getLogger().info("Missing file extracted: " + name);
                extracted++;
            } catch (IllegalArgumentException iae) {
                plugin.getLogger().warning("NBS not found in JAR file: " + res + " please, report this in github.com");
            } catch (Exception e) {
                plugin.getLogger().warning("Error at missing file extracting " + res + ": " + e.getMessage());
            }
        }

        if (extracted == 0) {
            plugin.getLogger().info("All NBS are complete.");
        } else {
            scanNbsFolder();
        }
    }
}
