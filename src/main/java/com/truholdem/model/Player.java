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

    // Tét emelése validációval
    public void placeBet(int amount) {
        if (amount < 0 || amount > chips) {
            throw new IllegalArgumentException("Invalid bet amount.");
        }
        chips -= amount;
        currentBet += amount;
    }

    // Nyert összeg hozzáadása
    public void addWinnings(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Winnings cannot be negative.");
        }
        chips += amount;
    }

    // Kártya hozzáadása a kézhez
    public void addCardToHand(Card card) {
        if (card != null) {
            hand.add(card);
        }
    }

    // Kártya eltávolítása a kézből
    public void removeCardFromHand(Card card) {
        hand.remove(card);
    }

    // Getterek
    public String getId() {
        return id;
    }

    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    public void clearHand() {
        this.hand.clear();
    }

    public int getChips() {
        return chips;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public boolean isFolded() {
        return folded;
    }

    // Setterek
    public void setId(String id) {
        this.id = id;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    @Override
    public String toString() {
        return "Player{" +
                "id='" + id + '\'' +
                ", hand=" + hand +
                ", chips=" + chips +
                ", currentBet=" + currentBet +
                ", folded=" + folded +
                '}';
    }

    public void setChips(int chips) {
        this.chips = chips;
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }
}