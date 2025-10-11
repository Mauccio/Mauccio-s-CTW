package com.mauccio.ctw.game;

import com.mauccio.ctw.CTW;
import com.mauccio.ctw.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;

public class PlayerManager {

    private class PlayerOptions {

        boolean viewOthersSpectators;
        boolean viewOthersDeathMessages;
        boolean viewBlood;
    }

    private final CTW plugin;
    private final File playersFile;
    private final YamlConfiguration playersConfig;
    private final TreeMap<String, PlayerOptions> playerOptions;
    private final TreeMap<String, TeamManager.TeamId> playerTeam;
    private final ReentrantLock _playerTeam_mutex;
    private final ItemStack helpBook;
    private final ItemStack joinMenuItem;
    private final TreeMap<String, Map.Entry<Long, String>> lastDamager;
    private final TreeSet<String> falseSpectators;

    public PlayerManager(CTW plugin) {
        this.playersFile = new File(plugin.getDataFolder(), "players.yml");
        this.playersConfig = new YamlConfiguration();
        this.plugin = plugin;
        playerOptions = new TreeMap<>();
        lastDamager = new TreeMap<>();
        playerTeam = new TreeMap<>();
        this._playerTeam_mutex = new ReentrantLock(true);
        this.helpBook = plugin.lm.getHelpBook();
        ItemStack menuItem = new ItemStack(Material.PAPER);
        ItemMeta im = menuItem.getItemMeta();
        im.setDisplayName(plugin.lm.getText("help-menu-item.title"));
        menuItem.setItemMeta(im);
        this.joinMenuItem = menuItem;
        falseSpectators = new TreeSet<>();
    }

    public void setFalseSpectator(Player player) {
        this._playerTeam_mutex.lock();

        try {
            this.falseSpectators.add(player.getName());
        } finally {
            this._playerTeam_mutex.unlock();
        }

    }

    public void setLastDamager(Player player, Player damager) {
        Map.Entry<Long, String> entry = new AbstractMap.SimpleEntry<>((new Date()).getTime(), damager.getName());
        this.lastDamager.put(player.getName(), entry);
    }

    public String getLastDamager(Player player) {
        String ret = null;
        Map.Entry<Long, String> entry = lastDamager.remove(player.getName());
        if (entry != null) {
            long upTo = (new Date()).getTime() - 10000L;
            if ((Long)entry.getKey() > upTo) {
                ret = (String)entry.getValue();
            }
        }

        return ret;
    }

    public ItemStack getMenuItem() {
        return this.joinMenuItem;
    }

    public void load() {
        try {
            this.playersConfig.load(this.playersFile);
        } catch (InvalidConfigurationException | IOException var4) {
            this.plugin.getLogger().severe(var4.toString());
        }
        for (String playerName : playersConfig.getKeys(false)) {
            PlayerOptions po = new PlayerOptions();
            po.viewOthersSpectators = playersConfig.getBoolean(playerName + "." + "view.others-spectators");
            po.viewOthersDeathMessages = playersConfig.getBoolean(playerName + "." + "view.others-deadMessages");
            po.viewBlood = playersConfig.getBoolean(playerName + "." + "view.blood");
            playerOptions.put(playerName, po);
        }
    }

    public boolean toggleSeeOthersSpectators(Player player) {
        PlayerOptions po = (PlayerOptions)this.playerOptions.get(player.getName());
        if (po != null) {
            po.viewOthersSpectators = !po.viewOthersSpectators;
        } else {
            po = new PlayerOptions();
            po.viewOthersSpectators = !this.canSeeOthersSpectators(player);
        }

        this.playerOptions.put(player.getName(), po);
        this.updatePlayerList(player);
        return po.viewOthersSpectators;
    }

    public boolean toogleOthersDeathMessages(Player player) {
        PlayerOptions po = (PlayerOptions)this.playerOptions.get(player.getName());
        if (po != null) {
            po.viewOthersDeathMessages = !po.viewOthersDeathMessages;
        } else {
            po = new PlayerOptions();
            po.viewOthersDeathMessages = !this.canSeeOthersDeathMessages(player);
        }

        this.playerOptions.put(player.getName(), po);
        return po.viewOthersDeathMessages;
    }

    public boolean toggleBloodEffect(Player player) {
        PlayerOptions po = (PlayerOptions)this.playerOptions.get(player.getName());
        if (po != null) {
            po.viewBlood = !po.viewBlood;
        } else {
            po = new PlayerOptions();
            po.viewBlood = !this.canSeeBloodEffect(player);
        }

        this.playerOptions.put(player.getName(), po);
        return po.viewBlood;
    }

