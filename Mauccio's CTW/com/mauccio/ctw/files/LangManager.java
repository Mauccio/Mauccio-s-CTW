package com.mauccio.ctw.files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mauccio.ctw.game.*;
import com.mauccio.ctw.CTW;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class LangManager {

    private final CTW plugin;
    private final YamlConfiguration lang;
    private final String messagePrefix;

    public LangManager(CTW plugin) {
        this.plugin = plugin;
        lang = new YamlConfiguration();
        File langFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!langFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        try {
            lang.load(langFile);
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.getLogger().severe("Error loading messages.yml: " + ex.getMessage());
        }

        messagePrefix = ChatColor.translateAlternateColorCodes('&',
                lang.getString("message-prefix"));
    }

    private String translateUnicode(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length();) {
            char c = input.charAt(i);
            if (c == '\\' && i + 5 < input.length() && input.charAt(i + 1) == 'u') {
                String hex = input.substring(i + 2, i + 6);
                try {
                    int code = Integer.parseInt(hex, 16);
                    sb.append((char) code);
                    i += 6;
                    continue;
                } catch (NumberFormatException ignored) {}
            }
            sb.append(c);
            i++;
        }
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }


    public String getChar(String path) {
        String raw = lang.getString(path);
        return translateUnicode(raw);
    }

    public List<String> getStringList(String path) {
        List<String> list = lang.getStringList(path);
        List<String> colored = new ArrayList<>();
        for (String line : list) {
            colored.add(translateUnicode(line));
        }
        return colored;
    }

    public String getText(String label) {
        String text = lang.getString(label);
        if (text == null) {
            text = label;
        } else {
            text = ChatColor.translateAlternateColorCodes('&', text);
        }
        return text;
    }

    public String getMessage(String label) {
        return messagePrefix + " " + getText(label);
    }

    public void sendMessage(String label, Player player) {
        player.sendMessage(getMessage(label));
    }

    public void sendMessage(String label, CommandSender cs) {
        cs.sendMessage(getMessage(label));
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }

    public void sendText(String baseLabel, Player player) {
        if (lang.getString(baseLabel) == null) {
            sendMessage(baseLabel, player);
            return;
        }
        for (String label : lang.getConfigurationSection(baseLabel).getKeys(false)) {
            sendMessage(baseLabel + "." + label, player);
        }
    }

    public ItemStack getHelpBook() {
        List<String> bookPages = new ArrayList<>();
        for (String page : lang.getConfigurationSection("help-book.pages").getKeys(false)) {
            try {
                Integer.parseInt(page);
            } catch (NumberFormatException ex) {
                continue;
            }
            String textPage = "";
            for (String line : lang.getConfigurationSection("help-book.pages." + page).getKeys(false)) {
                if (line != null) {
                    String text = ChatColor.translateAlternateColorCodes('&',
                            lang.getString("help-book.pages." + page + "." + line));
                    textPage = textPage.concat(text).concat("\n");
                }
            }
            bookPages.add(textPage);
        }
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta bm = (BookMeta) book.getItemMeta();
        bm.setDisplayName(ChatColor.translateAlternateColorCodes('&', lang.getString("help-book.title")));
        bm.setAuthor(ChatColor.translateAlternateColorCodes('&', lang.getString("help-book.author")));
        bm.setTitle(ChatColor.translateAlternateColorCodes('&', lang.getString("help-book.title")));
        bm.setPages(bookPages);
        bm.addEnchant(Enchantment.LUCK, 1, true);
        book.setItemMeta(bm);
        return book;
    }

    public String getTitleMessage(String label) {
        return this.getText(label);
    }

    public void sendVerbatimTextToWorld(String text, World world, Player filter) {
        for (Player receiver : world.getPlayers()) {
            if (filter != null && receiver.getName().equals(filter.getName())) {
                continue;
            }
            receiver.sendMessage(messagePrefix + " " + text);
        }
    }

    public void sendMessageToWorld(String label, World world, Player filter) {
        String text = getText(label);
        for (Player receiver : world.getPlayers()) {
            if (filter != null && receiver.getName().equals(filter.getName())) {
                continue;
            }
            receiver.sendMessage(messagePrefix + " " + text);
        }
    }

    public void sendVerbatimMessageToTeam(String message, Player player) {
        TeamManager.TeamId playerTeam = plugin.getPlayerManager().getTeamId(player);
        for (Player receiver : player.getWorld().getPlayers()) {
            if (playerTeam == plugin.getPlayerManager().getTeamId(receiver)) {
                receiver.sendMessage(messagePrefix + " " + message);
            }
        }
    }

    public String getMurderText(Player player, Player killer, ItemStack is) {
        String ret = ChatColor.translateAlternateColorCodes('&',
                lang.getString("death-events.by-player.message"));
        ret = ret.replace("%KILLER%", killer.getName());
        ret = ret.replace("%KILLED%", player.getName());
        ret = ret.replace("%KILLER_COLOR%", plugin.getPlayerManager().getChatColor(killer) + "");
        ret = ret.replace("%KILLED_COLOR%", plugin.getPlayerManager().getChatColor(player) + "");
        String how;
        if (is != null) {
            how = lang.getString("death-events.by-player.melee.".concat(is.getType().name()));
            if (how == null) {
                how = lang.getString("death-events.by-player.melee._OTHER_");
            }
        } else {
            how = lang.getString("death-events.by-player.melee.PULL");
        }
        ret = ret.replace("%HOW%", how);
        return ret;
    }

    public String getRangeMurderText(Player player, Player killer, int distance, boolean headshoot) {
        String ret = ChatColor.translateAlternateColorCodes('&',
                lang.getString("death-events.by-player.message"));
        ret = ret.replace("%KILLER%", killer.getName());
        ret = ret.replace("%KILLED%", player.getName());
        ret = ret.replace("%KILLER_COLOR%", plugin.getPlayerManager().getChatColor(killer) + "");
        ret = ret.replace("%KILLED_COLOR%", plugin.getPlayerManager().getChatColor(player) + "");
        if (headshoot) {
            ret = ret.replace("%HOW%", lang.getString("death-events.by-player.range.HEADSHOT"));
        } else {
            ret = ret.replace("%HOW%", lang.getString("death-events.by-player.range.BODYSHOT"));
        }
        ret = ret.replace("%DISTANCE%", distance + "");
        return ret;
    }

    public String getNaturalDeathText(Player player, EntityDamageEvent.DamageCause cause) {
        String ret = ChatColor.translateAlternateColorCodes('&',
                lang.getString("death-events.natural.message"));
        ret = ret.replace("%KILLED%", player.getName());
        ret = ret.replace("%KILLED_COLOR%", plugin.getPlayerManager().getChatColor(player) + "");
        String how = lang.getString("death-events.natural.cause.".concat(cause.name()));
        if (how == null) {
            how = lang.getString("death-events.natural.cause._OTHER_");
        }
        ret = ret.replace("%HOW%", how);
        return ret;
    }

    public String getWoolName(DyeColor color) {
        String label = "wool-names." + color.name();
        String woolName = getText(label);

        if (woolName.equals(label)) {
            woolName = color.name();
        }

        return woolName;
    }
}
