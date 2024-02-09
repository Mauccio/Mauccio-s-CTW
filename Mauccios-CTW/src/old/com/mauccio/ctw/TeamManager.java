package com.mauccio.ctw;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Wool;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TeamManager {

    private final Main plugin;
    private final Scoreboard scoreboard;
    private final TreeMap<TeamId, TeamInfo> teams;
    public final String armourBrandName;
    private final String bTeamText;
    public static Inventory joinMenuInventory;

    /**
     * Name of the team
     */
    public enum TeamId {

        RED, BLUE, SPECTATOR;
    }
    
    private class TeamInfo {

        @SuppressWarnings("unused")
		TeamId id;
        Team team;
        Color tshirtColor;
        @SuppressWarnings("unused")
		DyeColor dye;
        ChatColor chatColor;
        String name;

        public TeamInfo(TeamId id, Color tshirtColor, DyeColor dye, ChatColor chatColor) {
            this.id = id;
            team = scoreboard.registerNewTeam(id.toString());
            team.setAllowFriendlyFire(false);
            team.setPrefix(chatColor + "");
            this.tshirtColor = tshirtColor;
            this.dye = dye;
            this.chatColor = chatColor;
            name = plugin.lm.getText(id.toString() + "-TEAM");
            team.setDisplayName(chatColor + name);
        }
    }

    public TeamManager(Main plugin) {
    	
        this.plugin = plugin;
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        teams = new TreeMap<>();
        TeamInfo teamInfo;
        teamInfo = new TeamInfo(TeamId.RED, Color.fromRGB(255, 85, 85), DyeColor.RED, ChatColor.RED);
        teams.put(TeamId.RED, teamInfo);
        teamInfo = new TeamInfo(TeamId.BLUE, Color.fromRGB(85, 85, 255), DyeColor.BLUE, ChatColor.BLUE);
        teams.put(TeamId.BLUE, teamInfo);		
        teamInfo = new TeamInfo(TeamId.SPECTATOR, Color.SILVER, null, ChatColor.GRAY);
        teams.put(TeamId.SPECTATOR, teamInfo);
        armourBrandName = plugin.lm.getText("armour-brand");
        bTeamText = plugin.lm.getText("brackets-team");
        joinMenuInventory = getTeamInventoryMenu();
    }
    
    @SuppressWarnings("deprecation")
	public void addToTeam(Player player, TeamId teamId) {
        teams.get(teamId).team.addPlayer(player);
    }

    @SuppressWarnings("deprecation")
	public void removeFromTeam(Player player, TeamId teamId) {
        if (teamId != null) {
            teams.get(teamId).team.removePlayer(player);
        }
    }

    public Color getTshirtColor(TeamId teamId) {
        return teams.get(teamId).tshirtColor;    
    }

    public ChatColor getChatColor(TeamId teamId) {
        return teams.get(teamId).chatColor;
    }

    public String getName(TeamId teamId) {
        return teams.get(teamId).name;
    }

    public void onArmourDrop(ItemSpawnEvent e) {
        List<String> lore = e.getEntity().getItemStack().getItemMeta().getLore();
        if (lore == null) {
            return;
        }
        if (lore.contains(armourBrandName)) {
            e.setCancelled(true);
        }
    }

    public void playerChat(AsyncPlayerChatEvent e) {
        Player sender = e.getPlayer();
        TeamId senderTi = plugin.pm.getTeamId(sender);
        e.setCancelled(true);

        if (senderTi == null) { // The player is not in game.
            String message = "" + e.getPlayer().getDisplayName() + ": " + e.getMessage();
            for (Player receiver : e.getPlayer().getWorld().getPlayers()) {
                receiver.sendMessage(message);
            }
            plugin.getConsole().sendMessage(message);
        } else { // The player is on a game.
            String message = getChatColor(senderTi) + bTeamText + " "
                    + sender.getDisplayName().replace(sender.getName(),
                            getChatColor(senderTi) + sender.getName())
                    + ": " + ChatColor.RESET + e.getMessage();

            for (Player receiver : sender.getWorld().getPlayers()) {
                TeamId receiverTi = plugin.pm.getTeamId(receiver);
                if (receiverTi == null || receiverTi != senderTi) {
                    continue;
                }
                receiver.sendMessage(message);
            }

        }
    }

	@SuppressWarnings("incomplete-switch")
	public void cancelSpectator(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player == false) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        if (plugin.pm.isSpectator(player)) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null) {
                if (e.getCurrentItem().equals(plugin.pm.getMenuItem())
                        && !joinMenuInventory.getViewers().contains(player)) {
                    player.openInventory(joinMenuInventory);
                } else {
                    switch (e.getCurrentItem().getType()) {
                        case NETHER_STAR:
                            player.closeInventory();
                            plugin.gm.movePlayerTo(player, null);
                            break;
                        case WOOL:
                            player.closeInventory();
                            Wool wool = (Wool) e.getCurrentItem().getData();
                            switch (wool.getColor()) {
                                case RED:
                                    plugin.gm.joinInTeam(player, TeamId.RED);
                                    break;
                                case BLUE:
                                    plugin.gm.joinInTeam(player, TeamId.BLUE);
                                    break;
                            }
                            break;
                    }
                }

            }
        }
    }

    public void cancelSpectator(PlayerInteractEvent e) {
        if (e.getItem() != null && e.getItem().equals(plugin.pm.getMenuItem())) {
            e.getPlayer().openInventory(joinMenuInventory);
        }
        if (e.isCancelled()) {
            return;
        }
        if (plugin.pm.isSpectator(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    public void cancelSpectator(PlayerDropItemEvent e) {
        if (plugin.pm.isSpectator(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    public void cancelSpectator(BlockPlaceEvent e) {
        if (plugin.pm.isSpectator(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    public void cancelSpectator(BlockBreakEvent e) {
        if (plugin.pm.isSpectator(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    public void cancelSpectator(PlayerPickupItemEvent e) {
        if (plugin.pm.isSpectator(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    public void cancelSpectator(EntityTargetEvent e) {
        if (e.getTarget() instanceof Player == false) {
            return;
        }
        Player player = (Player) e.getTarget();
        if (plugin.pm.isSpectator(player)) {
            e.setCancelled(true);
        }
    }

    public void cancelSpectator(BlockDamageEvent e) {
        if (plugin.pm.isSpectator(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    public void cancelSpectator(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player == false) {
            return;
        }
        Player player = (Player) e.getEntity();
        if (plugin.pm.isSpectator(player)) {
            e.setCancelled(true);
        }
    }

    public void cancelSpectator(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player == false) {
            return;
        }
        Player player = (Player) e.getEntity();
        if (plugin.pm.isSpectator(player)) {
            e.setCancelled(true);
        }
    }

    public void cancelSpectator(PlayerInteractEntityEvent e) {
        if (plugin.pm.isSpectator(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    public Inventory getTeamInventoryMenu() {
        Inventory teamMenu;
        teamMenu = Bukkit.createInventory(null, 9, plugin.lm.getText("pick-your-team"));

        List<String> ayuda = new ArrayList<>();

        ItemStack option = new ItemStack(Material.NETHER_STAR);
        ItemMeta im = option.getItemMeta();
        im = option.getItemMeta();
        im.setDisplayName(plugin.lm.getText("auto-join"));
        ayuda.add(plugin.lm.getText("auto-join-help"));
        im.setLore(ayuda);
        option.setItemMeta(im);
        teamMenu.setItem(4, option);

        ayuda.clear();
        Wool wool = new Wool(DyeColor.BLUE); 
        option = wool.toItemStack();
        im = option.getItemMeta();
        im.setDisplayName(plugin.lm.getText("join-blue"));
        ayuda.add(plugin.lm.getText("blue-join-help"));
        im.setLore(ayuda);
        option.setItemMeta(im);
        teamMenu.setItem(6, option);

        ayuda.clear();
        wool = new Wool(DyeColor.RED); 
        option = wool.toItemStack();
        im = option.getItemMeta();
        im.setDisplayName(plugin.lm.getText("join-red"));
        ayuda.add(plugin.lm.getText("red-join-help"));
        im.setLore(ayuda);
        option.setItemMeta(im);
        teamMenu.setItem(2, option);
        

        return teamMenu;
        
    }

    public void cancelSameTeam(PlayerFishEvent e) {
        if (e.getCaught() instanceof Player) {
            Player damager = e.getPlayer();
            Player player = (Player) e.getCaught();
            TeamId playerTeam = plugin.pm.getTeamId(player);
            TeamId damagerTeam = plugin.pm.getTeamId(damager);
            if (playerTeam == damagerTeam) {
                e.setCancelled(true);
            }
        }
    }

	public void cancelSpectatorOrSameTeam(EntityDamageByEntityEvent e) {
        Arrow arrow;
        if (e.getEntity() instanceof Player == false) {
            return;
        }
        final Player player = (Player) e.getEntity();

        if (plugin.pm.isSpectator(player)) {
            e.setCancelled(true);
            return;
        }

        TeamId playerTeam = plugin.pm.getTeamId(player);

        if (playerTeam == null) {
            return;
        }
        Player damager;
        if (e.getDamager() instanceof Player == false) {
            if (e.getDamager() instanceof Arrow == false) {
                return;
            } else {
                arrow = (Arrow) e.getDamager();
                if (arrow.getShooter() instanceof Player) {
                    damager = (Player) arrow.getShooter();
                } else {
                    return;
                }
            }
        } else {
            damager = (Player) e.getDamager();
        }

        if (plugin.pm.isSpectator(damager)) {
            e.setCancelled(true);
            return;
        }
        
        TeamId damagerTeam = plugin.pm.getTeamId(damager);

        if (damagerTeam == null) {
            return;
        }
        if (damagerTeam == playerTeam || playerTeam == TeamId.SPECTATOR) {
            e.setCancelled(true);
            return;
        }
        plugin.pm.setLastDamager(player, damager);
    }

	public void manageDeath(PlayerDeathEvent e) {
        if (!plugin.rm.isInGame(e.getEntity().getWorld())) {
            return;
        }
        
        String roomName = plugin.rm.getRoom(e.getEntity().getWorld());
        if (plugin.gm.getState(roomName) != GameManager.GameState.IN_GAME) {
            e.getEntity().setHealth(20);
            return;
        }

        e.setDeathMessage("");

        Player player = e.getEntity();
        Player killer = null;
        int blockDistance = 0;
        boolean headhoot = false;
        e.setDeathMessage("");
        if (e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent entityDamageByEntityEvent = (EntityDamageByEntityEvent) e.getEntity().getLastDamageCause();
            if (entityDamageByEntityEvent.getDamager() instanceof Player) {
                killer = (Player) entityDamageByEntityEvent.getDamager();
            } else if (entityDamageByEntityEvent.getDamager() instanceof Arrow) {
                final Arrow arrow = (Arrow) entityDamageByEntityEvent.getDamager();
                if (arrow.getShooter() instanceof Player) {
                    killer = (Player) arrow.getShooter();
                    blockDistance = (int) player.getLocation().distance(killer.getLocation());
                    double y = arrow.getLocation().getY();
                    double shotY = player.getLocation().getY();
                    headhoot = y - shotY > 1.35d;
                }
            }
        }

        final String murderText;
        if (killer != null) {
            if (blockDistance == 0) {
                ItemStack is = killer.getItemInHand();
                murderText = plugin.lm.getMurderText(player, killer, is);
                
            } else {
                murderText = plugin.lm.getRangeMurderText(player, killer, blockDistance, headhoot);
            }
        } else {
            EntityDamageEvent ede = e.getEntity().getLastDamageCause();
            if (ede != null) {
                if (e.getEntity().getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.VOID) {
                    String killerName = plugin.pm.getLastDamager(player);
                    if (killerName != null) {
                        killer = plugin.getServer().getPlayer(killerName);
                        if (killer != null) {
                            murderText = plugin.lm.getMurderText(player, killer, null);
                            player.playSound(player.getLocation(), Sound.NOTE_PLING, 10.0F, 2);
                        } else {
                            murderText = plugin.lm.getNaturalDeathText(player, ede.getCause());
                            player.playSound(player.getLocation(), Sound.NOTE_PLING, 10.0F, 2);
                        }
                    } else {
                        murderText = plugin.lm.getNaturalDeathText(player, ede.getCause());
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 10.0F, 2);
                    }
                } else {
                    murderText = plugin.lm.getNaturalDeathText(player, ede.getCause());
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 10.0F, 2);
                }
            } else {
                murderText = plugin.lm.getNaturalDeathText(player, EntityDamageEvent.DamageCause.SUICIDE);
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 10.0F, 2);
            }
        }

        for (Player receiver : player.getWorld().getPlayers()) {
            if (!plugin.pm.canSeeOthersDeathMessages(receiver)) {
                if (!receiver.getName().equals(player.getName())
                        && (killer == null || !receiver.getName().equals(killer.getName()))) {
                    continue;
                }
            }
            receiver.sendMessage(murderText);
        }

        if (plugin.db != null) {
            final String playerName = player.getName();
            if (killer != null) {
                final String killerName = killer.getName();
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    @Override
                    public void run() {
                        plugin.db.addEvent(killerName, playerName, "KILL|" + murderText);
                        plugin.db.incScore(killerName, plugin.scores.kill);
                        plugin.db.incKill(killerName, 1);
                    }
                });
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    @Override
                    public void run() {
                        plugin.db.addEvent(playerName, killerName, "DEAD|" + murderText);
                        plugin.db.incScore(playerName, plugin.scores.death);
                    }
                });
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    @Override
                    public void run() {
                        plugin.db.addEvent(playerName, "SUICIDE|" + murderText);
                        plugin.db.incScore(playerName, plugin.scores.death);
                    }
                });
            }

        }

    }

}
