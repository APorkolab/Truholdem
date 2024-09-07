package com.truholdem.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class GameStatus {
    private List<Card> communityCards = new ArrayList<>();
    private List<Player> players = new ArrayList<>();
    private GamePhase phase = GamePhase.PRE_FLOP; // Kezdő állapot
    private int currentPot = 0;
    private String message;

    private int currentBet;

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Játékos hozzáadása
    public void addPlayer(PlayerInfo playerInfo) {
        Player player = new Player(playerInfo.getName(), playerInfo.getStartingChips(), playerInfo.isBot());
        this.players.add(player);
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
            phase = GamePhase.SHOWDOWN;
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

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public int getPot() {
        return currentPot;
    }

    private Map<String, Boolean> playerActions = new HashMap<>();

    public void setPlayerActions(Map<String, Boolean> playerActions) {
        this.playerActions = playerActions;
    }

    public Map<String, Boolean> getPlayerActions() {
        return playerActions;
    }

    public boolean areAllPlayersActed() {
        return playerActions.values().stream().allMatch(acted -> acted);
    }
}
