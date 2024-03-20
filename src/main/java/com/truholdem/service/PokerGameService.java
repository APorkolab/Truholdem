package com.truholdem.service;

import com.truholdem.model.Card;
import com.truholdem.model.Deck;
import com.truholdem.model.GameStatus;
import com.truholdem.model.Player;
import com.truholdem.model.PlayerInfo;
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
        resetGame();
    }

    private void resetGame() {
        this.deck = new Deck();
        this.deck.shuffle();
        this.gameStatus = new GameStatus();
        this.gameStatus.clearCommunityCards();
        gameStarted = false;
        currentBet = 0;
        pot = 0;
        // Minden játékos kezét kiürítjük, de megtartjuk a játékosokat és a zsetonjaikat
        gameStatus.getPlayers().forEach(Player::clearHand);
        // További játék-specifikus állapotok alaphelyzetbe állítása, ha szükséges
    }


    public GameStatus startGame(List<PlayerInfo> playersInfo) {
        resetGame();
        deck.shuffle();

        // Ha a lista null vagy üres, akkor alapértelmezett tesztjátékosokat adunk hozzá
        if (playersInfo == null || playersInfo.isEmpty()) {
            registerPlayer("Anna", 1000, false); // Feltételezve, hogy Anna egy valódi játékos 1000 kezdő zsetonnal
            registerPlayer("BotBéla", 1000, true);
            registerPlayer("BotCili", 1000, true);
            registerPlayer("BotJuli", 1000, true);
        } else {
            for (PlayerInfo playerInfo : playersInfo) {
                registerPlayer(playerInfo.getName(), playerInfo.getStartingChips(), playerInfo.isBot());
            }
        }

        setBlinds();
        dealInitialCards();
        makeBotDecisions();
        gameStatus.setPhase(GameStatus.GamePhase.PRE_FLOP);
        gameStarted = true;
        return gameStatus;
    }

    private void setBlinds() {
        List<Player> activePlayers = gameStatus.getPlayers();
        if (activePlayers.size() >= 2) {
            // Feltételezve, hogy az activePlayers lista már tartalmazza a játékosokat a kívánt sorrendben
            smallBlindPlayer = activePlayers.get(0);
            bigBlindPlayer = activePlayers.get(1);

            // Kisvak és nagyvak tétjeinek levonása és a potba helyezése
            playerBet(smallBlindPlayer.getId(), smallBlindAmount);
            playerBet(bigBlindPlayer.getId(), bigBlindAmount);
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

    private void rotateBlinds() {
        Collections.rotate(gameStatus.getPlayers(), -1);
        // Újra beállítja a vakokat a következő körre
        setBlinds();
    }

    public boolean registerPlayer(String playerName, int startingChips, boolean isBot) {
        if (!gameStarted && gameStatus.getPlayers().size() < 4) { // Max 4 játékos
            Player newPlayer = new Player(playerName);
            newPlayer.setChips(startingChips);
            if (isBot) {
                newPlayer.setName("Bot" + playerName); // Bot játékosok esetén a névhez "Bot" előtag hozzáadása
            } else {
                newPlayer.setName(playerName);
            }
            gameStatus.addPlayer(newPlayer);
            return true;
        }
        return false;
    }


    private void addBots() {
        for (int i = 1; i <= 3; i++) { // Mindig 3 bot hozzáadása
            gameStatus.getPlayers().add(new Player("Bot " + i));
        }
    }

    // Deal the flop
    public Optional<GameStatus> dealFlop() {
        if (gameStarted && gameStatus.getPhase() == GameStatus.GamePhase.PRE_FLOP) {
            performFlop();
            makeBotDecisions();
            gameStatus.setPhase(GameStatus.GamePhase.FLOP);
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    // Deal the turn
    public Optional<GameStatus> dealTurn() {
        if (gameStarted && gameStatus.getPhase() == GameStatus.GamePhase.FLOP) {
            performTurn();
            gameStatus.setPhase(GameStatus.GamePhase.TURN);
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    // Deal the river
    public Optional<GameStatus> dealRiver() {
        if (gameStarted && gameStatus.getPhase() == GameStatus.GamePhase.TURN) {
            performRiver();
            gameStatus.setPhase(GameStatus.GamePhase.RIVER);
            return Optional.of(gameStatus);
        }
        return Optional.empty();
    }

    public boolean playerFold(String playerId) {
        // Megkeressük a játékost az ID alapján.
        Player player = findPlayerById(playerId);

        // Ellenőrizzük, hogy a játékos létezik-e és még nem passzolt-e.
        if (player != null && !player.isFolded()) {
            player.setFolded(true);
            checkForEarlyWin();

            return true; // Sikeres passzolás
        }

        return false; // Nem sikerült passzolni (pl. a játékos nem létezik, vagy már passzolt)
    }


    private void performFlop() {
        deck.drawCard(); // Burn
        List<Card> flop = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            flop.add(deck.drawCard());
        }
        makeBotDecisions();
        gameStatus.setCommunityCards(flop);
    }

    private void performTurn() {
        deck.drawCard(); // Burn
        Card turnCard = deck.drawCard();
        makeBotDecisions();
        gameStatus.addCardToCommunity(turnCard);
    }

    private void performRiver() {
        deck.drawCard(); // Burn
        Card riverCard = deck.drawCard();
        makeBotDecisions();
        gameStatus.addCardToCommunity(riverCard);
    }

    // Játékos tétje és passzolása
    public boolean playerBet(String playerId, int amount) {
        Player player = findPlayerById(playerId);
        if (gameStarted && player != null && amount >= gameStatus.getCurrentBet() && player.getChips() >= amount) {
            if (amount > gameStatus.getCurrentBet()) {
                gameStatus.setCurrentBet(amount);
            }
            player.setChips(player.getChips() - amount);
            gameStatus.setCurrentPot(gameStatus.getCurrentPot() + amount);
            System.out.println("Bet placed: " + playerId + " bet " + amount);
            return true;
        } else {
            System.out.println("Bet failed: " + (player == null ? "Player not found" : "Invalid bet") + " by " + playerId);
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
            // Logika a győztes bejelentésére és a játék állapotának frissítésére
            System.out.println("Game ended early. Winner is: " + winner.getId() + " with pot: " + pot);

            // Játék újraindítása vagy befejezése
            resetGame();
        }
    }

    public String endGame() {
        if (gameStarted) {
            String winnerId = determineWinner();
            gameStarted = false;
            dealInitialCards(); // Újraindítja a játékot a következő körre
            rotateBlinds();
            resetGame();
            return winnerId;

        }
        return null;
    }

    private boolean areAllBetsEqual() {
        int expectedBet = currentBet;
        return gameStatus.getPlayers().stream()
                .filter(p -> !p.isFolded()) 
                .allMatch(p -> p.getCurrentBet() == expectedBet);
    }

    // Következő kör vagy játék vége
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
                    resetGame();
                    break;
            }
        }
    }

    public GameStatus getGameStatus() {
        return gameStarted ? gameStatus : null;
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
            // Megvan a győztes, hozzáadjuk a nyereményt és kiírjuk az üzenetet
            Player winningPlayer = winner.get();
            winningPlayer.addWinnings(pot);
            System.out.println("Winner is: " + winningPlayer.getId() + " with pot: " + pot);
        }

        resetGame(); // Újraindítjuk a játékot a következő körre
        return winner.map(Player::getId).orElse("");
    }

    private int compareHighCard(HandResult hand1, HandResult hand2) {
        List<Card> cards1 = hand1.getCards();
        List<Card> cards2 = hand2.getCards();

        // A lapokat értékek alapján hasonlítjuk össze, kezdve a legmagasabbal
        for (int i = cards1.size() - 1; i >= 0; i--) {
            int compare = cards2.get(i).getValue().ordinal() - cards1.get(i).getValue().ordinal();
            if (compare != 0) {
                // Ha találunk különbséget, akkor visszatérünk az összehasonlítás eredményével
                return compare;
            }
        }
        // Ha az összes lap megegyezik, akkor döntetlen van
        return 0;
    }

    private Player findPlayerById(String playerId) {
        return gameStatus.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    private void makeBotDecisions() {
        Random random = new Random();
        gameStatus.getPlayers().stream()
                .filter(player -> player.getId().startsWith("Bot"))
                .forEach(player -> {
                    if (random.nextBoolean()) {
                        player.setFolded(true);
                        playerFold(player.getId());
                    } else {
                        // A minimum tét, amit a botnak meg kell tennie (figyelembe véve a jelenlegi tétet)
                        int minBet = Math.max(currentBet, 20);
                        if (player.getChips() > minBet) {
                            // A bot rendelkezésére álló maximális tét, ami nem haladhatja meg a rendelkezésre álló zsetonokat
                            int maxBetPossible = Math.min(player.getChips(), minBet + random.nextInt(100) + 1);
                            playerBet(player.getId(), maxBetPossible);
                        } else {
                            // Ha a bot nem tudja megtenni a minimum tétet, passzol
                            player.setFolded(true);
                            playerFold(player.getId());
                        }
                    }
                });
    }

    // Pot növelése validálással
    public void addToPot(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
        gameStatus.setCurrentPot(gameStatus.getCurrentPot() + amount);
    }

    public void updateCurrentBet(int newBet) {
        gameStatus.setCurrentBet(newBet);
    }
}
