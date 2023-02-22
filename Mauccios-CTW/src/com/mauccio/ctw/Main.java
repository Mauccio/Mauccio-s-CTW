package com.mauccio.ctw;

import org.bukkit.plugin.java.*;
import net.milkbowl.vault.economy.*;
import com.sk89q.worldedit.bukkit.*;
import org.bukkit.configuration.file.*;
import java.io.*;
import org.bukkit.configuration.*;
import java.sql.*;

import org.bukkit.plugin.*;
import org.bukkit.generator.*;
import org.bukkit.command.*;
import org.bukkit.*;
import org.bukkit.entity.*;

public class Main extends JavaPlugin {
	
    public static Economy econ;
    WorldManager wm;
    CommandManager cm;
    EventManager em;
    TeamManager tm;
    LangManager lm;
    MapManager mm;
    WorldEditPlugin we;
    RoomManager rm;
    SignManager sm;
    GameManager gm;
    ConfigManager cf;
    PlayerManager pm;
    DBManager db;
    Scores scores;
    
    static {
        Main.econ = null;
    }
    
    
    
    public void onEnable() {
        this.lm = new LangManager(this);
        this.we = (WorldEditPlugin)this.getServer().getPluginManager().getPlugin("WorldEdit");
        if (this.we == null) {
            this.alert(this.lm.getText("we-not-enabled"));
            return;
        }
        try {
            Class.forName("com.nametagedit.plugin.NametagEdit");
        }
        catch (ClassNotFoundException ex2) {
            this.alert(this.lm.getText("ta-not-enabled"));
            return;
        }
        this.cf = new ConfigManager(this);
        this.wm = new WorldManager(this);
        this.tm = new TeamManager(this);
        this.pm = new PlayerManager(this);
        this.removeAllItems();
        this.cm = new CommandManager(this);
        this.em = new EventManager(this);
        this.mm = new MapManager(this);
        this.rm = new RoomManager(this);
        this.gm = new GameManager(this);
        this.rm.init();
        this.sm = new SignManager(this);
        if (!this.setupEconomy()) {
            Bukkit.getConsoleSender().sendMessage(String.format("Vault not found, disabling...", this.getDescription().getName()));
            this.getServer().getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        this.saveDefaultConfig();
        this.scores = new Scores();
        final File statsFile = new File(this.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            this.saveResource("stats.yml", true);
        }
        final YamlConfiguration stats = new YamlConfiguration();
        try {
            stats.load(statsFile);
            if (stats.getBoolean("enable")) {
                final String database = stats.getString("database.name");
                final String user = stats.getString("database.user");
                final String password = stats.getString("database.pass");
                if (stats.getString("database.type").equalsIgnoreCase("mysql")) {
                    this.db = new DBManager(this, DBManager.DBType.MySQL, database, user, password);
                }
                else {
                    this.db = new DBManager(this, DBManager.DBType.SQLITE, null, null, null);
                }
                this.scores.capture = stats.getInt("scores.capture");
                this.scores.kill = stats.getInt("scores.kill");
                this.scores.death = stats.getInt("scores.death");
            }
        }
        catch (IOException | InvalidConfigurationException | ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex4) {
            this.alert(ex4.getMessage());
            this.db = null;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPILCTWR(this).register();
        }
    }
    
    public void reload() {
        this.cf.load();
        this.wm.load();
        this.mm.load();
        this.rm.load();
        this.sm.load();
    }
    
    public void save() {
        if (this.wm != null) {
            this.wm.persist();
        }
        if (this.mm != null) {
            this.mm.persist();
        }
        if (this.rm != null) {
            this.rm.persist();
        }
        if (this.sm != null) {
            this.sm.persists();
        }
    }
    
    public void onDisable() {
        this.save();
        this.moveAllToLobby();
    }
    
    public boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        Main.econ = (Economy)rsp.getProvider();
        return Main.econ != null;
    }
    
    public Economy getEconomy() {
        return Main.econ;
    }
    
    public ChunkGenerator getDefaultWorldGenerator(final String worldName, final String id) {
        return this.wm.getEmptyWorldGenerator();
    }
    
    public boolean hasPermission(final Player player, final String permission) {
        return player.hasPermission("ctw." + permission);
    }
    
    public ConsoleCommandSender getConsole() {
        return this.getServer().getConsoleSender();
    }
    
    public void alert(final String message) {
        final String prefix = ChatColor.GRAY + "[" + ChatColor.DARK_RED + ChatColor.BOLD + this.getName() + ChatColor.GRAY + "]";
        final String prefixedMessage = String.valueOf(prefix) + " " + ChatColor.RED + "(Alert!) " + message;
        this.getServer().getConsoleSender().sendMessage(prefixedMessage);
        for (final Player player : this.getServer().getOnlinePlayers()) {
            if (this.hasPermission(player, "receive-alerts")) {
                player.sendMessage(prefixedMessage);
            }
        }
    }
    
    public void moveAllToLobby() {
        for (final Player player : this.getServer().getOnlinePlayers()) {
            if (this.rm.isInGame(player.getWorld())) {
                this.pm.dress(player);
                player.teleport(this.wm.getNextLobbySpawn());
            }
        }
    }
    
    public void removeAllItems() {
        for (final World world : this.getServer().getWorlds()) {
            for (final Entity entity : world.getEntities()) {
                if (entity.getType() != EntityType.PLAYER && entity.getType() != EntityType.ITEM_FRAME && entity.getType() != EntityType.UNKNOWN) {
                    entity.remove();
                }
            }
        }
    }
    
    public class Scores
    {
        int death;
        int kill;
        int capture;
    }
}
// Mauccio's CTW
