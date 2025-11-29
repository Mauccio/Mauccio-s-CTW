package com.mauccio.ctw;

import com.mauccio.ctw.files.*;
import com.mauccio.ctw.game.*;
import com.mauccio.ctw.map.*;
import com.mauccio.ctw.listeners.*;
import com.mauccio.ctw.commands.*;
import com.mauccio.ctw.lobby.LobbyManager;
import com.mauccio.ctw.game.NametagManager;
import com.mauccio.ctw.utils.PlaceholderCTW;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

public class CTW extends JavaPlugin {

    public static class Scores {
        public int death;
        public int kill;
        public int capture;
        public int coins_capture;
        public int coins_kill;
    }

    private LangManager lm;
    private MapManager mm;
    private TeamManager tm;
    private PlayerManager pm;
    private SignManager sm;
    private ConfigManager cf;
    private DBManager db;
    private GameManager gm;
    private CommandManager cm;
    private RoomManager rm;
    private WorldManager wm;
    private EventManager em;
    private Scores scores;
    private KitManager km;
    private LobbyManager lb;
    private NametagManager nm;
    private SoundManager so;
    private TitleManager ts;
    private Economy econ;

    @Override
    public void onEnable() {
        getLogger().info("Enabling...");
        this.lm = new LangManager(this);
        WorldEditPlugin we = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        if (we == null) {
            alert(lm.getText("we-not-enabled"));
            return;
        }

        setupEconomy();
        setupAudio();

        this.cf = new ConfigManager(this);
        this.wm = new WorldManager(this);
        this.tm = new TeamManager(this);
        this.pm = new PlayerManager(this);
        this.removeAllItems();
        this.cm = new CommandManager(this);
        this.em = new EventManager(this);
        this.mm = new MapManager(this);
        this.rm = new RoomManager(this);
        this.nm = new NametagManager(this, tm);
        this.gm = new GameManager(this);
        this.rm.init();
        this.sm = new SignManager(this);
        this.km = new KitManager(this);
        this.lb = new LobbyManager(this);
        this.so = new SoundManager(this);
        so.ensureNbsExtractedAndScanned();
        this.ts = new TitleManager(this);

        scores = new Scores();

        File statsFile = new File(getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            saveResource("stats.yml", true);
        }

        YamlConfiguration stats = new YamlConfiguration();
        try {
            stats.load(statsFile);
            if (stats.getBoolean("enable")) {
                String host = stats.getString("database.host");
                String database = stats.getString("database.name");
                int port = stats.getInt("database.port");
                String user = stats.getString("database.user");
                String password = stats.getString("database.pass");
                if (stats.getString("database.type").equalsIgnoreCase("mysql")) {
                    db = new DBManager(this, DBManager.DBType.MySQL, host, port, database, user, password);
                } else {
                    db = new DBManager(this, DBManager.DBType.SQLITE, null, 0, null, null, null);
                }
                scores.capture = stats.getInt("scores.capture");
                scores.kill = stats.getInt("scores.kill");
                scores.death = stats.getInt("scores.death");
                scores.coins_capture = stats.getInt("coins.capture");
                scores.coins_kill = stats.getInt("coins.kill");
            }
        } catch (IOException | InvalidConfigurationException |
                 SQLException ex) {
            alert(ex.getMessage());
            db = null;
        }

        if(db != null) {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                new PlaceholderCTW(this).register();
            }
        }



        lm.updateChecker();
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        getLogger().info("Disabling...");
        save();
        moveAllToLobby();
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            alert(lm.getText("vault-not-enabled"));
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            alert(lm.getText("economy-addon-not-found"));
            return;
        }
        econ = rsp.getProvider();
        String msg = lm.getText("economy-addon-connected").replace("%ADDON%", econ.getName());
        alert(msg);
    }

    private void setupAudio() {
        if(getServer().getPluginManager().getPlugin("NoteBlockAPI") == null) {
            alert(lm.getText("nb-not-enabled"));
            return;
        }
        String msg = lm.getText("nb-detected");
        alert(msg);
    }

    public Economy getEconomy() {
        return econ;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return wm.getEmptyWorldGenerator();
    }

    public boolean hasPermission(Player player, String permission) {
        return player.hasPermission("ctw." + permission);
    }

    public void alert(String message) {
        String prefix = lm.getText("alert-prefix");
        String prefixedMessage = prefix + message;
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
        lm.reload();
        km.reloadKits();
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

    public MapManager getMapManager() {
        return mm;
    }

    public TeamManager getTeamManager() {
        return tm;
    }

    public PlayerManager getPlayerManager() {
        return pm;
    }

    public ConfigManager getConfigManager() {
        return cf;
    }

    public GameManager getGameManager() {
        return gm;
    }

    public WorldManager getWorldManager() {
        return wm;
    }

    public DBManager getDBManager() {
        return db;
    }

    public RoomManager getRoomManager() {
        return rm;
    }

    public KitManager getKitManager() {
        return km;
    }

    public LobbyManager getLobbyManager() {
        return lb;
    }

    public NametagManager getNametagManager() {
        return nm;
    }

    public LangManager getLangManager() {
        return lm;
    }

    public SignManager getSignManager() {
        return sm;
    }

    public CommandManager getCommandManager() {
        return cm;
    }

    public EventManager getEventManager() {
        return em;
    }

    public SoundManager getSoundManager() {
        return so;
    }

    public TitleManager getTitleManager () {
        return ts;
    }

    public Scores getScores() {
        return scores;
    }
}
