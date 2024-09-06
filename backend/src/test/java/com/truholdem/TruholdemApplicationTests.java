package com.truholdem;

import com.truholdem.controller.PokerGameController;
import com.truholdem.model.*;
import com.truholdem.service.HandEvaluator;
import com.truholdem.service.HandResult;
import com.truholdem.service.PokerGameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class TruholdemApplicationTests {

    @Mock
    private PokerGameService pokerGameService;

    @InjectMocks
    private PokerGameController pokerGameController;
    private HandEvaluator handEvaluator;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        handEvaluator = new HandEvaluator();
        pokerGameController = new PokerGameController(pokerGameService);
    }

    @Test
    void contextLoads() {
        assertNotNull(pokerGameController, "PokerGameController should not be null");
        assertNotNull(pokerGameService, "PokerGameService should not be null");
    }

    @Test
    void testStartGame() {
        List<PlayerInfo> playersInfo = Arrays.asList(
                new PlayerInfo("Player1", 1000, false),
                new PlayerInfo("Player2", 1000, false));
        GameStatus mockGameStatus = new GameStatus();
        when(pokerGameService.startGame(playersInfo)).thenReturn(mockGameStatus);

        ResponseEntity<GameStatus> response = pokerGameController.startGame(playersInfo);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testDealFlop() {
        GameStatus mockGameStatus = new GameStatus();
        when(pokerGameService.dealFlop()).thenReturn(Optional.of(mockGameStatus));

        ResponseEntity<GameStatus> response = pokerGameController.dealFlop();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testPlayerBet() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("playerId", "player1");
        payload.put("amount", 100);

        when(pokerGameService.playerBet("player1", 100)).thenReturn(true);

        ResponseEntity<Map<String, String>> response = pokerGameController.playerBet(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("Bet placed successfully.", body.get("message"));
    }

    @Test
    void testPlayerFold() {

        when(pokerGameService.playerFold("player1")).thenReturn(true);

        ResponseEntity<String> response = pokerGameController.playerFold("player1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Player folded successfully.", response.getBody());

        verify(pokerGameService, times(1)).playerFold("player1");
    }

    @Test
    void testRegisterPlayer() {
        // Arrange
        List<PlayerInfo> playerInfos = Collections.singletonList(new PlayerInfo("NewPlayer", 1000, false));
        when(pokerGameService.registerPlayer("NewPlayer", 1000, false)).thenReturn(true);

        // Act
        ResponseEntity<String> response = pokerGameController.registerPlayer(playerInfos);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("All players registered successfully.", response.getBody());

        // Verify the interaction with the service
        verify(pokerGameService, times(1)).registerPlayer("NewPlayer", 1000, false);
    }

    @Test
    void testGetGameStatus() {
        GameStatus mockGameStatus = new GameStatus();
        when(pokerGameService.getGameStatus()).thenReturn(mockGameStatus);

        ResponseEntity<GameStatus> response = pokerGameController.getGameStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testEndGame() {
        when(pokerGameService.endGame()).thenReturn("player1");

        ResponseEntity<String> response = pokerGameController.endGame();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Game ended. Winner is: player1", response.getBody());
    }

    @Test
    void testStartNewMatch() {
        GameStatus mockGameStatus = new GameStatus();
        mockGameStatus.addPlayer(new Player("Player1", 1000));
        when(pokerGameService.startNewMatch()).thenReturn(mockGameStatus);

        ResponseEntity<GameStatus> response = pokerGameController.startNewMatch();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        GameStatus body = response.getBody();
        assertNotNull(body);
        assertFalse(body.getPlayers().isEmpty());
        assertEquals(1, body.getPlayers().size());
    }

    @Test
    void testResetGame() {
        Map<String, Boolean> request = new HashMap<>();
        request.put("keepPlayers", true);
        when(pokerGameService.resetGame(true)).thenReturn(true);

        ResponseEntity<String> response = pokerGameController.resetGame(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Game has been reset successfully.", response.getBody());
    }

    @Test
    void testHandEvaluatorRoyalFlush() {
        List<Card> playerHand = Arrays.asList(
                new Card(Suit.HEARTS, Value.ACE),
                new Card(Suit.HEARTS, Value.KING));
        List<Card> communityCards = Arrays.asList(
                new Card(Suit.HEARTS, Value.QUEEN),
                new Card(Suit.HEARTS, Value.JACK),
                new Card(Suit.HEARTS, Value.TEN),
                new Card(Suit.CLUBS, Value.TWO),
                new Card(Suit.DIAMONDS, Value.THREE));

        HandResult result = handEvaluator.evaluate(playerHand, communityCards);
        assertEquals(1000, result.getHandStrength());
    }

    @Test
    void testHandEvaluatorFullHouse() {
        List<Card> playerHand = Arrays.asList(
                new Card(Suit.HEARTS, Value.ACE),
                new Card(Suit.SPADES, Value.ACE));
        List<Card> communityCards = Arrays.asList(
                new Card(Suit.DIAMONDS, Value.ACE),
                new Card(Suit.CLUBS, Value.KING),
                new Card(Suit.HEARTS, Value.KING),
                new Card(Suit.SPADES, Value.TWO),
                new Card(Suit.DIAMONDS, Value.THREE));

        HandResult result = handEvaluator.evaluate(playerHand, communityCards);
        assertEquals(700, result.getHandStrength());
    }

    @Test
    void testDeckInitialization() {
        Deck deck = new Deck();
        assertEquals(52, deck.cardsLeft());
    }

    @Test
    void testDeckDrawCard() {
        Deck deck = new Deck();
        Card drawnCard = deck.drawCard();
        assertNotNull(drawnCard);
        assertEquals(51, deck.cardsLeft());
    }

    @Test
    void testPlayerPlaceBet() {
        Player player = new Player("TestPlayer", 1000);
        player.placeBet(100);
        assertEquals(900, player.getChips());
        assertEquals(100, player.getBetAmount());
    }

    @Test
    void testPlayerAddWinnings() {
        Player player = new Player("TestPlayer", 1000);
        player.addWinnings(500);
        assertEquals(1500, player.getChips());
    }

    @Test
    void testPlayerRaise() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("playerId", "player1");
        payload.put("amount", 200);

        when(pokerGameService.playerRaise("player1", 200)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = pokerGameController.playerRaise(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Raise successful.", body.get("message"));
    }

    @Test
    void testBotAction() {

        PokerGameService pokerGameService = spy(new PokerGameService());

        Player mockBotPlayer = new Player("Player1", 1000);
        var mockBotPlayerId = mockBotPlayer.getId();

        GameStatus mockGameStatus = mock(GameStatus.class);
        when(pokerGameService.getGameStatus()).thenReturn(mockGameStatus);
        when(mockGameStatus.getCurrentBet()).thenReturn(200);
        when(mockGameStatus.getPlayers()).thenReturn(List.of(mockBotPlayer));

        doReturn(true).when(pokerGameService).playerBet(eq(mockBotPlayerId), eq(200));

        pokerGameService.automateBotAction(mockBotPlayer);

        verify(pokerGameService, times(1)).playerBet(eq(mockBotPlayerId), eq(200));
    }

    @Test
    void testEarlyWin() {
        Player player1 = new Player("Player1", 1000);
        player1.setFolded(false);

        Player player2 = new Player("Player2", 1000);
        player2.setFolded(true);

        GameStatus gameStatus = new GameStatus();
        gameStatus.addPlayer(player1);
        gameStatus.addPlayer(player2);

        when(pokerGameService.getGameStatus()).thenReturn(gameStatus);

        pokerGameService.checkForEarlyWin();

        verify(pokerGameService, times(1)).checkForEarlyWin();

        int expectedChips = 1000 + gameStatus.getPot();
        assertEquals(expectedChips, player1.getChips());
    }

    @Test
    void testDealTurn() {
        GameStatus mockGameStatus = new GameStatus();
        when(pokerGameService.dealTurn()).thenReturn(Optional.of(mockGameStatus));

        ResponseEntity<GameStatus> response = pokerGameController.dealTurn();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testDealRiver() {
        GameStatus mockGameStatus = new GameStatus();
        when(pokerGameService.dealRiver()).thenReturn(Optional.of(mockGameStatus));

        ResponseEntity<?> response = pokerGameController.dealRiver();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof GameStatus);
    }

    @Test
    void testSynchronizedPlayerBet() throws InterruptedException {
        Player player1 = new Player("Player1", 1000);
        GameStatus mockGameStatus = new GameStatus();
        mockGameStatus.addPlayer(player1);

        when(pokerGameService.getGameStatus()).thenReturn(mockGameStatus);
        when(pokerGameService.playerBet(anyString(), anyInt())).thenReturn(true);

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(2);

        Thread thread1 = new Thread(() -> {
            latch.countDown();
            pokerGameService.playerBet("Player1", 200);
        });

        Thread thread2 = new Thread(() -> {
            latch.countDown();
            pokerGameService.playerBet("Player1", 300);
        });

        thread1.start();
        thread2.start();

        latch.await();
        thread1.join();
        thread2.join();

        verify(pokerGameService, atLeastOnce()).playerBet("Player1", 200);
        verify(pokerGameService, atLeastOnce()).playerBet("Player1", 300);
    }

    @Test
    void testBotActionWithNotEnoughChips() {
        PokerGameService pokerGameService = spy(new PokerGameService());

        GameStatus gameStatus = new GameStatus();
        Player player1 = new Player("Player1", 100);
        gameStatus.addPlayer(player1);
        gameStatus.setCurrentBet(200);
        pokerGameService.setGameStatus(gameStatus);

        pokerGameService.automateBotAction(player1);

        assertTrue(player1.isFolded(), "Player1 should have folded due to insufficient chips.");
    }

    @Test
    void testBotActionWithExactChips() {
        PokerGameService pokerGameService = spy(new PokerGameService());

        Player player1 = new Player("Player1", 200);
        var player1Id = player1.getId();

        GameStatus mockGameStatus = mock(GameStatus.class);
        when(pokerGameService.getGameStatus()).thenReturn(mockGameStatus);
        when(mockGameStatus.getCurrentBet()).thenReturn(200);
        when(mockGameStatus.getPlayers()).thenReturn(List.of(player1));

        doReturn(true).when(pokerGameService).playerBet(eq(player1Id), eq(200));

        pokerGameService.automateBotAction(player1);

        verify(pokerGameService, times(1)).playerBet(eq(player1Id), eq(200));
    }

    @Test
    void testBotActionWithEnoughChips() {
        PokerGameService pokerGameService = spy(new PokerGameService());

        Player player1 = new Player("Player1", 1000);
        var player1Id = player1.getId();

        GameStatus mockGameStatus = mock(GameStatus.class);
        when(pokerGameService.getGameStatus()).thenReturn(mockGameStatus);
        when(mockGameStatus.getCurrentBet()).thenReturn(200);
        when(mockGameStatus.getPlayers()).thenReturn(List.of(player1));

        doReturn(true).when(pokerGameService).playerBet(eq(player1Id), eq(200));

        pokerGameService.automateBotAction(player1);

        verify(pokerGameService, times(1)).playerBet(eq(player1Id), eq(200));
    }

    @Test
    void testEarlyWinAndPotDistribution() {
        PokerGameService.setGameStarted(true);
        Player player1 = new Player("Player1", 1000);
        player1.setFolded(false);

        Player player2 = new Player("Player2", 1000);
        player2.setFolded(true);

        GameStatus gameStatus = new GameStatus();
        gameStatus.addPlayer(player1);
        gameStatus.addPlayer(player2);
        gameStatus.setCurrentPot(500);

        when(pokerGameService.getGameStatus()).thenReturn(gameStatus);

        pokerGameService.checkForEarlyWin();

        verify(pokerGameService, times(1)).checkForEarlyWin();

        player1.addWinnings(gameStatus.getCurrentPot());

        assertEquals(1500, player1.getChips());
    }

    @Test
    void testAllPlayersActed() {
        PokerGameService.setGameStarted(true);
        Player player1 = new Player("Player1", 1000);
        Player player2 = new Player("Player2", 1000);

        GameStatus gameStatus = new GameStatus();
        gameStatus.addPlayer(player1);
        gameStatus.addPlayer(player2);

        Map<String, Boolean> playerActions = new HashMap<>();
        playerActions.put(player1.getId(), true);
        playerActions.put(player2.getId(), true);

        gameStatus.setPlayerActions(playerActions);

        when(pokerGameService.getGameStatus()).thenReturn(gameStatus);

        boolean allPlayersActed = gameStatus.areAllPlayersActed();

        assertTrue(allPlayersActed);
    }

    @Test
    void testPlayerEliminationWhenOutOfChips() {

        PokerGameService pokerGameService = new PokerGameService();
        PokerGameService.setGameStarted(true);

        Player player1 = new Player("Player1", 0);
        Player player2 = new Player("Player2", 1000);

        GameStatus mockGameStatus = new GameStatus();
        mockGameStatus.addPlayer(player1);
        mockGameStatus.addPlayer(player2);

        pokerGameService.setGameStatus(mockGameStatus);

        GameStatus newGameStatus = pokerGameService.startNewMatch();

        assertNotNull(newGameStatus);
        assertEquals(1, newGameStatus.getPlayers().size());
        assertEquals("Player2", newGameStatus.getPlayers().get(0).getName());
    }

    @Test
    void testDetermineWinner() {
        PokerGameService pokerGameService = new PokerGameService();
        PokerGameService.setGameStarted(true);

        Player player1 = new Player("Player1", 600);
        Player player2 = new Player("Player2", 1000);

        GameStatus mockGameStatus = new GameStatus();
        mockGameStatus.addPlayer(player1);
        mockGameStatus.addPlayer(player2);

        mockGameStatus.setPhase(GameStatus.GamePhase.RIVER);
        pokerGameService.setGameStatus(mockGameStatus);

        player1.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
        player1.addCardToHand(new Card(Suit.HEARTS, Value.KING));
        player2.addCardToHand(new Card(Suit.CLUBS, Value.TWO));
        player2.addCardToHand(new Card(Suit.CLUBS, Value.THREE));

        pokerGameService.getGameStatus().setCurrentPot(200);

        String winnerId = pokerGameService.endGame();

        assertEquals("Player1", winnerId, "The winner should be Player1 based on the stronger hand.");
    }

    @Test
    void testAllPlayersAllInAndWinnerTakesPot() {
        PokerGameService pokerGameService = new PokerGameService();
        PokerGameService.setGameStarted(true);

        Player player1 = new Player("Player1", 0);
        Player player2 = new Player("Player2", 1000);

        GameStatus mockGameStatus = new GameStatus();
        mockGameStatus.addPlayer(player1);
        mockGameStatus.addPlayer(player2);

        pokerGameService.setGameStatus(mockGameStatus);

        player1.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
        player1.addCardToHand(new Card(Suit.HEARTS, Value.KING));

        player2.addCardToHand(new Card(Suit.CLUBS, Value.TWO));
        player2.addCardToHand(new Card(Suit.CLUBS, Value.THREE));

        pokerGameService.getGameStatus().setCurrentPot(2000);
        player1.setChips(0);
        player2.setChips(0);

        Player winner = pokerGameService.checkAllPlayersAllIn();

        assertNotNull(winner);
        assertEquals(player1, winner);
        assertEquals(2000, winner.getChips());
        assertEquals(0, player2.getChips());
    }

}