package com.mauccio.ctw;

import org.bukkit.configuration.file.*;
import java.io.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.command.*;
import org.bukkit.inventory.*;
import java.util.*;
import org.bukkit.enchantments.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.*;
import org.bukkit.event.entity.*;

public final class LangManager {
    private final Main plugin;
    private final YamlConfiguration lang;
    private final String messagePrefix;
    private final int minVersion = 4;
    
    public LangManager(final Main plugin) {
        this.plugin = plugin;
        this.lang = new YamlConfiguration();
        final File langFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("lang-file"));
        if (!langFile.exists()) {
            this.saveDefaultLangFiles();
        }
        if (langFile.exists()) {
            try {
                this.lang.load(langFile);
            }
            catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().severe(ex.toString());
            }
            final int langVersion = this.lang.getInt("version", 0);
            if ((langVersion < minVersion && (langFile.getName().equals("spanish.yml") || langFile.getName().equals("english.yml"))) || langFile.getName().equals("italian.yml")) {
                final File backUpFile = new File(langFile.getParent(), String.valueOf(langFile.getName()) + "-" + langVersion + ".bak");
                langFile.renameTo(backUpFile);
                plugin.saveResource(langFile.getName(), true);
                try {
                    this.lang.load(langFile);
                }
                catch (IOException | InvalidConfigurationException ex4) {
                    plugin.getLogger().severe(ex4.toString());
                }
            }
        } else {
            plugin.getLogger().severe("Configured language file does not exists: ".concat(langFile.getAbsolutePath()));
        }
        this.messagePrefix = ChatColor.translateAlternateColorCodes('&', this.lang.getString("message-prefix"));
    }
    
    public void saveDefaultLangFiles() {
        File defaultLangFile = new File(this.plugin.getDataFolder(), "spanish.yml");
        if (!defaultLangFile.exists()) {
            this.plugin.saveResource(defaultLangFile.getName(), false);
        }
        defaultLangFile = new File(this.plugin.getDataFolder(), "english.yml");
        if (!defaultLangFile.exists()) {
            this.plugin.saveResource(defaultLangFile.getName(), false);
        }
        defaultLangFile = new File(this.plugin.getDataFolder(), "italian.yml");
        if (!defaultLangFile.exists()) {
            this.plugin.saveResource(defaultLangFile.getName(), false);
        }
    }
    
    public String getText(final String label) {
        String text = this.lang.getString(label);
        if (text == null) {
            text = label;
        }
        else {
            text = ChatColor.translateAlternateColorCodes('&', text);
        }
        return text;
    }
    
    public String getMessage(final String label) {
        return String.valueOf(this.messagePrefix) + " " + this.getText(label);
    }
    
    public String getTitleMessage(final String label) {
        return this.getText(label);
    }
    
    public void sendMessage(final String label, final Player player) {
        player.sendMessage(this.getMessage(label));
    }
    
    public void sendMessage(final String label, final CommandSender cs) {
        cs.sendMessage(this.getMessage(label));
    }
    
    public String getMessagePrefix() {
        return this.messagePrefix;
    }
    
    public void sendText(final String baseLabel, final Player player) {
        if (this.lang.getString(baseLabel) == null) {
            this.sendMessage(baseLabel, player);
            return;
        }
        for (final String label : this.lang.getConfigurationSection(baseLabel).getKeys(false)) {
            this.sendMessage(String.valueOf(baseLabel) + "." + label, player);
        }
    }
    
    public ItemStack getHelpBook() {
        final List<String> bookPages = new ArrayList<String>();
        for (final String page : this.lang.getConfigurationSection("help-book.pages").getKeys(false)) {
            try {
                Integer.parseInt(page);
            }
            catch (NumberFormatException ex) {
                continue;
            }
            String textPage = "";
            for (final String line : this.lang.getConfigurationSection("help-book.pages." + page).getKeys(false)) {
                if (line != null) {
                    final String text = ChatColor.translateAlternateColorCodes('&', this.lang.getString("help-book.pages." + page + "." + line));
                    textPage = textPage.concat(text).concat("\n");
                }
            }
            bookPages.add(textPage);
        }
        final ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
        final BookMeta bm = (BookMeta)book.getItemMeta();
        bm.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.lang.getString("help-book.title")));
        bm.setAuthor(ChatColor.translateAlternateColorCodes('&', this.lang.getString("help-book.author")));
        bm.setTitle(ChatColor.translateAlternateColorCodes('&', this.lang.getString("help-book.title")));
        bm.setPages((List<String>)bookPages);
        bm.addEnchant(Enchantment.LUCK, 1, true);
        book.setItemMeta((ItemMeta)bm);
        return book;
    }
    
    public void sendVerbatimTextToWorld(final String text, final World world, final Player filter) {
        for (final Player receiver : world.getPlayers()) {
            if (filter != null && receiver.getName().equals(filter.getName())) {
                continue;
            }
            receiver.sendMessage(String.valueOf(this.messagePrefix) + " " + text);
        }
    }
    
    public void sendMessageToWorld(final String label, final World world, final Player filter) {
        final String text = this.getText(label);
        for (final Player receiver : world.getPlayers()) {
            if (filter != null && receiver.getName().equals(filter.getName())) {
                continue;
            }
            receiver.sendMessage(String.valueOf(this.messagePrefix) + " " + text);
        }
    }
    
    public void sendMessageToTeam(final String label, final Player player) {
        this.sendVerbatimMessageToTeam(this.getText(label), player);
    }
    
    public void sendVerbatimMessageToTeam(final String message, final Player player) {
        final TeamManager.TeamId playerTeam = this.plugin.pm.getTeamId(player);
        for (final Player receiver : player.getWorld().getPlayers()) {
            if (playerTeam == this.plugin.pm.getTeamId(receiver)) {
                receiver.sendMessage(String.valueOf(this.messagePrefix) + " " + message);
            }
        }
    }
    
    public String getMurderText(final Player player, final Player killer, final ItemStack is) {
        String ret = ChatColor.translateAlternateColorCodes('&', this.lang.getString("death-events.by-player.message"));
        ret = ret.replace("%KILLER%", killer.getName());
        ret = ret.replace("%KILLED%", player.getName());
        ret = ret.replace("%KILLER_COLOR%", new StringBuilder().append(this.plugin.pm.getChatColor(killer)).toString());
        ret = ret.replace("%KILLED_COLOR%", new StringBuilder().append(this.plugin.pm.getChatColor(player)).toString());
        String how;
        if (is != null) {
            how = this.lang.getString("death-events.by-player.melee.".concat(is.getType().name()));
            if (how == null) {
                how = this.lang.getString("death-events.by-player.melee._OTHER_");
            }
        }
        else {
            how = this.lang.getString("death-events.by-player.melee.PULL");
        }
        ret = ret.replace("%HOW%", how);
        return ret;
    }
    
    public String getRangeMurderText(final Player player, final Player killer, final int distance, final boolean headshoot) {
        String ret = ChatColor.translateAlternateColorCodes('&', this.lang.getString("death-events.by-player.message"));
        ret = ret.replace("%KILLER%", killer.getName());
        ret = ret.replace("%KILLED%", player.getName());
        ret = ret.replace("%KILLER_COLOR%", new StringBuilder().append(this.plugin.pm.getChatColor(killer)).toString());
        ret = ret.replace("%KILLED_COLOR%", new StringBuilder().append(this.plugin.pm.getChatColor(player)).toString());
        if (headshoot) {
            ret = ret.replace("%HOW%", this.lang.getString("death-events.by-player.range.HEADSHOT"));
        }
        else {
            ret = ret.replace("%HOW%", this.lang.getString("death-events.by-player.range.BODYSHOT"));
        }
        ret = ret.replace("%DISTANCE%", new StringBuilder(String.valueOf(distance)).toString());
        return ret;
    }
    
    public String getNaturalDeathText(final Player player, final EntityDamageEvent.DamageCause cause) {
        String ret = ChatColor.translateAlternateColorCodes('&', this.lang.getString("death-events.natural.message"));
        ret = ret.replace("%KILLED%", player.getName());
        ret = ret.replace("%KILLED_COLOR%", new StringBuilder().append(this.plugin.pm.getChatColor(player)).toString());
        String how = this.lang.getString("death-events.natural.cause.".concat(cause.name()));
        if (how == null) {
            how = this.lang.getString("death-events.natural.cause._OTHER_");
        }
        ret = ret.replace("%HOW%", how);
        return ret;
    }
}
