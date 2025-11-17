package com.mauccio.ctw.listeners;

import com.mauccio.ctw.CTW;
import com.mauccio.ctw.libs.titleapi.TitleAPI;
import org.bukkit.entity.Player;

public class TitleManager {

    private final CTW plugin;
    private final Title title;
    private final Subtitle subtitle;

    public TitleManager(CTW plugin) {
        this.plugin = plugin;
        this.title = new Title();
        this.subtitle = new Subtitle();
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

            joinRoom = plugin.getLangManager().getTitleMessage("titles.join.room");
            joinRed = plugin.getLangManager().getTitleMessage("titles.join.red");
            joinBlue = plugin.getLangManager().getTitleMessage("titles.join.blue");
            changeMap = plugin.getLangManager().getTitleMessage("titles.change-map");
            winRed = plugin.getLangManager().getTitleMessage("titles.team-win.red");
            winBlue = plugin.getLangManager().getTitleMessage("titles.team-win.blue");
            headshot = plugin.getLangManager().getTitleMessage("titles.headshot");
            woolPickup = plugin.getLangManager().getTitleMessage("titles.wool-pickup");
            woolPlaced = plugin.getLangManager().getTitleMessage("titles.wool-placed");
            count30 = plugin.getLangManager().getTitleMessage("titles.countdown.thirty-seconds");
            count20 = plugin.getLangManager().getTitleMessage("titles.countdown.twenty-seconds");
            count10 = plugin.getLangManager().getTitleMessage("titles.countdown.ten-seconds");
            count5 = plugin.getLangManager().getTitleMessage("titles.countdown.five-seconds");
            count4 = plugin.getLangManager().getTitleMessage("titles.countdown.four-seconds");
            count3 = plugin.getLangManager().getTitleMessage("titles.countdown.three-seconds");
            count2 = plugin.getLangManager().getTitleMessage("titles.countdown.two-seconds");
            count1 = plugin.getLangManager().getTitleMessage("titles.countdown.one-second");

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

            joinRoom = plugin.getLangManager().getTitleMessage("subtitles.join.room");
            joinRed = plugin.getLangManager().getTitleMessage("subtitles.join.red");
            joinBlue = plugin.getLangManager().getTitleMessage("subtitles.join.blue");
            changeMap = plugin.getLangManager().getTitleMessage("subtitles.change-map");
            winRed = plugin.getLangManager().getTitleMessage("subtitles.team-win.red");
            winBlue = plugin.getLangManager().getTitleMessage("subtitles.team-win.blue");
            headshot = plugin.getLangManager().getTitleMessage("subtitles.headshot");
            woolPickup = plugin.getLangManager().getTitleMessage("subtitles.wool-pickup");
            woolPlaced = plugin.getLangManager().getTitleMessage("subtitles.wool-placed");
            count30 = plugin.getLangManager().getTitleMessage("subtitles.countdown.thirty-seconds");
            count20 = plugin.getLangManager().getTitleMessage("subtitles.countdown.twenty-seconds");
            count10 = plugin.getLangManager().getTitleMessage("subtitles.countdown.ten-seconds");
            count5 = plugin.getLangManager().getTitleMessage("subtitles.countdown.five-seconds");
            count4 = plugin.getLangManager().getTitleMessage("subtitles.countdown.four-seconds");
            count3 = plugin.getLangManager().getTitleMessage("subtitles.countdown.three-seconds");
            count2 = plugin.getLangManager().getTitleMessage("subtitles.countdown.two-seconds");
            count1 = plugin.getLangManager().getTitleMessage("subtitles.countdown.one-second");

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

    public void sendJoinRoom(Player player) {
        TitleAPI.sendFullTitle(player,
                10, 30, 10,
                title.getJoinRoom(),
                subtitle.getJoinRoom());
    }

    public void sendJoinRed(Player player) {
        TitleAPI.sendFullTitle(player,
                10, 30, 10,
                title.getJoinRed(), subtitle.getJoinRed());
    }

    public void sendJoinBlue(Player player) {
        TitleAPI.sendFullTitle(player,
                10, 30, 10,
                title.getJoinBlue(),
                subtitle.getJoinBlue());
    }

    public void sendChangeMap(Player player) {
        TitleAPI.sendFullTitle(player,
                10, 30, 10,
                title.getChangeMap(),
                subtitle.getChangeMap());
    }

    public void sendWinRed(Player player) {
        TitleAPI.sendFullTitle(player,
                10, 30, 10,
                title.getWinRed(),
                subtitle.getWinRed());
    }

    public void sendWinBlue(Player player) {
        TitleAPI.sendFullTitle(player,
                10, 30, 10,
                title.getWinBlue(),
                subtitle.getWinBlue());
    }

    public void sendHeadshot(Player player) {
        TitleAPI.sendFullTitle(player,
                10, 30, 10,
                title.getHeadshot(),
                subtitle.getHeadshot());
    }

    public void sendWoolPickup(Player player, String playerNameColored, String woolNameColored) {
        TitleAPI.sendFullTitle(
                player,
                10, 60, 10,
                title.getWoolPickup(),
                subtitle.getWoolPickup()
                        .replace("%PLAYER%", playerNameColored)
                        .replace("%WOOL%", woolNameColored)
        );
    }


    public void sendWoolPlaced(Player player, String playerNameColored, String woolNameColored) {
        TitleAPI.sendFullTitle(player,
                10, 30, 10,
                title.getWoolPlaced(),
                subtitle.getWoolPlaced()
                .replace("%PLAYER%", playerNameColored)
                .replace("%WOOL%", woolNameColored));
    }

    public void sendCountdown30(Player player) {
        TitleAPI.sendFullTitle(player, 10, 30, 10, title.getCount30(), subtitle.getCount30());
    }

    public void sendCountdown20(Player player) {
        TitleAPI.sendFullTitle(player, 10, 30, 10, title.getCount20(), subtitle.getCount20());
    }

    public void sendCountdown10(Player player) {
        TitleAPI.sendFullTitle(player, 10, 30, 10, title.getCount10(), subtitle.getCount10());
    }

    public void sendCountdown5(Player player) {
        TitleAPI.sendFullTitle(player, 10, 30, 10, title.getCount5(), subtitle.getCount5());
    }

    public void sendCountdown4(Player player) {
        TitleAPI.sendFullTitle(player, 10, 30, 10, title.getCount4(), subtitle.getCount4());
    }

    public void sendCountdown3(Player player) {
        TitleAPI.sendFullTitle(player, 10, 30, 10, title.getCount3(), subtitle.getCount3());
    }

    public void sendCountdown2(Player player) {
        TitleAPI.sendFullTitle(player, 10, 30, 10, title.getCount2(), subtitle.getCount2());
    }

    public void sendCountdown1(Player player) {
        TitleAPI.sendFullTitle(player, 10, 30, 10, title.getCount1(), subtitle.getCount1());
    }
}
