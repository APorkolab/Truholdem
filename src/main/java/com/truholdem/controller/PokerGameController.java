package com.truholdem.controller;

import com.truholdem.model.GameStatus;
import com.truholdem.model.Player;
import com.truholdem.service.PokerGameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/poker")
public class PokerGameController {

    private final PokerGameService pokerGameService;

    public PokerGameController(PokerGameService pokerGameService) {
        this.pokerGameService = pokerGameService;
    }

    @GetMapping("/start")
    public ResponseEntity<GameStatus> startGame() {
        Optional<GameStatus> gameStatus = Optional.ofNullable(pokerGameService.startGame());
        return gameStatus.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
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
    public ResponseEntity<String> playerBet(@RequestParam String playerId, @RequestParam int amount) {
        if (pokerGameService.playerBet(playerId, amount)) {
            return ResponseEntity.ok("Bet placed successfully.");
        } else {
            return ResponseEntity.badRequest().body("Bet placement failed.");
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
    public ResponseEntity<String> registerPlayer(@RequestBody String playerId) {
        if (pokerGameService.registerPlayer(playerId)) {
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
}
