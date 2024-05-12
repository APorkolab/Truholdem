package com.truholdem.model;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String id;
    private String name;
    private List<Card> hand = new ArrayList<>();
    private int chips = 1000; // Kezdő zsetonok
    private int betAmount = 0; // Jelenlegi tét
    private boolean folded = false; // Jelzi, hogy a játékos passzolt-e

    public Player(String id) {
        this.id = id;
    }

    // Tét emelése
    public void placeBet(int amount) {
        if (amount > chips) {
            throw new IllegalArgumentException("Not enough chips.");
        }
        betAmount += amount;
        chips -= amount;
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

    // Getterek és Setterek
    public String getId() {
        return id;
    }

    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    public int getChips() {
        return chips;
    }

    public int getBetAmount() {
        return betAmount;
    }

    public boolean isFolded() {
        return folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    public void clearHand() {
        hand.clear();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // A játékos állapotának JSON formátumban való kiíratása
    public String toJson() {
        return String.format("{\"id\":\"%s\", \"hand\":%s, \"chips\":%d, \"betAmount\":%d, \"isFolded\":%b}",
                id, hand.toString(), chips, betAmount, folded);
    }

    // További setterek
    public void setChips(int chips) {
        this.chips = chips;
    }

    public void setBetAmount(int betAmount) {
        this.betAmount = betAmount;
    }

    @Override
    public String toString() {
        return "Player{" +
                "id='" + id + '\'' +
                ", hand=" + hand +
                ", chips=" + chips +
                ", betAmount=" + betAmount +
                ", folded=" + folded +
                '}';
    }
}
