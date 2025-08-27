package com.truholdem.controller;

import com.truholdem.dto.PlayerActionRequest;
import com.truholdem.model.Game;
import com.truholdem.model.PlayerInfo;
import com.truholdem.service.PokerGameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/poker/game")
@Tag(name = "Poker Game API", description = "Operations for managing a poker game")
public class PokerGameController {

    private final PokerGameService pokerGameService;

    public PokerGameController(PokerGameService pokerGameService) {
        this.pokerGameService = pokerGameService;
    }

    @PostMapping("/start")
    @Operation(summary = "Start a new game", description = "Initializes a new poker game with the provided players and returns the new game state")
    public ResponseEntity<Game> startGame(@RequestBody List<PlayerInfo> playersInfo) {
        Game newGame = pokerGameService.createNewGame(playersInfo);
        return ResponseEntity.ok(newGame);
    }

    @GetMapping("/{gameId}")
    @Operation(summary = "Get game status", description = "Fetches the current status of the specified poker game")
    public ResponseEntity<Game> getGameStatus(@PathVariable UUID gameId) {
        Optional<Game> game = pokerGameService.getGame(gameId);
        return game.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{gameId}/player/{playerId}/action")
    @Operation(summary = "Player action", description = "Allows a player to perform an action (fold, check, call, bet, raise)")
    public ResponseEntity<Game> playerAction(
            @PathVariable UUID gameId,
            @PathVariable UUID playerId,
            @RequestBody PlayerActionRequest request) {
        Game updatedGame = pokerGameService.playerAct(gameId, playerId, request.getAction(), request.getAmount());
        return ResponseEntity.ok(updatedGame);
    }
}