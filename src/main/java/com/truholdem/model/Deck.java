package com.truholdem.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Deck {
    private final List<Card> cards = new LinkedList<>();

    public Deck() {
        resetDeck();
    }

    // A pakli újrainicializálása és keverése
    public void resetDeck() {
        cards.clear();
        for (Suit suit : Suit.values()) {
            for (Value value : Value.values()) {
                cards.add(new Card(suit, value));
            }
        }
        Collections.shuffle(cards);
    }

    // Egy kártya húzása a pakliból
    public Card drawCard() {
        if (!cards.isEmpty()) {
            return cards.remove(0);
        }
        return null;
    }

    // A pakliban maradt kártyák számának lekérdezése
    public int cardsLeft() {
        return cards.size();
    }

    // A pakli tartalmának kiíratása (fejlesztési célokra)
    public void printDeck() {
        cards.forEach(card -> System.out.println(card.toString()));
    }

    // A pakli keverése anélkül, hogy újrainicializálnánk
    public void shuffle() {
        Collections.shuffle(cards);
    }
}
