// Recovered v1.3

package com.mauccio.ctw;

import com.mauccio.ctw.files.*;
import com.mauccio.ctw.game.*;
import com.mauccio.ctw.map.*;
import com.mauccio.ctw.listeners.*;
import com.mauccio.ctw.commands.*;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class CTW extends JavaPlugin {

    public static class Scores {

        public int death;
        public int kill;
        public int capture;
    }

    public LangManager lm;
    public MapManager mm;
    public TeamManager tm;
    public PlayerManager pm;
    public SignManager sm;
    public ConfigManager cf;
    public DBManager db;
    public GameManager gm;
    public CommandManager cm;
    public RoomManager rm;
    public WorldManager wm;
    public EventManager em;
    public WorldEditPlugin we;
    public Scores scores;

    @Override
    public void onEnable() {
        getLogger().info("Plugin enabled");
        this.lm = new LangManager(this);
        we = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        if (we == null) {
            alert(lm.getText("we-not-enabled"));
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
        this.tm = new TeamManager(this); //
        this.pm = new PlayerManager(this);
        this.removeAllItems();
        this.cm = new CommandManager(this);
        this.em = new EventManager(this);
        this.mm = new MapManager(this);
        this.rm = new RoomManager(this);
        this.gm = new GameManager(this);
        this.rm.init();
        this.sm = new SignManager(this);

        scores = new Scores();

        File statsFile = new File(getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            saveResource("stats.yml", true);
        }

        YamlConfiguration stats = new YamlConfiguration();
        try {
            stats.load(statsFile);
            if (stats.getBoolean("enable")) {
                String database = stats.getString("database.name");
                String user = stats.getString("database.user");
                String password = stats.getString("database.pass");
                if (stats.getString("database.type").equalsIgnoreCase("mysql")) {
                    db = new DBManager(this, DBManager.DBType.MySQL, database, user, password);
                } else {
                    db = new DBManager(this, DBManager.DBType.SQLITE, null, null, null);
                }
                scores.capture = stats.getInt("scores.capture");
                scores.kill = stats.getInt("scores.kill");
                scores.death = stats.getInt("scores.death");
            }
        } catch (IOException | InvalidConfigurationException | ClassNotFoundException | InstantiationException | IllegalAccessException |
                 SQLException ex) {
            alert(ex.getMessage());
            db = null;
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled");
        save();
        moveAllToLobby();
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return wm.getEmptyWorldGenerator();
    }

    public boolean hasPermission(Player player, String permission) {
        return player.hasPermission("ctw." + permission);
    }

    public void alert(String message) {
        String prefix = ChatColor.YELLOW + "["
                + ChatColor.GOLD + ChatColor.BOLD + this.getName()
                + ChatColor.YELLOW + "]";
        String prefixedMessage = prefix + " " + ChatColor.RED + "(alert) " + message;
        getServer().getConsoleSender().sendMessage(prefixedMessage);
        for (Player player : getServer().getOnlinePlayers()) {
            if (hasPermission(player, "receive-alerts")) {
                player.sendMessage(prefixedMessage);
            }
        }
    }

    public void removeAllItems() {
        for (World world : getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getType() != EntityType.PLAYER
                        && entity.getType() != EntityType.ITEM_FRAME
                        && entity.getType() != EntityType.UNKNOWN) {
                    entity.remove();
                }
            }
        }
    }

    public void moveAllToLobby() {
        for (Player player : getServer().getOnlinePlayers()) {
            if (rm.isInGame(player.getWorld())) {
                pm.dress(player);
                player.teleport(wm.getNextLobbySpawn());
            }
        }
    }

    public void reload() {
        cf.load();
        wm.load();
        mm.load();
        rm.load();
        sm.load();
    }

    public void save() {
        if (wm != null) {
            wm.persist();
        }

        if (mm != null) {
            mm.persist();
        }

        if (rm != null) {
            rm.persist();
        }

        if (sm != null) {
            sm.persists();
        }
    }
}
