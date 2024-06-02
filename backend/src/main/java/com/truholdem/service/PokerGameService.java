package com.truholdem.service;

import com.truholdem.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PokerGameService {
    private int smallBlindAmount = 5; // Kisvak érték módosítása
    private int bigBlindAmount = 15; // Nagyvak érték módosítása
    private Player smallBlindPlayer;
    private Player bigBlindPlayer;
    private GameStatus gameStatus;
    private Deck deck;
    private boolean gameStarted;
    private int currentBet;
    private int pot;
    private int dealerPosition; // Dealer pozíció nyomon követésére
    private Map<String, Boolean> playerActions;

    public PokerGameService() {
        this.gameStatus = new GameStatus();
        this.deck = new Deck();
        resetGame(false);
        this.playerActions = new HashMap<>();
    }

    public boolean resetGame(boolean keepPlayers) {
        this.deck.shuffle();
        this.gameStatus.clearCommunityCards();

        gameStarted = false;
        currentBet = 0;
        pot = 0;

        if (!keepPlayers) {
            for (Player player : gameStatus.getPlayers()) {
                player.setFolded(false);
                player.setChips(player.getStartingChips()); // Kezdeti vagyon beállítása újraindításkor
            }
            gameStatus.getPlayers().clear();
        } else {
            for (Player player : gameStatus.getPlayers()) {
                player.clearHand();
                player.setFolded(false);
                player.setBetAmount(0); // Bet amount reset
                player.setChips(player.getStartingChips()); // Kezdeti vagyon beállítása újraindításkor
                playerActions.put(player.getId(), false);
            }
        }

        dealerPosition = 0; // Újraindításkor a dealer pozíció nullázása
        return true;
    }

    public GameStatus startGame(List<PlayerInfo> playersInfo) {
        resetGame(false); // Reset the game without keeping the players

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
            // Frissítjük a dealer pozíciót
            dealerPosition = (dealerPosition + 1) % activePlayers.size();
            int smallBlindPosition = (dealerPosition + 1) % activePlayers.size();
            int bigBlindPosition = (dealerPosition + 2) % activePlayers.size();

            smallBlindPlayer = activePlayers.get(smallBlindPosition);
            bigBlindPlayer = activePlayers.get(bigBlindPosition);

            // Kisvak és nagyvak tétek elhelyezése
            placeBlindBet(smallBlindPlayer, smallBlindAmount);
            placeBlindBet(bigBlindPlayer, bigBlindAmount);

            currentBet = bigBlindAmount;
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
                player.addCardToHand(deck.drawCard());
            }
            playerActions.put(player.getId(), false);
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
                .filter(player -> !player.isFolded()) // Csak a még aktív játékosokat vizsgáljuk
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

    private void performRiver() {
        deck.drawCard(); // Burn
        Card riverCard = deck.drawCard();
        gameStatus.addCardToCommunity(riverCard);
    }

    public boolean playerFold(String playerId) {
        Player player = findPlayerById(playerId);

        if (player != null && !player.isFolded()) {
            player.setFolded(true);
            playerActions.put(playerId, true);

            // Check if only one player remains active
            if (gameStatus.getPlayers().stream().filter(p -> !p.isFolded()).count() == 1) {
                endGameEarly();
            } else {
                proceedToNextRound();
            }
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

    public boolean playerBet(String playerId, int amount) {
        Player player = findPlayerById(playerId);
        if (player != null) {
            int betIncrement = amount - player.getBetAmount();
            if (betIncrement <= 0 || amount < currentBet) {
                System.out.println("Invalid bet: betIncrement=" + betIncrement + ", amount=" + amount + ", currentBet=" + currentBet);
                return false; // Bet must increase and must be at least the current bet
            }
            if (player.getChips() >= betIncrement) {
                player.setChips(player.getChips() - betIncrement);
                player.setBetAmount(amount);
                pot += betIncrement;
                currentBet = amount; // Update the current bet to the new bet
                playerActions.put(playerId, true);
                proceedToNextRound();
                return true;
            }
        }
        return false;
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
        int expectedBet = currentBet;
        boolean allEqual = true;
        for (Player player : gameStatus.getPlayers()) {
            if (!player.isFolded() && player.getBetAmount() != expectedBet) {
                System.out.println("Player " + player.getName() + " has bet " + player.getBetAmount() + " which is not equal to the expected bet " + expectedBet);
                allEqual = false;
            }
        }
        return allEqual;
    }

    private void proceedToNextRound() {
        gameStatus.getPlayers().forEach(player -> {
            if (!playerActions.get(player.getId()) && player.getName().startsWith("Bot")) {
                automateBotAction(player);
            }
        });

        ensureBotsMatchBets(); // Ensure bots place the correct bets

        if (areAllBetsEqual() && allPlayersActed()) {
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
            resetPlayerActions();
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
            return winningPlayer.getName();
        }

        resetGame(false);
        return winner.map(Player::getName).orElse("");
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

        resetGame(true); // Reset the game but keep the players
        this.deck.shuffle();
        gameStatus.setPlayers(players); // Add remaining players
        dealInitialCards();
        setBlinds();
        gameStatus.setPhase(GameStatus.GamePhase.PRE_FLOP);
        gameStarted = true;
        return gameStatus;
    }

    private void automateBotAction(Player bot) {
        if (!bot.isFolded()) {
            if (bot.getChips() >= currentBet) {
                playerBet(bot.getId(), currentBet);
            } else {
                playerFold(bot.getId());
            }
            playerActions.put(bot.getId(), true); // Update playerActions after bot action
        }
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
        if (gameStarted && player != null && amount > currentBet && player.getChips() >= (amount - player.getBetAmount())) {
            int raiseAmount = amount - player.getBetAmount();
            player.setChips(player.getChips() - raiseAmount);
            player.setBetAmount(amount);
            pot += raiseAmount;
            currentBet = amount;
            playerActions.put(playerId, true);
            proceedToNextRound();
            return true;
        } else {
            System.out.println("Invalid raise: playerId=" + playerId + ", amount=" + amount + ", currentBet=" + currentBet + ", playerChips=" + (player != null ? player.getChips() : "null"));
            return false;
        }
    }
}