    public boolean canSeeBloodEffect(Player player) {
        PlayerOptions po = this.playerOptions.get(player.getName());
        return po == null || po.viewBlood;
    }

    public boolean canSeeOthersSpectators(Player player) {
        PlayerOptions po = this.playerOptions.get(player.getName());
        return po == null || po.viewOthersSpectators;
    }

    public boolean canSeeOthersDeathMessages(Player player) {
        PlayerOptions po = this.playerOptions.get(player.getName());
        return po == null || po.viewOthersDeathMessages;
    }

    public ChatColor getChatColor(Player player) {
        ChatColor cc = ChatColor.WHITE;
        TeamManager.TeamId teamId = (TeamManager.TeamId)this.playerTeam.get(player.getName());
        if (teamId != null) {
            cc = this.plugin.tm.getChatColor(teamId);
        }

        return cc;
    }

    public void addPlayerTo(Player player, TeamManager.TeamId teamId) {
        this._playerTeam_mutex.lock();
        this.falseSpectators.remove(player.getName());

        try {
            TeamManager.TeamId previousTeam = (TeamManager.TeamId)this.playerTeam.put(player.getName(), teamId);
            if (previousTeam != null) {
                this.plugin.tm.removeFromTeam(player, previousTeam);
            }

            this.plugin.tm.addToTeam(player, teamId);
            this.clearInventory(player);
            if (teamId != TeamManager.TeamId.SPECTATOR) {
                this.disguise(player, teamId);
            } else {
                this.setSpectator(player);
            }

            this.updatePlayerList(player);
            player.sendMessage(this.plugin.lm.getMessage("moved-to-" + teamId.name().toLowerCase()));
        } finally {
            this._playerTeam_mutex.unlock();
        }

    }

    public TeamManager.TeamId clearTeam(Player player) {
        this._playerTeam_mutex.lock();
        this.falseSpectators.remove(player.getName());

        TeamManager.TeamId teamId;
        try {
            this.clearInventory(player);
            this.dress(player);
            teamId = (TeamManager.TeamId)this.playerTeam.remove(player.getName());
            this.plugin.tm.removeFromTeam(player, teamId);
        } finally {
            this._playerTeam_mutex.unlock();
        }

        return teamId;
    }

    public TeamManager.TeamId getTeamId(Player player) {
        return (TeamManager.TeamId)this.playerTeam.get(player.getName());
    }

    public boolean isSpectator(Player player) {
        boolean resp = false;
        TeamManager.TeamId teamId = (TeamManager.TeamId)this.playerTeam.get(player.getName());
        if (teamId != null) {
            if (teamId != TeamManager.TeamId.SPECTATOR) {
                if (this.falseSpectators.contains(player.getName())) {
                    resp = true;
                }
            } else {
                resp = true;
            }
        }

        return resp;
    }

    public void clearInventory(Player player) {
        ItemStack air = new ItemStack(Material.AIR);
        player.getInventory().clear();
        player.getInventory().setBoots(air);
        player.getInventory().setChestplate(air);
        player.getInventory().setHelmet(air);
        player.getInventory().setLeggings(air);
        player.getActivePotionEffects();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setFireTicks(0);
    }

