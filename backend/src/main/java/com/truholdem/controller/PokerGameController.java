package com.truholdem.controller;

import com.truholdem.model.GameStatus;
import com.truholdem.model.PlayerInfo;
import com.truholdem.service.PokerGameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game started successfully", content = @Content(schema = @Schema(implementation = GameStatus.class), examples = @ExampleObject(value = "{ \"players\": [...], \"communityCards\": [...], \"currentPot\": 1000 }"))),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<GameStatus> startGame(
            @RequestBody @Parameter(description = "List of players to start the game with", schema = @Schema(implementation = PlayerInfo.class)) List<PlayerInfo> playersInfo) {
        GameStatus gameStatus = pokerGameService.startGame(playersInfo);
        if (gameStatus != null) {
            return ResponseEntity.ok(gameStatus);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/flop")
    @Operation(summary = "Deal flop", description = "Deals the flop cards in the poker game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flop dealt successfully", content = @Content(schema = @Schema(implementation = GameStatus.class), examples = @ExampleObject(value = "{ \"communityCards\": [\"2 of Hearts\", \"5 of Spades\", \"Ace of Diamonds\"] }"))),
            @ApiResponse(responseCode = "400", description = "Invalid game state")
    })
    public ResponseEntity<GameStatus> dealFlop() {
        Optional<GameStatus> gameStatus = pokerGameService.dealFlop();
        return gameStatus.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping("/turn")
    @Operation(summary = "Deal turn", description = "Deals the turn card in the poker game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Turn dealt successfully", content = @Content(schema = @Schema(implementation = GameStatus.class), examples = @ExampleObject(value = "{ \"communityCards\": [\"2 of Hearts\", \"5 of Spades\", \"Ace of Diamonds\", \"Jack of Clubs\"] }"))),
            @ApiResponse(responseCode = "400", description = "Invalid game state")
    })
    public ResponseEntity<GameStatus> dealTurn() {
        Optional<GameStatus> gameStatus = pokerGameService.dealTurn();
        return gameStatus.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping("/river")
    @Operation(summary = "Deal river", description = "Deals the river card in the poker game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "River dealt successfully", content = @Content(schema = @Schema(implementation = GameStatus.class), examples = @ExampleObject(value = "{ \"communityCards\": [\"2 of Hearts\", \"5 of Spades\", \"Ace of Diamonds\", \"Jack of Clubs\", \"Queen of Spades\"] }"))),
            @ApiResponse(responseCode = "400", description = "Invalid game state"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @Operation(summary = "Place a bet", description = "Allows a player to place a bet in the current game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bet placed successfully", content = @Content(schema = @Schema(implementation = Map.class), examples = @ExampleObject(value = "{ \"message\": \"Bet placed successfully.\" }"))),
            @ApiResponse(responseCode = "400", description = "Invalid bet or game state", content = @Content(schema = @Schema(implementation = Map.class), examples = @ExampleObject(value = "{ \"error\": \"Bet placement failed. Check the game state.\" }")))
    })
    public ResponseEntity<Map<String, String>> playerBet(
            @RequestBody @Parameter(description = "Bet details including player ID and amount", example = "{ \"playerId\": \"12345\", \"amount\": 100 }") Map<String, Object> payload) {
        String playerId = (String) payload.get("playerId");
        int amount;
        try {
            amount = (int) payload.get("amount");
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Invalid amount format.");
            return ResponseEntity.badRequest().body(response);
        }

        boolean betPlaced = pokerGameService.playerBet(playerId, amount);
        Map<String, String> response = new HashMap<>();
        if (betPlaced) {
            response.put("message", "Bet placed successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Bet placement failed. Check the game state.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/fold")
    @Operation(summary = "Fold hand", description = "Allows a player to fold their hand in the current game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Player folded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid player ID or game state")
    })
    public ResponseEntity<String> playerFold(
            @RequestParam @Parameter(description = "ID of the player folding") String playerId) {
        boolean success = pokerGameService.playerFold(playerId);
        return success ? ResponseEntity.ok("Player folded successfully.")
                : ResponseEntity.badRequest().body("Folding failed.");
    }

    @PostMapping("/register")
    @Operation(summary = "Register players", description = "Registers players for the poker game. If no players are provided, default players will be used.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Players registered successfully", content = @Content(schema = @Schema(implementation = GameStatus.class), examples = @ExampleObject(value = "{ \"players\": [...], \"communityCards\": [] }"))),
            @ApiResponse(responseCode = "400", description = "Invalid player information")
    })
    public ResponseEntity<GameStatus> registerPlayers(
            @RequestBody(required = false) @Parameter(description = "List of players to register", schema = @Schema(implementation = PlayerInfo.class)) List<PlayerInfo> playerInfos) {
        if (playerInfos == null || playerInfos.isEmpty()) {
            playerInfos = pokerGameService.getDefaultPlayers();
        }

        GameStatus gameStatus = pokerGameService.registerPlayers(playerInfos);

        if (gameStatus != null) {
            return ResponseEntity.ok(gameStatus);
        } else {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Get current game status", description = "Fetches the current status of the ongoing poker game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game status retrieved successfully", content = @Content(schema = @Schema(implementation = GameStatus.class), examples = @ExampleObject(value = "{ \"players\": [...], \"communityCards\": [...], \"currentPot\": 1000 }"))),
            @ApiResponse(responseCode = "404", description = "No active game found")
    })
    public ResponseEntity<GameStatus> getGameStatus() {
        GameStatus status = pokerGameService.getGameStatus();
        return status != null ? ResponseEntity.ok(status) : ResponseEntity.notFound().build();
    }

    @GetMapping("/end")
    @Operation(summary = "End the game", description = "Ends the current game and announces the winner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game ended successfully", content = @Content(schema = @Schema(implementation = String.class), examples = @ExampleObject(value = "Game ended. Winner is: Player123"))),
            @ApiResponse(responseCode = "400", description = "Failed to end game or no winner")
    })
    public ResponseEntity<String> endGame() {
        String winnerId = pokerGameService.endGame();
        return winnerId != null ? ResponseEntity.ok("Game ended. Winner is: " + winnerId)
                : ResponseEntity.badRequest().body("Game end failed or no winner.");
    }

    @PostMapping("/new-match")
    @Operation(summary = "Start a new match", description = "Begins a new match within the poker game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New match started successfully", content = @Content(schema = @Schema(implementation = GameStatus.class), examples = @ExampleObject(value = "{ \"players\": [...], \"communityCards\": [] }"))),
            @ApiResponse(responseCode = "400", description = "Failed to start new match")
    })
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game reset successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to reset the game")
    })
    public ResponseEntity<String> resetGame(
            @RequestBody @Parameter(description = "Reset options") Map<String, Boolean> request) {
        boolean keepPlayers = request.getOrDefault("keepPlayers", false);
        boolean resetResult = pokerGameService.resetGame(keepPlayers);
        if (resetResult) {
            return ResponseEntity.ok("Game has been reset successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to reset the game.");
        }
    }

    @PostMapping("/raise")
    @Operation(summary = "Raise bet", description = "Allows a player to raise the current bet in the game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Raise successful", content = @Content(schema = @Schema(implementation = Map.class), examples = @ExampleObject(value = "{ \"message\": \"Raise successful.\" }"))),
            @ApiResponse(responseCode = "400", description = "Invalid raise or game state")
    })
    public ResponseEntity<Map<String, Object>> playerRaise(
            @RequestBody @Parameter(description = "Raise details including player ID and amount", example = "{ \"playerId\": \"12345\", \"amount\": 200 }") Map<String, Object> payload) {
        String playerId = (String) payload.get("playerId");
        int amount = (int) payload.get("amount");

        boolean success = pokerGameService.playerRaise(playerId, amount);
        Map<String, Object> response = new HashMap<>();
        response.put("message", success ? "Raise successful." : "Raise failed.");
        return ResponseEntity.ok(response);
    }
}
