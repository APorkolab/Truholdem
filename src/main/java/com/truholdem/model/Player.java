package com.truholdem.model;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String id;
    private List<Card> hand = new ArrayList<>();
    private int chips = 1000; // Kezdő zsetonok
    private int currentBet = 0; // Jelenlegi tét
    private boolean folded = false; // Jelzi, hogy a játékos passzolt-e

    public Player(String id) {
        this.id = id;
    }

    // Tét emelése
    public void placeBet(int amount) {
        chips -= amount;
        currentBet += amount;
    }

    // Nyert összeg hozzáadása
    public void addWinnings(int amount) {
        chips += amount;
    }

    // Getterek és setterek
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public int getChips() {
        return chips;
    }

    public void setChips(int chips) {
        this.chips = chips;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    public boolean isFolded() {
        return folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }
}