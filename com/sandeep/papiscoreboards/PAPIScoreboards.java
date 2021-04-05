package com.sandeep.papiscoreboards;

import com.google.common.collect.Maps;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.stream.Collectors;

// Imports here

public class PAPIScoreboards extends JavaPlugin implements Listener {

    private static List<UUID> toggledOff;

    // Map I made to store the scoreboard data in case I need to refresh it/update it
    private static Map<UUID, ScoreboardData> scoreboardDataMap;
    private final static String specialKey = "Oa4AuHRaOn";

    private int currentIndex; // Holding the iteration of players

    private static List<String> untranslated; // Holds the lines, so I don't need to keep reading the config
    private static String title; // Holds the title, so I don't need to keep reading the config

    public static void togglePlayer(UUID player) {
        if (toggledOff.contains(player))
            toggledOff.remove(player);
        else
            toggledOff.add(player);
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this); // Register events

        scoreboardDataMap = Maps.newHashMap(); // Initialise the map
        toggledOff = new ArrayList<>();

        saveDefaultConfig(); // Save/load the config

        currentIndex = 0; // initialise the index to 0 (first player)
        untranslated = getConfig().getStringList("scoreboard.lines"); // initialise the variables from the config
        title = getConfig().getString("scoreboard.title"); // same as above

        // Running task
        Bukkit.getScheduler().runTaskTimer(this, () -> {

            if (Bukkit.getOnlinePlayers().isEmpty()) return; // Won't do anything if no players are online

            Player player; // Make player instance

            try { // Try and make the player equal to the %currentIndex%th player, so if currentIndex is 0 then first player, 1 = second player, 2 = ....
                player = Bukkit.getOnlinePlayers().toArray(new Player[]{})[currentIndex];
                if (player == null) { // If player isn't found then restart the cycle
                    currentIndex = 0;
                }
            } catch (Exception ex) { // IF an errors then restart cycle (errors with the player count being lower than the currentIndex)
                currentIndex = 0;
            }
            player = Bukkit.getOnlinePlayers().toArray(new Player[]{})[currentIndex]; // set the p;layer

            if (toggledOff.contains(player.getUniqueId())) {
                currentIndex++;
                return;
            }
            // Enter the player into the map
            scoreboardDataMap.put(player.getUniqueId(), new ScoreboardData(player.getUniqueId(), PAPIScoreboards.this.runTask(player)));

            currentIndex++; // Increase index

        }, 5L, 5L); // Task runs every 5 ticks ( 20 ticks = 1 second )

