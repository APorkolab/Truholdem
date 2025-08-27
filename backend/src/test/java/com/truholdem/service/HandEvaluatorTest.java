package com.truholdem.service;

import com.truholdem.model.Card;
import com.truholdem.model.Suit;
import com.truholdem.model.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandEvaluatorTest {

    private HandEvaluator handEvaluator;

    @BeforeEach
    void setUp() {
        handEvaluator = new HandEvaluator();
    }

    private Card card(String str) {
        if (str == null || str.length() != 2) {
            throw new IllegalArgumentException("Invalid card string: " + str);
        }
        Value value = switch (str.charAt(0)) {
            case 'T' -> Value.TEN;
            case 'J' -> Value.JACK;
            case 'Q' -> Value.QUEEN;
            case 'K' -> Value.KING;
            case 'A' -> Value.ACE;
            default -> Value.values()[Character.getNumericValue(str.charAt(0)) - 2];
        };
        Suit suit = switch (str.charAt(1)) {
            case 'h' -> Suit.HEARTS;
            case 'd' -> Suit.DIAMONDS;
            case 'c' -> Suit.CLUBS;
            case 's' -> Suit.SPADES;
            default -> throw new IllegalArgumentException("Invalid suit: " + str.charAt(1));
        };
        return new Card(suit, value);
    }

    private List<Card> cards(String... strs) {
        return Stream.of(strs).map(this::card).collect(Collectors.toList());
    }

    @Test
    void testHighCard() {
        List<Card> playerHand = cards("Ah", "2d");
        List<Card> communityCards = cards("Jc", "9s", "7h", "4c", "3d");
        HandRanking result = handEvaluator.evaluate(playerHand, communityCards);
        assertEquals(HandRanking.HandType.HIGH_CARD, result.getHandType());
        assertEquals(List.of(Value.ACE, Value.JACK, Value.NINE, Value.SEVEN, Value.FOUR), result.getKickerValues());
    }

    @Test
    void testOnePair() {
        List<Card> playerHand = cards("Ah", "Ad");
        List<Card> communityCards = cards("Jc", "9s", "7h", "4c", "3d");
        HandRanking result = handEvaluator.evaluate(playerHand, communityCards);
        assertEquals(HandRanking.HandType.ONE_PAIR, result.getHandType());
        assertEquals(List.of(Value.ACE), result.getRankValues());
        assertEquals(List.of(Value.JACK, Value.NINE, Value.SEVEN), result.getKickerValues());
    }

    @Test
    void testTwoPair() {
        List<Card> playerHand = cards("Ah", "Ad");
        List<Card> communityCards = cards("Jc", "Js", "7h", "4c", "3d");
        HandRanking result = handEvaluator.evaluate(playerHand, communityCards);
        assertEquals(HandRanking.HandType.TWO_PAIR, result.getHandType());
        assertEquals(List.of(Value.ACE, Value.JACK), result.getRankValues());
        assertEquals(List.of(Value.SEVEN), result.getKickerValues());
    }

    @Test
    void testThreeOfAKind() {
        List<Card> playerHand = cards("Ah", "Ad");
        List<Card> communityCards = cards("Ac", "Js", "7h", "4c", "3d");
        HandRanking result = handEvaluator.evaluate(playerHand, communityCards);
        assertEquals(HandRanking.HandType.THREE_OF_A_KIND, result.getHandType());
        assertEquals(List.of(Value.ACE), result.getRankValues());
        assertEquals(List.of(Value.JACK, Value.SEVEN), result.getKickerValues());
    }

    @Test
    void testStraight() {
        List<Card> playerHand = cards("Ah", "2d");
        List<Card> communityCards = cards("3c", "4s", "5h", "Jc", "Qd");
        HandRanking result = handEvaluator.evaluate(playerHand, communityCards);
        assertEquals(HandRanking.HandType.STRAIGHT, result.getHandType());
        assertEquals(List.of(Value.FIVE), result.getRankValues());
    }

    @Test
    void testFlush() {
        List<Card> playerHand = cards("Ah", "2h");
        List<Card> communityCards = cards("Jh", "9h", "7h", "4c", "3d");
        HandRanking result = handEvaluator.evaluate(playerHand, communityCards);
        assertEquals(HandRanking.HandType.FLUSH, result.getHandType());
        assertEquals(List.of(Value.ACE, Value.JACK, Value.NINE, Value.SEVEN, Value.TWO), result.getKickerValues());
    }

    @Test
    void testFullHouse() {
        List<Card> playerHand = cards("Ah", "Ad");
        List<Card> communityCards = cards("Ac", "Js", "Jd", "4c", "3d");
        HandRanking result = handEvaluator.evaluate(playerHand, communityCards);
        assertEquals(HandRanking.HandType.FULL_HOUSE, result.getHandType());
        assertEquals(List.of(Value.ACE, Value.JACK), result.getRankValues());
    }

    @Test
    void testFourOfAKind() {
        List<Card> playerHand = cards("Ah", "Ad");
        List<Card> communityCards = cards("Ac", "As", "Jd", "4c", "3d");
        HandRanking result = handEvaluator.evaluate(playerHand, communityCards);
        assertEquals(HandRanking.HandType.FOUR_OF_A_KIND, result.getHandType());
        assertEquals(List.of(Value.ACE), result.getRankValues());
        assertEquals(List.of(Value.JACK), result.getKickerValues());
    }

    @Test
    void testStraightFlush() {
        List<Card> playerHand = cards("8h", "9h");
        List<Card> communityCards = cards("Th", "Jh", "Qh", "4c", "3d");
        HandRanking result = handEvaluator.evaluate(playerHand, communityCards);
        assertEquals(HandRanking.HandType.STRAIGHT_FLUSH, result.getHandType());
        assertEquals(List.of(Value.QUEEN), result.getRankValues());
    }

    @Test
    void testRoyalFlush() {
        List<Card> playerHand = cards("Th", "Jh");
        List<Card> communityCards = cards("Qh", "Kh", "Ah", "4c", "3d");
        HandRanking result = handEvaluator.evaluate(playerHand, communityCards);
        assertEquals(HandRanking.HandType.ROYAL_FLUSH, result.getHandType());
        assertEquals(List.of(Value.ACE), result.getRankValues());
    }

    @Test
    void testKickerComparison_HighCard() {
        List<Card> hand1_player = cards("Ah", "2d");
        List<Card> hand1_community = cards("Jc", "9s", "7h", "4c", "3d");
        HandRanking result1 = handEvaluator.evaluate(hand1_player, hand1_community);

        List<Card> hand2_player = cards("Kh", "2d");
        List<Card> hand2_community = cards("Jc", "9s", "7h", "4c", "3d");
        HandRanking result2 = handEvaluator.evaluate(hand2_player, hand2_community);

        assertTrue(result1.compareTo(result2) > 0);
    }

    @Test
    void testKickerComparison_Pair() {
        List<Card> hand1_player = cards("Ah", "Ad");
        List<Card> hand1_community = cards("Jc", "9s", "7h", "4c", "2d");
        HandRanking result1 = handEvaluator.evaluate(hand1_player, hand1_community);

        List<Card> hand2_player = cards("Ah", "Ad");
        List<Card> hand2_community = cards("Jc", "9s", "6h", "4c", "2d");
        HandRanking result2 = handEvaluator.evaluate(hand2_player, hand2_community);

        assertTrue(result1.compareTo(result2) > 0);
    }
}
