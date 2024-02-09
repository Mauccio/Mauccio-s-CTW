package com.mauccio.ctw.files;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TreeMap;

import com.mauccio.ctw.CTW;
import org.bukkit.configuration.ConfigurationSection;

public class DBManager {

    private final CTW plugin;
    private Connection connection;

    public enum DBType {

        MySQL,
        SQLITE
    }

    public DBManager(CTW plugin, DBType dbType, String database, String user, String password)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
        this.plugin = plugin;
        if (dbType == DBType.MySQL) {
            plugin.getLogger().info("Activating MySQL engine.");
            MySQLConnection(database, user, password);
        } else {
            plugin.getLogger().info("Activating SQLite engine.");
            SQLiteConnection();
        }
        createTables();
    }

    private void MySQLConnection(String database, String user, String password)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + database,
                user, password);
    }

    private void SQLiteConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:"
                + plugin.getDataFolder().getAbsolutePath()
                + "/" + plugin.getName() + ".db");
    }

    private boolean executeUpdate(String query) {
        Statement statement;
        boolean results = false;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(query);
            statement.close();
            results = true;
        } catch (SQLException ex) {
            plugin.getLogger().severe(ex.getMessage());
        }
        return results;
    }

    private ResultSet getResultSet(String query) {
        Statement statement;
        ResultSet results = null;
        try {
            statement = connection.createStatement();
            results = statement.executeQuery(query);
        } catch (SQLException ex) {
            plugin.getLogger().severe(ex.getMessage());
        }
        return results;
    }

    private boolean createTables() {
        boolean ret = executeUpdate("CREATE TABLE IF NOT EXISTS scores(player_name VARCHAR(16) PRIMARY KEY, score INTEGER);");
        ret = ret && executeUpdate("CREATE TABLE IF NOT EXISTS events(event_date_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, player_name VARCHAR(16), related_player_name VARCHAR(16), description VARCHAR(256));");
        return ret;
    }

    public boolean incScore(String playerName, int value) {
        return setScore(playerName, getScore(playerName) + value);
    }

    public boolean setScore(String playerName, int score) {
        return executeUpdate("REPLACE INTO scores VALUES('" + playerName + "', " + score + ");");
    }

    public boolean addEvent(String playerName, String description) {
        return addEvent(playerName, null, description);
    }

    public boolean addEvent(String playerName, String relatedPlayerName, String description) {

        return executeUpdate("INSERT INTO events VALUES( null, '" + playerName + "', "
                + (relatedPlayerName == null ? "null," : "'" + relatedPlayerName + "', ")
                + "'" + description + "'"
                + ");");
    }

    public int getScore(String playerName) {
        int result = 0;
        ResultSet rs = getResultSet("SELECT score FROM scores WHERE player_name='" + playerName + "';");
        if (rs != null) {
            try {
                if (rs.next()) {
                    result = rs.getInt("score");
                }
                rs.close();
            } catch (SQLException ex) {
                plugin.getLogger().severe(ex.getMessage());
            }
        }
        return result;
    }

    protected TreeMap<String, Integer> getScores() {
        TreeMap<String, Integer> results = new TreeMap<>();
        ResultSet rs = getResultSet("SELECT player_name, score FROM scores;");
        try {
            while (rs.next()) {
                results.put(rs.getString("player_name"), rs.getInt("score"));
            }
            rs.close();
        } catch (SQLException ex) {
            plugin.getLogger().severe(ex.getMessage());
        }
        return results;
    }

    protected boolean setWoolsCaptured(String playerName, int wool_captured) {
        return this.executeUpdate("REPLACE INTO wools_captured VALUES('" + playerName + "', " + wool_captured + ");");
    }

    public boolean incWoolCaptured(String playerName, int value) {
        return this.setWoolsCaptured(playerName, this.getWoolCaptured(playerName) + value);
    }

    public int getWoolCaptured(String playerName) {
        int result = 0;
        ResultSet rs = this.getResultSet("SELECT wools_placed FROM wools_captured WHERE player_name='" + playerName + "';");
        if (rs != null) {
            try {
                if (rs.next()) {
                    result = rs.getInt("wools_placed");
                }
                rs.close();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().severe(ex.getMessage());
            }
        }
        return result;
    }

    protected boolean incKill(String playerName, int value) {
        return this.setCombatKills(playerName, this.getKill(playerName) + value);
    }

    protected boolean setCombatKills(String playerName, int kill) {
        return this.executeUpdate("REPLACE INTO kills VALUES('" + playerName + "', " + kill + ");");
    }

    public int getKill(String playerName) {
        int result = 0;
        ResultSet rs = this.getResultSet("SELECT combatkills FROM kills WHERE player_name='" + playerName + "';");
        if (rs != null) {
            try {
                if (rs.next()) {
                    result = rs.getInt("combatkills");
                }
                rs.close();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().severe(ex.getMessage());
            }
        }
        return result;
    }

}
