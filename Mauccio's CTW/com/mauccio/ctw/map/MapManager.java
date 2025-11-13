package com.mauccio.ctw.map;

import com.mauccio.ctw.CTW;
import com.mauccio.ctw.utils.Utils;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;

public class MapManager {
    private final CTW plugin;

    private final YamlConfiguration mapsConfig;

    private final File mapsConfigFile;

    private final TreeMap<String, MapData> maps;

    private boolean showRoomTip;

    private static int MAX_PLAYERS_DEFAULT = 0;

    private static int GRACE_PERIOD = 30;

    private static boolean WEATHER_FIXED = true;

    private static boolean WEATHER_STORM = false;

    public static class Weather {
        public boolean fixed;

        public boolean storm;
    }

    public static class Texts {
        List<String> atBeginingAnnounce;

        List<String> atStartAnnounce;

        List<String> atFinishedAnnounce;

        List<String> description;

        String authors;
    }

    public static class MapData {
        public int maxPlayers;
        public MapManager.Weather weather;
        public World world;
        public TreeMap<String, Location> woolSpawners;
        public Location redSpawn;
        public Location blueSpawn;
        public Location mapSpawn;
        public TreeSet<Selection> redInaccessibleAreas;
        public TreeSet<Selection> blueInaccessibleAreas;
        public TreeSet<Selection> protectedAreas;
        public TreeMap<String, Location> redWoolWinPoints;
        public TreeMap<String, Location> blueWoolWinPoints;
        public Selection restaurationArea;
        int gracePeriodSeconds;
        public TreeSet<Material> noDropOnBreak;
        public MapManager.Texts texts;
        public Inventory kitInv;
        public boolean kitArmour;
    }

    public MapManager(CTW plugin) {
        this.plugin = plugin;
        mapsConfigFile = new File(plugin.getDataFolder(), "maps.yml");
        mapsConfig = new YamlConfiguration();
        maps = new TreeMap<>();
        load();
        showRoomTip = maps.isEmpty();
    }


