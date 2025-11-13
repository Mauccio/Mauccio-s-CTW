package com.mauccio.ctw.utils;

import com.mauccio.ctw.CTW;
import com.sk89q.worldedit.bukkit.selections.Selection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Utils {

    public static class Chars {
        public static String wool = "⬅"; // Old: \u2752
        public static String invisible = "\u200B";
    }

    public static class PlayerComparator implements Comparator<Player> {

        @Override
        public int compare(Player o1, Player o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static class LocationBlockComparator implements Comparator<Location> {

        @Override
        public int compare(Location o1, Location o2) {
            int result = o1.getBlockX() - o2.getBlockX();
            if (result == 0) {
                result = o1.getBlockY() - o2.getBlockY();
                if (result == 0) {
                    result = o1.getBlockZ() - o2.getBlockZ();
                    if (result == 0) {
                        result = o1.getWorld().getName().compareTo(o2.getWorld().getName());
                    }
                }
            }

            return result;
        }

    }

    public static class WorldComparator implements Comparator<World> {

        @Override
        public int compare(World o1, World o2) {
            return o1.getName().compareTo(o2.getName());
        }

    }

    public static class SelectionComparator implements Comparator<Selection> {

        @Override
        public int compare(Selection o1, Selection o2) {
            Location o1Max = o1.getMaximumPoint();
            Location o1Min = o1.getMinimumPoint();
            Location o2Max = o2.getMaximumPoint();
            Location o2Min = o2.getMinimumPoint();
            int result;
            result = o1Max.getBlockX() - o2Max.getBlockX();
            if (result == 0) {
                result = o1Max.getBlockY() - o2Max.getBlockY();
                if (result == 0) {
                    result = o1Max.getBlockZ() - o2Max.getBlockZ();
                    if (result == 0) {
                        result = o1Min.getBlockX() - o2Min.getBlockX();
                        if (result == 0) {
                            result = o1Min.getBlockY() - o2Min.getBlockY();
                            if (result == 0) {
                                result = o1Min.getBlockZ() - o2Min.getBlockZ();
                                if (result == 0) {
                                    result = o1.getWorld().getName().compareTo(o2.getWorld().getName());
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }
    }

    public static void firework(CTW plugin, Location loc,
                                Color color1, Color color2, Color color3,
                                FireworkEffect.Type type) {
        World world = loc.getWorld();
        new org.bukkit.scheduler.BukkitRunnable() {

            @Override
            public void run() {

                for (int i = -2; i < 3; i++) {
                    org.bukkit.entity.Firework firework = world.spawn(new org.bukkit.Location(loc.getWorld(), loc.getX() + (i * 5), loc.getY(), loc.getZ()), org.bukkit.entity.Firework.class);
                    org.bukkit.inventory.meta.FireworkMeta data = firework.getFireworkMeta();
                    data.addEffects(org.bukkit.FireworkEffect.builder()
                            .withColor(color1).withColor(color2).withColor(color3).with(type)
                            .trail(new java.util.Random().nextBoolean()).flicker(new java.util.Random().nextBoolean()).build());
                    data.setPower(new java.util.Random().nextInt(2) + 2);
                    firework.setFireworkMeta(data);
                }
            }
        }.runTaskLater(plugin, 10);
    }

    public static ChatColor toChatColor(DyeColor color) {
        ChatColor result;

        switch (color) {
            case BLACK:
                result = ChatColor.BLACK;
                break;
            case BLUE:
                result = ChatColor.DARK_BLUE;
                break;
            case BROWN:
                result = ChatColor.GOLD;
                break;
            case CYAN:
                result = ChatColor.DARK_AQUA;
                break;
            case GRAY:
                result = ChatColor.DARK_GRAY;
                break;
            case GREEN:
                result = ChatColor.DARK_GREEN;
                break;
            case LIGHT_BLUE:
                result = ChatColor.AQUA;
                break;
            case LIME:
                result = ChatColor.GREEN;
                break;
            case MAGENTA:
                result = ChatColor.DARK_PURPLE;
                break;
            case ORANGE:
                result = ChatColor.GOLD;
                break;
            case PINK:
                result = ChatColor.LIGHT_PURPLE;
                break;
            case PURPLE:
                result = ChatColor.DARK_PURPLE;
                break;
            case RED:
                result = ChatColor.RED;
                break;
            case SILVER:
                result = ChatColor.GRAY;
                break;
            case WHITE:
                result = ChatColor.WHITE;
                break;
            case YELLOW:
                result = ChatColor.YELLOW;
                break;
            default:
                result = ChatColor.WHITE;
        }
        return result;
    }

    public static DyeColor chatColorToDyeColor(ChatColor chatColor) {
        if (chatColor == null) {
            return DyeColor.WHITE; 
        }

        switch (chatColor) {
            case BLACK:
                return DyeColor.BLACK;
            case DARK_BLUE:
            case BLUE:
                return DyeColor.BLUE;
            case DARK_GREEN:
                return DyeColor.GREEN;
            case DARK_AQUA:
            case AQUA:
                return DyeColor.LIGHT_BLUE; 
            case DARK_RED:
            case RED:
                return DyeColor.RED;
            case DARK_PURPLE:
                return DyeColor.PURPLE;
            case GOLD:
            case YELLOW:
                return DyeColor.YELLOW;
            case GRAY:
            case DARK_GRAY:
                return DyeColor.GRAY;
            case GREEN:
                return DyeColor.LIME;
            case LIGHT_PURPLE:
                return DyeColor.MAGENTA;
            case WHITE:
            default:
                return DyeColor.WHITE;
        }
    }

    public static Color chatColorToBukkitColor(ChatColor chatColor) {
        if (chatColor == null) return Color.WHITE;

        switch (chatColor) {
            case BLACK:
                return Color.BLACK;
            case DARK_BLUE:
                return Color.NAVY;
            case DARK_GREEN:
                return Color.GREEN;
            case DARK_AQUA:
                return Color.TEAL;
            case DARK_RED:
                return Color.MAROON;
            case DARK_PURPLE:
                return Color.PURPLE;
            case GOLD:
                return Color.ORANGE;
            case GRAY:
                return Color.SILVER;
            case DARK_GRAY:
                return Color.GRAY;
            case BLUE:
                return Color.BLUE;
            case GREEN:
                return Color.LIME;
            case AQUA:
                return Color.AQUA;
            case RED:
                return Color.RED;
            case LIGHT_PURPLE:
                return Color.FUCHSIA;
            case YELLOW:
                return Color.YELLOW;
            case WHITE:
            default:
                return Color.WHITE;
        }
    }


    public static String randomIdentifier() {

        String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890";

        java.util.Random rand = new java.util.Random();

        Set<String> identifiers = new HashSet<String>();
        StringBuilder builder = new StringBuilder();
        while (builder.toString().length() == 0) {
            int length = rand.nextInt(5) + 5;
            for (int i = 0; i < length; i++) {
                builder.append(lexicon.charAt(rand.nextInt(lexicon.length())));
            }
            if (identifiers.contains(builder.toString())) {
                builder = new StringBuilder();
            }
        }
        return builder.toString();
    }

    public static String toString(Selection sel) {
        Location min = sel.getMinimumPoint();
        Location max = sel.getMaximumPoint();
        return "X:" + min.getBlockX() + ", Y:" + min.getBlockY()
                + ", Z:" + min.getBlockZ() + " -> X:" + max.getBlockX()
                + ", Y:" + max.getBlockY() + ", Z:" + max.getBlockZ();
    }

    public static ChatColor parseChatColor(String name) {
        try { return ChatColor.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return ChatColor.WHITE; }
    }

    public static org.bukkit.Color parseLeatherColor(String name) {
        switch(name.toLowerCase()) {
            case "black": return org.bukkit.Color.fromRGB(10,10,10);
            case "red": return org.bukkit.Color.fromRGB(255,0,0);
            case "blue": return org.bukkit.Color.fromRGB(0,0,255);
            case "cyan": case "aqua": return org.bukkit.Color.fromRGB(0,255,255);
            case "pink": return org.bukkit.Color.fromRGB(255,128,192);
            case "green": return org.bukkit.Color.fromRGB(0,170,0);
            case "yellow": return org.bukkit.Color.fromRGB(255,255,0);
            case "orange": return org.bukkit.Color.fromRGB(255,128,0);
            case "purple": return org.bukkit.Color.fromRGB(128,0,255);
            case "white": return org.bukkit.Color.fromRGB(255,255,255);
            case "gray": case "grey": return org.bukkit.Color.fromRGB(128,128,128);
            case "light_gray": case "light_grey": return org.bukkit.Color.fromRGB(192,192,192);
            case "brown": return org.bukkit.Color.fromRGB(153,76,0);
            case "lime": return org.bukkit.Color.fromRGB(128,255,0);
            case "magenta": return org.bukkit.Color.fromRGB(255,0,255);
            default: return org.bukkit.Color.fromRGB(255,255,255);
        }
    }

    public static String normalizeId(String raw) {
        return raw.trim().toLowerCase().replaceAll("\\s+", "_");
    }
}
