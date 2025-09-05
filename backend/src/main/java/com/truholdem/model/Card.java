package com.truholdem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class Card {
    @Enumerated(EnumType.STRING)
    private Suit suit;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_value")
    private Value value;

    public Card(Suit suit, Value value) {
        this.suit = suit;
        this.value = value;
    }

    // JPA requires a no-arg constructor
    public Card() {
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

