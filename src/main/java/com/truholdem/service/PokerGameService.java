package com.truholdem.service;

import com.truholdem.model.Card;
import com.truholdem.model.Deck;
import com.truholdem.model.GameStatus;
import com.truholdem.model.Player;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PokerGameService {
    private GameStatus gameStatus;
    private Deck deck;
    private boolean gameStarted;
    private int currentBet;
    private int pot;

    public PokerGameService() {
        resetGame();
        startGame();
    }

    private void resetGame() {
        gameStatus = new GameStatus();
        deck = new Deck();
        deck.shuffle();
        gameStarted = false;
        currentBet = 0;
        pot = 0;
    }

    public GameStatus startGame() {
        deck.resetDeck();
        registerPlayer("Anna");
        registerPlayer("Béla");
        registerPlayer("Cili");
        registerPlayer("Juli");
        dealInitialCards();
        gameStatus.setPhase(GameStatus.GamePhase.PRE_FLOP);
        gameStarted = true;
        System.out.println(gameStatus.getPlayers().size());
        return gameStatus; // Visszaadjuk a játék állapotát az indulás után

    }

    private void dealInitialCards() {
        gameStatus.getPlayers().forEach(player -> {
            player.clearHand(); // Tiszta kezdés minden játékosnak
            for (int i = 0; i < 2; i++) {
                player.addCardToHand(deck.drawCard());
            }
        });
    }

    public boolean registerPlayer(String playerId) {
        if (!gameStarted && gameStatus.getPlayers().size() < 4) { // Max 4 játékos, beleértve a botokat is
            gameStatus.addPlayer(new Player(playerId));
            if (gameStatus.getPlayers().size() == 1) {
                addBots(); // Automatikusan adj hozzá botokat, ha az első játékos regisztrál
            }
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
        Card turnCard = deck.drawCard(); // Draw the turn card
        gameStatus.getCommunityCards().add(turnCard); // Add the turn card to the community cards
    }

    private void performRiver() {
        deck.drawCard(); // Burn
        Card riverCard = deck.drawCard(); // Draw the river card
        gameStatus.getCommunityCards().add(riverCard); // Add the river card to the community cards
    }


    // Játékos tétje és passzolása
    public boolean playerBet(String playerId, int amount) {
        Player player = findPlayerById(playerId);
        if (gameStarted && player != null && amount >= currentBet && player.getChips() >= amount) {
            if (amount > currentBet) {
                currentBet = amount; // Frissítjük a jelenlegi tétet, ha nagyobb
            }
            player.setChips(player.getChips() - amount); // Levonjuk a tétet a játékos zsetonjaiból
            pot += amount; // Növeljük a pot méretét
            return true;
        }
        return false;
    }

    public boolean playerFold(String playerId) {
        Player player = findPlayerById(playerId);
        if (gameStarted && player != null) {
            player.setFolded(true);
            checkForEarlyWin();
            return true;
        }
        return false;
    }

    private void checkForEarlyWin() {
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


}
