package com.mauccio.ctw.game;

import com.mauccio.ctw.CTW;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NametagManager {

    private final CTW plugin;
    private final TeamManager teamManager;
    private final Scoreboard mainScoreboard;

    public NametagManager(CTW plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        initializeTeams(mainScoreboard);
    }
    public void initializeTeams(Scoreboard sb) {
        if (sb.getTeam("RED") == null) {
            Team redTeam = sb.registerNewTeam("RED");
            redTeam.setPrefix(teamManager.getChatColor(TeamManager.TeamId.RED) + "");
            redTeam.setCanSeeFriendlyInvisibles(false);
        }
        if (sb.getTeam("BLUE") == null) {
            Team blueTeam = sb.registerNewTeam("BLUE");
            blueTeam.setPrefix(teamManager.getChatColor(TeamManager.TeamId.BLUE) + "");
            blueTeam.setCanSeeFriendlyInvisibles(false);
        }
        if (sb.getTeam("SPECTATOR") == null) {
            Team spectatorTeam = sb.registerNewTeam("SPECTATOR");
            spectatorTeam.setPrefix(teamManager.getChatColor(TeamManager.TeamId.SPECTATOR) + "");
            spectatorTeam.setCanSeeFriendlyInvisibles(false);
        }
    }
    public void updateNametag(Player player, TeamManager.TeamId teamId, Scoreboard scoreboard) {
        if (scoreboard == null) {
            plugin.getLogger().warning("NametagManager: scoreboard destino es null. Usando mainScoreboard.");
            scoreboard = mainScoreboard;
        }
        initializeTeams(scoreboard);

        for (Team t : scoreboard.getTeams()) {
            if (t.hasEntry(player.getName())) {
                t.removeEntry(player.getName());
            }
        }
        Team team = getTeam(scoreboard, teamId);
        if (team != null) {
            team.setPrefix(teamManager.getChatColor(teamId) + "");
            team.addEntry(player.getName());
        } else {
            plugin.getLogger().warning("NametagManager: Team " + teamId + " no encontrado en el scoreboard dado.");
        }
        ChatColor color = teamManager.getChatColor(teamId);
        player.setDisplayName(color + player.getName());
        player.setPlayerListName(color + player.getName());
    }

    private Team getTeam(Scoreboard sb, TeamManager.TeamId teamId) {
        switch (teamId) {
            case RED: return sb.getTeam("RED");
            case BLUE: return sb.getTeam("BLUE");
            case SPECTATOR: return sb.getTeam("SPECTATOR");
            default: return null;
        }
    }
}