    public void dress(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        this.clearInventory(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public void updatePlayerList(Player player) {
        boolean playerCanSeeOtherHidden = this.canSeeOthersSpectators(player);
        boolean playerIsSpect = this.isSpectator(player);
        TreeSet<Player> playersInGame = new TreeSet<>(new Utils.PlayerComparator());
        playersInGame.addAll(player.getWorld().getPlayers());

        for (Player other : playersInGame) {
            boolean otherIsSpectator = isSpectator(other);
            boolean canSeeOthersSpectators = canSeeOthersSpectators(other);

            if (playerIsSpect) {
                if (!otherIsSpectator) {
                    other.hidePlayer(player);
                } else {
                    if (canSeeOthersSpectators) {
                        other.showPlayer(player);
                    } else {
                        other.hidePlayer(player);
                    }
                }
            } else {
                other.showPlayer(player);
            }

            if (!otherIsSpectator) {
                player.showPlayer(other);
            } else {
                if (!playerIsSpect) {
                    player.hidePlayer(other);
                } else {
                    if (playerCanSeeOtherHidden) {
                        player.showPlayer(other);
                    } else {
                        player.hidePlayer(other);
                    }
                }
            }
        }

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void disguise(Player player, TeamManager.TeamId teamId) {
        List<String> armourBrand = new ArrayList<>();
        armourBrand.add(this.plugin.tm.armourBrandName);
        Color tshirtColor = this.plugin.tm.getTshirtColor(teamId);
        ChatColor teamChatColor = this.plugin.tm.getChatColor(teamId);
        String teamName = this.plugin.tm.getName(teamId);

        this.clearInventory(player);

        ItemStack tshirt = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta leatherMeta = (LeatherArmorMeta) tshirt.getItemMeta();
        leatherMeta.setColor(tshirtColor);
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        tshirt.setItemMeta(leatherMeta);

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        leatherMeta = (LeatherArmorMeta) boots.getItemMeta();
        leatherMeta.setColor(tshirtColor);
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        boots.setItemMeta(leatherMeta);

        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        leatherMeta = (LeatherArmorMeta) leggings.getItemMeta();
        leatherMeta.setColor(tshirtColor);
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        leggings.setItemMeta(leatherMeta);

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        leatherMeta = (LeatherArmorMeta) helmet.getItemMeta();
        leatherMeta.setColor(tshirtColor);
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        helmet.setItemMeta(leatherMeta);

        ItemStack vipHelmet = helmet, vipChestplate = tshirt, vipLeggings = leggings, vipBoots = boots;
        if (player.hasPermission("ctw.vip-armor")) {
            Color vipColor = (teamId == TeamManager.TeamId.BLUE) ?
                    Color.fromRGB(plugin.getConfig().getInt("vip-armors.blue.R"),
                            plugin.getConfig().getInt("vip-armors.blue.G"),
                            plugin.getConfig().getInt("vip-armors.blue.B")) :
                    Color.fromRGB(plugin.getConfig().getInt("vip-armors.red.R"),
                            plugin.getConfig().getInt("vip-armors.red.G"),
                            plugin.getConfig().getInt("vip-armors.red.B"));

            vipHelmet = new ItemStack(Material.LEATHER_HELMET);
            leatherMeta = (LeatherArmorMeta) vipHelmet.getItemMeta();
            leatherMeta.setColor(vipColor);
            leatherMeta.setDisplayName(teamChatColor + teamName);
            leatherMeta.setLore(armourBrand);
            vipHelmet.setItemMeta(leatherMeta);

            vipChestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
            leatherMeta = (LeatherArmorMeta) vipChestplate.getItemMeta();
            leatherMeta.setColor(vipColor);
            leatherMeta.setDisplayName(teamChatColor + teamName);
            leatherMeta.setLore(armourBrand);
            vipChestplate.setItemMeta(leatherMeta);

            vipLeggings = new ItemStack(Material.LEATHER_LEGGINGS);
            leatherMeta = (LeatherArmorMeta) vipLeggings.getItemMeta();
            leatherMeta.setColor(vipColor);
            leatherMeta.setDisplayName(teamChatColor + teamName);
            leatherMeta.setLore(armourBrand);
            vipLeggings.setItemMeta(leatherMeta);

            vipBoots = new ItemStack(Material.LEATHER_BOOTS);
            leatherMeta = (LeatherArmorMeta) vipBoots.getItemMeta();
            leatherMeta.setColor(vipColor);
            leatherMeta.setDisplayName(teamChatColor + teamName);
            leatherMeta.setLore(armourBrand);
            vipBoots.setItemMeta(leatherMeta);
        }

        if (player.hasPermission("ctw.vip-armor") && teamId != TeamManager.TeamId.SPECTATOR) {
            if (teamId == TeamManager.TeamId.BLUE) {
                player.getInventory().setBoots(vipBoots);
                player.getInventory().setChestplate(vipChestplate);
                player.getInventory().setLeggings(vipLeggings);
                player.getInventory().setHelmet(vipHelmet);
            } else if (teamId == TeamManager.TeamId.RED) {
                player.getInventory().setBoots(vipBoots);
                player.getInventory().setChestplate(vipChestplate);
                player.getInventory().setLeggings(vipLeggings);
                player.getInventory().setHelmet(vipHelmet);
            }
        } else {
            player.getInventory().setBoots(boots);
            player.getInventory().setChestplate(tshirt);
            player.getInventory().setLeggings(leggings);
            player.getInventory().setHelmet(helmet);
        }

        player.setGameMode(GameMode.SURVIVAL);
        player.setFireTicks(0);

        plugin.nm.updateNametag(player, teamId);

        this.updatePlayerList(player);
    }

    private void setSpectator(Player player) {
        player.setHealth(20.0);
        player.setFoodLevel(20);
        this.plugin.pm.clearInventory(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.getInventory().addItem(new ItemStack[]{this.helpBook});
        this.updatePlayerList(player);
    }
}
