package com.truholdem.model;

import java.util.ArrayList;
import java.util.List;

public class GameStatus {
    private List<Card> communityCards = new ArrayList<>();
    private List<Player> players = new ArrayList<>();
    private GamePhase phase = GamePhase.PRE_FLOP; // Kezdő állapot
    private int currentPot = 0; // Jelenlegi pot mérete

    // Játékfázisok enumja
    public enum GamePhase {
        PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN;
    }

    // Játékfázis frissítése
    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    // Pot növelése
    public void addToPot(int amount) {
        currentPot += amount;
    }

    // Getterek és Setterek
    public List<Card> getCommunityCards() {
        return communityCards;
    }

    public void setCommunityCards(List<Card> communityCards) {
        this.communityCards = communityCards;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public int getCurrentPot() {
        return currentPot;
    }

    public void setCurrentPot(int currentPot) {
        this.currentPot = currentPot;
    }
}