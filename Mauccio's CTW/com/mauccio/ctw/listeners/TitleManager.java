package com.mauccio.ctw.listeners;

import com.mauccio.ctw.CTW;
import com.mauccio.ctw.libs.titleapi.TitleAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class TitleManager {

    private final CTW plugin;
    private final Title title;
    private final Subtitle subtitle;
    private final FileConfiguration config;

    public TitleManager(CTW plugin) {
        this.plugin = plugin;
        this.title = new Title();
        this.subtitle = new Subtitle();
        this.config = plugin.getConfig();
    }

    private class Title {

        String joinRoom;
        String changeMap;
        String joinRed;
        String joinBlue;
        String winRed;
        String winBlue;
        String headshot;
        String woolPickup;
        String woolPlaced;
        String count30;
        String count20;
        String count10;
        String count5;
        String count4;
        String count3;
        String count2;
        String count1;

        public Title() {

            joinRoom = plugin.getLangManager().getText("titles.join.room");
            joinRed = plugin.getLangManager().getText("titles.join.red");
            joinBlue = plugin.getLangManager().getText("titles.join.blue");
            changeMap = plugin.getLangManager().getText("titles.change-map");
            winRed = plugin.getLangManager().getText("titles.team-win.red");
            winBlue = plugin.getLangManager().getText("titles.team-win.blue");
            headshot = plugin.getLangManager().getText("titles.headshot");
            woolPickup = plugin.getLangManager().getText("titles.wool-pickup");
            woolPlaced = plugin.getLangManager().getText("titles.wool-placed");
            count30 = plugin.getLangManager().getText("titles.countdown.thirty-seconds");
            count20 = plugin.getLangManager().getText("titles.countdown.twenty-seconds");
            count10 = plugin.getLangManager().getText("titles.countdown.ten-seconds");
            count5 = plugin.getLangManager().getText("titles.countdown.five-seconds");
            count4 = plugin.getLangManager().getText("titles.countdown.four-seconds");
            count3 = plugin.getLangManager().getText("titles.countdown.three-seconds");
            count2 = plugin.getLangManager().getText("titles.countdown.two-seconds");
            count1 = plugin.getLangManager().getText("titles.countdown.one-second");

        }

        public String getJoinRoom() {
            return joinRoom;
        }

        public String getChangeMap() {
            return changeMap;
        }

        public String getJoinRed() {
            return joinRed;
        }

        public String getJoinBlue() {
            return joinBlue;
        }

        public String getWinRed() {
            return winRed;
        }

        public String getWinBlue() {
            return winBlue;
        }

        public String getHeadshot() {
            return headshot;
        }

        public String getWoolPickup() {
            return woolPickup;
        }

        public String getWoolPlaced() {
            return woolPlaced;
        }

        public String getCount30() {
            return count30;
        }

        public String getCount20() {
            return count20;
        }

        public String getCount10() {
            return count10;
        }

        public String getCount5() {
            return count5;
        }

        public String getCount4() {
            return count4;
        }

        public String getCount3() {
            return count3;
        }

        public String getCount2() {
            return count2;
        }

        public String getCount1() {
            return count1;
        }
    }

    public class Subtitle {

        String joinRoom;
        String changeMap;
        String joinRed;
        String joinBlue;
        String winRed;
        String winBlue;
        String headshot;
        String woolPickup;
        String woolPlaced;
        String count30;
        String count20;
        String count10;
        String count5;
        String count4;
        String count3;
        String count2;
        String count1;

        public Subtitle() {

            joinRoom = plugin.getLangManager().getText("subtitles.join.room");
            joinRed = plugin.getLangManager().getText("subtitles.join.red");
            joinBlue = plugin.getLangManager().getText("subtitles.join.blue");
            changeMap = plugin.getLangManager().getText("subtitles.change-map");
            winRed = plugin.getLangManager().getText("subtitles.team-win.red");
            winBlue = plugin.getLangManager().getText("subtitles.team-win.blue");
            headshot = plugin.getLangManager().getText("subtitles.headshot");
            woolPickup = plugin.getLangManager().getText("subtitles.wool-pickup");
            woolPlaced = plugin.getLangManager().getText("subtitles.wool-placed");
            count30 = plugin.getLangManager().getText("subtitles.countdown.thirty-seconds");
            count20 = plugin.getLangManager().getText("subtitles.countdown.twenty-seconds");
            count10 = plugin.getLangManager().getText("subtitles.countdown.ten-seconds");
            count5 = plugin.getLangManager().getText("subtitles.countdown.five-seconds");
            count4 = plugin.getLangManager().getText("subtitles.countdown.four-seconds");
            count3 = plugin.getLangManager().getText("subtitles.countdown.three-seconds");
            count2 = plugin.getLangManager().getText("subtitles.countdown.two-seconds");
            count1 = plugin.getLangManager().getText("subtitles.countdown.one-second");

        }

        public String getJoinRoom() {
            return joinRoom;
        }

        public String getChangeMap() {
            return changeMap;
        }

        public String getJoinRed() {
            return joinRed;
        }

        public String getJoinBlue() {
            return joinBlue;
        }

        public String getWinRed() {
            return winRed;
        }

        public String getWinBlue() {
            return winBlue;
        }

        public String getHeadshot() {
            return headshot;
        }

        public String getWoolPickup() {
            return woolPickup;
        }

        public String getWoolPlaced() {
            return woolPlaced;
        }

        public String getCount30() {
            return count30;
        }

        public String getCount20() {
            return count20;
        }

        public String getCount10() {
            return count10;
        }

        public String getCount5() {
            return count5;
        }

        public String getCount4() {
            return count4;
        }

        public String getCount3() {
            return count3;
        }

        public String getCount2() {
            return count2;
        }

        public String getCount1() {
            return count1;
        }
    }

    /**
     *           Central Title Sender
     * @param player : Player to send the title.
     * @param key : Uses config.yml to get title times.
     * @param title : Text to display into the title.
     * @param subtitle : Text to display into the subtitle.
     */

    public void send(Player player, String key, String title, String subtitle) {
        int fadeIn = config.getInt("titles." + key + ".fade-in",
                10);
        int stay   = config.getInt("titles." + key + ".stay",
                30);
        int fadeOut= config.getInt("titles." + key + ".fade-out",
                10);
        TitleAPI.sendFullTitle(player, fadeIn, stay, fadeOut, title, subtitle);
    }

    public void sendTabList(Player player, String header, String footer) {
        TitleAPI.sendTabTitle(player,
                header,
                footer);
    }

    /**
     *             Title Senders
     * @param player : Player to send the title.
     */

    public void sendJoinRoom(Player player) {
        send(player, "join.room",
                title.getJoinRoom(),
                subtitle.getJoinRoom());
    }

    public void sendJoinRed(Player player) {
        send(player, "join.red",
                title.getJoinRed(),
                subtitle.getJoinRed());
    }

    public void sendJoinBlue(Player player) {
        send(player, "join.blue",
                title.getJoinBlue(),
                subtitle.getJoinBlue());
    }

    public void sendChangeMap(Player player) {
        send(player, "change-map",
                title.getChangeMap(),
                subtitle.getChangeMap());
    }

    public void sendWinRed(Player player) {
        send(player, "team-win.red",
                title.getWinRed(),
                subtitle.getWinRed());
    }

    public void sendWinBlue(Player player) {
        send(player, "team-win.blue",
                title.getWinBlue(),
                subtitle.getWinBlue());
    }

    public void sendHeadshot(Player player) {
        send(player, "headshot",
                title.getHeadshot(),
                subtitle.getHeadshot());
    }

    /**
     *                      Wool Pickup
     * @param playerNameColored : Player name with Team Color.
     * @param woolNameColored : Wool name with Chat Color.
     */
    public void sendWoolPickup(Player player, String playerNameColored, String woolNameColored) {
        send(player, "wool-pickup",
                title.getWoolPickup(),
                subtitle.getWoolPickup()
                        .replace("%PLAYER%", playerNameColored)
                        .replace("%WOOL%", woolNameColored)
        );
    }

    /**
     *                      Wool Placed
     * @param playerNameColored : Player name with Team Color.
     * @param woolNameColored : Wool name with Chat Color.
     */
    public void sendWoolPlaced(Player player, String playerNameColored, String woolNameColored) {
        send(player, "wool-placed",
                title.getWoolPlaced(),
                subtitle.getWoolPlaced()
                .replace("%PLAYER%", playerNameColored)
                .replace("%WOOL%", woolNameColored)
        );
    }

    public void sendCountdown30(Player player) {
        send(player, "countdown", title.getCount30(), subtitle.getCount30());
    }

    public void sendCountdown20(Player player) {
        send(player, "countdown", title.getCount20(), subtitle.getCount20());
    }

    public void sendCountdown10(Player player) {
        send(player, "countdown", title.getCount10(), subtitle.getCount10());
    }

    public void sendCountdown5(Player player) {
        send(player, "countdown", title.getCount5(), subtitle.getCount5());
    }

    public void sendCountdown4(Player player) {
        send(player, "countdown", title.getCount4(), subtitle.getCount4());
    }

    public void sendCountdown3(Player player) {
        send(player, "countdown", title.getCount3(), subtitle.getCount3());
    }

    public void sendCountdown2(Player player) {
        send(player, "countdown", title.getCount2(), subtitle.getCount2());
    }

    public void sendCountdown1(Player player) {
        send(player, "countdown", title.getCount1(), subtitle.getCount1());
    }

    public void sendActionBar(Player player, String message) {
        TitleAPI.sendActionBar(player, message);
    }

    /*
    public void sendPointActionBar(Player player) {
        sendActionBar(
                player,
                plugin.getLangManager().getText("actionbars.points")
        );
    }
     */
}
