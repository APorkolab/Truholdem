import { Card } from "./card";
import { Player } from "./player";

export interface Game {
	currentPot: number;
	players: Player[];
	communityCards: Card[];
	phase: string;
	currentBet: number;
	playerActions: { [playerId: string]: boolean };
	playersWhoHaveNotActed?: Player[];  // Opcionális mező, mivel néha hiányozhat
}

