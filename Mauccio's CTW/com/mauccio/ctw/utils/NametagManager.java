package com.mauccio.ctw.utils;

import com.mauccio.ctw.CTW;
import com.mauccio.ctw.game.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NametagManager {

    private final CTW plugin;
    private final TeamManager teamManager;
    private Scoreboard scoreboard;

    public NametagManager(CTW plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        initializeTeams();
    }

    private void initializeTeams() {
        if (scoreboard.getTeam("RED") == null) {
            Team redTeam = scoreboard.registerNewTeam("RED");
            redTeam.setPrefix(teamManager.getChatColor(TeamManager.TeamId.RED).toString());
            redTeam.setCanSeeFriendlyInvisibles(false);
        }
        if (scoreboard.getTeam("BLUE") == null) {
            Team blueTeam = scoreboard.registerNewTeam("BLUE");
            blueTeam.setPrefix(teamManager.getChatColor(TeamManager.TeamId.BLUE).toString());
            blueTeam.setCanSeeFriendlyInvisibles(false);
        }
        if (scoreboard.getTeam("SPECTATOR") == null) {
            Team spectatorTeam = scoreboard.registerNewTeam("SPECTATOR");
            spectatorTeam.setPrefix(teamManager.getChatColor(TeamManager.TeamId.SPECTATOR).toString());
            spectatorTeam.setCanSeeFriendlyInvisibles(false);
        }
    }

    public void updateNametag(Player player, TeamManager.TeamId teamId) {

        for (Team team : scoreboard.getTeams()) {
            if (team.hasPlayer(player)) {
                team.removePlayer(player);
            }
        }

        Team team = getTeam(teamId);
        if (team != null) {
            team.addPlayer(player);
        }

        ChatColor color = teamManager.getChatColor(teamId);
        player.setDisplayName(color + player.getName());
        player.setPlayerListName(color + player.getName());

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.setScoreboard(scoreboard);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setScoreboard(scoreboard); 
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.setScoreboard(scoreboard);
            }
        }, 1L);
    }

    public void clearNametag(Player player) {
        for (Team team : scoreboard.getTeams()) {
            if (team.hasPlayer(player)) {
                team.removePlayer(player);
            }
        }
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.setScoreboard(scoreboard);
        }
    }

    public void reloadScoreboard() {
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        initializeTeams();
        for (Player player : Bukkit.getOnlinePlayers()) {
            TeamManager.TeamId teamId = plugin.pm.getTeamId(player);
            if (teamId != null) {
                updateNametag(player, teamId);
            } else {
                clearNametag(player);
            }
        }
    }

    private Team getTeam(TeamManager.TeamId teamId) {
        switch (teamId) {
            case RED:
                return scoreboard.getTeam("RED");
            case BLUE:
                return scoreboard.getTeam("BLUE");
            case SPECTATOR:
                return scoreboard.getTeam("SPECTATOR");
            default:
                return null;
        }
    }
}
