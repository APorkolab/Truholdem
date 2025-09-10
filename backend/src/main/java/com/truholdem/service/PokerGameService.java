package com.truholdem.service;

import com.truholdem.model.*;
import com.truholdem.repository.GameRepository;
import org.springframework.stereotype.Service;
import com.truholdem.model.GamePhase;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PokerGameService {

    private final GameRepository gameRepository;
    private final HandEvaluator handEvaluator;

    public PokerGameService(GameRepository gameRepository, HandEvaluator handEvaluator) {
        this.gameRepository = gameRepository;
        this.handEvaluator = handEvaluator;
    }

    public Game createNewGame(List<PlayerInfo> playersInfo) {
        if (playersInfo == null || playersInfo.size() < 2 || playersInfo.size() > 4) {
            throw new IllegalArgumentException("Player count must be between 2 and 4.");
        }

        Game game = new Game();
        Deck deck = new Deck();
        deck.shuffle();

        playersInfo.forEach(info -> {
            Player player = new Player(info.getName(), info.getStartingChips(), info.isBot());
            player.addCardToHand(deck.drawCard());
            player.addCardToHand(deck.drawCard());
            game.addPlayer(player);
        });

        game.setDeck(deck.getCards());
        setBlinds(game);
        game.setPhase(GamePhase.PRE_FLOP);

        return gameRepository.save(game);
    }

    public Game playerAct(UUID gameId, UUID playerId, PlayerAction action, int amount) {
        Game game = findGameById(gameId);
        Player player = findPlayerInGame(game, playerId);

        // Validate that it is the player's turn
        if (!game.getPlayers().get(game.getCurrentPlayerIndex()).getId().equals(playerId)) {
            throw new IllegalStateException("It is not this player's turn.");
        }

        switch (action) {
            case FOLD:
                player.setFolded(true);
                break;
            case CHECK:
                if (player.getBetAmount() < game.getCurrentBet()) {
                    throw new IllegalStateException("Cannot check when a bet has been made. Must call or raise.");
                }
                break;
            case CALL:
                int callAmount = game.getCurrentBet() - player.getBetAmount();
                if (player.getChips() < callAmount) {
                    throw new IllegalStateException("Not enough chips to call.");
                }
                player.setChips(player.getChips() - callAmount);
                game.setCurrentPot(game.getCurrentPot() + callAmount);
                player.setBetAmount(game.getCurrentBet());
                break;
            case BET:
            case RAISE:
                if (amount <= game.getCurrentBet()) {
                    throw new IllegalArgumentException("Raise amount must be greater than the current bet.");
                }
                int totalBet = amount - player.getBetAmount();
                if (player.getChips() < totalBet) {
                    throw new IllegalStateException("Not enough chips to raise.");
                }
                player.setChips(player.getChips() - totalBet);
                game.setCurrentPot(game.getCurrentPot() + totalBet);
                player.setBetAmount(amount);
                game.setCurrentBet(amount);
                // When a player raises, all other players need to act again.
                game.getPlayers().forEach(p -> {
                    if (!p.getId().equals(playerId)) {
                        p.setHasActed(false);
                    }
                });
                break;
        }

        player.setHasActed(true);
        advanceGame(game);

        return gameRepository.save(game);
    }

    private void advanceGame(Game game) {
        if (isBettingRoundComplete(game)) {
            advanceToNextPhase(game);
        } else {
            advanceToNextPlayer(game);
        }
    }

    private boolean isBettingRoundComplete(Game game) {
        List<Player> activePlayers = game.getPlayers().stream()
                .filter(p -> !p.isFolded()).toList();

        if (activePlayers.size() <= 1) {
            return true;
        }

        boolean allHaveActed = activePlayers.stream().allMatch(Player::hasActed);
        if (!allHaveActed) {
            return false;
        }

        long distinctBets = activePlayers.stream()
                .map(Player::getBetAmount)
                .distinct()
                .count();

        return distinctBets == 1;
    }

    private void advanceToNextPhase(Game game) {
        game.getPlayers().forEach(p -> {
            p.setBetAmount(0);
            p.setHasActed(false);
        });
        game.setCurrentBet(0);
        game.setCurrentPlayerIndex(0); // Start from the first player for the new round

        switch (game.getPhase()) {
            case PRE_FLOP:
                game.setPhase(GamePhase.FLOP);
                dealFlop(game);
                break;
            case FLOP:
                game.setPhase(GamePhase.TURN);
                dealTurn(game);
                break;
            case TURN:
                game.setPhase(GamePhase.RIVER);
                dealRiver(game);
                break;
            case RIVER:
                game.setPhase(GamePhase.SHOWDOWN);
                // TODO: Implement showdown logic
                break;
            default:
                break;
        }
    }

    private void advanceToNextPlayer(Game game) {
        int currentIndex = game.getCurrentPlayerIndex();
        int nextIndex = (currentIndex + 1) % game.getPlayers().size();

        while (game.getPlayers().get(nextIndex).isFolded()) {
            nextIndex = (nextIndex + 1) % game.getPlayers().size();
        }
        game.setCurrentPlayerIndex(nextIndex);
    }

    private void dealFlop(Game game) {
        drawCardFromDeck(game); // Burn
        for (int i = 0; i < 3; i++) {
            game.addCommunityCard(drawCardFromDeck(game));
        }
    }

    private void dealTurn(Game game) {
        drawCardFromDeck(game); // Burn
        game.addCommunityCard(drawCardFromDeck(game));
    }

    private void dealRiver(Game game) {
        drawCardFromDeck(game); // Burn
        game.addCommunityCard(drawCardFromDeck(game));
    }

    private void setBlinds(Game game) {
        List<Player> players = game.getPlayers();
        if (players.size() < 2) return;

        Player smallBlindPlayer = players.get(0);
        placeBlindBet(game, smallBlindPlayer, game.getSmallBlind());

        Player bigBlindPlayer = players.get(1);
        placeBlindBet(game, bigBlindPlayer, game.getBigBlind());

        game.setCurrentBet(game.getBigBlind());
        game.setCurrentPlayerIndex(players.size() > 2 ? 2 : 0);
    }

    private void placeBlindBet(Game game, Player player, int amount) {
        int bet = Math.min(player.getChips(), amount);
        player.setChips(player.getChips() - bet);
        player.setBetAmount(bet);
        game.setCurrentPot(game.getCurrentPot() + bet);
    }

    private Card drawCardFromDeck(Game game) {
        if (game.getDeck().isEmpty()) {
            throw new IllegalStateException("Cannot draw card from an empty deck.");
        }
        return game.getDeck().remove(0);
    }

    public Optional<Game> getGame(UUID gameId) {
        return gameRepository.findById(gameId);
    }

    private Game findGameById(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new NoSuchElementException("Game not found with ID: " + gameId));
    }

    private Player findPlayerInGame(Game game, UUID playerId) {
        return game.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Player not found with ID: " + playerId + " in game " + game.getId()));
    }
}
