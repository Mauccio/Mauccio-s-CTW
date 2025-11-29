package com.mauccio.ctw.utils;

import com.mauccio.ctw.CTW;
import me.clip.placeholderapi.expansion.*;
import org.bukkit.entity.*;

public class PlaceholderCTW extends PlaceholderExpansion {
    private final CTW plugin;

    public PlaceholderCTW(CTW plugin) {
        this.plugin = plugin;
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String getAuthor() {
        return "Diego Lucio D'onofrio (Original LibelulaUCTW), Mauccio (Mauccio's CTW)";
    }

    public String getIdentifier() {
        return "mctw";
    }

    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        if (identifier.equals("score")) {
            return String.valueOf(this.plugin.getDBManager().getScore(player.getName()));
        }
        if (identifier.equals("kills")) {
            return String.valueOf(this.plugin.getDBManager().getKill(player.getName()));
        }
        if (identifier.equals("wools_placed")) {
            return String.valueOf(this.plugin.getDBManager().getWoolCaptured(player.getName()));
        }
        if (identifier.equals("deaths")) {
            return String.valueOf(this.plugin.getDBManager().getDeath(player.getName()));
        }
        return null;
    }
}