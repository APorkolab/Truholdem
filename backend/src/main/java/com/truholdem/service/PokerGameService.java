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
            gameStatus.resetPlayerActions();
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    public Optional<GameStatus> dealTurn() {
        if (gameStarted && gameStatus.getPhase() == GamePhase.FLOP) {
            performTurn();
            gameStatus.setPhase(GamePhase.TURN);
            gameStatus.resetPlayerActions();
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    public Optional<GameStatus> dealRiver() {
        if (gameStarted && gameStatus.getPhase() == GamePhase.TURN) {
            performRiver();
            gameStatus.setPhase(GamePhase.RIVER);
            gameStatus.resetPlayerActions();
            if (gameStatus.areAllBetsEqual() && gameStatus.allPlayersActed()) {
                // Proceed directly to end game if all actions are done
                gameStatus.setPhase(GamePhase.SHOWDOWN);
                endGame();  // Make sure to end the game after the river
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

            // Ha minden tét egyenlő és mindenki cselekedett, lépj a következő fázisba
            //if (gameStatus.areAllBetsEqual() && gameStatus.allPlayersActed()) {
                if (gameStatus.getPhase() == GamePhase.RIVER) {
                    gameStatus.setPhase(GamePhase.SHOWDOWN); // Utolsó tétkör után lépünk a Showdown-ba
                }
                proceedToNextPhase();
            //}
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

            if (gameStatus.areAllBetsEqual() && gameStatus.allPlayersActed()) {
                proceedToNextPhase();
            }
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
        } else if (gameStatus.allPlayersActed()) {
            proceedToNextPhase();
        }
        return true;
    }

    public boolean playerCheck(String playerId) {
        Player player = findPlayerById(playerId);
        if (player != null && player.getBetAmount() == currentBet) {
            gameStatus.getPlayerActions().put(playerId, true);
            if (gameStatus.allPlayersActed()) {
                proceedToNextPhase();
            }
            return true;
        }
        return false;
    }

    public synchronized void proceedToNextPhase() {
        switch (gameStatus.getPhase()) {
            case FLOP:
                dealFlop();
                break;
            case TURN:
                dealTurn();
                break;
            case RIVER:
                dealRiver();
                break;
            case SHOWDOWN:
                endGame();  // Call endGame when it's time for showdown
                break;
            default:
                break;
        }
    }


    private Player findPlayerById(String playerId) {
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
        gameStatus.getPlayers().forEach(player -> {
            player.addCardToHand(deck.drawCard());
            player.addCardToHand(deck.drawCard());
        });
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

        if (players.isEmpty()) {
            return null;
        }

        resetGame(true);
        this.deck.shuffle();
        gameStatus.setPlayers(players);
        dealInitialCards();
        setBlinds();
        gameStatus.setPhase(GamePhase.PRE_FLOP);
        gameStarted = true;
        return gameStatus;
    }

    private Player determineWinner(List<Player> players) {
        // Ellenőrzés, ha csak egy aktív játékos maradt (nem dobott és van zsetonja)
        List<Player> activePlayers = players.stream()
                .filter(player -> !player.isFolded() && player.getChips() > 0)
                .collect(Collectors.toList());

        if (activePlayers.size() == 1) {
            // Ha csak egy játékos maradt, ő nyeri a potot automatikusan
            Player winner = activePlayers.get(0);
            System.out.println("Only one player remaining, automatic winner: " + winner.getName());
            return winner;
        }

        // Ha több játékos maradt, értékeljük a kezeket a HandEvaluator segítségével
        HandEvaluator evaluator = new HandEvaluator();
        List<Card> communityCards = gameStatus.getCommunityCards(); // Közösségi lapok lekérése

        return players.stream()
                .filter(player -> !player.isFolded()) // Csak a játékban maradt játékosokat vizsgáljuk
                .max(Comparator.comparingInt(player -> {
                    HandResult result = evaluator.evaluate(player.getHand(), communityCards);
                    return result.getHandStrength(); // A játékos kéz erősségének összehasonlítása
                }))
                .orElse(null); // Legjobb kéz kiválasztása
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
            players.forEach(player -> gameStatus.addPlayer(player));
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

}
