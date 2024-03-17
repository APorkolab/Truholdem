import { Card } from "./card";
import { Player } from "./player";

export interface Game {
	pot: any;
	communityCards: Card[];
	players: Player[];
	phase: 'PRE_FLOP' | 'FLOP' | 'TURN' | 'RIVER';
	currentPot: number;
}