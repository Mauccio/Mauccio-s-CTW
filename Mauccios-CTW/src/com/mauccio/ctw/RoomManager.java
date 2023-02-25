package com.mauccio.ctw;

import org.bukkit.configuration.file.*;
import java.io.*;
import org.bukkit.configuration.*;
import org.bukkit.*;
import java.util.*;
import org.bukkit.entity.*;

public final class RoomManager
{
    private final Main plugin;
    private final YamlConfiguration roomsConfig;
    private final File roomsConfigFile;
    private final TreeMap<String, Room> rooms;
    
    public void removeWools(final String mapName, final World newWorld) {
        for (final Location loc : this.plugin.mm.getWoolWinLocations(mapName)) {
            final Location capturePoint = new Location(newWorld, (double)loc.getBlockX(), (double)loc.getBlockY(), (double)loc.getBlockZ());
            capturePoint.getBlock().setType(Material.AIR);
        }
    }
    
    public RoomManager(final Main plugin) {
        this.plugin = plugin;
        this.roomsConfig = new YamlConfiguration();
        this.roomsConfigFile = new File(plugin.getDataFolder(), "rooms.yml");
        this.rooms = new TreeMap<String, Room>();
    }
    
    public void init() {
        if (this.rooms.isEmpty()) {
            this.load();
            for (String roomName : this.getRooms(true)) {
                this.plugin.gm.addGame(roomName);
            }
        }
    }
    
    public void load() {
        if (this.roomsConfigFile.exists()) {
            try {
                this.roomsConfig.load(this.roomsConfigFile);
            }
            catch (IOException | InvalidConfigurationException ex3) {
                this.plugin.getLogger().severe(ex3.toString());
            }
        }
        this.rooms.clear();
        for (final String roomName : this.roomsConfig.getKeys(false)) {
            final Room room = new Room(roomName);
            room.enabled = this.roomsConfig.getBoolean(String.valueOf(roomName) + ".enabled");
            final List<String> worldNames = (List<String>)this.roomsConfig.getStringList(String.valueOf(roomName) + ".map");
            if (worldNames != null) {
                room.worlds = new ArrayList<World>();
                room.maps = new ArrayList<World>();
                for (final String worldName : worldNames) {
                    final World map = this.plugin.wm.loadWorld(worldName);
                    if (map != null) {
                        if (this.plugin.mm.getRestaurationArea(worldName) == null) {
                            this.plugin.alert("Ignoring map \"" + worldName + "\": restauration area is not set.");
                        }
                        else {
                            room.maps.add(map);
                            this.plugin.wm.cloneWorld(map, String.valueOf(roomName) + "_" + worldName);
                            final World world = this.plugin.wm.loadWorld(String.valueOf(roomName) + "_" + worldName);
                            room.worlds.add(world);
                            this.plugin.wm.restoreMap(this.plugin.mm.getMapData(worldName), world);
                            this.removeWools(map.getName(), world);
                        }
                    }
                }
            }
            if (room.maps == null || room.maps.isEmpty()) {
                room.enabled = false;
            }
            else if (room.worlds == null || room.worlds.isEmpty()) {
                room.enabled = false;
                this.plugin.getLogger().info("There is no worlds to load for " + room.name);
            }
            this.rooms.put(roomName, room);
        }
    }
    
    public void persist() {
        for (final Room room : this.rooms.values()) {
            if (room.maps != null) {
                final List<String> mapList = new ArrayList<String>();
                for (final World map : room.maps) {
                    mapList.add(map.getName());
                }
                this.roomsConfig.set(String.valueOf(room.name) + ".map", (Object)mapList);
            }
            this.roomsConfig.set(String.valueOf(room.name) + ".enabled", (Object)room.enabled);
        }
        try {
            this.roomsConfig.save(this.roomsConfigFile);
        }
        catch (IOException ex) {
            this.plugin.getLogger().severe(ex.toString());
        }
    }
    
    public boolean add(final String roomName) {
        if (this.rooms.containsKey(roomName)) {
            return false;
        }
        final Room room = new Room(roomName);
        this.rooms.put(roomName, room);
        return true;
    }
    
    public boolean exists(final String roomName) {
        return this.rooms.containsKey(roomName);
    }
    
    public boolean isEnabled(final String roomName) {
        final Room room = this.rooms.get(roomName);
        return room != null && room.enabled;
    }
    
    public boolean hasMaps(final String roomName) {
        final Room room = this.rooms.get(roomName);
        return room != null && room.maps != null;
    }
    
    public boolean addMap(final String roomName, final World map) {
        final Room room = this.rooms.get(roomName);
        if (room == null) {
            return false;
        }
        if (room.enabled) {
            return false;
        }
        if (room.maps == null) {
            room.maps = new ArrayList<World>();
        }
        room.maps.add(map);
        return true;
    }
    
    public boolean hasMap(final String roomName, final World map) {
        final Room room = this.rooms.get(roomName);
        return room != null && room.maps != null && room.maps.contains(map);
    }
    
    public boolean removeMap(final String roomName, final World map) {
        final Room room = this.rooms.get(roomName);
        return room != null && !room.enabled && room.maps != null && room.maps.remove(map);
    }
    
    public boolean remove(final String roomName) {
        final Room room = this.rooms.get(roomName);
        if (room == null) {
            return false;
        }
        if (room.enabled) {
            return false;
        }
        this.rooms.remove(roomName);
        return true;
    }
    
