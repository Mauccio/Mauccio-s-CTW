package com.mauccio.ctw;

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
import com.nametagedit.plugin.NametagEdit;


public class PlayerManager {

    private class PlayerOptions {

        boolean viewOthersSpectators;
        boolean viewOthersDeathMessages;
		boolean viewBlood;
    }

    private final Main plugin;
    private final File playersFile;
    private final YamlConfiguration playersConfig;
    private final TreeMap<String, PlayerOptions> playerOptions;
    private final TreeMap<String, TeamManager.TeamId> playerTeam;
    private final ReentrantLock _playerTeam_mutex;
    private final ItemStack helpBook;
    private final ItemStack joinMenuItem;
    private final TreeMap<String, Map.Entry<Long, String>> lastDamager;
    private final TreeSet<String> falseSpectators;
    
    
    public PlayerManager(Main plugin) {
        playersFile = new File(plugin.getDataFolder(), "players.yml");
        playersConfig = new YamlConfiguration();
        this.plugin = plugin;
        playerOptions = new TreeMap<>();
        lastDamager = new TreeMap<>();
        playerTeam = new TreeMap<>();
        _playerTeam_mutex = new ReentrantLock(true);
        helpBook = plugin.lm.getHelpBook();
        ItemStack menuItem = new ItemStack(Material.PAPER);
        ItemMeta im = menuItem.getItemMeta();
        im.setDisplayName(plugin.lm.getText("help-menu-item.title"));
        menuItem.setItemMeta(im);
        joinMenuItem = menuItem;
        falseSpectators = new TreeSet<>();
    }
    
    public void setFalseSpectator(Player player) {
        _playerTeam_mutex.lock();
        try {
            falseSpectators.add(player.getName());
        } finally {
            _playerTeam_mutex.unlock();
        }
    }

    public void setLastDamager(Player player, Player damager) {
        Map.Entry<Long, String> entry = new AbstractMap.SimpleEntry<>(new Date().getTime(), damager.getName());
        lastDamager.put(player.getName(), entry);
    }

    public String getLastDamager(Player player) {
        String ret = null;
        Map.Entry<Long, String> entry = lastDamager.remove(player.getName());
        if (entry != null) {
            long upTo = new Date().getTime() - (1000 * 10);
            if (entry.getKey() > upTo) {
                ret = entry.getValue();
            }
        }
        return ret;
    }

    public ItemStack getMenuItem() {
        return joinMenuItem;
    }

