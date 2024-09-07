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
    public ResponseEntity<Map<String, Object>> dealRiver() {
        try {
            Optional<GameStatus> gameStatus = pokerGameService.dealRiver();
            Map<String, Object> response = new HashMap<>();
            if (gameStatus.isPresent()) {
                response.put("status", "success");
                response.put("gameStatus", gameStatus.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Cannot deal river at this phase.");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error during dealing the river.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
    public ResponseEntity<Map<String, String>> playerFold(
            @RequestParam @Parameter(description = "ID of the player folding") String playerId) {
        boolean success = pokerGameService.playerFold(playerId);
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("message", "Player folded successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Folding failed.");
            return ResponseEntity.badRequest().body(response);
        }
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
    public ResponseEntity<Map<String, String>> endGame() {
        String winnerId = pokerGameService.endGame();
        Map<String, String> response = new HashMap<>();
        if (winnerId != null) {
            response.put("message", "Game ended. Winner is: " + winnerId);
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Game end failed or no winner.");
            return ResponseEntity.badRequest().body(response);
        }
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
    public ResponseEntity<Map<String, String>> resetGame(
            @RequestBody @Parameter(description = "Reset options") Map<String, Boolean> request) {
        boolean keepPlayers = request.getOrDefault("keepPlayers", false);
        boolean resetResult = pokerGameService.resetGame(keepPlayers);
        Map<String, String> response = new HashMap<>();
        if (resetResult) {
            response.put("message", "Game has been reset successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Failed to reset the game.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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

    @PostMapping("/check")
    @Operation(summary = "Check", description = "Allows a player to check if no bet has been placed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Player checked successfully"),
            @ApiResponse(responseCode = "400", description = "Check failed due to invalid game state or bet")
    })
    public ResponseEntity<Map<String, String>> playerCheck(
            @RequestParam @Parameter(description = "ID of the player checking") String playerId) {
        boolean success = pokerGameService.playerCheck(playerId);
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("message", "Player checked successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Check failed. Check the current bet or game state.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/change-name")
    @Operation(summary = "Change player name", description = "Change a player name based on ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Name successfully changed"),
            @ApiResponse(responseCode = "400", description = "Invalid player ID or name")
    })
    public ResponseEntity<Map<String, String>> changePlayerName(
            @RequestBody @Parameter(description = "Player ID and new name", example = "{ \"playerId\": \"12345\", \"newName\": \"NewName\" }") Map<String, String> payload) {
        String playerId = payload.get("playerId");
        String newName = payload.get("newName");

        boolean success = pokerGameService.changePlayerName(playerId, newName);
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("message", "Játékos neve sikeresen megváltoztatva.");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Nem sikerült megváltoztatni a játékos nevét.");
            return ResponseEntity.badRequest().body(response);
        }
    }

}