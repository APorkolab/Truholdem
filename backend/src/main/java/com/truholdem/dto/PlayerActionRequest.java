package com.truholdem.dto;

import com.truholdem.model.PlayerAction;

public class PlayerActionRequest {

    private String playerId;
    private PlayerAction action;
    private int amount;

    public PlayerActionRequest() {
    }

    public PlayerActionRequest(String playerId, PlayerAction action, int amount) {
        this.playerId = playerId;
        this.action = action;
        this.amount = amount;
    }

    // Getters and Setters
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public PlayerAction getAction() {
        return action;
    }

    public void setAction(PlayerAction action) {
        this.action = action;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
