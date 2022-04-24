package me.libelula.capturethewool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TreeMap;

public class DBManager {

    private final Main plugin;
    private Connection connection;

    public enum DBType {
        MySQL,
        SQLITE
    }

    public DBManager(Main plugin, DBType dbType, String database, String user, String password)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
        this.plugin = plugin;
        if (dbType == DBType.MySQL) {
            plugin.getLogger().info("Initializing MySQL engine");
            MySQLConnection(database, user, password);
        } else {
            plugin.getLogger().info("Initializing SQLite engine...");
            SQLiteConnection();
        }
        createTables();
    }

	private void MySQLConnection(String database, String user, String password)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        connection = DriverManager.getConnection("jdbc:mysql://:3306/" + database,
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
        ret = ret && executeUpdate("CREATE TABLE IF NOT EXISTS kills(player_name VARCHAR(16) PRIMARY KEY, combatkills INTEGER);");
        ret = ret && executeUpdate("CREATE TABLE IF NOT EXISTS wools_captured(player_name VARCHAR(16) PRIMARY KEY, wools_placed INTEGER);");
        return ret;
    }

    protected boolean incScore(String playerName, int value) {
        return setScore(playerName, getScore(playerName) + value);
    }
    
    protected boolean incKill(String playerName, int value) {
        return setCombatKills(playerName, getKill(playerName) + value);
    }
    
    protected boolean incWoolCaptured(String playerName, int value) {
        return setWoolsCaptured(playerName, getWoolCaptured(playerName) + value);
    }
    
    protected boolean setScore(String playerName, int score) {
        return executeUpdate("REPLACE INTO scores VALUES('" + playerName + "', " + score + ");");
    }
    
    protected boolean setCombatKills(String playerName, int kill) {
    	return executeUpdate("REPLACE INTO kills VALUES('" + playerName + "', " + kill + ");");
    }
    
    protected boolean setWoolsCaptured(String playerName, int wool_captured) {
    	return executeUpdate("REPLACE INTO wools_captured VALUES('" + playerName + "', " + wool_captured + ");");
    }

    protected boolean addEvent(String playerName, String description) {
        return addEvent(playerName, null, description);
    }

    protected boolean addEvent(String playerName, String relatedPlayerName, String description) {

        return executeUpdate("INSERT INTO events VALUES( null, '" + playerName + "', "
                + (relatedPlayerName == null ? "null," : "'" + relatedPlayerName + "', ")
                + "'" + description + "'"
                + ");");
    }

    protected int getScore(String playerName) {
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
    
    protected int getKill(String playerName) {
        int result = 0;
        ResultSet rs = getResultSet("SELECT combatkills FROM kills WHERE player_name='" + playerName + "';");
        if (rs != null) {
            try {
                if (rs.next()) {
                    result = rs.getInt("combatkills");
                }
                rs.close();
            } catch (SQLException ex) {
                plugin.getLogger().severe(ex.getMessage());
            }
        }
        return result;
    }
    
    protected int getWoolCaptured(String playerName) {
        int result = 0;
        ResultSet rs = getResultSet("SELECT wools_placed FROM wools_captured WHERE player_name='" + playerName + "';");
        if (rs != null) {
            try {
                if (rs.next()) {
                    result = rs.getInt("wools_placed");
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
    
    protected TreeMap<String, Integer> getKills() {
        TreeMap<String, Integer> results = new TreeMap<>();
        ResultSet rs = getResultSet("SELECT player_name, combatkills FROM kills;");
        try {
            while (rs.next()) {
                results.put(rs.getString("player_name"), rs.getInt("combatkills"));
            }
            rs.close();
        } catch (SQLException ex) {
            plugin.getLogger().severe(ex.getMessage());
        }
        return results;
    }
    
    protected TreeMap<String, Integer> getWoolsCaptured() {
        TreeMap<String, Integer> results = new TreeMap<>();
        ResultSet rs = getResultSet("SELECT player_name, wools_placed FROM wools_captured;");
        try {
            while (rs.next()) {
                results.put(rs.getString("player_name"), rs.getInt("wools_placed"));
            }
            rs.close();
        } catch (SQLException ex) {
            plugin.getLogger().severe(ex.getMessage());
        }
        return results;
    }
}