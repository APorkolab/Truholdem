package com.truholdem.controller;

import com.truholdem.model.GameStatus;
import com.truholdem.service.PokerGameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/poker")
public class PokerGameController {

    @Autowired
    private PokerGameService pokerGameService;

    @GetMapping("/start")
    public ResponseEntity<GameStatus> startGame() {
        GameStatus status = pokerGameService.startGame();
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    // A flop lapjainak kiosztása
    @GetMapping("/flop")
    public ResponseEntity<GameStatus> dealFlop() {
        GameStatus status = pokerGameService.dealFlop();
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    // A turn lapjának kiosztása
    @GetMapping("/turn")
    public ResponseEntity<GameStatus> dealTurn() {
        GameStatus status = pokerGameService.dealTurn();
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    // A river lapjának kiosztása
    @GetMapping("/river")
    public ResponseEntity<GameStatus> dealRiver() {
        GameStatus status = pokerGameService.dealRiver();
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    // Játékos döntése: emelés
    @PostMapping("/bet")
    public ResponseEntity<String> playerBet(@RequestParam String playerId, @RequestParam int amount) {
        boolean result = pokerGameService.playerBet(playerId, amount);
        if (result) {
            return ResponseEntity.ok("Bet placed successfully.");
        } else {
            return ResponseEntity.badRequest().body("Bet placement failed.");
        }
    }

    // Játékos döntése: passzolás
    @PostMapping("/fold")
    public ResponseEntity<String> playerFold(@RequestParam String playerId) {
        boolean result = pokerGameService.playerFold(playerId);
        if (result) {
            return ResponseEntity.ok("Player folded successfully.");
        } else {
            return ResponseEntity.badRequest().body("Folding failed.");
        }
    }

    // Játékos regisztrációja a játékba
    @PostMapping("/register")
    public ResponseEntity<String> registerPlayer(@RequestBody String playerId) {
        boolean success = pokerGameService.registerPlayer(playerId);
        if (success) {
            return ResponseEntity.ok("Player registered successfully.");
        } else {
            return ResponseEntity.badRequest().body("Player registration failed.");
        }
    }

    // A játék aktuális állapotának lekérdezése
    @GetMapping("/status")
    public ResponseEntity<GameStatus> getGameStatus() {
        GameStatus status = pokerGameService.getGameStatus();
        if (status != null) {
            return new ResponseEntity<>(status, HttpStatus.OK);
        } else {
            // Ha nincs játék állapot, térjünk vissza 404-el vagy üres testtel rendelkező 200-as válasszal
            return ResponseEntity.notFound().build(); // vagy ResponseEntity.ok().body(new GameStatus()); az üres állapot jelzésére
        }
    }

    // A játék befejezése és a győztes kihirdetése
    @GetMapping("/end")
    public ResponseEntity<String> endGame() {
        String winnerId = pokerGameService.endGame();
        if (winnerId != null) {
            return ResponseEntity.ok("Game ended. Winner is: " + winnerId);
        } else {
            return ResponseEntity.badRequest().body("Game end failed or no winner.");
        }
    }
}
