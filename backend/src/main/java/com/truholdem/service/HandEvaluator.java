package com.truholdem.service;

import com.truholdem.model.Card;
import com.truholdem.model.Suit;
import com.truholdem.model.Value;

import java.util.*;
import java.util.stream.Collectors;


public class HandEvaluator {
    private static final int HIGH_CARD = 100;
    private static final int ONE_PAIR = 200;
    private static final int TWO_PAIR = 300;
    private static final int THREE_OF_A_KIND = 400;
    private static final int STRAIGHT = 500;
    private static final int FLUSH = 600;
    private static final int FULL_HOUSE = 700;
    private static final int FOUR_OF_A_KIND = 800;
    private static final int STRAIGHT_FLUSH = 900;
    private static final int ROYAL_FLUSH = 1000;

    public HandResult evaluate(List<Card> playerHand, List<Card> communityCards) {
        List<Card> allCards = new ArrayList<>(playerHand);

        allCards.addAll(communityCards);
        allCards.sort(Comparator.comparingInt(c -> c.getValue().ordinal()));

        boolean flush = isFlush(allCards);
        boolean straight = isStraight(allCards);
        int[] counts = cardCounts(allCards);

        if (flush && straight && allCards.get(allCards.size() - 1).getValue() == Value.ACE) {
            return new HandResult(ROYAL_FLUSH, allCards);
        }
        if (flush && straight) {
            return new HandResult(STRAIGHT_FLUSH, allCards);
        }
        int four = countKind(counts, 4);
        if (four > 0) {
            return new HandResult(FOUR_OF_A_KIND, allCards);
        }
        if (countKind(counts, 3) > 0 && countKind(counts, 2) > 0) {
            return new HandResult(FULL_HOUSE, allCards);
        }
        if (flush) {
            return new HandResult(FLUSH, allCards);
        }
        if (straight) {
            return new HandResult(STRAIGHT, allCards);
        }
        int three = countKind(counts, 3);
        if (three > 0) {
            return new HandResult(THREE_OF_A_KIND, allCards);
        }
        int pairs = countPairs(counts);
        if (pairs == 2) {
            return new HandResult(TWO_PAIR, allCards);
        }
        if (pairs == 1) {
            return new HandResult(ONE_PAIR, allCards);
        }
        return new HandResult(HIGH_CARD, allCards);
    }

    private boolean isFlush(List<Card> cards) {
        Map<Suit, Long> suitCount = cards.stream().collect(Collectors.groupingBy(Card::getSuit, Collectors.counting()));
        return suitCount.values().stream().anyMatch(count -> count >= 5);
    }

    private boolean isStraight(List<Card> cards) {
        Set<Integer> uniqueValues = cards.stream().map(card -> card.getValue().ordinal()).collect(Collectors.toSet());
        List<Integer> sortedValues = new ArrayList<>(uniqueValues);
        Collections.sort(sortedValues);
        int consecutiveCount = 1;
        for (int i = 1; i < sortedValues.size(); i++) {
            if (sortedValues.get(i) - sortedValues.get(i - 1) == 1) {
                consecutiveCount++;
                if (consecutiveCount >= 5) {
                    return true;
                }
            } else {
                consecutiveCount = 1;
            }
        }
        // Special case for Ace to Five straight
        if (uniqueValues.contains(Value.ACE.ordinal()) && uniqueValues.contains(Value.TWO.ordinal()) &&
                uniqueValues.contains(Value.THREE.ordinal()) && uniqueValues.contains(Value.FOUR.ordinal()) &&
                uniqueValues.contains(Value.FIVE.ordinal())) {
            return true;
        }
        return false;
    }

    private int[] cardCounts(List<Card> cards) {
        int[] counts = new int[Value.values().length];
        for (Card card : cards) {
            counts[card.getValue().ordinal()]++;
        }
        return counts;
    }

    private int countKind(int[] counts, int kind) {
        for (int count : counts) {
            if (count == kind) {
                return 1;
            }
        }
        return 0;
    }

    private int countPairs(int[] counts) {
        int pairs = 0;
        for (int count : counts) {
            if (count == 2) {
                pairs++;
            }
        }
        return pairs;
    }
}