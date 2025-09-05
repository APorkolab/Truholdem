package com.truholdem.dto;

import com.truholdem.model.PlayerAction;

public class PlayerActionRequest {

    private PlayerAction action;
    private int amount;

    public PlayerActionRequest() {
    }

    public PlayerActionRequest(PlayerAction action, int amount) {
        this.action = action;
        this.amount = amount;
    }

    // Getters and Setters
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
