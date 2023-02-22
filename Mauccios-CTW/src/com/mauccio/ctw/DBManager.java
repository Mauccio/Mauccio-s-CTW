package com.mauccio.ctw;

import java.sql.*;
import java.util.*;

public class DBManager
{
    private final Main plugin;
    private Connection connection;
    
    public DBManager(final Main plugin, final DBType dbType, final String database, final String user, final String password) throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
        this.plugin = plugin;
        if (dbType == DBType.MySQL) {
            plugin.getLogger().info("Initializing MySQL engine");
            this.MySQLConnection(database, user, password);
        }
        else {
            plugin.getLogger().info("Initializing SQLite engine...");
            this.SQLiteConnection();
        }
        this.createTables();
    }
    
    private void MySQLConnection(final String database, final String user, final String password) throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        this.connection = DriverManager.getConnection("jdbc:mysql://:3306/" + database, user, password);
    }
    
    private void SQLiteConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.plugin.getDataFolder().getAbsolutePath() + "/" + this.plugin.getName() + ".db");
    }
    
    private boolean executeUpdate(final String query) {
        boolean results = false;
        try {
            final Statement statement = this.connection.createStatement();
            statement.executeUpdate(query);
            statement.close();
            results = true;
        }
        catch (SQLException ex) {
            this.plugin.getLogger().severe(ex.getMessage());
        }
        return results;
    }
    
    private ResultSet getResultSet(final String query) {
        ResultSet results = null;
        try {
            final Statement statement = this.connection.createStatement();
            results = statement.executeQuery(query);
        }
        catch (SQLException ex) {
            this.plugin.getLogger().severe(ex.getMessage());
        }
        return results;
    }
    
    private boolean createTables() {
        boolean ret = this.executeUpdate("CREATE TABLE IF NOT EXISTS scores(player_name VARCHAR(16) PRIMARY KEY, score INTEGER);");
        ret = (ret && this.executeUpdate("CREATE TABLE IF NOT EXISTS events(event_date_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, player_name VARCHAR(16), related_player_name VARCHAR(16), description VARCHAR(256));"));
        ret = (ret && this.executeUpdate("CREATE TABLE IF NOT EXISTS kills(player_name VARCHAR(16) PRIMARY KEY, combatkills INTEGER);"));
        ret = (ret && this.executeUpdate("CREATE TABLE IF NOT EXISTS wools_captured(player_name VARCHAR(16) PRIMARY KEY, wools_placed INTEGER);"));
        return ret;
    }
    
    protected boolean incScore(final String playerName, final int value) {
        return this.setScore(playerName, this.getScore(playerName) + value);
    }
    
    protected boolean incKill(final String playerName, final int value) {
        return this.setCombatKills(playerName, this.getKill(playerName) + value);
    }
    
    protected boolean incWoolCaptured(final String playerName, final int value) {
        return this.setWoolsCaptured(playerName, this.getWoolCaptured(playerName) + value);
    }
    
    protected boolean setScore(final String playerName, final int score) {
        return this.executeUpdate("REPLACE INTO scores VALUES('" + playerName + "', " + score + ");");
    }
    
    protected boolean setCombatKills(final String playerName, final int kill) {
        return this.executeUpdate("REPLACE INTO kills VALUES('" + playerName + "', " + kill + ");");
    }
    
    protected boolean setWoolsCaptured(final String playerName, final int wool_captured) {
        return this.executeUpdate("REPLACE INTO wools_captured VALUES('" + playerName + "', " + wool_captured + ");");
    }
    
    protected boolean addEvent(final String playerName, final String description) {
        return this.addEvent(playerName, null, description);
    }
    
    protected boolean addEvent(final String playerName, final String relatedPlayerName, final String description) {
        return this.executeUpdate("INSERT INTO events VALUES( null, '" + playerName + "', " + ((relatedPlayerName == null) ? "null," : ("'" + relatedPlayerName + "', ")) + "'" + description + "'" + ");");
    }
    
    protected int getScore(final String playerName) {
        int result = 0;
        final ResultSet rs = this.getResultSet("SELECT score FROM scores WHERE player_name='" + playerName + "';");
        if (rs != null) {
            try {
                if (rs.next()) {
                    result = rs.getInt("score");
                }
                rs.close();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().severe(ex.getMessage());
            }
        }
        return result;
    }
    
    protected int getKill(final String playerName) {
        int result = 0;
        final ResultSet rs = this.getResultSet("SELECT combatkills FROM kills WHERE player_name='" + playerName + "';");
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
    
    protected int getWoolCaptured(final String playerName) {
        int result = 0;
        final ResultSet rs = this.getResultSet("SELECT wools_placed FROM wools_captured WHERE player_name='" + playerName + "';");
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
    
    protected TreeMap<String, Integer> getScores() {
        final TreeMap<String, Integer> results = new TreeMap<String, Integer>();
        final ResultSet rs = this.getResultSet("SELECT player_name, score FROM scores;");
        try {
            while (rs.next()) {
                results.put(rs.getString("player_name"), rs.getInt("score"));
            }
            rs.close();
        }
        catch (SQLException ex) {
            this.plugin.getLogger().severe(ex.getMessage());
        }
        return results;
    }
    
    protected TreeMap<String, Integer> getKills() {
        final TreeMap<String, Integer> results = new TreeMap<String, Integer>();
        final ResultSet rs = this.getResultSet("SELECT player_name, combatkills FROM kills;");
        try {
            while (rs.next()) {
                results.put(rs.getString("player_name"), rs.getInt("combatkills"));
            }
            rs.close();
        }
        catch (SQLException ex) {
            this.plugin.getLogger().severe(ex.getMessage());
        }
        return results;
    }
    
    protected TreeMap<String, Integer> getWoolsCaptured() {
        final TreeMap<String, Integer> results = new TreeMap<String, Integer>();
        final ResultSet rs = this.getResultSet("SELECT player_name, wools_placed FROM wools_captured;");
        try {
            while (rs.next()) {
                results.put(rs.getString("player_name"), rs.getInt("wools_placed"));
            }
            rs.close();
        }
        catch (SQLException ex) {
            this.plugin.getLogger().severe(ex.getMessage());
        }
        return results;
    }
    
    public enum DBType
    {
        MySQL("MySQL", 0), 
        SQLITE("SQLITE", 1);
        
        private DBType(final String s, final int n) {
        }
    }
}
