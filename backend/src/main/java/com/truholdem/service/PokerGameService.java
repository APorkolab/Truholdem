package com.truholdem.service;

import com.truholdem.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PokerGameService {
    private int smallBlindAmount = 50;
    private int bigBlindAmount = 100;
    private Player smallBlindPlayer;
    private Player bigBlindPlayer;
    private GameStatus gameStatus;
    private Deck deck;
    private boolean gameStarted;
    private int currentBet;
    private int pot;

    public PokerGameService() {
        this.gameStatus = new GameStatus();
        this.deck = new Deck();
        resetGame(false);
    }

    public boolean resetGame(boolean keepPlayers) {
        this.deck.shuffle();
        this.gameStatus.clearCommunityCards();
        gameStarted = false;
        currentBet = 0;
        pot = 0;

        if (!keepPlayers) {
            gameStatus.getPlayers().clear();
        } else {
            for (Player player : gameStatus.getPlayers()) {
                player.clearHand();
                player.setFolded(false);
            }
        }

        return true;
    }


    public GameStatus startGame(List<PlayerInfo> playersInfo) {
        resetGame(false);

        if (playersInfo != null && !playersInfo.isEmpty()) {
            for (PlayerInfo playerInfo : playersInfo) {
                registerPlayer(playerInfo.getName(), playerInfo.getStartingChips(), playerInfo.isBot());
            }
        }

        setBlinds();
        dealInitialCards();
        gameStatus.setPhase(GameStatus.GamePhase.PRE_FLOP);
        gameStarted = true;
        return gameStatus;
    }

    private void setBlinds() {
        List<Player> activePlayers = gameStatus.getPlayers();
        if (activePlayers.size() >= 2) {
            smallBlindPlayer = activePlayers.get(0);
            bigBlindPlayer = activePlayers.get(1);

            playerBet(smallBlindPlayer.getId(), smallBlindAmount);
            playerBet(bigBlindPlayer.getId(), bigBlindAmount);

            currentBet = bigBlindAmount;
        }
    }

    private void dealInitialCards() {
        gameStatus.getPlayers().forEach(player -> {
            player.clearHand();
            for (int i = 0; i < 2; i++) {
                player.addCardToHand(deck.drawCard());
            }
        });
    }


    public boolean registerPlayer(String playerName, int startingChips, boolean isBot) {
        if (!gameStarted && gameStatus.getPlayers().size() < 4) {
            Player newPlayer = new Player(playerName);
            newPlayer.setChips(startingChips);
            newPlayer.setFolded(false);
            if (isBot) {
                newPlayer.setName("Bot" + playerName);
            } else {
                newPlayer.setName(playerName);
            }
            gameStatus.addPlayer(newPlayer);
            return true;
        }
        return false;
    }

    public GameStatus getGameStatus() {
        return gameStarted ? gameStatus : null;
    }


    public Optional<GameStatus> dealFlop() {
        if (gameStarted && gameStatus.getPhase() == GameStatus.GamePhase.PRE_FLOP) {
            performFlop();
            gameStatus.setPhase(GameStatus.GamePhase.FLOP);
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    public Optional<GameStatus> dealTurn() {
        if (gameStarted && gameStatus.getPhase() == GameStatus.GamePhase.FLOP) {
            performTurn();
            gameStatus.setPhase(GameStatus.GamePhase.TURN);
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    public Optional<GameStatus> dealRiver() {
        if (gameStarted && gameStatus.getPhase() == GameStatus.GamePhase.TURN) {
            performRiver();
            gameStatus.setPhase(GameStatus.GamePhase.RIVER);
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    public boolean playerFold(String playerId) {
        Player player = findPlayerById(playerId);

        if (player != null && !player.isFolded()) {
            player.setFolded(true);
            checkForEarlyWin();

            return true;
        }

        return false;
    }

    private void performFlop() {
        deck.drawCard(); // Burn
        List<Card> flop = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            flop.add(deck.drawCard());
        }
        gameStatus.setCommunityCards(flop);
    }

    private void performTurn() {
        deck.drawCard(); // Burn
        Card turnCard = deck.drawCard();
        gameStatus.addCardToCommunity(turnCard);
    }

    private void performRiver() {
        deck.drawCard(); // Burn
        Card riverCard = deck.drawCard();
        gameStatus.addCardToCommunity(riverCard);
    }

    public boolean playerBet(String playerId, int amount) {
        Player player = findPlayerById(playerId);
        if (gameStarted && player != null && amount >= currentBet && player.getChips() >= amount) {
            if (amount > currentBet) {
                currentBet = amount;
            }
            player.setChips(player.getChips() - amount);
            player.setBetAmount(amount);
            pot += amount;
            return true;
        } else {
            return false;
        }
    }

    public void checkForEarlyWin() {
        long activePlayers = gameStatus.getPlayers().stream().filter(p -> !p.isFolded()).count();
        if (activePlayers == 1) {
            endGameEarly();
        }
    }

    private void endGameEarly() {
        Player winner = gameStatus.getPlayers().stream()
                .filter(p -> !p.isFolded())
                .findFirst()
                .orElse(null);
        if (winner != null) {
            winner.addWinnings(pot);
            resetGame(false);
        }
    }

    public String endGame() {
        if (gameStarted) {
            String winnerId = determineWinner();
            gameStarted = false;
            resetGame(false);
            return winnerId;
        }
        return null;
    }

    private boolean areAllBetsEqual() {
        int expectedBet = currentBet;
        return gameStatus.getPlayers().stream()
                .filter(p -> !p.isFolded())
                .allMatch(p -> p.getBetAmount() == expectedBet);
    }

    private void proceedToNextRound() {
        if (areAllBetsEqual()) {
            switch (gameStatus.getPhase()) {
                case PRE_FLOP:
                    performFlop();
                    gameStatus.setPhase(GameStatus.GamePhase.FLOP);
                    break;
                case FLOP:
                    performTurn();
                    gameStatus.setPhase(GameStatus.GamePhase.TURN);
                    break;
                case TURN:
                    performRiver();
                    gameStatus.setPhase(GameStatus.GamePhase.RIVER);
                    break;
                case RIVER:
                    determineWinner();
                    resetGame(false);
                    break;
            }
        }
    }


    private String determineWinner() {
        HandEvaluator evaluator = new HandEvaluator();
        Optional<Player> winner = gameStatus.getPlayers().stream()
                .filter(player -> !player.isFolded())
                .max(Comparator.comparing(player -> {
                    HandResult handResult = evaluator.evaluate(player.getHand(), gameStatus.getCommunityCards());
                    return handResult.getHandStrength();
                }));

        if (winner.isPresent()) {
            Player winningPlayer = winner.get();
            winningPlayer.addWinnings(pot);
            return winningPlayer.getId();
        }

        resetGame(false);
        return winner.map(Player::getId).orElse("");
    }

    private Player findPlayerById(String playerId) {
        return gameStatus.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public GameStatus startNewMatch() {
        List<Player> players = new ArrayList<>(gameStatus.getPlayers());
        players.removeIf(player -> player.getChips() <= 0);
        if (players.isEmpty()) {
            return null; // No players with chips left
        }
        resetGame(true);
        this.deck.shuffle();
        gameStatus.setPlayers(players); // Add remaining players
        dealInitialCards();
        setBlinds();
        gameStatus.setPhase(GameStatus.GamePhase.PRE_FLOP);
        gameStarted = true;
        return gameStatus;
    }
}