    public List<String> list() {
        final List<String> list = new ArrayList<String>();
        for (final Room room : this.rooms.values()) {
            String entry = ChatColor.AQUA + room.name;
            if (room.enabled) {
                entry = entry.concat(ChatColor.GREEN + " (" + this.plugin.lm.getText("enabled") + ") ");
            }
            else {
                entry = entry.concat(ChatColor.RED + " (" + this.plugin.lm.getText("disabled") + ") ");
            }
            String mapList;
            if (room.maps != null) {
                mapList = ChatColor.GREEN + "[ ";
                for (final World world : room.maps) {
                    mapList = mapList.concat(world.getName()).concat(" ");
                }
                mapList = mapList.concat("]");
            }
            else {
                mapList = ChatColor.RED + "[" + this.plugin.lm.getText("none") + "]";
            }
            entry = entry.concat(ChatColor.AQUA + this.plugin.lm.getText("maps") + ": " + mapList);
            list.add(entry);
        }
        return list;
    }
    
    public boolean enable(final String roomName) {
        final Room room = this.rooms.get(roomName);
        if (room == null) {
            return false;
        }
        if (room.enabled) {
            return false;
        }
        room.enabled = true;
        this.prepareRoom(room);
        this.plugin.gm.addGame(roomName);
        this.plugin.sm.updateSigns(roomName);
        return true;
    }
    
    public boolean disable(final String roomName) {
        final Room room = this.rooms.get(roomName);
        if (room == null) {
            return false;
        }
        if (!room.enabled) {
            return false;
        }
        room.enabled = false;
        if (room.worlds != null) {
            for (final World world : room.worlds) {
                this.plugin.wm.unloadWorld(world);
            }
        }
        room.mapIndex = 0;
        this.plugin.gm.removeGame(roomName);
        this.plugin.sm.updateSigns(roomName);
        return true;
    }
    
    public Set<String> getRooms() {
        return this.rooms.keySet();
    }
    
    public List<String> getRooms(final boolean enabled) {
        final List<String> results = new ArrayList<String>();
        for (final Room room : this.rooms.values()) {
            if (room.enabled == enabled) {
                results.add(room.name);
            }
        }
        return results;
    }
    
    public Collection<World> getMaps(final String roomName) {
        final Room room = this.rooms.get(roomName);
        if (room == null) {
            return null;
        }
        return room.maps;
    }
    
    public String getCurrentMap(final String roomName) {
        final Room room = this.rooms.get(roomName);
        if (room == null) {
            return null;
        }
        return room.maps.get(room.mapIndex).getName();
    }
    
    public String getNextMap(final String roomName) {
        final Room room = this.rooms.get(roomName);
        if (room == null) {
            return null;
        }
        if (room.mapIndex + 1 >= room.maps.size()) {
            return room.maps.get(0).getName();
        }
        return room.maps.get(room.mapIndex + 1).getName();
    }
    
    public World getCurrentWorld(final String roomName) {
        final Room room = this.rooms.get(roomName);
        if (room == null || room.worlds == null) {
            return null;
        }
        if (room.worlds.isEmpty()) {
            return null;
        }
        return room.worlds.get(room.mapIndex);
    }
    
    public World getNextWorld(final String roomName) {
        final Room room = this.rooms.get(roomName);
        if (room == null) {
            return null;
        }
        if (room.mapIndex + 1 >= room.maps.size()) {
            return room.worlds.get(0);
        }
        return room.worlds.get(room.mapIndex + 1);
    }
    
    public void swapMap(final String roomName) {
        final Room room = this.rooms.get(roomName);
        if (room == null) {
            return;
        }
        this.plugin.wm.restoreMap(this.plugin.mm.getMapData(room.maps.get(room.mapIndex).getName()), room.worlds.get(room.mapIndex));
        if (room.maps.size() <= room.mapIndex + 1) {
            room.mapIndex = 0;
        }
        else {
            final Room room2 = room;
            ++room2.mapIndex;
        }
    }
    
    public boolean isInGame(World world) {
        for (Room room : this.rooms.values()) {
            if (room.worlds.contains(world)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isProhibited(final Item item) {
        final String roomName = this.getRoom(item.getWorld());
        if (roomName == null) {
            return false;
        }
        final MapManager.MapData data = this.plugin.mm.getMapData(this.getCurrentMap(roomName));
        return data.noDropOnBreak != null && data.noDropOnBreak.contains(item.getItemStack().getType());
    }
    
    public String getRoom(final World world) {
        for (final String roomName : this.rooms.keySet()) {
            final World currentWorld = this.getCurrentWorld(roomName);
            if (currentWorld != null && currentWorld.getName().equals(world.getName())) {
                return roomName;
            }
        }
        return null;
    }
    
    private void prepareRoom(final Room room) {
        room.worlds = new ArrayList<World>();
        for (final World map : room.maps) {
            final String worldName = String.valueOf(room.name) + "_" + map.getName();
            World world = this.plugin.wm.loadWorld(worldName);
            if (world == null) {
                world = this.plugin.wm.cloneWorld(map, worldName);
            }
            this.removeWools(map.getName(), world);
            room.worlds.add(world);
            this.plugin.wm.clearEntities(world);
        }
    }
    
    private class Room
    {
        String name;
        List<World> maps;
        List<World> worlds;
        boolean enabled;
        int mapIndex;
        
        public Room(final String name) {
            this.name = name;
        }
    }
}
