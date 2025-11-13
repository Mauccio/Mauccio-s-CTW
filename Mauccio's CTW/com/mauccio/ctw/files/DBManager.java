package com.mauccio.ctw.files;

import com.mauccio.ctw.CTW;
import com.mauccio.ctw.utils.PlayerStats;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DBManager {

    private final CTW plugin;
    private Connection connection;
    private HikariDataSource dataSource;

    private final Map<String, PlayerStats> statsCache = new HashMap<>();

    public enum DBType {
        MySQL,
        SQLITE
    }

    private final DBType dbType;
    private final String host;
    private final String database;
    private final String user;
    private final String password;
    private final int port;

    public DBManager(CTW plugin, DBType dbType, String host, int port, String database, String user, String password)
            throws SQLException {
        this.plugin = plugin;
        this.dbType = dbType;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;

        if (dbType == DBType.MySQL) {
            plugin.getLogger().info("Activating MySQL engine with HikariCP.");
            setupMySQLPool();
        } else {
            plugin.getLogger().info("Activating SQLite engine.");
            SQLiteConnection();
        }

        createTables();
        if (plugin.getConfigManager().isKitSQL()) {
            createKitTables();
        }
    }

    private void setupMySQLPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
        config.setUsername(user);
        config.setPassword(password);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(600000); // 10 min
        config.setConnectionTimeout(30000); // 30s
        config.setValidationTimeout(5000);
        config.setPoolName("CTW-Pool");

        this.dataSource = new HikariDataSource(config);
    }

    private void SQLiteConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" +
                    plugin.getDataFolder().getAbsolutePath() + "/" + plugin.getName() + ".db");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dbType == DBType.MySQL) {
            return dataSource.getConnection();
        } else {
            return connection;
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }

    boolean executeUpdate(String query) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(query);
            return true;
        } catch (SQLException ex) {
            plugin.getLogger().severe(ex.getMessage());
            return false;
        }
    }

    ResultSet getResultSet(String query) {
        try {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(query);
        } catch (SQLException ex) {
            plugin.getLogger().severe(ex.getMessage());
            return null;
        }
    }

    private boolean createTables() {
        boolean ret = executeUpdate("CREATE TABLE IF NOT EXISTS scores(player_name VARCHAR(16) PRIMARY KEY, score INTEGER);");
        ret = ret && executeUpdate("CREATE TABLE IF NOT EXISTS events(event_date_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, player_name VARCHAR(16), related_player_name VARCHAR(16), description VARCHAR(256));");
        ret = ret && executeUpdate("CREATE TABLE IF NOT EXISTS kills(player_name VARCHAR(16) PRIMARY KEY, combatkills INTEGER);");
        ret = ret && executeUpdate("CREATE TABLE IF NOT EXISTS deaths(player_name VARCHAR(16) PRIMARY KEY, combatdeaths INTEGER);");
        ret = ret && executeUpdate("CREATE TABLE IF NOT EXISTS wools_captured(player_name VARCHAR(16) PRIMARY KEY, wools_placed INTEGER);");
        return ret;
    }

    public void createKitTables() {
        executeUpdate("CREATE TABLE IF NOT EXISTS player_kits (" +
                "player_uuid VARCHAR(36) PRIMARY KEY," +
                "kit_data TEXT NOT NULL);");
    }

    public boolean addEvent(String playerName, String description) {
        return addEvent(playerName, null, description);
    }

    public boolean addEvent(String playerName, String relatedPlayerName, String description) {
        String sql = "INSERT INTO events (player_name, related_player_name, description) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            if (relatedPlayerName != null) {
                ps.setString(2, relatedPlayerName);
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, description);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            plugin.getLogger().severe("Error inserting event: " + ex.getMessage());
            return false;
        }
    }
    public void incScore(String playerName, int value) {
        int newScore = getScore(playerName) + value;
        boolean ok = setScore(playerName, newScore);
        if (ok) updateStatsCache(playerName, newScore, null, null, null);
    }

    public boolean setScore(String playerName, int score) {
        boolean ok = executeUpdate("REPLACE INTO scores VALUES('" + playerName + "', " + score + ");");
        if (ok) updateStatsCache(playerName, score, null, null, null);
        return ok;
    }

    public void incWoolCaptured(String playerName, int value) {
        int newWools = getWoolCaptured(playerName) + value;
        boolean ok = setWoolsCaptured(playerName, newWools);
        if (ok) updateStatsCache(playerName, null, null, null, newWools);
    }

    protected boolean setWoolsCaptured(String playerName, int wool_captured) {
        boolean ok = executeUpdate("REPLACE INTO wools_captured VALUES('" + playerName + "', " + wool_captured + ");");
        if (ok) updateStatsCache(playerName, null, null, null, wool_captured);
        return ok;
    }

    public void incKill(String playerName, int value) {
        int newKills = getKill(playerName) + value;
        boolean ok = setCombatKills(playerName, newKills);
        if (ok) updateStatsCache(playerName, null, newKills, null, null);
    }

    protected boolean setCombatKills(String playerName, int kill) {
        boolean ok = executeUpdate("REPLACE INTO kills VALUES('" + playerName + "', " + kill + ");");
        if (ok) updateStatsCache(playerName, null, kill, null, null);
        return ok;
    }

    public void incDeath(String playerName, int value) {
        int newDeaths = getDeath(playerName) + value;
        boolean ok = setDeaths(playerName, newDeaths);
        if (ok) updateStatsCache(playerName, null, null, newDeaths, null);
    }

    protected boolean setDeaths(String playerName, int death) {
        boolean ok = executeUpdate("REPLACE INTO deaths VALUES('" + playerName + "', " + death + ");");
        if(ok) updateStatsCache(playerName, null, null, death, null);
        return ok;
    }

    public int getScore(String playerName) {
        PlayerStats cached = statsCache.get(playerName);
        if (cached != null) return cached.score;

        int result = 0;
        try (ResultSet rs = getResultSet("SELECT score FROM scores WHERE player_name='" + playerName + "';")) {
            if (rs != null && rs.next()) {
                result = rs.getInt("score");
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe(ex.getMessage());
        }
        return result;
    }

    public int getWoolCaptured(String playerName) {
        PlayerStats cached = statsCache.get(playerName);
        if (cached != null) return cached.wools;

        int result = 0;
        try (ResultSet rs = this.getResultSet("SELECT wools_placed FROM wools_captured WHERE player_name='" + playerName + "';")) {
            if (rs != null && rs.next()) {
                result = rs.getInt("wools_placed");
            }
        } catch (SQLException ex) {
            this.plugin.getLogger().severe(ex.getMessage());
        }
        return result;
    }

    public int getKill(String playerName) {
        PlayerStats cached = statsCache.get(playerName);
        if (cached != null) return cached.kills;

        int result = 0;
        try (ResultSet rs = this.getResultSet("SELECT combatkills FROM kills WHERE player_name='" + playerName + "';")) {
            if (rs != null && rs.next()) {
                result = rs.getInt("combatkills");
            }
        } catch (SQLException ex) {
            this.plugin.getLogger().severe(ex.getMessage());
        }
        return result;
    }

    public int getDeath(String playerName) {
        PlayerStats cached = statsCache.get(playerName);
        if(cached != null) return cached.deaths;

        int result = 0;
        try(ResultSet rs = this.getResultSet("SELECT combatdeaths FROM deaths WHERE player_name='" + playerName + "';")) {
            if(rs != null && rs.next()) {
                result = rs.getInt("combatdeaths");
            }
        } catch(SQLException ex) {
            plugin.getLogger().severe(ex.getMessage());
        }
        return result;
    }

    public PlayerStats getPlayerStats(String playerName) {
        PlayerStats cached = statsCache.get(playerName);
        if (cached != null) {
            return cached;
        }

        String sql = "SELECT COALESCE(s.score,0) AS score, " +
                "COALESCE(k.combatkills,0) AS kills, " +
                "COALESCE(w.wools_placed,0) AS wools, " +
                "COALESCE(d.combatdeaths,0) AS deaths " +
                "FROM scores s " +
                "LEFT JOIN kills k ON s.player_name = k.player_name " +
                "LEFT JOIN wools_captured w ON s.player_name = w.player_name " +
                "LEFT JOIN deaths d ON s.player_name = d.player_name " +
                "WHERE s.player_name = ? OR k.player_name = ? OR w.player_name = ? OR d.player_name = ? " +
                "LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.setString(2, playerName);
            ps.setString(3, playerName);
            ps.setString(4, playerName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlayerStats stats = new PlayerStats(
                            rs.getInt("score"),
                            rs.getInt("kills"),
                            rs.getInt("deaths"),
                            rs.getInt("wools")
                    );
                    statsCache.put(playerName, stats);
                    return stats;
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Error fetching stats: " + ex.getMessage());
        }

        PlayerStats zero = new PlayerStats(0, 0, 0, 0);
        statsCache.put(playerName, zero);
        return zero;
    }

    private void updateStatsCache(String playerName, Integer score, Integer kills, Integer deaths, Integer wools) {
        PlayerStats current = statsCache.getOrDefault(playerName, new PlayerStats(0, 0, 0, 0));
        int newScore = (score != null ? score : current.score);
        int newKills = (kills != null ? kills : current.kills);
        int newDeaths = (deaths != null ? deaths : current.deaths);
        int newWools = (wools != null ? wools : current.wools);
        statsCache.put(playerName, new PlayerStats(newScore, newKills, newDeaths, newWools));
    }

    public void clearStatsCache(String playerName) {
        statsCache.remove(playerName);
    }

    public void clearAllStatsCache() {
        statsCache.clear();
    }

    protected TreeMap<String, Integer> getScores() {
        TreeMap<String, Integer> results = new TreeMap<>();
        try (ResultSet rs = getResultSet("SELECT player_name, score FROM scores;")) {
            while (rs != null && rs.next()) {
                results.put(rs.getString("player_name"), rs.getInt("score"));
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe(ex.getMessage());
        }
        return results;
    }
}
