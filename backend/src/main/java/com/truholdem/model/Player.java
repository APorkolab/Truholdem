package com.truholdem.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Player {
    private String id;
    private String name;
    private List<Card> hand = new ArrayList<>();
    private int chips;
    private int betAmount;
    private boolean isFolded;
    private boolean isBot;

    public Player(String name, int startingChips, boolean isBot) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.chips = startingChips;
        this.isFolded = false;
        this.isBot = isBot;
    }

    public void placeBet(int amount) {
        if (amount <= 0 || amount > chips) {
            throw new IllegalArgumentException("Invalid bet amount");
        }
        betAmount += amount;
        chips -= amount;
    }

    public void addWinnings(int amount) {
        this.chips += amount;
    }

    public void fold() {
        isFolded = true;
    }

    public void addCardToHand(Card card) {
        hand.add(card);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isFolded() {
        return isFolded;
    }

    public int getBetAmount() {
        return betAmount;
    }

    public List<Card> getHand() {
        return hand;
    }

    public int getChips() {
        return chips;
    }

    public void setFolded(boolean folded) {
        isFolded = folded;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }

    public void setBetAmount(int betAmount) {
        this.betAmount = betAmount;
    }

    public void setChips(int chips) {
        this.chips = chips;
    }

    public boolean isBot() {
        return isBot;
    }

    public void clearHand() {
        this.hand.clear();
    }
}