    private MapData loadMapData(String mapName)
            throws IOException, InvalidConfigurationException {
        MapData mapData = maps.get(mapName);
        if (mapData == null) {
            mapData = new MapData();
        }
        World world = plugin.getWorldManager().loadWorld(mapName);
        if (world == null) {
            plugin.getLogger().log(Level.SEVERE, "Unable to load map {0}. It world cannot be loaded.", mapName);
            return null;
        }
        mapData.world = world;
        File localConfigFile = new File(mapName + "/ctw.yml");
        YamlConfiguration localConfigAux = new YamlConfiguration();

        mapData.texts = getText(mapName);

        localConfigAux.load(localConfigFile);
        ConfigurationSection localConfig = localConfigAux.getConfigurationSection(mapName);

        if (localConfig == null) {
            localConfig = new MemoryConfiguration();
        }

        mapData.maxPlayers = localConfig.getInt("max-players",
                mapsConfig.getInt(mapName + ".max-players", MAX_PLAYERS_DEFAULT));

        if (!localConfig.isSet("map-spawn")) {
            if (mapsConfig.isSet(mapName + ".map-spawn")) {
                double x = mapsConfig.getDouble(mapName + ".map-spawn.x");
                double y = mapsConfig.getDouble(mapName + ".map-spawn.y");
                double z = mapsConfig.getDouble(mapName + ".map-spawn.z");
                double yaw = mapsConfig.getDouble(mapName + ".map-spawn.yaw");
                double pitch = mapsConfig.getDouble(mapName + ".map-spawn.pitch");
                mapData.mapSpawn = new Location(world, x, y, z, (float) yaw, (float) pitch);
            } else {
                mapData.mapSpawn = world.getSpawnLocation();
            }
        } else {
            double x = localConfig.getDouble("map-spawn.x");
            double y = localConfig.getDouble("map-spawn.y");
            double z = localConfig.getDouble("map-spawn.z");
            double yaw = localConfig.getDouble("map-spawn.yaw");
            double pitch = localConfig.getDouble("map-spawn.pitch");
            mapData.mapSpawn = new Location(world, x, y, z, (float) yaw, (float) pitch);
        }

        if (!localConfig.isSet("red-spawn")) {
            if (mapsConfig.isSet(mapName + ".red-spawn")) {
                double x = mapsConfig.getDouble(mapName + ".red-spawn.x");
                double y = mapsConfig.getDouble(mapName + ".red-spawn.y");
                double z = mapsConfig.getDouble(mapName + ".red-spawn.z");
                double yaw = mapsConfig.getDouble(mapName + ".red-spawn.yaw");
                double pitch = mapsConfig.getDouble(mapName + ".red-spawn.pitch");
                mapData.redSpawn = new Location(world, x, y, z, (float) yaw, (float) pitch);
            }
        } else {
            double x = localConfig.getDouble("red-spawn.x");
            double y = localConfig.getDouble("red-spawn.y");
            double z = localConfig.getDouble("red-spawn.z");
            double yaw = localConfig.getDouble("red-spawn.yaw");
            double pitch = localConfig.getDouble("red-spawn.pitch");
            mapData.redSpawn = new Location(world, x, y, z, (float) yaw, (float) pitch);
        }

        if (!localConfig.isSet("blue-spawn")) {
            if (mapsConfig.isSet(mapName + ".blue-spawn")) {
                double x = mapsConfig.getDouble(mapName + ".blue-spawn.x");
                double y = mapsConfig.getDouble(mapName + ".blue-spawn.y");
                double z = mapsConfig.getDouble(mapName + ".blue-spawn.z");
                double yaw = mapsConfig.getDouble(mapName + ".blue-spawn.yaw");
                double pitch = mapsConfig.getDouble(mapName + ".blue-spawn.pitch");
                mapData.blueSpawn = new Location(world, x, y, z, (float) yaw, (float) pitch);
            }
        } else {
            double x = localConfig.getDouble("blue-spawn.x");
            double y = localConfig.getDouble("blue-spawn.y");
            double z = localConfig.getDouble("blue-spawn.z");
            double yaw = localConfig.getDouble("blue-spawn.yaw");
            double pitch = localConfig.getDouble("blue-spawn.pitch");
            mapData.blueSpawn = new Location(world, x, y, z, (float) yaw, (float) pitch);
        }

        if (!localConfig.isSet("weather")) {
            if (mapsConfig.isSet(mapName + ".weather")) {
                mapData.weather = new Weather();
                mapData.weather.fixed = mapsConfig.getBoolean(mapName + ".weather.fixed", WEATHER_FIXED);
                mapData.weather.storm = mapsConfig.getBoolean(mapName + ".weather.storm", WEATHER_STORM);
            }
        } else {
            mapData.weather = new Weather();
            mapData.weather.fixed = localConfig.getBoolean("weather.fixed", WEATHER_FIXED);
            mapData.weather.storm = mapsConfig.getBoolean("weather.storm", WEATHER_STORM);
        }

        if (mapsConfig.isSet(mapName + ".blue-inaccessible-area")) {
            mapData.blueInaccessibleAreas = getSelectionList(
                    mapsConfig.getConfigurationSection(mapName + ".blue-inaccessible-area"), world);
        }

        if (localConfig.isSet("blue-inaccessible-area")) {
            if (mapData.blueInaccessibleAreas == null) {
                mapData.blueInaccessibleAreas = getSelectionList(
                        localConfig.getConfigurationSection("blue-inaccessible-area"), world);
            } else {
                mapData.blueInaccessibleAreas.addAll(getSelectionList(
                        localConfig.getConfigurationSection("blue-inaccessible-area"), world));
            }
        }

        if (mapsConfig.isSet(mapName + ".red-inaccessible-area")) {
            mapData.redInaccessibleAreas = getSelectionList(
                    mapsConfig.getConfigurationSection(mapName + ".red-inaccessible-area"), world);
        }

        if (localConfig.isSet("red-inaccessible-area")) {
            if (mapData.redInaccessibleAreas == null) {
                mapData.redInaccessibleAreas = getSelectionList(
                        localConfig.getConfigurationSection("red-inaccessible-area"), world);
            } else {
                mapData.redInaccessibleAreas.addAll(getSelectionList(
                        localConfig.getConfigurationSection("red-inaccessible-area"), world));
            }
        }

        if (mapsConfig.isSet(mapName + ".protected-area")) {
            mapData.protectedAreas = getSelectionList(
                    mapsConfig.getConfigurationSection(mapName + ".protected-area"), world);
        }

        if (localConfig.isSet("protected-area")) {
            if (mapData.protectedAreas == null) {
                mapData.protectedAreas = getSelectionList(
                        localConfig.getConfigurationSection("protected-area"), world);
            } else {
                mapData.protectedAreas.addAll(getSelectionList(
                        localConfig.getConfigurationSection("protected-area"), world));
            }
        }

        if (mapsConfig.isSet(mapName + ".red-wool-win-point")) {
            mapData.redWoolWinPoints = getWoolLocations(
                    mapsConfig.getConfigurationSection(mapName + ".red-wool-win-point"), world);
        }

        if (localConfig.isSet("red-wool-win-point")) {
            if (mapData.redWoolWinPoints == null) {
                mapData.redWoolWinPoints = getWoolLocations(
                        localConfig.getConfigurationSection("red-wool-win-point"), world);
            } else {
                mapData.redWoolWinPoints.putAll(getWoolLocations(
                        localConfig.getConfigurationSection("red-wool-win-point"), world));
            }
        }

        if (mapsConfig.isSet(mapName + ".blue-wool-win-point")) {
            mapData.blueWoolWinPoints = getWoolLocations(
                    mapsConfig.getConfigurationSection(mapName + ".blue-wool-win-point"), world);
        }

        if (localConfig.isSet("blue-wool-win-point")) {
            if (mapData.blueWoolWinPoints == null) {
                mapData.blueWoolWinPoints = getWoolLocations(
                        localConfig.getConfigurationSection("blue-wool-win-point"), world);
            } else {
                mapData.blueWoolWinPoints.putAll(getWoolLocations(
                        localConfig.getConfigurationSection("blue-wool-win-point"), world));
            }
        }

        if (mapsConfig.isSet(mapName + ".wool-spawner")) {
            mapData.woolSpawners = getWoolLocations(
                    mapsConfig.getConfigurationSection(mapName + ".wool-spawner"), world);
        }

        if (localConfig.isSet("wool-spawner")) {
            mapData.woolSpawners = getWoolLocations(
                    localConfig.getConfigurationSection("wool-spawner"), world);
        }

        if (mapsConfig.isSet(mapName + ".restauration-area")) {
            mapData.restaurationArea = getCuboidSelection(
                    mapsConfig.getConfigurationSection(mapName + ".restauration-area"), world);
        }

        if (localConfig.isSet("restauration-area")) {
            mapData.restaurationArea = getCuboidSelection(
                    localConfig.getConfigurationSection("restauration-area"), world);
        }

        mapData.gracePeriodSeconds = localConfig.getInt("grace-period", GRACE_PERIOD);

        if (localConfig.isSet("no-drop-on-break")) {
            mapData.noDropOnBreak = new TreeSet<>();
            for (String materialName : localConfig.getStringList("no-drop-on-break")) {
                try {
                    Material material = Material.valueOf(materialName);
                    mapData.noDropOnBreak.add(material);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "Ignoring invalid material name for map {0}: {1}",
                            new Object[]{mapName, materialName});
                }
            }
        }

