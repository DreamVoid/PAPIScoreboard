package com.sandeep.papiscoreboards;

import java.util.UUID;

public class ScoreboardData {

    // Basic OOP class that stores String data and UUID of player
    private String data;

    private UUID player;

    public ScoreboardData(UUID player, String data) {
        this.data = data;
        this.player = player;
    }

    // Method that tells me whether new data is equal to old data and if it is not it will return true so I can update the scoreboard
    public boolean canUpdate(String newData) {
        return !data.equalsIgnoreCase(newData);
    }

    public String getData() {
        return data;
    }
}
