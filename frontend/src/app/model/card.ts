export interface Card {
	[x: string]: any;
	suit: 'HEARTS' | 'CLUBS' | 'DIAMONDS' | 'SPADES';
	value: 'TWO' | 'THREE' | 'FOUR' | 'FIVE' | 'SIX' | 'SEVEN' | 'EIGHT' | 'NINE' | 'TEN' | 'JACK' | 'QUEEN' | 'KING' | 'ACE';
}