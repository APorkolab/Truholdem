package com.truholdem.service;

import com.truholdem.model.Value;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class HandRanking implements Comparable<HandRanking> {

    public enum HandType {
        HIGH_CARD,
        ONE_PAIR,
        TWO_PAIR,
        THREE_OF_A_KIND,
        STRAIGHT,
        FLUSH,
        FULL_HOUSE,
        FOUR_OF_A_KIND,
        STRAIGHT_FLUSH,
        ROYAL_FLUSH
    }

    private final HandType handType;
    private final List<Value> rankValues; // e.g., for Two Pair (A, A, 5, 5, K), this would be [ACE, FIVE]
    private final List<Value> kickerValues; // e.g., for the above, this would be [KING]

    public HandRanking(HandType handType, List<Value> rankValues, List<Value> kickerValues) {
        this.handType = handType;
        // Sort values descending for consistent comparison
        this.rankValues = rankValues.stream().sorted(Comparator.reverseOrder()).toList();
        this.kickerValues = kickerValues.stream().sorted(Comparator.reverseOrder()).toList();
    }

    public HandType getHandType() {
        return handType;
    }

    public List<Value> getRankValues() {
        return rankValues;
    }

    public List<Value> getKickerValues() {
        return kickerValues;
    }

    @Override
    public int compareTo(HandRanking other) {
        // First, compare by the hand type enum's ordinal. Higher is better.
        int typeComparison = Integer.compare(this.handType.ordinal(), other.handType.ordinal());
        if (typeComparison != 0) {
            return typeComparison;
        }

        // If hand types are the same, compare by the primary rank values.
        for (int i = 0; i < this.rankValues.size(); i++) {
            int rankValueComparison = Integer.compare(
                this.rankValues.get(i).ordinal(),
                other.rankValues.get(i).ordinal()
            );
            if (rankValueComparison != 0) {
                return rankValueComparison;
            }
        }

        // If rank values are also the same, compare by kickers.
        for (int i = 0; i < this.kickerValues.size(); i++) {
            int kickerComparison = Integer.compare(
                this.kickerValues.get(i).ordinal(),
                other.kickerValues.get(i).ordinal()
            );
            if (kickerComparison != 0) {
                return kickerComparison;
            }
        }

        // If all are equal, it's a tie.
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HandRanking that = (HandRanking) o;
        return handType == that.handType &&
               Objects.equals(rankValues, that.rankValues) &&
               Objects.equals(kickerValues, that.kickerValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(handType, rankValues, kickerValues);
    }

    @Override
    public String toString() {
        return "HandRanking{" +
                "handType=" + handType +
                ", rankValues=" + rankValues +
                ", kickerValues=" + kickerValues +
                '}';
    }
}
