package com.truholdem.model;

public class Card {
    private Suit suit;
    private Value value;

    public Card(Suit suit, Value value) {
        this.suit = suit;
        this.value = value;
    }

    // Getter a színhez
    public Suit getSuit() {
        return suit;
    }

    // Setter a színhez
    public void setSuit(Suit suit) {
        this.suit = suit;
    }

    // Getter az értékhez
    public Value getValue() {
        return value;
    }

    // Setter az értékhez
    public void setValue(Value value) {
        this.value = value;
    }

    // toString() metódus a kártya szöveges reprezentációjához
    @Override
    public String toString() {
        return value + " of " + suit;
    }
}