        getCommand("reloadscoreboard").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                double now = System.currentTimeMillis();
                reloadConfig();
                scoreboardDataMap = Maps.newHashMap(); // Initialise the map
                untranslated = getConfig().getStringList("scoreboard.lines"); // initialise the variables from the config
                title = getConfig().getString("scoreboard.title"); // same as above
                commandSender.sendMessage(ChatColor.GREEN + "Reloaded config in " + (((double)System.currentTimeMillis() - now) / 1000) + "s");
                return false;
            }
        });

    }

    public static List<String> translate(Player player) {
        return PlaceholderAPI.setPlaceholders(player, untranslated); // This will translate all lines according to the given player
    }

    public static String runTask(Player player) {
        StringBuilder data = new StringBuilder(); // Initialise newData

        List<String> translated = translate(player); // Get translated lines

        for (String next : translated) {
            data.append(next).append(specialKey);
        }

        if (scoreboardDataMap.containsKey(player.getUniqueId())) { // If the player has been updated before... (in the map)
            ScoreboardData scoreboardData = scoreboardDataMap.get(player.getUniqueId());// Get their scoreboard data (from map)
            if (scoreboardData.canUpdate(data.toString())) { // check if we can update their scoreboard?
//                showBoard(player, translated);// if we can then resetBoard
                nfShowBoard(player, data.toString(), scoreboardData.getData());
            }
        } else {
//            showBoard(player, translated); // If it is first time then we just show scoreboard
            showBoard(player, translated);
        }

        return data.toString(); // Return the newData that is on the scoreboard right now
    }

    @EventHandler
    public void onDie(PlayerRespawnEvent event) {
        runTask(event.getPlayer());
        showBoard(event.getPlayer(), translate(event.getPlayer()));
    }

    public static void showBoard(Player player, List<String> translated) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard(); // New scoreboard
        Objective objective = scoreboard.registerNewObjective("pwScoreboard", "dummy"); // Set objective as dummy
        objective.setDisplaySlot(DisplaySlot.SIDEBAR); // Set slot on SIDEBAR
        String header = ChatColor.translateAlternateColorCodes('&',
                PlaceholderAPI.setPlaceholders(player, title));
        if (header.length() > 32) {
            objective.setDisplayName(header.substring(0, 32)); // Set the name (top of scoreboard)
        } else {
            objective.setDisplayName(header);
        }

        List<String> completed = new ArrayList<>(); // Make list to hold repeated scores since repeated scores don't work

        for (int i = 0; i < translated.size(); i++) {  // iterate through the list size
            String s = translated.get(i); // get the object from the current list index so the LINE
//
//            while (completed.contains(s)) { // while LINE is already on the scoreboard (it won't show if it is already on there)
//                s = ChatColor.values()[new Random().nextInt(ChatColor.values().length)] + ChatColor.values()[new Random().nextInt(ChatColor.values().length)].toString()
//                + ChatColor.values()[new Random().nextInt(ChatColor.values().length)] + ChatColor.values()[new Random().nextInt(ChatColor.values().length)].toString() + s;
//                // We add couple invisible color codes to the start of it so there is no visible difference and servers thinks it is not the same
//            }

            completed.add(s); // Add new value to the completed list

            if (s.length() > 40) { // Make sure the LINE is under 40 characters
                s = s.substring(0, 40); // We use substring here
            }

            Score score = objective.getScore(ChatColor.translateAlternateColorCodes('&', s)); // Then register the new score
            score.setScore(translated.size() - i); // Set the score's score to 15 minus %PLACEMENT%, so it goes 15,14,13,12...
        }
        player.setScoreboard(scoreboard); // show scoreboard

    }

    private static String mapString(String s) {
//        while (s.length() < 32) { // while LINE is already on the scoreboard (it won't show if it is already on there)
//            s = ChatColor.values()[new Random().nextInt(ChatColor.values().length)] + ChatColor.values()[new Random().nextInt(ChatColor.values().length)].toString()
//                    + ChatColor.values()[new Random().nextInt(ChatColor.values().length)] + ChatColor.values()[new Random().nextInt(ChatColor.values().length)].toString() + s;
//            // We add couple invisible color codes to the start of it so there is no visible difference and servers thinks it is not the same
//        }
        if (s.length() > 40) { // Make sure the LINE is under 40 characters
            s = s.substring(0, 40); // We use substring here
        }

        return s;
    }

    private static void nfShowBoard(Player player, String newData, String oldData) {
        String header = ChatColor.translateAlternateColorCodes('&',
                PlaceholderAPI.setPlaceholders(player, title));
        if (header.length() > 32) {
            header = header.substring(0, 32); // Set the name (top of scoreboard)
        }

        boolean foundHeader = false;

        Objective objective = null;

        for (final Objective obj : player.getScoreboard().getObjectives()) {
            if (ChatColor.stripColor(obj.getDisplayName()).equalsIgnoreCase(ChatColor.stripColor(header)) && obj.getDisplaySlot() == DisplaySlot.SIDEBAR) {
                foundHeader = true;
                objective = obj;
                break;
            }
        }

        if (!foundHeader || objective == null) {
            showBoard(player, translate(player));
            return;
        }
        List<String> currentLines = Arrays.stream(oldData.split(specialKey)).map(PAPIScoreboards::mapString).collect(Collectors.toList());
        List<String> newLines = Arrays.stream(newData.split(specialKey)).map(PAPIScoreboards::mapString).collect(Collectors.toList());

        for (int i = 0; i < currentLines.size(); i++) {
            if (!ChatColor.stripColor(currentLines.get(i)) .equalsIgnoreCase( ChatColor.stripColor(newLines.get(i)))) {
                int score = objective.getScore(currentLines.get(i)).getScore();
                objective.getScoreboard().resetScores(currentLines.get(i));
                objective.getScore(newLines.get(i)).setScore(score);
            }
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this); // When plugin disables we cancel the task above ^ (onEnable)
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { // When player leaves
//        CraftPlayer cp = (CraftPlayer)event.getPlayer();
//        CraftScoreboardManager sbm = (CraftScoreboardManager) Bukkit.getScoreboardManager();
//        sbm.removePlayer(cp); // We want to make sure there are no scoreboard memory leaks, so we remove the scoreboard from the player

        event.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());

        // Remove them from the scoreboardDataMap, so their scoreboard is fresh and new when they log back in!
        scoreboardDataMap.remove(event.getPlayer().getUniqueId());
    }

}
