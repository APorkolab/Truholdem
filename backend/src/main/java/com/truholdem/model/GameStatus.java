package com.truholdem.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class GameStatus {
    private List<Card> communityCards = new ArrayList<>();
    private List<Player> players = new ArrayList<>();
    private GamePhase phase = GamePhase.PRE_FLOP;
    private int currentPot = 0;
    private String message;
    private int currentPlayerIndex;

    private int currentBet;
    private Map<String, Boolean> playerActions = new HashMap<>();

    // Játékfázisok enumja
    public enum GamePhase {
        PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN;
    }

    public void addPlayer(PlayerInfo playerInfo) {
        if (players.stream().noneMatch(p -> p.getName().equals(playerInfo.getName()))) {
            Player player = new Player(playerInfo.getName(), playerInfo.getStartingChips(), playerInfo.isBot());
            this.players.add(player);
            this.playerActions.put(player.getId(), false);
        } else {
            System.out.println("Player with this name already exists.");
        }
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public void addCommunityCard(Card card) {
        if (card != null) {
            this.communityCards.add(card);
        }
    }

    public void nextPhase() {
        if (!allPlayersActed() || !areAllBetsEqual()) {
            System.out.println("Cannot proceed to next phase yet.");
            return;
        }

        if (phase.ordinal() < GamePhase.values().length - 1) {
            phase = GamePhase.values()[phase.ordinal() + 1];
        } else {
            phase = GamePhase.SHOWDOWN;
        }

        resetPlayerActions();
        System.out.println("Phase advanced to: " + phase);
    }

    public void resetPlayerActions() {
        players.forEach(player -> playerActions.put(player.getId(), false));
    }

    public boolean allPlayersActed() {
        return players.stream()
                .filter(player -> !player.isFolded())
                .allMatch(player -> playerActions.get(player.getId()));
    }

    public List<Player> getPlayersWhoHaveNotActed() {
        return players.stream()
                .filter(player -> !player.isFolded() && !Boolean.TRUE.equals(playerActions.get(player.getId())))
                .collect(Collectors.toList());
    }

    public boolean areAllBetsEqual() {
        int activeBet = players.stream()
                .filter(player -> !player.isFolded())
                .mapToInt(Player::getBetAmount)
                .min().orElse(0);

        return players.stream()
                .filter(player -> !player.isFolded())
                .allMatch(player -> player.getBetAmount() == activeBet);
    }

    // Getters and Setters
    public List<Card> getCommunityCards() {
        return communityCards;
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

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    public Map<String, Boolean> getPlayerActions() {
        return playerActions;
    }

    public void clearCommunityCards() {
        this.communityCards.clear();
    }

    public void setCurrentPlayerIndex(int index) {
        this.currentPlayerIndex = index;
    }

    public int getCurrentPlayerIndex() {
        return this.currentPlayerIndex;
    }
}