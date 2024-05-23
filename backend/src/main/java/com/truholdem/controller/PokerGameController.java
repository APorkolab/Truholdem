package com.truholdem.controller;

import com.truholdem.model.GameStatus;
import com.truholdem.model.PlayerInfo;
import com.truholdem.service.PokerGameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/poker")
public class PokerGameController {

    private final PokerGameService pokerGameService;

    public PokerGameController(PokerGameService pokerGameService) {
        this.pokerGameService = pokerGameService;
    }

    @PostMapping("/start")
    public ResponseEntity<GameStatus> startGame(@RequestBody List<PlayerInfo> playersInfo) {
        GameStatus gameStatus = pokerGameService.startGame(playersInfo);
        if (gameStatus != null) {
            return ResponseEntity.ok(gameStatus);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/flop")
    public ResponseEntity<GameStatus> dealFlop() {
        Optional<GameStatus> gameStatus = pokerGameService.dealFlop();
        return gameStatus.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping("/turn")
    public ResponseEntity<GameStatus> dealTurn() {
        Optional<GameStatus> gameStatus = pokerGameService.dealTurn();
        return gameStatus.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping("/river")
    public ResponseEntity<GameStatus> dealRiver() {
        Optional<GameStatus> gameStatus = pokerGameService.dealRiver();
        return gameStatus.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/bet")
    public ResponseEntity<Map<String, String>> playerBet(@RequestParam String playerId, @RequestParam int amount) {
        boolean betPlaced = pokerGameService.playerBet(playerId, amount);
        Map<String, String> response = new HashMap<>();
        if (betPlaced) {
            response.put("message", "Bet placed successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Bet placement failed.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/fold")
    public ResponseEntity<String> playerFold(@RequestParam String playerId) {
        boolean foldResult = pokerGameService.playerFold(playerId);
        if (foldResult) {
            return ResponseEntity.ok("Player folded successfully.");
        } else {
            return ResponseEntity.badRequest().body("Folding failed or player already folded.");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerPlayer(@RequestBody PlayerInfo playerInfo) {
        if (pokerGameService.registerPlayer(playerInfo.getName(), playerInfo.getStartingChips(), playerInfo.isBot())) {
            return ResponseEntity.ok("Player registered successfully.");
        } else {
            return ResponseEntity.badRequest().body("Player registration failed.");
        }
    }

    @GetMapping("/status")
    public ResponseEntity<GameStatus> getGameStatus() {
        GameStatus status = pokerGameService.getGameStatus();
        return status != null ? ResponseEntity.ok(status) : ResponseEntity.notFound().build();
    }

    @GetMapping("/end")
    public ResponseEntity<String> endGame() {
        String winnerId = pokerGameService.endGame();
        return winnerId != null ? ResponseEntity.ok("Game ended. Winner is: " + winnerId)
                : ResponseEntity.badRequest().body("Game end failed or no winner.");
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetGame() {
        pokerGameService.resetGame(false);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/new-match")
    public ResponseEntity<GameStatus> startNewMatch() {
        GameStatus gameStatus = pokerGameService.startNewMatch();
        if (gameStatus != null && !gameStatus.getPlayers().isEmpty()) {
            return ResponseEntity.ok(gameStatus);
        } else {
            return ResponseEntity.badRequest().body(new GameStatus());
        }
    }



}
