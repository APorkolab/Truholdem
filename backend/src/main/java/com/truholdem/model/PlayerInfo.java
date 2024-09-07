package com.truholdem.model;

public class PlayerInfo extends Player {
    private String name;
    private int startingChips;
    private boolean isBot;

    // Konstruktor és getterek/setterek
    public PlayerInfo(String name, int startingChips, boolean isBot) {
        super();
        this.name = name;
        this.startingChips = startingChips;
        this.isBot = isBot;
    }

    public String getName() {
        return name;
    }

    public int getStartingChips() {
        return startingChips;
    }

    public boolean isBot() {
        return isBot;
    }
}
