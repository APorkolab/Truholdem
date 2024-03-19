package com.truholdem.model;

import java.util.ArrayList;
import java.util.List;

public class GameStatus {
    private List<Card> communityCards = new ArrayList<>();
    private List<Player> players = new ArrayList<>();
    private GamePhase phase = GamePhase.PRE_FLOP; // Kezdő állapot
    private int currentPot = 0;

    private int currentBet = 0;

    public int getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    // Játékfázisok enumja
    public enum GamePhase {
        PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN;
    }



    // Játékos hozzáadása
    public void addPlayer(Player player) {
        if (player != null && !players.contains(player)) {
            players.add(player);
        }
    }

    // Játékos eltávolítása
    public void removePlayer(Player player) {
        players.remove(player);
    }

    // Közösségi kártya hozzáadása
    public void addCommunityCard(Card card) {
        if (card != null) {
            communityCards.add(card);
        }
    }

    // Fázis frissítése biztonságos módon
    public void nextPhase() {
        if (phase.ordinal() < GamePhase.values().length - 1) {
            phase = GamePhase.values()[phase.ordinal() + 1];
        } else {
            throw new IllegalStateException("Cannot move beyond the final game phase.");
        }
    }

    // ToString metódus a játék állapotának szöveges reprezentációjához
    @Override
    public String toString() {
        return "GameStatus{" +
                "communityCards=" + communityCards +
                ", players=" + players +
                ", phase=" + phase +
                ", currentPot=" + currentPot +
                '}';
    }

    // Getterek és Setterek
    public List<Card> getCommunityCards() {
        return new ArrayList<>(communityCards);
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public GamePhase getPhase() {
        return phase;
    }

    public int getCurrentPot() {
        return currentPot;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public void setCurrentPot(int currentPot) {
        this.currentPot = currentPot;
    }

    public void setCommunityCards(List<Card> communityCards) {
        this.communityCards = communityCards;
    }

    public void addCardToCommunity(Card card) {
        List<Card> currentCards = this.getCommunityCards();
        currentCards.add(card);
        this.setCommunityCards(currentCards);
    }

    public void clearCommunityCards() {
        communityCards.clear();
    }

}
