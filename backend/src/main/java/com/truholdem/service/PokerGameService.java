package com.truholdem.service;

import com.truholdem.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PokerGameService {
    private Map<String, Boolean> playerActions;
    private Deck deck;
    private GameStatus gameStatus;
    private int dealerPosition;
    private boolean gameStarted = false;
    private int currentBet = 0;
    private int pot = 0;
    private Player smallBlindPlayer;
    private Player bigBlindPlayer;
    private int smallBlindAmount;
    private int bigBlindAmount;

    public PokerGameService() {
        this.playerActions = new HashMap<>(); // Inicializáljuk a playerActions mapet
        this.deck = new Deck();
        resetGame(false); // Hívjuk meg a resetGame metódust
    }

    public boolean resetGame(boolean keepPlayers) {
        this.deck.shuffle();
        if (this.gameStatus == null) {
            this.gameStatus = new GameStatus();
        } else {
            this.gameStatus.clearCommunityCards();
        }

        // Inicializáljuk a game state-et és a playerActions-t
        this.playerActions.clear(); // Mivel már inicializáltuk a konstruktorban

        if (!keepPlayers) {
            gameStatus.getPlayers().clear();
            playerActions.clear(); // Clear actions when players are cleared
        } else {
            List<String> currentPlayerIds = gameStatus.getPlayers().stream().map(Player::getId)
                    .collect(Collectors.toList());
            playerActions.keySet().removeIf(id -> !currentPlayerIds.contains(id)); // Remove actions for removed players
        }

        for (Player player : gameStatus.getPlayers()) {
            player.clearHand();
            player.setFolded(false);
            player.setBetAmount(0);
            player.setChips(player.getStartingChips());
            playerActions.put(player.getId(), false); // Ensure actions are reset
        }

        dealerPosition = 0;
        return true;
    }

    public GameStatus startGame(List<PlayerInfo> playersInfo) {
        resetGame(true); // Keep the players

        if (playersInfo != null && playersInfo.size() >= 2) {
            for (PlayerInfo playerInfo : playersInfo) {
                if (gameStatus.getPlayers().stream().noneMatch(p -> p.getName().equals(playerInfo.getName()))) {
                    registerPlayer(playerInfo.getName(), playerInfo.getStartingChips(), playerInfo.isBot());
                }
            }

            setBlinds();
            dealInitialCards();
            gameStatus.setPhase(GameStatus.GamePhase.PRE_FLOP);
            gameStarted = true;
            return gameStatus;
        } else {
            System.out.println("Not enough players to start the game. Minimum 2 players required.");
            return null;
        }
    }

    private void setBlinds() {
        List<Player> activePlayers = gameStatus.getPlayers().stream()
                .filter(player -> player.getChips() > 0 && !player.isFolded())
                .collect(Collectors.toList());

        if (activePlayers.size() >= 3) {
            dealerPosition = (dealerPosition + 1) % activePlayers.size();
            int smallBlindPosition = (dealerPosition + 1) % activePlayers.size();
            int bigBlindPosition = (dealerPosition + 2) % activePlayers.size();

            smallBlindPlayer = activePlayers.get(smallBlindPosition);
            bigBlindPlayer = activePlayers.get(bigBlindPosition);

            placeBlindBet(smallBlindPlayer, smallBlindAmount);
            placeBlindBet(bigBlindPlayer, bigBlindAmount);

            currentBet = bigBlindAmount;
        } else {
            throw new IllegalStateException("Not enough players for setting blinds.");
        }
    }

    private void placeBlindBet(Player player, int amount) {
        if (player != null && player.getChips() >= amount) {
            player.setChips(player.getChips() - amount);
            player.setBetAmount(amount);
            pot += amount;
        }
    }

    private void dealInitialCards() {
        gameStatus.getPlayers().forEach(player -> {
            player.clearHand();
            for (int i = 0; i < 2; i++) {
                if (deck.cardsLeft() == 0) {
                    deck.shuffle(); // Keverés, ha kifogytunk a lapokból
                }
                player.addCardToHand(deck.drawCard());
            }
            if (player.getHand().size() != 2) {
                throw new IllegalStateException("Player did not receive two cards.");
            }

            if (!playerActions.containsKey(player.getId())) {
                playerActions.put(player.getId(), false);
            }
        });
    }

    public boolean registerPlayer(String playerName, int startingChips, boolean isBot) {
        if (!gameStarted && gameStatus.getPlayers().size() < 4) {
            Player newPlayer = new Player(playerName, startingChips);
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

    private boolean allPlayersActed() {
        return gameStatus.getPlayers().stream()
                .filter(player -> !player.isFolded())
                .allMatch(player -> playerActions.get(player.getId()));
    }

    public Optional<GameStatus> dealFlop() {
        if (gameStarted && gameStatus.getPhase() == GameStatus.GamePhase.PRE_FLOP && allPlayersActed()) {
            performFlop();
            gameStatus.setPhase(GameStatus.GamePhase.FLOP);
            resetPlayerActions();
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    public Optional<GameStatus> dealTurn() {
        if (gameStarted && gameStatus.getPhase() == GameStatus.GamePhase.FLOP && allPlayersActed()) {
            performTurn();
            gameStatus.setPhase(GameStatus.GamePhase.TURN);
            resetPlayerActions();
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    public Optional<GameStatus> dealRiver() {
        if (gameStarted && gameStatus.getPhase() == GameStatus.GamePhase.TURN && allPlayersActed()) {
            performRiver();
            gameStatus.setPhase(GameStatus.GamePhase.RIVER);
            resetPlayerActions();
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    public boolean playerFold(String playerId) {
        Player player = findPlayerById(playerId);
        if (player == null || player.isFolded()) {
            throw new IllegalArgumentException("Player not found or already folded.");
        }
        player.setFolded(true);
        playerActions.put(playerId, true);
        checkForEarlyWin();
        return true;
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

    public synchronized boolean playerBet(String playerId, int amount) {
        Player player = findPlayerById(playerId);
        if (player != null) {
            int betIncrement = amount - player.getBetAmount();
            if (betIncrement <= 0 || amount < currentBet) {
                System.out.println("Invalid bet: betIncrement=" + betIncrement + ", amount=" + amount + ", currentBet="
                        + currentBet);
                return false;
            }
            if (player.getChips() >= betIncrement) {
                player.setChips(player.getChips() - betIncrement);
                player.setBetAmount(amount);
                pot += betIncrement;
                currentBet = amount;
                playerActions.put(playerId, true);

                checkForEarlyWin();

                return true;
            }
        }
        return false;
    }

    public void checkForEarlyWin() {
        long activePlayers = gameStatus.getPlayers().stream().filter(p -> !p.isFolded()).count();
        if (activePlayers == 1) {
            endGameEarly();
        } else {
            proceedToNextRound();
        }
    }

    private void endGameEarly() {
        Player winner = gameStatus.getPlayers().stream()
                .filter(p -> !p.isFolded())
                .findFirst()
                .orElse(null);
        if (winner != null) {
            winner.addWinnings(pot);
            resetGame(true);
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
        return gameStatus.getPlayers().stream()
                .filter(player -> !player.isFolded())
                .allMatch(player -> player.getBetAmount() == currentBet);
    }

    private void proceedToNextRound() {
        gameStatus.getPlayers().forEach(player -> {
            if (!playerActions.get(player.getId()) && player.getName().startsWith("Bot")) {
                automateBotAction(player);
            }
        });

        ensureBotsMatchBets();

        if (areAllBetsEqual() && allPlayersActed()) {
            switch (gameStatus.getPhase()) {
                case PRE_FLOP:
                    dealFlop();
                    break;
                case FLOP:
                    dealTurn();
                    break;
                case TURN:
                    dealRiver();
                    break;
                case RIVER:
                    determineWinner();
                    resetGame(false);
                    break;
                case SHOWDOWN:
                    break;
                default:
                    break;
            }
            resetPlayerActions();
        }
    }

    private String determineWinner() {
        HandEvaluator evaluator = new HandEvaluator();
        Player winner = gameStatus.getPlayers().stream()
                .filter(player -> !player.isFolded())
                .max(Comparator.comparing(player -> evaluator.evaluate(player.getHand(), gameStatus.getCommunityCards())
                        .getHandStrength()))
                .orElse(null);

        if (winner != null) {
            winner.addWinnings(pot);
            return winner.getName();
        }
        resetGame(false);
        return "";
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
            return null;
        }

        resetGame(true);
        this.deck.shuffle();
        gameStatus.setPlayers(players);
        dealInitialCards();
        setBlinds();
        gameStatus.setPhase(GameStatus.GamePhase.PRE_FLOP);
        gameStarted = true;
        return gameStatus;
    }

    public void automateBotAction(Player bot) {
        if (!bot.isFolded() && currentBet > 0) {
            if (bot.getChips() >= currentBet) {
                playerBet(bot.getId(), currentBet); // Place bet
            } else {
                playerFold(bot.getId()); // Fold if no enough chips
            }
            playerActions.put(bot.getId(), true); // Mark action as done
        }
    }

    private void updateGameStatus() {
        gameStatus.setCurrentPot(pot);
        gameStatus.setCurrentBet(currentBet);
    }

    private void ensureBotsMatchBets() {
        for (Player player : gameStatus.getPlayers()) {
            if (!player.isFolded() && player.getName().startsWith("Bot") && player.getBetAmount() < currentBet) {
                playerBet(player.getId(), currentBet);
            }
        }
    }

    private void resetPlayerActions() {
        playerActions.replaceAll((id, actionTaken) -> false);
    }

    public synchronized boolean playerRaise(String playerId, int amount) {
        Player player = findPlayerById(playerId);
        if (gameStarted && player != null && amount > currentBet
                && player.getChips() >= (amount - player.getBetAmount())) {
            int raiseAmount = amount - player.getBetAmount();
            if (raiseAmount < bigBlindAmount) {
                System.out.println("Raise must be at least the size of the big blind.");
                return false;
            }
            player.setChips(player.getChips() - raiseAmount);
            player.setBetAmount(amount);
            pot += raiseAmount;
            currentBet = amount;
            playerActions.put(playerId, true);

            if (player.getChips() == 0) {
                proceedToNextRound();
            } else {
                proceedToNextRound();
            }
            return true;
        } else {
            System.out.println("Invalid raise: playerId=" + playerId + ", amount=" + amount + ", currentBet="
                    + currentBet + ", playerChips=" + (player != null ? player.getChips() : "null"));
            return false;
        }
    }
}
