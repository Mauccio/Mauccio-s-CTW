package com.mauccio.ctw;

import me.clip.placeholderapi.expansion.*;
import org.bukkit.entity.*;

public class PlaceholderAPILCTWR extends PlaceholderExpansion
{
    private final Main plugin;
    
    public PlaceholderAPILCTWR(final Main plugin) {
        this.plugin = plugin;
    }
    
    public boolean persist() {
        return true;
    }
    
    public boolean canRegister() {
        return true;
    }
    
    public String getAuthor() {
        return "Diego Lucio D'onofrio (Original), Mauccio (LibelulaCTW-Reborn)";
    }
    
    public String getIdentifier() {
        return "lctwr";
    }
    
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }
    
    public String onPlaceholderRequest(final Player player, final String identifier) {
        if (player == null) {
            return "";
        }
        if (identifier.equals("score")) {
            return new StringBuilder(String.valueOf(this.plugin.db.getScore(player.getName()))).toString();
        }
        if (identifier.equals("kills")) {
            return new StringBuilder(String.valueOf(this.plugin.db.getKill(player.getName()))).toString();
        }
        if (identifier.equals("wools_placed")) {
            return new StringBuilder(String.valueOf(this.plugin.db.getWoolCaptured(player.getName()))).toString();
        }
        return null;
    }
}

