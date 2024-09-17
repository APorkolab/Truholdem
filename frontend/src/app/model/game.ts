import { Card } from "./card";
import { Player } from "./player";

export interface Game {
	currentPot: number;
	players: Player[];
	communityCards: Card[];
	phase: string;
	currentBet: number;
	playerActions: { [playerId: string]: boolean };
	// Új mező hozzáadása
	playersWhoHaveNotActed?: Player[];  // Opcionális mező, mivel néha hiányozhat
}

