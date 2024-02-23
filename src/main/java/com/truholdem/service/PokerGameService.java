package com.truholdem.service;

import com.truholdem.model.Card;
import com.truholdem.model.Deck;
import com.truholdem.model.GameStatus;
import com.truholdem.model.Player;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PokerGameService {
    private GameStatus gameStatus = new GameStatus();
    private Deck deck = new Deck();
    private boolean gameStarted = false;
    private int currentBet = 0;
    private int pot = 0;

    public PokerGameService() {
        initializeGame();
    }

    private void initializeGame() {
        gameStatus = new GameStatus();
        deck = new Deck();
        gameStarted = false;
        currentBet = 0;
        pot = 0;
        deck.shuffle();
    }

    public GameStatus startGame() {
        if (!gameStarted && gameStatus.getPlayers().size() == 1) {
            addBots();
            deck.resetDeck();
            dealInitialCards();
            gameStatus.setPhase(GameStatus.GamePhase.PRE_FLOP);
            gameStarted = true;
            return gameStatus;
        }
        return null;
    }

    private void addBots() {
        for (int i = 1; i <= 3; i++) {
            gameStatus.getPlayers().add(new Player("Bot " + i));
        }
    }
    private void dealInitialCards() {
        gameStatus.getPlayers().forEach(player -> {
            player.getHand().add(deck.drawCard());
            player.getHand().add(deck.drawCard());
        });
    }

    public boolean registerPlayer(String playerId) {
        if (!gameStarted) {
            gameStatus.getPlayers().add(new Player(playerId));
            return true;
        }
        return false;
    }

    public GameStatus dealFlop() {
        if (gameStarted && gameStatus.getPhase() == GameStatus.GamePhase.PRE_FLOP) {
            performFlop();
            gameStatus.setPhase(GameStatus.GamePhase.FLOP);
            return gameStatus;
        }
        return null;
    }

    private void performFlop() {
        // "Burn" egy lapot, ami azt jelenti, hogy eldobjuk a pakli tetejéről a lapot
        deck.drawCard();
        // Kiosztunk három közösségi lapot a flop-hoz
        for (int i = 0; i < 3; i++) {
            gameStatus.getCommunityCards().add(deck.drawCard());
        }
    }

    private void performTurn() {
        // Ismét "burn" egy lapot
        deck.drawCard();
        // Kiosztunk egy lapot a turn-hoz
        gameStatus.getCommunityCards().add(deck.drawCard());
    }

    private void performRiver() {
        // Megint csak "burn" egy lapot
        deck.drawCard();
        // Kiosztunk egy lapot a river-hez
        gameStatus.getCommunityCards().add(deck.drawCard());
    }


    public GameStatus dealTurn() {
        if (gameStarted && gameStatus.getPhase() == GameStatus.GamePhase.FLOP) {
            performTurn();
            gameStatus.setPhase(GameStatus.GamePhase.TURN);
            return gameStatus;
        }
        return null;
    }

    public GameStatus dealRiver() {
        if (gameStarted && gameStatus.getPhase() == GameStatus.GamePhase.TURN) {
            performRiver();
            gameStatus.setPhase(GameStatus.GamePhase.RIVER);
            return gameStatus;
        }
        return null;
    }

    public boolean playerBet(String playerId, int amount) {
        Player player = findPlayerById(playerId);
        if (gameStarted && player != null && amount >= currentBet && player.getChips() >= amount) {
            // Frissítsd a jelenlegi tétet, ha az új tét magasabb
            currentBet = Math.max(currentBet, amount);

            // Csökkentsd a játékos zsetonjainak számát és növeld a pot méretét
            player.setChips(player.getChips() - amount); // Feltételezve, hogy van `chips` mező a Player osztályban
            pot += amount;
            player.setCurrentBet(player.getCurrentBet() + amount); // Feltételezve, hogy van `currentBet` mező

            // Ellenőrizd, hogy mindenki megadta-e a tétet, ha igen, lépj a következő körbe
            if (areAllBetsEqual()) {
                proceedToNextRound(); // Ez a metódus kezeli a játék következő szakaszába való lépést
            }

            return true;
        }
        return false;
    }

    private boolean areAllBetsEqual() {
        // Ellenőrizd, hogy minden játékban maradt játékos azonos összeget tett-e meg
        int expectedBet = currentBet;
        return gameStatus.getPlayers().stream()
                .filter(p -> !p.isFolded())
                .allMatch(p -> p.getCurrentBet() == expectedBet);
    }

    private void proceedToNextRound() {
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
                break;
            default:
                throw new IllegalStateException("Unexpected game phase: " + gameStatus.getPhase());
        }
    }
    public boolean playerFold(String playerId) {
        Player player = findPlayerById(playerId);
        if (gameStarted && player != null) {
            player.setFolded(true);

            long activePlayers = gameStatus.getPlayers().stream().filter(p -> !p.isFolded()).count();
            // Ha csak egy aktív játékos marad, ő nyeri a potot
            if (activePlayers == 1) {
                endGameEarly();
            }
            return true;
        }
        return false;
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

    private void resetGame() {
        // Itt inicializáld újra a játékot a következő körhöz
        initializeGame();
        // Alternatívaként beállíthatod a gameStarted flag-et false-ra, ha nem szeretnéd automatikusan újraindítani a játékot
        gameStarted = false;
    }

    public GameStatus getGameStatus() {
        return gameStarted ? gameStatus : null;
    }

    public String endGame() {
        if (gameStarted) {
            String winnerId = determineWinner();
            gameStarted = false;
            initializeGame(); // Újraindítja a játékot a következő körre
            return winnerId;
        }
        return null;
    }

    private String determineWinner() {
        HandEvaluator evaluator = new HandEvaluator();
        HandResult bestHand = null;
        Player winner = null;

        for (Player player : gameStatus.getPlayers()) {
            if (!player.isFolded()) { // Csak azokat a játékosokat értékeljük, akik nem passzoltak
                HandResult result = evaluator.evaluate(player.getHand(), gameStatus.getCommunityCards());
                if (bestHand == null || result.getHandStrength() > bestHand.getHandStrength() ||
                        (result.getHandStrength() == bestHand.getHandStrength() && compareHighCard(result, bestHand) > 0)) {
                    bestHand = result;
                    winner = player;
                }
            }
        }

        if (winner != null) {
            // Itt adjuk hozzá a pot értékét a győztes zsetonjaihoz
            winner.addWinnings(gameStatus.getCurrentPot());
            return winner.getId(); // Visszatérünk a győztes játékos azonosítójával
        }

        return ""; // Ha valamiért nem sikerül meghatározni a győztest
    }

    // Egy egyszerűsített metódus, amely összehasonlítja a legmagasabb lapot két azonos erősségű kéz között
    private int compareHighCard(HandResult hand1, HandResult hand2) {
        // Feltételezzük, hogy a lapok már rendezve vannak a kézben
        List<Card> cards1 = hand1.getCards();
        List<Card> cards2 = hand2.getCards();

        for (int i = 0; i < Math.min(cards1.size(), cards2.size()); i++) {
            int compare = cards2.get(i).getValue().ordinal() - cards1.get(i).getValue().ordinal();
            if (compare != 0) {
                // Ha a két lap nem egyenlő, akkor az összehasonlítás eredménye megadja, melyik a magasabb
                return compare;
            }
        }

        // Ha minden összehasonlított lap egyenlő, akkor a két kéz azonos
        return 0;
    }


    private Player findPlayerById(String playerId) {
        return gameStatus.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }
}
