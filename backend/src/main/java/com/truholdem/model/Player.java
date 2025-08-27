package com.truholdem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "player_hand", joinColumns = @JoinColumn(name = "player_id"))
    @OrderColumn
    private List<Card> hand = new ArrayList<>();

    private int chips;
    private int betAmount;
    private boolean isFolded;
    private boolean isBot;
    private boolean hasActed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    @JsonBackReference
    private Game game;

    public Player(String name, int startingChips, boolean isBot) {
        this.name = name;
        this.chips = startingChips;
        this.isFolded = false;
        this.isBot = isBot;
    }

    public Player() {
        // JPA requires a no-arg constructor
    }

    public void placeBet(int amount) {
        if (amount <= 0 || amount > chips) {
            throw new IllegalArgumentException("Invalid bet amount");
        }
        betAmount += amount;
        chips -= amount;
    }

    public void addWinnings(int amount) {
        this.chips += amount;
    }

    public void fold() {
        isFolded = true;
    }

    public void addCardToHand(Card card) {
        hand.add(card);
    }

    public UUID getId() {
        return id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public String getName() {
        return name;
    }

    public boolean isFolded() {
        return isFolded;
    }

    public int getBetAmount() {
        return betAmount;
    }

    public List<Card> getHand() {
        return hand;
    }

    public int getChips() {
        return chips;
    }

    public void setFolded(boolean folded) {
        isFolded = folded;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }

    public void setBetAmount(int betAmount) {
        this.betAmount = betAmount;
    }

    public void setChips(int chips) {
        this.chips = chips;
    }

    public boolean isBot() {
        return isBot;
    }

    public void clearHand() {
        this.hand.clear();
    }

    public boolean hasActed() {
        return hasActed;
    }

    public void setHasActed(boolean hasActed) {
        this.hasActed = hasActed;
    }
}