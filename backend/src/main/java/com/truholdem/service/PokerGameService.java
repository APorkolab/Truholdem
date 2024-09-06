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
    private static boolean gameStarted = false;
    private int currentBet = 0;
    private Player smallBlindPlayer;
    private Player bigBlindPlayer;
    private int smallBlindAmount;
    private int bigBlindAmount;

    public PokerGameService() {
        this.playerActions = new HashMap<>();
        this.deck = new Deck();
        resetGame(false);
        this.gameStatus = new GameStatus();
    }

    public GameStatus getGameStatus() {
        if (gameStatus == null) {
            this.gameStatus = new GameStatus();
            initializeDefaultPlayers();
        }
        return gameStarted ? gameStatus : null;
    }

    private void initializeDefaultPlayers() {
        gameStatus.addPlayer(new Player("Játékos", 1000));
        gameStatus.addPlayer(new Player("Bot1", 1000, true));
        gameStatus.addPlayer(new Player("Bot2", 1000, true));
        gameStatus.addPlayer(new Player("Bot3", 1000, true));
    }

    public Map<String, Boolean> getPlayerActions() {
        return playerActions;
    }

    public void setPlayerActions(Map<String, Boolean> playerActions) {
        this.playerActions = playerActions;
    }

    public Deck getDeck() {
        return deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public int getDealerPosition() {
        return dealerPosition;
    }

    public void setDealerPosition(int dealerPosition) {
        this.dealerPosition = dealerPosition;
    }

    public static boolean isGameStarted() {
        return gameStarted;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    public Player getSmallBlindPlayer() {
        return smallBlindPlayer;
    }

    public void setSmallBlindPlayer(Player smallBlindPlayer) {
        this.smallBlindPlayer = smallBlindPlayer;
    }

    public Player getBigBlindPlayer() {
        return bigBlindPlayer;
    }

    public void setBigBlindPlayer(Player bigBlindPlayer) {
        this.bigBlindPlayer = bigBlindPlayer;
    }

    public int getSmallBlindAmount() {
        return smallBlindAmount;
    }

    public void setSmallBlindAmount(int smallBlindAmount) {
        this.smallBlindAmount = smallBlindAmount;
    }

    public int getBigBlindAmount() {
        return bigBlindAmount;
    }

    public void setBigBlindAmount(int bigBlindAmount) {
        this.bigBlindAmount = bigBlindAmount;
    }

    public boolean resetGame(boolean keepPlayers) {
        this.deck.shuffle();
        if (this.gameStatus == null) {
            this.gameStatus = new GameStatus();
        } else {
            this.gameStatus.clearCommunityCards();
        }
        this.playerActions.clear();

        if (!keepPlayers) {
            gameStatus.getPlayers().clear();
        } else {
            List<String> currentPlayerIds = gameStatus.getPlayers().stream().map(Player::getId)
                    .collect(Collectors.toList());
            playerActions.keySet().removeIf(id -> !currentPlayerIds.contains(id));
        }

        for (Player player : gameStatus.getPlayers()) {
            player.clearHand();
            player.setFolded(false);
            player.setBetAmount(0);
            if (!keepPlayers) {
                player.setChips(player.getStartingChips());
            }
            playerActions.put(player.getId(), false);
        }

        dealerPosition = 0;
        currentBet = 0;
        return true;
    }

    public GameStatus startGame(List<PlayerInfo> playersInfo) {

        if (this.gameStatus == null) {
            this.gameStatus = new GameStatus();
        }

        resetGame(true);

        if (playersInfo != null && playersInfo.size() >= 2) {
            for (PlayerInfo playerInfo : playersInfo) {
                if (gameStatus.getPlayers().stream().noneMatch(p -> p.getName().equals(playerInfo.getName()))) {
                    registerPlayer(playerInfo.getName(), playerInfo.getStartingChips(), playerInfo.isBot());
                }
            }
            if (gameStatus.getPlayers().size() >= 3) {
                setBlinds(); // Blindok beállítása
            }

            dealInitialCards(); // Kezdeti lapok osztása
            gameStatus.setPhase(GameStatus.GamePhase.PRE_FLOP);
            gameStarted = true;
            return gameStatus;
        } else {
            System.out.println("Not enough players to start the game. Minimum 2 players required.");
            return null;
        }
    }

    public GameStatus registerPlayers(List<PlayerInfo> players) {
        if (players == null || players.isEmpty()) {
            players = getDefaultPlayers();
        }
        return startGame(players);
    }

    public List<PlayerInfo> getDefaultPlayers() {
        List<PlayerInfo> defaultPlayers = new ArrayList<>();
        defaultPlayers.add(new PlayerInfo("Játékos", 1000, false));
        defaultPlayers.add(new PlayerInfo("Bot1", 1000, true));
        defaultPlayers.add(new PlayerInfo("Bot2", 1000, true));
        defaultPlayers.add(new PlayerInfo("Bot3", 1000, true));
        return defaultPlayers;
    }

    public void setBlinds() {
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

    private void dealInitialCards() {
        gameStatus.getPlayers().forEach(player -> {
            player.clearHand();
            for (int i = 0; i < 2; i++) {
                if (deck.cardsLeft() == 0) {
                    deck.shuffle();
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

    public static boolean setGameStarted(boolean newGameStarted) {
        gameStarted = newGameStarted;
        return gameStarted;
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
            return false;
        }
        player.setFolded(true);
        playerActions.put(playerId, true);
        checkForEarlyWin();
        return true;
    }

    @SuppressWarnings("unused")
    private Player findPlayerByName(String name) {
        return gameStatus.getPlayers().stream()
                .filter(player -> player.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private void performFlop() {
        deck.drawCard();
        List<Card> flop = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            flop.add(deck.drawCard());
        }
        gameStatus.setCommunityCards(flop);
    }

    private void performTurn() {
        deck.drawCard();
        Card turnCard = deck.drawCard();
        gameStatus.addCardToCommunity(turnCard);
    }

    private void performRiver() {
        deck.drawCard();
        Card riverCard = deck.drawCard();
        gameStatus.addCardToCommunity(riverCard);
    }

    public synchronized boolean playerBet(String playerId, int amount) {
        Player player = findPlayerById(playerId);
        if (player != null && amount > 0) {
            int maxBet = player.getChips() + player.getBetAmount();
            int actualBet = Math.min(amount, maxBet);

            int betIncrement = actualBet - player.getBetAmount();
            player.setChips(player.getChips() - betIncrement);
            player.setBetAmount(actualBet);
            gameStatus.setCurrentPot(gameStatus.getCurrentPot() + betIncrement);

            if (actualBet > currentBet) {
                currentBet = actualBet;
            }

            playerActions.put(playerId, true);
            checkForEarlyWin();
            return true;
        }
        return false;
    }

    public void checkForEarlyWin() {
        long activePlayers = gameStatus.getPlayers().stream()
                .filter(p -> !p.isFolded() && p.getChips() > 0)

                .count();

        if (activePlayers == 1) {
            endGameEarly();
        }
    }

    public Player checkAllPlayersAllIn() {
        List<Player> activePlayers = gameStatus.getPlayers().stream()
                .filter(p -> !p.isFolded())
                .collect(Collectors.toList());

        if (activePlayers.stream().allMatch(p -> p.getChips() == 0)) {

            Player winner = determineWinner(activePlayers);
            if (winner != null) {
                winner.addWinnings(gameStatus.getCurrentPot());
                gameStatus.setCurrentPot(0);
            }
            return winner;
        }
        return null;
    }

    private int evaluateHand(List<Card> hand) {

        return hand.stream().mapToInt(card -> card.getValue().getStrength()).sum();
    }

    private void endGameEarly() {
        Player winner = gameStatus.getPlayers().stream()
                .filter(p -> !p.isFolded())
                .findFirst()
                .orElse(null);
        if (winner != null) {
            winner.addWinnings(gameStatus.getCurrentPot());
            gameStatus.setCurrentPot(0);
            resetGame(true);
        }
    }

    public String endGame() {
        if (gameStarted) {
            List<Player> activePlayers = gameStatus.getPlayers().stream()
                    .filter(player -> !player.isFolded())
                    .collect(Collectors.toList());

            if (activePlayers.isEmpty()) {
                return null;
            }

            Player winner = determineWinner(activePlayers);
            if (winner != null) {
                String winnerName = winner.getName();
                winner.addWinnings(gameStatus.getCurrentPot());
                gameStatus.setCurrentPot(0);
                gameStarted = false;
                resetGame(false);
                return winnerName;
            }
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
                    determineWinner(gameStatus.getPlayers().stream()
                            .filter(player -> !player.isFolded())
                            .collect(Collectors.toList()));
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

    public boolean areAllPlayersActed() {
        return gameStatus.getPlayers().stream()
                .filter(player -> !player.isFolded() && player.getChips() > 0)
                .allMatch(player -> playerActions.getOrDefault(player.getName(), false));
    }

    private Player determineWinner(List<Player> players) {

        return players.stream()
                .max(Comparator.comparingInt(p -> evaluateHand(p.getHand())))
                .orElse(null);
    }

    @SuppressWarnings("unused")
    private int compareHands(List<Card> hand1, List<Card> hand2) {
        List<Card> communityCards = gameStatus.getCommunityCards();
        List<Card> fullHand1 = new ArrayList<>(hand1);
        List<Card> fullHand2 = new ArrayList<>(hand2);
        fullHand1.addAll(communityCards);
        fullHand2.addAll(communityCards);
        HandEvaluator evaluator = new HandEvaluator();
        HandResult rank1 = evaluator.evaluate(fullHand1, communityCards);
        HandResult rank2 = evaluator.evaluate(fullHand2, communityCards);

        return Integer.compare(rank1.getHandStrength(), rank2.getHandStrength());
    }

    private Player findPlayerById(String playerId) {
        return gameStatus.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public GameStatus startNewMatch() {
        List<Player> players = new ArrayList<>(gameStatus.getPlayers());
        playerEliminationWhenOutOfChips();
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

    public void automateBotAction(Player botPlayer) {
        System.out.println("Automating bot action for: " + botPlayer.getId());

        GameStatus gameStatus = getGameStatus();

        if (gameStatus == null) {
            System.out.println("Game status is null, cannot proceed with bot action.");
            return;
        }

        int currentBet = gameStatus.getCurrentBet();
        int botChips = botPlayer.getChips();
        var botId = botPlayer.getId();

        System.out.println("Current bet: " + currentBet + ", Bot chips: " + botChips);

        if (botChips >= currentBet) {
            playerBet(botId, currentBet);
            System.out.println("Bot bet: " + currentBet);
        } else {
            playerFold(botId);
            System.out.println("Bot folded due to insufficient chips.");
        }
    }

    public boolean playerEliminationWhenOutOfChips() {
        List<Player> playersToRemove = gameStatus.getPlayers().stream()
                .filter(player -> player.getChips() <= 0)
                .collect(Collectors.toList());

        gameStatus.getPlayers().removeAll(playersToRemove);
        playersToRemove.forEach(player -> playerActions.remove(player.getId()));

        if (gameStatus.getPlayers().size() == 1) {
            endGameEarly();
            return false;
        }
        return true;
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
            gameStatus.setCurrentPot(gameStatus.getCurrentPot() + raiseAmount);
            currentBet = amount;
            playerActions.put(playerId, true);

            proceedToNextRound();
            return true;
        }
        return false;
    }

}
