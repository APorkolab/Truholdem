import { Card } from "./card";
import { Player } from "./player";

export interface Game {
	currentPot: number;
	players: Player[];
	communityCards: Card[];
	phase: 'PRE_FLOP' | 'FLOP' | 'TURN' | 'RIVER';
}