        if (mapData.noDropOnBreak == null) {
            mapData.noDropOnBreak = new TreeSet<>();
        }

        mapData.kitArmour = localConfig.getBoolean("kit-armour",
                mapsConfig.getBoolean(mapName + ".kit-armour", false));

        return mapData;
    }

    private Texts getText(String mapName) throws IOException, FileNotFoundException, InvalidConfigurationException {
        File localConfigFile = new File(mapName + "/ctw.yml");
        YamlConfiguration localConfig = new YamlConfiguration();
        if (!localConfigFile.exists()) {
            plugin.getLogger().log(Level.INFO, "Creating new configuration from template for: {0}", mapName);
            File localConfigDefaultFile = new File(plugin.getDataFolder(), "defaultmap.yml");
            YamlConfiguration ycAux = new YamlConfiguration();
            ycAux.load(localConfigDefaultFile);
            localConfig.set(mapName + ".grace-period", ycAux.getString("grace-period"));
            localConfig.set(mapName + ".no-drop-on-break", ycAux.getStringList("no-drop-on-break"));
            localConfig.set(mapName + ".announce.at-begining", ycAux.getStringList("announce.at-begining"));
            localConfig.set(mapName + ".announce.at-begining", ycAux.getStringList("announce.at-begining"));
            localConfig.set(mapName + ".announce.at-start", ycAux.getStringList("announce.at-start"));
            localConfig.set(mapName + ".announce.at-finish", ycAux.getStringList("announce.at-finish"));
            localConfig.set(mapName + ".announce.at-finish", ycAux.getStringList("announce.at-finish"));
            localConfig.set(mapName + ".info.authors", ycAux.getString("info.authors"));
            localConfig.set(mapName + ".info.description", ycAux.getStringList("info.description"));
            localConfig.save(mapName + "/ctw.yml");
        }
        Texts texts = new Texts();
        texts.authors = localConfig.getString("info.authors");
        texts.description = localConfig.getStringList("info.description");
        texts.atBeginingAnnounce = localConfig.getStringList("announce.at-begining");
        texts.atFinishedAnnounce = localConfig.getStringList("announce.at-finish");
        texts.atStartAnnounce = localConfig.getStringList("announce.at-start");
        return texts;
    }

    private static CuboidSelection getCuboidSelection(ConfigurationSection cs, World world) {
        Vector min = new Vector(cs.getInt("min.x", 0), cs.getInt("min.y", 0), cs.getInt("min.z", 0));
        Vector max = new Vector(cs.getInt("max.x", 0), cs.getInt("max.y", 0), cs.getInt("max.z", 0));
        return new CuboidSelection(world, min, max);
    }

    private static TreeSet<Selection> getSelectionList(ConfigurationSection cs, World world) {
        TreeSet<Selection> result = new TreeSet<>(new Utils.SelectionComparator());
        for (String area : cs.getKeys(false)) {
            Vector min = new Vector(cs.getInt(area + ".min.x", 0), cs.getInt(area + ".min.y", 0), cs.getInt(area + ".min.z", 0));
            Vector max = new Vector(cs.getInt(area + ".max.x", 0), cs.getInt(area + ".max.y", 0), cs.getInt(area + ".max.z", 0));
            result.add(new CuboidSelection(world, min, max));
        }
        return result;
    }

    private static TreeMap<String, Location> getWoolLocations(ConfigurationSection cs, World world) {
        TreeMap<String, Location> result = new TreeMap<>();
        for (String colorName : cs.getKeys(false)) {
            result.put(colorName, new Location(world,
                    cs.getInt(colorName + ".location.x"),
                    cs.getInt(colorName + ".location.y"),
                    cs.getInt(colorName + ".location.z")));
        }
        return result;
    }

    public void load() {
        if (mapsConfigFile.exists()) {
            try {
                mapsConfig.load(mapsConfigFile);
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().severe(ex.toString());
            }
        }
        maps.clear();
        for (String mapName : mapsConfig.getKeys(false)) {
            MapData mapData;
            try {
                mapData = loadMapData(mapName);
                if (mapData == null) {
                    continue;
                }
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().severe(ex.toString());
                continue;
            }
            maps.put(mapName, mapData);
        }
    }

    public void persist() {
        for (String mapName : maps.keySet()) {
            MapData data = maps.get(mapName);
            /*if (data.kitInv != null) {
                ItemStack[] content = data.kitInv.getContents();
                for (int i = 0; i < content.length; i++) {
                    ItemStack is = content[i];
                    if (is != null) {
                        mapsConfig.set(mapName + ".kit." + i + ".material", is.getType().name());
                        mapsConfig.set(mapName + ".kit." + i + ".amount", is.getAmount());
                        mapsConfig.set(mapName + ".kit." + i + ".durability", is.getDurability());
                        for (Enchantment enchantment : is.getEnchantments().keySet()) {
                            mapsConfig.set(mapName + ".kit." + i + ".enchantment." + enchantment.getName() + ".level", is.getEnchantmentLevel(enchantment));
                        }
                    }
                }
            }*/

            if (data.maxPlayers > 0) {
                mapsConfig.set(mapName + ".max-players", data.maxPlayers);
            }

            if (data.mapSpawn != null) {
                mapsConfig.set(mapName + ".map-spawn.x", data.mapSpawn.getX());
                mapsConfig.set(mapName + ".map-spawn.y", data.mapSpawn.getY());
                mapsConfig.set(mapName + ".map-spawn.z", data.mapSpawn.getZ());
                mapsConfig.set(mapName + ".map-spawn.yaw", data.mapSpawn.getYaw());
                mapsConfig.set(mapName + ".map-spawn.pitch", data.mapSpawn.getPitch());
            }

            if (data.redSpawn != null) {
                mapsConfig.set(mapName + ".red-spawn.x", data.redSpawn.getX());
                mapsConfig.set(mapName + ".red-spawn.y", data.redSpawn.getY());
                mapsConfig.set(mapName + ".red-spawn.z", data.redSpawn.getZ());
                mapsConfig.set(mapName + ".red-spawn.yaw", data.redSpawn.getYaw());
                mapsConfig.set(mapName + ".red-spawn.pitch", data.redSpawn.getPitch());
            }

            if (data.blueSpawn != null) {
                mapsConfig.set(mapName + ".blue-spawn.x", data.blueSpawn.getX());
                mapsConfig.set(mapName + ".blue-spawn.y", data.blueSpawn.getY());
                mapsConfig.set(mapName + ".blue-spawn.z", data.blueSpawn.getZ());
                mapsConfig.set(mapName + ".blue-spawn.yaw", data.blueSpawn.getYaw());
                mapsConfig.set(mapName + ".blue-spawn.pitch", data.blueSpawn.getPitch());
            }

            if (data.weather != null) {
                mapsConfig.set(mapName + ".weather.fixed", data.weather.fixed);
                mapsConfig.set(mapName + ".weather.storm", data.weather.storm);
            }

            if (data.blueInaccessibleAreas != null) {
                int count = 0;
                for (Selection sel : data.blueInaccessibleAreas) {
                    mapsConfig.set(mapName + ".blue-inaccessible-area." + count + ".min.x", sel.getMinimumPoint().getBlockX());
                    mapsConfig.set(mapName + ".blue-inaccessible-area." + count + ".min.y", sel.getMinimumPoint().getBlockY());
                    mapsConfig.set(mapName + ".blue-inaccessible-area." + count + ".min.z", sel.getMinimumPoint().getBlockZ());

                    mapsConfig.set(mapName + ".blue-inaccessible-area." + count + ".max.x", sel.getMaximumPoint().getBlockX());
                    mapsConfig.set(mapName + ".blue-inaccessible-area." + count + ".max.y", sel.getMaximumPoint().getBlockY());
                    mapsConfig.set(mapName + ".blue-inaccessible-area." + count + ".max.z", sel.getMaximumPoint().getBlockZ());
                    count++;
                }
            }

            if (data.redInaccessibleAreas != null) {
                int count = 0;
                for (Selection sel : data.redInaccessibleAreas) {
                    mapsConfig.set(mapName + ".red-inaccessible-area." + count + ".min.x", sel.getMinimumPoint().getBlockX());
                    mapsConfig.set(mapName + ".red-inaccessible-area." + count + ".min.y", sel.getMinimumPoint().getBlockY());
                    mapsConfig.set(mapName + ".red-inaccessible-area." + count + ".min.z", sel.getMinimumPoint().getBlockZ());

                    mapsConfig.set(mapName + ".red-inaccessible-area." + count + ".max.x", sel.getMaximumPoint().getBlockX());
                    mapsConfig.set(mapName + ".red-inaccessible-area." + count + ".max.y", sel.getMaximumPoint().getBlockY());
                    mapsConfig.set(mapName + ".red-inaccessible-area." + count + ".max.z", sel.getMaximumPoint().getBlockZ());
                    count++;
                }
            }

            if (data.protectedAreas != null) {
                int count = 0;
                for (Selection sel : data.protectedAreas) {
                    mapsConfig.set(mapName + ".protected-area." + count + ".min.x", sel.getMinimumPoint().getBlockX());
                    mapsConfig.set(mapName + ".protected-area." + count + ".min.y", sel.getMinimumPoint().getBlockY());
                    mapsConfig.set(mapName + ".protected-area." + count + ".min.z", sel.getMinimumPoint().getBlockZ());

                    mapsConfig.set(mapName + ".protected-area." + count + ".max.x", sel.getMaximumPoint().getBlockX());
                    mapsConfig.set(mapName + ".protected-area." + count + ".max.y", sel.getMaximumPoint().getBlockY());
                    mapsConfig.set(mapName + ".protected-area." + count + ".max.z", sel.getMaximumPoint().getBlockZ());
                    count++;
                }
            }

            if (data.redWoolWinPoints != null) {
                for (String woolColor : data.redWoolWinPoints.keySet()) {
                    Location loc = data.redWoolWinPoints.get(woolColor);
                    mapsConfig.set(mapName + ".red-wool-win-point." + woolColor + ".location.x", loc.getBlockX());
                    mapsConfig.set(mapName + ".red-wool-win-point." + woolColor + ".location.y", loc.getBlockY());
                    mapsConfig.set(mapName + ".red-wool-win-point." + woolColor + ".location.z", loc.getBlockZ());
                }
            }

            if (data.blueWoolWinPoints != null) {
                for (String woolColor : data.blueWoolWinPoints.keySet()) {
                    Location loc = data.blueWoolWinPoints.get(woolColor);
                    mapsConfig.set(mapName + ".blue-wool-win-point." + woolColor + ".location.x", loc.getBlockX());
                    mapsConfig.set(mapName + ".blue-wool-win-point." + woolColor + ".location.y", loc.getBlockY());
                    mapsConfig.set(mapName + ".blue-wool-win-point." + woolColor + ".location.z", loc.getBlockZ());
                }
            }

            if (data.woolSpawners != null) {
                for (String woolColor : data.woolSpawners.keySet()) {
                    Location loc = data.woolSpawners.get(woolColor);
                    mapsConfig.set(mapName + ".wool-spawner." + woolColor + ".location.x", loc.getBlockX());
                    mapsConfig.set(mapName + ".wool-spawner." + woolColor + ".location.y", loc.getBlockY());
                    mapsConfig.set(mapName + ".wool-spawner." + woolColor + ".location.z", loc.getBlockZ());
                }
            }

            if (data.restaurationArea != null) {
                mapsConfig.set(mapName + ".restauration-area.min.x", data.restaurationArea.getMinimumPoint().getBlockX());
                mapsConfig.set(mapName + ".restauration-area.min.y", data.restaurationArea.getMinimumPoint().getBlockY());
                mapsConfig.set(mapName + ".restauration-area.min.z", data.restaurationArea.getMinimumPoint().getBlockZ());
                mapsConfig.set(mapName + ".restauration-area.max.x", data.restaurationArea.getMaximumPoint().getBlockX());
                mapsConfig.set(mapName + ".restauration-area.max.y", data.restaurationArea.getMaximumPoint().getBlockY());
                mapsConfig.set(mapName + ".restauration-area.max.z", data.restaurationArea.getMaximumPoint().getBlockZ());
            }
            try {
                maps.get(mapName).texts = getText(mapName);
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().severe(ex.toString());
            }

            if (data.noDropOnBreak != null) {
                List<String> materialList = new ArrayList<>();
                for (Material material : data.noDropOnBreak) {
                    materialList.add(material.name());
                }
                mapsConfig.set(mapName + ".grace-period", data.gracePeriodSeconds);
                mapsConfig.set(mapName + ".info.authors", data.texts.authors);
                mapsConfig.set(mapName + ".info.description", data.texts.description);
                mapsConfig.set(mapName + ".announce.at-begining", data.texts.atBeginingAnnounce);
                mapsConfig.set(mapName + ".announce.at-finish", data.texts.atFinishedAnnounce);
                mapsConfig.set(mapName + ".announce.at-start", data.texts.atStartAnnounce);
                mapsConfig.set(mapName + ".no-drop-on-break", materialList);
            }

            try {
                YamlConfiguration mapConfig = new YamlConfiguration();
                mapConfig.set(mapName, mapsConfig.get(mapName));
                mapConfig.save(mapName + "/ctw.yml");
                mapsConfig.set(mapName, new MemoryConfiguration());
            } catch (IOException ex) {
                plugin.getLogger().severe(ex.toString());
            }
        }

        try {
            mapsConfig.save(mapsConfigFile);
        } catch (IOException ex) {
            plugin.getLogger().severe(ex.toString());
        }
    }

    public boolean add(World world) {
        if (maps.containsKey(world.getName())) {
            return false;
        }
        MapData mapData = new MapData();
        mapData.world = world;
        mapData.mapSpawn = world.getSpawnLocation();
        maps.put(world.getName(), mapData);
        return true;
    }

    public boolean isMap(World world) {
        return maps.containsKey(world.getName());
    }

    public void setupTip(Player player) {
        MapData mapData = maps.get(player.getWorld().getName());
        if (mapData != null) {
            if (mapData.redSpawn == null) {
                plugin.getLangManager().sendMessage("map-tip-redspawn", player);
                return;
            }
            if (mapData.blueSpawn == null) {
                plugin.getLangManager().sendMessage("map-tip-bluespawn", player);
                return;
            }
            if (mapData.maxPlayers == 0) {
                plugin.getLangManager().sendMessage("map-tip-maxplayers", player);
                return;
            }
            if (mapData.redWoolWinPoints == null) {
                plugin.getLangManager().sendText("map-tip-redwoolwin", player);
                return;
            }
            if (mapData.blueWoolWinPoints == null) {
                plugin.getLangManager().sendText("map-tip-bluewoolwin", player);
                return;
            }
            if (mapData.redInaccessibleAreas == null) {
                plugin.getLangManager().sendText("map-tip-rednoaccess", player);
                return;
            }
            if (mapData.blueInaccessibleAreas == null) {
                plugin.getLangManager().sendText("map-tip-bluenoaccess", player);
                return;
            }
            if (mapData.protectedAreas == null) {
                plugin.getLangManager().sendText("map-tip-protected", player);
                return;
            }
            if (mapData.weather == null) {
                plugin.getLangManager().sendText("map-tip-weather", player);
                return;
            }
            if (mapData.woolSpawners == null) {
                plugin.getLangManager().sendText("map-tip-woolspawner", player);
                return;
            }
            if (mapData.restaurationArea == null) {
                plugin.getLangManager().sendMessage("map-tip-restauration", player);
                return;
            }
            if (showRoomTip) {
                plugin.getLangManager().sendMessage("map-seems-done", player);
                plugin.getLangManager().sendMessage("room-tip", player);
                showRoomTip = false;
            }
        }
    }

    /**
     * @param location of the red spawn
     */

    public void setRedSpawn(Location location) {
        MapData mapData = maps.get(location.getWorld().getName());
        if (mapData == null) {
            return;
        }
        mapData.redSpawn = location;
    }

    /**
     * @param location of the blue spawn
     */
    public void setBlueSpawn(Location location) {
        MapData mapData = maps.get(location.getWorld().getName());
        if (mapData == null) {
            return;
        }
        mapData.blueSpawn = location;
    }

    /**
     * @param location of the map spawn
     */
    public void setSpawn(Location location) {
        MapData mapData = maps.get(location.getWorld().getName());
        if (mapData == null) {
            return;
        }
        mapData.mapSpawn = location;
        location.getWorld().setSpawnLocation(location.getBlockX(),
                location.getBlockY(), location.getBlockZ());
    }

    public Location getSpawn(World world) {
        MapData mapData = maps.get(world.getName());
        if (mapData == null || mapData.mapSpawn == null) {
            return world.getSpawnLocation();
        }
        Location loc = mapData.mapSpawn.clone();
        if (loc.getWorld() == null) {
            loc.setWorld(world);
        }
        return loc;
    }

    public void setMaxPlayers(World world, int maxPlayers) {
        MapData mapData = maps.get(world.getName());
        if (mapData == null) {
            return;
        }
        mapData.maxPlayers = maxPlayers;
    }

    /**
     * @param world of the map
     */
    public boolean deleteMap(World world) {
        MapData mapData = maps.remove(world.getName());
        return mapData != null;
    }

    public Set<String> getMaps() {
        return maps.keySet();
    }

    /**
     * Clones a map into another one.
     *
     * @param source      the world of the source map.
     * @param destination the world of the destination map.
     */
    public void cloneMap(World source, World destination) {
        if (source == null || destination == null) {
            return;
        }
        MapData mapData = maps.get(source.getName());
        if (mapData == null) {
            return;
        }
        if (maps.containsKey(destination.getName())) {
            return;
        }

        persist();
        MapData newMap = getMapData(source.getName());
        newMap.world = destination;
        maps.put(destination.getName(), newMap);
        persist();
        load();

    }

    public void addRedNoAccessArea(World world, Selection area) {
        MapData mapData = maps.get(world.getName());
        if (mapData == null) {
            return;
        }
        if (mapData.redInaccessibleAreas == null) {
            mapData.redInaccessibleAreas = new TreeSet<>(new Utils.SelectionComparator());
        }
        mapData.redInaccessibleAreas.add(new CuboidSelection(area.getWorld(), area.getNativeMaximumPoint(), area.getNativeMinimumPoint()));
    }

    public boolean isRedNoAccessArea(World world, Selection area) {
        MapData mapData = maps.get(world.getName());
        if (mapData == null) {
            return false;
        }
        if (mapData.redInaccessibleAreas == null) {
            return false;
        }
        return mapData.redInaccessibleAreas.contains(area);
    }

    public void addBlueNoAccessArea(World world, Selection area) {
        MapData mapData = maps.get(world.getName());
        if (mapData == null) {
            return;
        }
        if (mapData.blueInaccessibleAreas == null) {
            mapData.blueInaccessibleAreas = new TreeSet<>(new Utils.SelectionComparator());
        }
        mapData.blueInaccessibleAreas.add(new CuboidSelection(area.getWorld(), area.getNativeMaximumPoint(), area.getNativeMinimumPoint()));
    }

    public boolean isBlueNoAccessArea(World world, Selection area) {
        MapData mapData = maps.get(world.getName());
        if (mapData == null) {
            return false;
        }
        if (mapData.blueInaccessibleAreas == null) {
            return false;
        }
        return mapData.blueInaccessibleAreas.contains(area);
    }

    public void addProtectedArea(World world, Selection area) {
        MapData mapData = maps.get(world.getName());
        if (mapData == null) {
            return;
        }
        if (mapData.protectedAreas == null) {
            mapData.protectedAreas = new TreeSet<>(new Utils.SelectionComparator());
        }
        mapData.protectedAreas.add(new CuboidSelection(area.getWorld(), area.getNativeMaximumPoint(), area.getNativeMinimumPoint()));
    }

    public boolean isProtectedArea(World world, Selection area) {
        MapData mapData = maps.get(world.getName());
        if (mapData == null) {
            return false;
        }
        if (mapData.protectedAreas == null) {
            return false;
        }
        return mapData.protectedAreas.contains(area);
    }

    private Map.Entry<String, Location> getWoolPointFrom(Block block) {
        @SuppressWarnings("deprecation")
        Wool wool = new Wool(block.getType(), block.getData());
        return new AbstractMap.SimpleEntry<>(wool.getColor().toString(), block.getLocation());
    }

    public boolean addRedWoolWinPoint(Block block) {
        MapData mapData = maps.get(block.getWorld().getName());
        if (mapData == null) {
            return false;
        }
        if (block.getType() != Material.WOOL) {
            return false;
        }
        Map.Entry<String, Location> wlEntry = getWoolPointFrom(block);
        if (mapData.redWoolWinPoints != null) {
            mapData.redWoolWinPoints.remove(wlEntry.getKey());
        } else {
            mapData.redWoolWinPoints = new TreeMap<>();
        }
        mapData.redWoolWinPoints.put(wlEntry.getKey(), wlEntry.getValue());
        return true;
    }

    public boolean addBlueWoolWinPoint(Block block) {
        MapData mapData = maps.get(block.getWorld().getName());
        if (mapData == null) {
            return false;
        }
        if (block.getType() != Material.WOOL) {
            return false;
        }
        Map.Entry<String, Location> wlEntry = getWoolPointFrom(block);
        if (mapData.blueWoolWinPoints != null) {
            mapData.blueWoolWinPoints.remove(wlEntry.getKey());
        } else {
            mapData.blueWoolWinPoints = new TreeMap<>();
        }
        mapData.blueWoolWinPoints.put(wlEntry.getKey(), wlEntry.getValue());
        return true;
    }

    public boolean addwoolSpawner(Block block) {
        MapData mapData = maps.get(block.getWorld().getName());
        if (mapData == null) {
            return false;
        }
        if (block.getType() != Material.WOOL) {
            return false;
        }
        @SuppressWarnings("deprecation")
        Wool wool = new Wool(block.getType(), block.getData());

        if (mapData.woolSpawners != null) {
            mapData.woolSpawners.remove(wool.getColor().toString());
        } else {
            mapData.woolSpawners = new TreeMap<>();
        }
        mapData.woolSpawners.put(wool.getColor().toString(), block.getLocation());
        placeWoolSpawner(block.getLocation(), wool.getColor().toString());
        return true;
    }

    private void placeWoolSpawner(Location loc, String colorName) {
        Wool wool = new Wool(DyeColor.valueOf(colorName));
        Block block = loc.getBlock();
        block.setType(Material.MOB_SPAWNER);
        BlockState genericBlockState = block.getState();
        CreatureSpawner cs = (CreatureSpawner) block.getState();
        ItemStack stack = wool.toItemStack(1);
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                cs.setSpawnedType(block.getWorld().dropItem(block.getLocation(), stack).getType());
                genericBlockState.update();
            }
        });

    }

    public void delRedWoolWinPoint(Block block) {
        MapData mapData = maps.get(block.getWorld().getName());
        if (mapData == null) {
            return;
        }
        if (block.getType() != Material.WOOL) {
            return;
        }
        Map.Entry<String, Location> wlEntry = getWoolPointFrom(block);
        mapData.redWoolWinPoints.remove(wlEntry.getKey());
    }

    public void delBlueWoolWinPoint(Block block) {
        MapData mapData = maps.get(block.getWorld().getName());
        if (mapData == null) {
            return;
        }
        if (block.getType() != Material.WOOL) {
            return;
        }
        Map.Entry<String, Location> wlEntry = getWoolPointFrom(block);
        mapData.blueWoolWinPoints.remove(wlEntry.getKey());
    }

    public void delWoolSpawner(Block block) {
        MapData mapData = maps.get(block.getWorld().getName());
        if (mapData == null) {
            return;
        }
        Map.Entry<String, Location> wlEntry = getWoolPointFrom(block);
        boolean ret;
        ret = mapData.woolSpawners.remove(wlEntry.getKey()) != null;
    }

    public Location getRedWoolWinLocation(World world, DyeColor woolColor) {
        MapData mapData = maps.get(world.getName());
        if (mapData == null) {
            return null;
        }
        if (mapData.redWoolWinPoints == null) {
            return null;
        }
        return mapData.redWoolWinPoints.get(woolColor.toString());
    }

    public Location getBlueWoolWinLocation(World world, DyeColor woolColor) {
        MapData mapData = maps.get(world.getName());
        if (mapData == null) {
            return null;
        }
        if (mapData.blueWoolWinPoints == null) {
            return null;
        }
        return mapData.blueWoolWinPoints.get(woolColor.toString());
    }

    public Location getWoolSpawnerLocation(World world, DyeColor woolColor) {
        MapData mapData = maps.get(world.getName());
        if (mapData == null) {
            return null;
        }
        if (mapData.woolSpawners == null) {
            return null;
        }
        return mapData.woolSpawners.get(woolColor.toString());
    }

    public void setWeather(World world, boolean fixed, boolean storm) {
        MapData mapData = maps.get(world.getName());
        if (mapData == null) {
            return;
        }
        if (mapData.weather == null) {
            mapData.weather = new Weather();
        }

        mapData.weather.fixed = fixed;
        mapData.weather.storm = storm;
    }

    public boolean exists(String mapName) {
        return maps.containsKey(mapName);
    }

    public int getMaxPlayers(String mapName) {
        MapData mapData = maps.get(mapName);
        if (mapData == null) {
            return 0;
        } else {
            return mapData.maxPlayers;
        }
    }

    public List<Location> getWoolWinLocations(String mapName) {
        MapData mapData = maps.get(mapName);
        List<Location> ret;
        if (mapData == null) {
            return null;
        } else {
            ret = new ArrayList<>();
            ret.addAll(mapData.blueWoolWinPoints.values());
            ret.addAll(mapData.redWoolWinPoints.values());
        }
        return ret;
    }

    public MapData getMapData(String mapName) {
        return maps.get(mapName);
    }

    public Selection getRestaurationArea(String mapName) {
        MapData mapData = maps.get(mapName);
        if (mapData == null) {
            return null;
        }
        return mapData.restaurationArea;
    }

    public void setRestaurationArea(Selection sel) {
        MapData mapData = maps.get(sel.getWorld().getName());
        if (mapData == null) {
            return;
        }
        mapData.restaurationArea = sel;
    }

    public boolean setNoDrop(Player player) {
        boolean ret = false;
        MapData mapData = maps.get(player.getWorld().getName());
        if (mapData != null) {
            TreeSet<Material> noDrop = new TreeSet<>();
            boolean warn = false;
            for (ItemStack is: player.getInventory().getContents()) {
                if (is == null){
                    continue;
                }
                if (!warn && noDrop.contains(is.getType())) {
                    warn=true;
                    plugin.getLangManager().sendMessage("repeated-material-warn", player);
                    plugin.getLangManager().sendMessage("repeated-material-info", player);
                    plugin.getLangManager().sendMessage("repeated-material-example", player);
                } else {
                    noDrop.add(is.getType());
                }
            }
            if (noDrop.isEmpty()) {
                plugin.getLangManager().sendMessage("no-drop-is-empty", player);
            }
            mapData.noDropOnBreak = noDrop;
            ret = true;
        } else {
            plugin.getLangManager().sendMessage("cmd-in-a-not-ctw-map", player);
        }
        return ret;
    }


    public void announceAreaBoundering(PlayerMoveEvent e) {
        MapData mapData = maps.get(e.getTo().getWorld().getName());
        if (mapData == null) {
            return;
        }
        Player player = e.getPlayer();
        String direction;
        if (mapData.blueInaccessibleAreas != null) {
            for (Selection sel : mapData.blueInaccessibleAreas) {
                boolean selContainsFrom = sel.contains(e.getFrom());
                boolean selContainsTo = sel.contains(e.getTo());

                if (!selContainsFrom && selContainsTo) {
                    direction = ChatColor.GREEN + "Entrando ";
                    plugin.getLangManager().sendMessage(ChatColor.BLUE + direction + "zona inaccesible para Azules "
                            + ChatColor.GREEN + "("
                            + Utils.toString(sel) + ")", player);
                } else if (selContainsFrom && !selContainsTo) {
                    direction = ChatColor.GREEN + "Saliendo ";
                    plugin.getLangManager().sendMessage(ChatColor.BLUE + direction + "zona inaccesible para Azules "
                            + ChatColor.GREEN + "("
                            + Utils.toString(sel) + ")", player);
                }
            }
        }

        if (mapData.redInaccessibleAreas != null) {
            for (Selection sel : mapData.redInaccessibleAreas) {
                boolean selContainsFrom = sel.contains(e.getFrom());
                boolean selContainsTo = sel.contains(e.getTo());

                if (!selContainsFrom && selContainsTo) {
                    direction = ChatColor.GREEN + "Entrando ";
                    plugin.getLangManager().sendMessage(ChatColor.RED + direction + "zona inaccesible para Rojos "
                            + ChatColor.GREEN + "("
                            + Utils.toString(sel) + ")", player);
                } else if (selContainsFrom && !selContainsTo) {
                    direction = ChatColor.GREEN + "Saliendo ";
                    plugin.getLangManager().sendMessage(ChatColor.RED + direction + "zona inaccesible para Rojos "
                            + ChatColor.GREEN + "("
                            + Utils.toString(sel) + ")", player);
                }
            }
        }
        if (mapData.protectedAreas != null) {
            for (Selection sel : mapData.protectedAreas) {
                boolean selContainsFrom = sel.contains(e.getFrom());
                boolean selContainsTo = sel.contains(e.getTo());

                if (!selContainsFrom && selContainsTo) {
                    direction = ChatColor.GREEN + "Entrando ";
                    plugin.getLangManager().sendMessage(ChatColor.YELLOW + direction + "zona Protegida "
                            + ChatColor.GREEN + "("
                            + Utils.toString(sel) + ")", player);
                } else if (selContainsFrom && !selContainsTo) {
                    direction = ChatColor.GREEN + "Saliendo ";
                    plugin.getLangManager().sendMessage(ChatColor.YELLOW + direction + "zona Protegida "
                            + ChatColor.GREEN + "("
                            + Utils.toString(sel) + ")", player);
                }
            }
        }
    }

    public void removeRegion(Player player) {
        MapData mapData = maps.get(player.getWorld().getName());
        if (mapData == null) {
            return;
        }
        Location loc = player.getLocation();
        if (mapData.blueInaccessibleAreas != null) {
            for (Selection sel : mapData.blueInaccessibleAreas) {
                if (sel.contains(loc)) {
                    mapData.blueInaccessibleAreas.remove(sel);
                    plugin.getLangManager().sendMessage(ChatColor.GREEN + "Eliminado" + ChatColor.BLUE + "zona inaccesible para Azules "
                            + ChatColor.GREEN + "("
                            + Utils.toString(sel) + ")", player);
                    break;
                }
            }
        }

        if (mapData.redInaccessibleAreas != null) {
            for (Selection sel : mapData.redInaccessibleAreas) {
                if (sel.contains(loc)) {
                    mapData.redInaccessibleAreas.remove(sel);
                    plugin.getLangManager().sendMessage(ChatColor.GREEN + "Eliminado" + ChatColor.RED + "zona inaccesible para Rojos "
                            + ChatColor.GREEN + "("
                            + Utils.toString(sel) + ")", player);
                    break;
                }
            }
        }

        if (mapData.protectedAreas != null) {
            for (Selection sel : mapData.protectedAreas) {
                if (sel.contains(loc)) {
                    mapData.protectedAreas.remove(sel);
                    plugin.getLangManager().sendMessage(ChatColor.GREEN + "Eliminado" + ChatColor.YELLOW + "zona Protegida "
                            + ChatColor.GREEN + "("
                            + Utils.toString(sel) + ")", player);
                    break;
                }
            }
        }
    }

    /*
    public void setDefaultKit(Player player) {
        MapData mapData = this.maps.get(player.getWorld().getName());
        if (mapData == null)
            return;
        mapData = (MapData)Bukkit.createInventory(null, InventoryType.PLAYER);
        mapData.kitInv.setContents(player.getInventory().getContents());
    }*/

    public void setKitarmour(World world, boolean active) {
        MapData mapData = this.maps.get(world.getName());
        if (mapData == null)
            return;
        mapData.kitArmour = active;
    }

    public boolean getKitarmour(World world) {
        MapData mapData = this.maps.get(world.getName());
        if (mapData == null)
            return false;
        return mapData.kitArmour;
    }

    public boolean getKitarmour(String mapName) {
        MapData mapData = this.maps.get(mapName);
        if (mapData == null)
            return false;
        return mapData.kitArmour;
    }

    /*
    public Inventory getDefaultKit(String mapName) {
        MapData mapData = this.maps.get(mapName);
        if (mapData == null)
            return null;
        return mapData.kitInv;
    }*/
}
