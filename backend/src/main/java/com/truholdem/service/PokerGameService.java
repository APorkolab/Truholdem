package com.truholdem.service;

import com.truholdem.model.*;
import com.truholdem.model.GameStatus.GamePhase;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PokerGameService {
    private Deck deck;
    private GameStatus gameStatus;
    private static boolean gameStarted = false;
    private int currentBet = 0;
    private int smallBlindAmount = 10;
    private int bigBlindAmount = 20;
    private Player smallBlindPlayer;
    private Player bigBlindPlayer;
    private int currentPlayerIndex;

    public PokerGameService() {
        this.deck = new Deck();
        this.gameStatus = new GameStatus();
    }

    public GameStatus startGame(List<PlayerInfo> playersInfo) {
        resetGame(false);
        if (playersInfo != null && playersInfo.size() >= 2 && playersInfo.size() <= 4) {
            playersInfo.forEach(info -> gameStatus.addPlayer(info));
            dealInitialCards();
            setBlinds();
            gameStatus.setPhase(GamePhase.PRE_FLOP);
            gameStarted = true;
            return gameStatus;
        } else {
            System.out.println("Player count must be between 2 and 4.");
            return null;
        }
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public boolean resetGame(boolean keepPlayers) {
        // Reset the deck
        deck.resetDeck();

        // Clear community cards and reset game phase
        gameStatus.clearCommunityCards();
        gameStatus.setPhase(GameStatus.GamePhase.PRE_FLOP);

        // Reset the pot and bet amounts
        gameStatus.setCurrentPot(0);
        gameStatus.setCurrentBet(0);

        // Optionally keep players or reset players
        if (!keepPlayers) {
            gameStatus.setPlayers(new ArrayList<>()); // Remove all players
        } else {
            gameStatus.getPlayers().forEach(player -> {
                player.clearHand(); // Clear hands for each player
                player.setBetAmount(0); // Reset player bet amounts
                player.setFolded(false); // Reset fold status
            });
        }

        gameStarted = false; // Set the game as not started
        return true; // Reset successfully
    }

    public Optional<GameStatus> dealFlop() {
        if (gameStarted && gameStatus.getPhase() == GamePhase.PRE_FLOP) {
            performFlop();
            gameStatus.setPhase(GamePhase.FLOP);
            performBotActions();
            gameStatus.resetPlayerActions();
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    public Optional<GameStatus> dealTurn() {
        if (gameStarted && gameStatus.getPhase() == GamePhase.FLOP) {
            performTurn();
            gameStatus.setPhase(GamePhase.TURN);
            performBotActions();
            gameStatus.resetPlayerActions();
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    public Optional<GameStatus> dealRiver() {
        if (gameStarted && gameStatus.getPhase() == GamePhase.TURN) {
            gameStatus.setPhase(GamePhase.RIVER);
            performRiver();
            performBotActions();
            gameStatus.resetPlayerActions();
            if (gameStatus.areAllBetsEqual() && gameStatus.allPlayersActed()) {
                // Proceed directly to end game if all actions are done
                endGame(); // Make sure to end the game after the river
            }
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    public boolean playerBet(String playerId, int amount) {
        Player player = findPlayerById(playerId);
        if (player != null && amount > 0 && amount <= player.getChips()) {
            int raiseAmount = amount - player.getBetAmount();
            player.setChips(player.getChips() - raiseAmount);
            player.setBetAmount(amount);
            gameStatus.setCurrentPot(gameStatus.getCurrentPot() + raiseAmount);
            gameStatus.getPlayerActions().put(playerId, true);
            checkAndProceedToNextPhase();
            return true;
        }
        return false;
    }

    public boolean playerRaise(String playerId, int amount) {
        Player player = findPlayerById(playerId);
        if (player != null && amount > currentBet && player.getChips() >= amount) {
            int raiseAmount = amount - player.getBetAmount();
            player.setChips(player.getChips() - raiseAmount);
            player.setBetAmount(amount);
            gameStatus.setCurrentPot(gameStatus.getCurrentPot() + raiseAmount);
            currentBet = amount;
            gameStatus.getPlayerActions().put(playerId, true);
            checkAndProceedToNextPhase();
            return true;
        }
        return false;
    }

    public boolean playerFold(String playerId) {
        Player player = findPlayerById(playerId);
        if (player == null || player.isFolded()) {
            return false;
        }
        player.setFolded(true);
        gameStatus.getPlayerActions().put(playerId, true);

        if (gameStatus.getPlayers().stream().filter(p -> !p.isFolded()).count() == 1) {
            endGameEarly();
        } else {
            checkAndProceedToNextPhase();
        }
        return true;
    }

    public boolean playerCheck(String playerId) {
        Player player = findPlayerById(playerId);
        if (player != null && player.getBetAmount() == currentBet) {
            gameStatus.getPlayerActions().put(playerId, true);
            checkAndProceedToNextPhase();
            return true;
        }
        return false;
    }

    public synchronized void proceedToNextPhase() {
        switch (gameStatus.getPhase()) {
            case PRE_FLOP:
                performFlop();
                gameStatus.setPhase(GamePhase.FLOP);
                break;
            case FLOP:
                performTurn();
                gameStatus.setPhase(GamePhase.TURN);
                break;
            case TURN:
                performRiver();
                gameStatus.setPhase(GamePhase.RIVER);
                break;
            case RIVER:
                gameStatus.setPhase(GamePhase.SHOWDOWN);
                endGame();
                return;
            default:
                return;
        }
        gameStatus.resetPlayerActions();
        currentBet = 0;
        gameStatus.setCurrentPlayerIndex(0);
        performBotActions();
    }

    public Player findPlayerById(String playerId) {
        return gameStatus.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    private void performFlop() {
        deck.drawCard(); // Burn card
        for (int i = 0; i < 3; i++) {
            gameStatus.addCommunityCard(deck.drawCard());
        }
    }

    private void performTurn() {
        deck.drawCard(); // Burn card
        gameStatus.addCommunityCard(deck.drawCard());
    }

    private void performRiver() {
        deck.drawCard(); // Burn card
        gameStatus.addCommunityCard(deck.drawCard());
    }

    private void dealInitialCards() {
        for (Player player : gameStatus.getPlayers()) {
            if (player.getChips() > 0) {
                player.clearHand();
                player.addCardToHand(deck.drawCard());
                player.addCardToHand(deck.drawCard());
            }
        }
        System.out.println("Initial cards dealt to " + gameStatus.getPlayers().size() + " players.");
    }

    private void setBlinds() {
        List<Player> activePlayers = gameStatus.getPlayers().stream()
                .filter(player -> player.getChips() > 0 && !player.isFolded())
                .collect(Collectors.toList());

        if (activePlayers.size() >= 3) {
            int dealerPosition = 0;
            int smallBlindPosition = (dealerPosition + 1) % activePlayers.size();
            int bigBlindPosition = (dealerPosition + 2) % activePlayers.size();

            smallBlindPlayer = activePlayers.get(smallBlindPosition);
            bigBlindPlayer = activePlayers.get(bigBlindPosition);

            placeBlindBet(smallBlindPlayer, smallBlindAmount);
            placeBlindBet(bigBlindPlayer, bigBlindAmount);

            currentBet = bigBlindAmount;
        } else {
            System.out.println("Not enough players to set blinds.");
        }
    }

    private void placeBlindBet(Player player, int amount) {
        if (player != null && player.getChips() >= amount) {
            player.setChips(player.getChips() - amount);
            player.setBetAmount(amount);
            gameStatus.setCurrentPot(gameStatus.getCurrentPot() + amount);
        } else if (player != null && player.getChips() > 0) {
            gameStatus.setCurrentPot(gameStatus.getCurrentPot() + player.getChips());
            player.setBetAmount(player.getChips());
            player.setChips(0);
        }
    }

    private void endGameEarly() {
        Player winner = gameStatus.getPlayers().stream()
                .filter(player -> !player.isFolded())
                .findFirst()
                .orElse(null);
        if (winner != null) {
            winner.setChips(winner.getChips() + gameStatus.getCurrentPot());
            gameStatus.setCurrentPot(0);
            gameStarted = false;
            resetGame(true);
        }
    }

    public String endGame() {
        gameStatus.setPhase(GamePhase.SHOWDOWN);
        List<Player> activePlayers = gameStatus.getPlayers().stream()
                .filter(player -> !player.isFolded())
                .collect(Collectors.toList());

        if (activePlayers.isEmpty()) {
            return null;
        }

        Player winner = determineWinner(activePlayers);
        if (winner != null) {
            winner.addWinnings(gameStatus.getCurrentPot());
            gameStatus.setCurrentPot(0);
            gameStarted = false;
            return winner.getName();
        }
        return null;
    }

    public GameStatus startNewMatch() {
        List<Player> players = new ArrayList<>(gameStatus.getPlayers());
        players.removeIf(player -> player.getChips() <= 0);

        if (players.size() < 2) {
            System.out.println("Not enough players with chips to start a new match.");
            return null;
        }

        resetGame(true);
        this.deck.shuffle();
        gameStatus.setPlayers(players);
        dealInitialCards();
        setBlinds();
        gameStatus.setPhase(GamePhase.PRE_FLOP);
        gameStarted = true;
        System.out.println("New match started with " + players.size() + " players.");
        return gameStatus;
    }

    private Player determineWinner(List<Player> players) {
        List<Player> activePlayers = players.stream()
                .filter(player -> !player.isFolded() && player.getChips() > 0)
                .collect(Collectors.toList());

        if (activePlayers.size() == 1) {
            Player winner = activePlayers.get(0);
            System.out.println("Only one player remaining, automatic winner: " + winner.getName());
            return winner;
        }

        HandEvaluator evaluator = new HandEvaluator();
        List<Card> communityCards = gameStatus.getCommunityCards();

        return players.stream()
                .filter(player -> !player.isFolded())
                .max(Comparator.comparingInt(player -> {
                    HandResult result = evaluator.evaluate(player.getHand(), communityCards);
                    return result.getHandStrength();
                }))
                .orElse(null);
    }

    public List<PlayerInfo> getDefaultPlayers() {
        List<PlayerInfo> defaultPlayers = new ArrayList<>();
        defaultPlayers.add(new PlayerInfo("Player1", 1000, false));
        defaultPlayers.add(new PlayerInfo("Bot1", 1000, true));
        defaultPlayers.add(new PlayerInfo("Bot2", 1000, true));
        return defaultPlayers;
    }

    public GameStatus registerPlayers(List<PlayerInfo> players) {
        if (players != null && !players.isEmpty()) {
            players.forEach(info -> gameStatus.addPlayer(info));
        }
        return gameStatus;
    }

    public boolean changePlayerName(String playerId, String newName) {
        Player player = findPlayerById(playerId);
        if (player != null && newName != null && !newName.trim().isEmpty()) {
            player.setName(newName);
            return true;
        }
        return false;
    }

    public void performBotActions() {
        List<Player> activePlayers = gameStatus.getPlayers().stream()
                .filter(player -> !player.isFolded() && player.getChips() > 0)
                .collect(Collectors.toList());

        for (Player player : activePlayers) {
            if (player.isBot() && !gameStatus.getPlayerActions().get(player.getId())) {
                performBotAction(player.getId());
                checkAndProceedToNextPhase();
            }
        }
    }

    public boolean performBotAction(String botId) {
        Player bot = findPlayerById(botId);

        // Ellenőrizzük, hogy a bot létezik-e
        if (bot == null) {
            System.out.println("Hiba: A megadott bot nem található.");
            return false;
        }

        // Ellenőrizzük, hogy a játékos bot-e
        if (!bot.isBot()) {
            System.out.println("Hiba: A megadott játékos nem bot.");
            return false;
        }

        // Ellenőrizzük, hogy a bot már végrehajtott-e akciót ebben a körben
        if (gameStatus.getPlayerActions().get(botId)) {
            System.out.println("A bot már végrehajtotta az akcióját ebben a körben.");
            return true;  // Nem dobunk hibát, csak visszatérünk, jelezve, hogy az akció már megtörtént
        }

        int botChips = bot.getChips();
        int botBet = bot.getBetAmount();

        // Ellenőrizzük, hogy van-e elég zsetonja
        if (botChips <= 0) {
            System.out.println("A botnak nincs elég zsetonja, automatikus bedobás.");
            return playerFold(botId); // true-t ad vissza, ha a bot bedobja a lapot
        }

        // Ha a bot zsetonjai nem elegendők a jelenlegi tét megadásához
        if (currentBet > 0 && botChips + botBet < currentBet) {
            System.out.println("A bot bedobta a lapját, mert nem tudta megadni a tétet.");
            return playerFold(botId); // true-t ad vissza a bedobás esetén
        }

        Random random = new Random();
        int action = random.nextInt(3); // 0: fold, 1: check/call, 2: raise

        switch (action) {
            case 0:
                System.out.println("A bot bedobta a lapját.");
                return playerFold(botId); // true-t ad vissza a bedobás esetén
            case 1:
                if (currentBet > botBet) {
                    System.out.println("A bot megadta a tétet.");
                    return playerBet(botId, currentBet - botBet); // true-t ad vissza a tét megadása esetén
                } else {
                    System.out.println("A bot passzolt.");
                    return playerCheck(botId); // true-t ad vissza a passzolás esetén
                }
            case 2:
                int raiseAmount = Math.min(currentBet + random.nextInt(20) + 1, botChips + botBet);
                System.out.println("A bot emelt a tétet: " + raiseAmount + " zsetonnal.");
                return playerRaise(botId, raiseAmount); // true-t ad vissza az emelés esetén
        }

        // Ha valamiért nem sikerül végrehajtani az akciót
        System.out.println("Ismeretlen akció.");
        return false;
    }


    private void checkAndProceedToNextPhase() {
        if (gameStatus.allPlayersActed() && gameStatus.areAllBetsEqual()) {
            proceedToNextPhase();
        } else {
            // Következő játékos kiválasztása
            int nextPlayerIndex = (gameStatus.getCurrentPlayerIndex() + 1) % gameStatus.getPlayers().size();
            gameStatus.setCurrentPlayerIndex(nextPlayerIndex);
        }
    }

    private void resetPlayerActions() {
        for (Player player : gameStatus.getPlayers()) {
            gameStatus.getPlayerActions().put(player.getId(), false);
        }
    }

    public void setCurrentPlayerIndex(int index) {
        this.currentPlayerIndex = index;
    }

    public int getCurrentPlayerIndex() {
        return this.currentPlayerIndex;
    }

}
