package com.truholdem.controller;

import com.truholdem.model.GameStatus;
import com.truholdem.model.PlayerInfo;
import com.truholdem.service.PokerGameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/poker")
@Tag(name = "Poker Game API", description = "Operations for managing a poker game")
public class PokerGameController {

    private final PokerGameService pokerGameService;

    public PokerGameController(PokerGameService pokerGameService) {
        this.pokerGameService = pokerGameService;
    }

    @PostMapping("/start")
    @Operation(summary = "Start a new game", description = "Initializes a new poker game with the provided players")
    public ResponseEntity<GameStatus> startGame(@RequestBody List<PlayerInfo> playersInfo) {
        GameStatus gameStatus = pokerGameService.startGame(playersInfo);
        if (gameStatus != null) {
            return ResponseEntity.ok(gameStatus);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/flop")
    @Operation(summary = "Deal flop", description = "Deals the flop cards in the poker game")
    public ResponseEntity<GameStatus> dealFlop() {
        Optional<GameStatus> gameStatus = pokerGameService.dealFlop();
        return gameStatus.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping("/turn")
    @Operation(summary = "Deal turn", description = "Deals the turn card in the poker game")
    public ResponseEntity<GameStatus> dealTurn() {
        Optional<GameStatus> gameStatus = pokerGameService.dealTurn();
        return gameStatus.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping("/river")
    @Operation(summary = "Deal river", description = "Deals the river card in the poker game")
    public ResponseEntity<?> dealRiver() {
        try {
            Optional<GameStatus> gameStatus = pokerGameService.dealRiver();
            if (gameStatus.isPresent()) {
                return ResponseEntity.ok(gameStatus.get());
            } else {
                return ResponseEntity.badRequest().body("Cannot deal river at this phase.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during dealing the river.");
        }
    }

    @PostMapping("/bet")
    @Operation(summary = "Place a bet", description = "Player places a bet with the specified amount")
    public ResponseEntity<Map<String, String>> playerBet(
            @Parameter(description = "Bet details including player ID and amount") @RequestBody Map<String, Object> payload) {
        String playerId = (String) payload.get("playerId");
        int amount;
        try {
            amount = (int) payload.get("amount");
        } catch (ClassCastException | NullPointerException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Invalid amount format.");
            return ResponseEntity.badRequest().body(response);
        }

        if (amount <= 0) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Amount must be greater than zero.");
            return ResponseEntity.badRequest().body(response);
        }

        boolean betPlaced = pokerGameService.playerBet(playerId, amount);
        Map<String, String> response = new HashMap<>();
        if (betPlaced) {
            response.put("message", "Bet placed successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Bet placement failed. Please check the game state and try again.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/fold")
    @Operation(summary = "Player folds", description = "The specified player folds their hand")
    public ResponseEntity<String> playerFold(
            @Parameter(description = "ID of the player who is folding") @RequestParam String playerId) {
        boolean foldResult = pokerGameService.playerFold(playerId);
        if (foldResult) {
            return ResponseEntity.ok("Player folded successfully.");
        } else {
            return ResponseEntity.badRequest().body("Folding failed or player already folded.");
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new player", description = "Registers a new player for the poker game")
    public ResponseEntity<String> registerPlayer(
            @Parameter(description = "Player information including name, starting chips, and if they are a bot") @RequestBody PlayerInfo playerInfo) {
        if (pokerGameService.registerPlayer(playerInfo.getName(), playerInfo.getStartingChips(), playerInfo.isBot())) {
            return ResponseEntity.ok("Player registered successfully.");
        } else {
            return ResponseEntity.badRequest().body("Player registration failed.");
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Get current game status", description = "Fetches the current status of the ongoing poker game")
    public ResponseEntity<GameStatus> getGameStatus() {
        GameStatus status = pokerGameService.getGameStatus();
        return status != null ? ResponseEntity.ok(status) : ResponseEntity.notFound().build();
    }

    @GetMapping("/end")
    @Operation(summary = "End the game", description = "Ends the current game and announces the winner")
    public ResponseEntity<String> endGame() {
        String winnerId = pokerGameService.endGame();
        return winnerId != null ? ResponseEntity.ok("Game ended. Winner is: " + winnerId)
                : ResponseEntity.badRequest().body("Game end failed or no winner.");
    }

    @PostMapping("/new-match")
    @Operation(summary = "Start a new match", description = "Begins a new match within the poker game")
    public ResponseEntity<GameStatus> startNewMatch() {
        GameStatus gameStatus = pokerGameService.startNewMatch();
        if (gameStatus != null && !gameStatus.getPlayers().isEmpty()) {
            return ResponseEntity.ok(gameStatus);
        } else {
            return ResponseEntity.badRequest().body(new GameStatus());
        }
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset the game", description = "Resets the current game, optionally keeping players")
    public ResponseEntity<String> resetGame(
            @Parameter(description = "Specifies if players should be kept after the reset") @RequestBody Map<String, Boolean> request) {
        boolean keepPlayers = request.getOrDefault("keepPlayers", false);
        boolean resetResult = pokerGameService.resetGame(keepPlayers);
        if (resetResult) {
            return ResponseEntity.ok("Game has been reset successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to reset the game.");
        }
    }

    @PostMapping("/raise")
    @Operation(summary = "Player raises", description = "A player raises the bet amount")
    public ResponseEntity<Map<String, Object>> playerRaise(
            @Parameter(description = "Raise details including player ID and amount") @RequestBody Map<String, Object> payload) {
        String playerId = (String) payload.get("playerId");
        int amount = (int) payload.get("amount");
        boolean success = pokerGameService.playerRaise(playerId, amount);

        Map<String, Object> response = new HashMap<>();
        response.put("message", success ? "Sikeres emelés." : "Sikertelen emelés.");
        return ResponseEntity.ok(response);
    }
}