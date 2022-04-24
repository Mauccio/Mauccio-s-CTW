package me.libelula.capturethewool;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderAPILCTWR extends PlaceholderExpansion {
	
	private final Main plugin;
	
	public PlaceholderAPILCTWR(Main plugin) {
		this.plugin = plugin;
	}

	public boolean persist(){
        return true;
    }

    public boolean canRegister(){
        return true;
    }
 
    public String getAuthor(){
        return "Diego Lucio D'onofrio (Original), Mauccio (LibelulaCTW-Reborn)";
    }

    public String getIdentifier(){
        return "lctwr";
    }

    public String getVersion(){
        return plugin.getDescription().getVersion();
    }
 
    @Override
    public String onPlaceholderRequest(Player player, String identifier){
 
        if(player == null){
            return "";
        }
 
        if(identifier.equals("score")) {
        	
        	return plugin.db.getScore(player.getName())+"";
        	
        } else if(identifier.equals("kills")) {
        	
        	return plugin.db.getKill(player.getName())+"";
        	
        } else if(identifier.equals("wools_placed")) {
        	
        	return plugin.db.getWoolCaptured(player.getName())+"";
        	
        }
        return null;
    }
}