    public void load() {
        try {
            playersConfig.load(playersFile);
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.getLogger().severe(ex.toString());
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
        PlayerOptions po = playerOptions.get(player.getName());
        if (po != null) {
            po.viewOthersSpectators = !po.viewOthersSpectators;
        } else {
            po = new PlayerOptions();
            po.viewOthersSpectators = !canSeeOthersSpectators(player);
        }
        playerOptions.put(player.getName(), po);
        updatePlayerList(player);
        return po.viewOthersSpectators;
    }

    public boolean toogleOthersDeathMessages(Player player) {
        PlayerOptions po = playerOptions.get(player.getName());
        if (po != null) {
            po.viewOthersDeathMessages = !po.viewOthersDeathMessages;
        } else {
            po = new PlayerOptions();
            po.viewOthersDeathMessages = !canSeeOthersDeathMessages(player);
        }
        playerOptions.put(player.getName(), po);
        return po.viewOthersDeathMessages;
    }

    public boolean toggleBloodEffect(Player player) {
    	PlayerOptions po = playerOptions.get(player.getName());
    	if(po != null) {
    		po.viewBlood = !po.viewBlood;
    	} else {
    		po = new PlayerOptions();
    		po.viewBlood = !canSeeBloodEffect(player);
    	}
    	playerOptions.put(player.getName(), po);
    	return po.viewBlood;
    }
    
    public boolean canSeeBloodEffect(Player player) {
    	PlayerOptions po = playerOptions.get(player.getName());
    	return po == null || po.viewBlood;
    }
    
    public boolean canSeeOthersSpectators(Player player) {
        PlayerOptions po = playerOptions.get(player.getName());
        return po == null || po.viewOthersSpectators;
    }

    public boolean canSeeOthersDeathMessages(Player player) {
        PlayerOptions po = playerOptions.get(player.getName());
        return po == null || po.viewOthersDeathMessages;
    }

    public ChatColor getChatColor(Player player) {
        ChatColor cc = ChatColor.WHITE;
        TeamManager.TeamId teamId = playerTeam.get(player.getName());
        if (teamId != null) {
            cc = plugin.tm.getChatColor(teamId);
        }
        return cc;
    }

	public void addPlayerTo(Player player, TeamManager.TeamId teamId) {
        _playerTeam_mutex.lock();
        falseSpectators.remove(player.getName());
        try {
            TeamManager.TeamId previousTeam = playerTeam.put(player.getName(), teamId);
            if (previousTeam != null) {
                plugin.tm.removeFromTeam(player, previousTeam);
                
            }
            plugin.tm.addToTeam(player, teamId);
            clearInventory(player);
            if (teamId != TeamManager.TeamId.SPECTATOR) {
                disguise(player, teamId);
            } else {
                setSpectator(player);
            }
            updatePlayerList(player);
            player.sendMessage(plugin.lm.getMessage("moved-to-" + teamId.name().toLowerCase()));
           
        } finally {
            _playerTeam_mutex.unlock();
        }
    }

    public TeamManager.TeamId clearTeam(Player player) {
        TeamManager.TeamId teamId;
        _playerTeam_mutex.lock();
        falseSpectators.remove(player.getName());
        try {
            clearInventory(player);
            dress(player);
            teamId = playerTeam.remove(player.getName());
            plugin.tm.removeFromTeam(player, teamId);
        } finally {
            _playerTeam_mutex.unlock();
        }
        return teamId;
    }

    public TeamManager.TeamId getTeamId(Player player) {
        return playerTeam.get(player.getName());
    }

    public boolean isSpectator(Player player) {
        boolean resp = false;
        TeamManager.TeamId teamId = playerTeam.get(player.getName());
        if (teamId != null) {
            if (teamId != TeamManager.TeamId.SPECTATOR) {
                if (falseSpectators.contains(player.getName())) {
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
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setFireTicks(0);
    }

    
	public void dress(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        clearInventory(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public void updatePlayerList(Player player) {
        boolean playerCanSeeOtherHidden = canSeeOthersSpectators(player);
        boolean playerIsSpect = isSpectator(player);

        TreeSet<Player> playersInGame = new TreeSet<>(new Tools.PlayerComparator());
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

        LeatherArmorMeta leatherMeta;
        List<String> armourBrand = new ArrayList<>();
        armourBrand.add(plugin.tm.armourBrandName);

        Color tshirtColor = plugin.tm.getTshirtColor(teamId);
        ChatColor teamChatColor = plugin.tm.getChatColor(teamId);
        String teamName = plugin.tm.getName(teamId);

        clearInventory(player);

        ItemStack tshirt = new ItemStack(Material.LEATHER_CHESTPLATE);
        leatherMeta = (LeatherArmorMeta) tshirt.getItemMeta();
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
        
        ItemStack VipBlueHelmet = new ItemStack(Material.LEATHER_HELMET);
        leatherMeta = (LeatherArmorMeta) helmet.getItemMeta();
        leatherMeta.setColor(Color.fromRGB(plugin.getConfig().getInt("vip-armors.blue.R"), plugin.getConfig().getInt("vip-armors.blue.G"), plugin.getConfig().getInt("vip-armors.blue.B")));
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        VipBlueHelmet.setItemMeta(leatherMeta);
        
        ItemStack VipBlueChestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        leatherMeta = (LeatherArmorMeta) helmet.getItemMeta();
        leatherMeta.setColor(Color.fromRGB(plugin.getConfig().getInt("vip-armors.blue.R"), plugin.getConfig().getInt("vip-armors.blue.G"), plugin.getConfig().getInt("vip-armors.blue.B")));
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        VipBlueChestplate.setItemMeta(leatherMeta);
        
        ItemStack VipBlueLeggings = new ItemStack(Material.LEATHER_LEGGINGS);
        leatherMeta = (LeatherArmorMeta) helmet.getItemMeta();
        leatherMeta.setColor(Color.fromRGB(plugin.getConfig().getInt("vip-armors.blue.R"), plugin.getConfig().getInt("vip-armors.blue.G"), plugin.getConfig().getInt("vip-armors.blue.B")));
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        VipBlueLeggings.setItemMeta(leatherMeta);
        
        ItemStack VipBlueBoots = new ItemStack(Material.LEATHER_BOOTS);
        leatherMeta = (LeatherArmorMeta) helmet.getItemMeta();
        leatherMeta.setColor(Color.fromRGB(plugin.getConfig().getInt("vip-armors.blue.R"), plugin.getConfig().getInt("vip-armors.blue.G"), plugin.getConfig().getInt("vip-armors.blue.B")));
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        VipBlueBoots.setItemMeta(leatherMeta);
        
        ItemStack VipRedHelmet = new ItemStack(Material.LEATHER_HELMET);
        leatherMeta = (LeatherArmorMeta) helmet.getItemMeta();
        leatherMeta.setColor(Color.fromRGB(plugin.getConfig().getInt("vip-armors.red.R"), plugin.getConfig().getInt("vip-armor.red.G"), plugin.getConfig().getInt("vip-armor.red.B")));
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        VipRedHelmet.setItemMeta(leatherMeta);
        
        ItemStack VipRedChestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        leatherMeta = (LeatherArmorMeta) helmet.getItemMeta();
        leatherMeta.setColor(Color.fromRGB(plugin.getConfig().getInt("vip-armors.red.R"), plugin.getConfig().getInt("vip-armor.red.G"), plugin.getConfig().getInt("vip-armor.red.B")));
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        VipRedChestplate.setItemMeta(leatherMeta);
        
        ItemStack VipRedLeggings = new ItemStack(Material.LEATHER_LEGGINGS);
        leatherMeta = (LeatherArmorMeta) helmet.getItemMeta();
        leatherMeta.setColor(Color.fromRGB(plugin.getConfig().getInt("vip-armors.red.R"), plugin.getConfig().getInt("vip-armor.red.G"), plugin.getConfig().getInt("vip-armor.red.B")));
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        VipRedLeggings.setItemMeta(leatherMeta);
        
        ItemStack VipRedBoots = new ItemStack(Material.LEATHER_BOOTS);
        leatherMeta = (LeatherArmorMeta) helmet.getItemMeta();
        leatherMeta.setColor(Color.fromRGB(plugin.getConfig().getInt("vip-armors.red.R"), plugin.getConfig().getInt("vip-armor.red.G"), plugin.getConfig().getInt("vip-armor.red.B")));
        leatherMeta.setDisplayName(teamChatColor + teamName);
        leatherMeta.setLore(armourBrand);
        VipRedBoots.setItemMeta(leatherMeta);
        

        if(player.hasPermission("vip-armor")) {
        	if(plugin.pm.getTeamId(player) == TeamManager.TeamId.BLUE) {
        		player.setPlayerListName(teamChatColor+""+player.getName());
        		NametagEdit.getApi().setPrefix(player, teamChatColor+"");
                player.setCustomName(teamChatColor +""+player.getName());
                player.setCustomNameVisible(true);
                player.getInventory().setBoots(VipBlueBoots);
                player.getInventory().setChestplate(VipBlueChestplate);
                player.getInventory().setLeggings(VipBlueLeggings);
                player.getInventory().setHelmet(VipBlueHelmet);
                player.setGameMode(GameMode.SURVIVAL);
                player.setFireTicks(0);
        	} else if(plugin.pm.getTeamId(player) == TeamManager.TeamId.RED) {
        		player.setPlayerListName(teamChatColor+""+player.getName());
        		NametagEdit.getApi().setPrefix(player, teamChatColor+"");
                player.setCustomName(teamChatColor +""+player.getName());
                player.setCustomNameVisible(true);
                player.getInventory().setBoots(VipRedBoots);
                player.getInventory().setChestplate(VipRedChestplate);
                player.getInventory().setLeggings(VipRedLeggings);
                player.getInventory().setHelmet(VipRedHelmet);
                player.setGameMode(GameMode.SURVIVAL);
                player.setFireTicks(0);
        	} else {
        		
        	}
        } else {
        	player.setPlayerListName(teamChatColor+""+player.getName());
            NametagEdit.getApi().setPrefix(player, teamChatColor+"");
            player.setCustomName(teamChatColor +""+player.getName());
            player.setCustomNameVisible(true);
            player.getInventory().setBoots(boots);
            player.getInventory().setChestplate(tshirt);
            player.getInventory().setLeggings(leggings);
            player.getInventory().setHelmet(helmet);
            player.setGameMode(GameMode.SURVIVAL);
            player.setFireTicks(0);
        }

        updatePlayerList(player);

        try {
        	NametagEdit.getApi().reloadNametag(player);
        } catch (Exception ex) {
            plugin.getLogger().warning(ex.toString());
        } 
    }

	private void setSpectator(Player player) { 
        player.setHealth(20);
        player.setFoodLevel(20);
        plugin.pm.clearInventory(player);
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.getInventory().addItem(helpBook);
        // player.getInventory().addItem(joinMenuItem);
        updatePlayerList(player);
    }
}
