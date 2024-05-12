package com.truholdem.service;

import com.truholdem.model.Card;

import java.util.ArrayList;
import java.util.List;

public class HandResult {
    private final int handStrength;
    private final List<Card> cards;

    public HandResult(int handStrength, List<Card> cards) {
        this.handStrength = handStrength;
        this.cards = new ArrayList<>(cards);
    }

    // Getter a kéz erősségéhez
    public int getHandStrength() {
        return handStrength;
    }

    // Getter a képező lapokhoz
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    // Egy egyszerűsített toString metódus, ami leírja a kéz erősségét és a lapokat
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Hand Strength: ").append(handStrengthToString(handStrength)).append("\n");
        sb.append("Cards: ");
        for (Card card : cards) {
            sb.append(card.toString()).append(" ");
        }
        return sb.toString().trim();
    }

    // Segédmetódus a kéz erősségének szöveges reprezentációjához
    private String handStrengthToString(int strength) {
        switch (strength) {
            case 100: return "High Card";
            case 200: return "One Pair";
            case 300: return "Two Pair";
            case 400: return "Three of a Kind";
            case 500: return "Straight";
            case 600: return "Flush";
            case 700: return "Full House";
            case 800: return "Four of a Kind";
            case 900: return "Straight Flush";
            case 1000: return "Royal Flush";
            default: return "Unknown";
        }
    }
}